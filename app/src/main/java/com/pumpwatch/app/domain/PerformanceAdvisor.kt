package com.pumpwatch.app.domain

enum class InsightType {
    INFO, WARNING, PROBLEM
}

data class AdvisorInsight(
    val type: InsightType,
    val title: String,
    val detail: String,
    val suggestion: String? = null
)

/**
 * Rule-based strategy coach.
 * Analyzes trade history and returns actionable insights.
 */
object PerformanceAdvisor {

    fun analyze(trades: List<SimulatedTrade>): List<AdvisorInsight> {
        if (trades.isEmpty()) return emptyList()

        val insights = mutableListOf<AdvisorInsight>()
        val closed = trades.filter { it.status == TradeStatus.CLOSED }
        if (closed.isEmpty()) return emptyList()

        val wins = closed.filter { (it.closedPnlPercent ?: 0.0) > 0 }
        val losses = closed.filter { (it.closedPnlPercent ?: 0.0) <= 0 }
        val winRate = wins.size.toDouble() / closed.size

        // 1. Win rate check
        if (winRate < 0.4 && closed.size >= 5) {
            insights.add(
                AdvisorInsight(
                    type = InsightType.PROBLEM,
                    title = "Win rate پایین",
                    detail = "فقط ${"%.0f".format(winRate * 100)}% معاملات سودده بودن.",
                    suggestion = "آستانه‌های ورود رو سخت‌گیرانه‌تر کن یا فیلتر BTC رو فعال کن."
                )
            )
        }

        // 2. Exit reason analysis
        val exitReasons = closed.groupingBy { it.exitReason }.eachCount()
        val hardStopCount = exitReasons[ExitReason.HARD_STOP_LOSS] ?: 0
        val hardStopRatio = hardStopCount.toDouble() / closed.size

        if (hardStopRatio > 0.4) {
            insights.add(
                AdvisorInsight(
                    type = InsightType.PROBLEM,
                    title = "خروج‌های Hard Stop زیاد",
                    detail = "${"%.0f".format(hardStopRatio * 100)}% معاملات با ضرر کامل بسته شدن.",
                    suggestion = "دیر وارد می‌شی. آستانه پامپ رو بالا ببر یا در بازار نزولی ترید نکن."
                )
            )
        }

        // 3. Profit factor check
        val grossProfit = wins.sumOf { it.closedProfitAmount ?: 0.0 }
        val grossLoss = kotlin.math.abs(losses.sumOf { it.closedProfitAmount ?: 0.0 })
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else Double.POSITIVE_INFINITY

        if (profitFactor in 1.0..1.5 && closed.size >= 5) {
            insights.add(
                AdvisorInsight(
                    type = InsightType.WARNING,
                    title = "Profit Factor لب مرز",
                    detail = "سود کلی فقط کمی از ضرر بیشتره (${ "%.2f".format(profitFactor) }).",
                    suggestion = "Take Profit رو کمی بازتر کن یا Trailing Stop رو تنگ‌تر کن."
                )
            )
        }

        // 4. Average win vs loss
        val avgWin = wins.mapNotNull { it.closedPnlPercent }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgLoss = losses.mapNotNull { it.closedPnlPercent }.average().takeIf { !it.isNaN() } ?: 0.0

        if (avgWin < kotlin.math.abs(avgLoss) * 1.5 && closed.size >= 5) {
            insights.add(
                AdvisorInsight(
                    type = InsightType.WARNING,
                    title = "سود متوسط کمتر از ضرر متوسط",
                    detail = "میانگین سود: ${"%.2f".format(avgWin)}%، میانگین ضرر: ${"%.2f".format(avgLoss)}%.",
                    suggestion = "بذار سودها بیشتر رشد کنن (Trailing Stop رو شل‌تر کن)."
                )
            )
        }

        // 5. Timeout check
        val timeoutCount = exitReasons[ExitReason.TIMEOUT] ?: 0
        if (timeoutCount > closed.size * 0.3) {
            insights.add(
                AdvisorInsight(
                    type = InsightType.INFO,
                    title = "معاملات راکد زیاد",
                    detail = "${timeoutCount} معامله به دلیل Timeout بسته شدن.",
                    suggestion = "آستانه‌های ورود رو سخت‌گیرانه‌تر کن تا فقط مومنتوم‌های قوی رو بگیری."
                )
            )
        }

        // 6. Healthy strategy
        if (insights.isEmpty()) {
            insights.add(
                AdvisorInsight(
                    type = InsightType.INFO,
                    title = "استراتژی سالم به نظر می‌رسه",
                    detail = "Win rate: ${"%.0f".format(winRate * 100)}%، Profit Factor: ${if (profitFactor.isInfinite()) "∞" else "%.2f".format(profitFactor)}."
                )
            )
        }

        return insights
    }
}
