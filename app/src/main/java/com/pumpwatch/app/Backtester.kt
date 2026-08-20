package com.pumpwatch.app.domain

import kotlin.math.max
import kotlin.math.min

/**
 * Replays historical price/volume snapshots through the exact same
 * PumpDetector + TradingEngine logic used live.
 * Supports both SPOT (LONG only) and FUTURES (LONG + SHORT) modes.
 */
object Backtester {

    data class RunResult(
        val closedTrades: List<SimulatedTrade>,
        val stillOpenTrade: SimulatedTrade?
    )

    fun run(
        coinId: String,
        symbol: String,
        name: String,
        fullHistory: List<PriceSnapshot>,
        pumpSettings: PumpSettings,
        tradeSettings: TradeSettings,
        mode: MarketMode = MarketMode.SPOT
    ): RunResult {
        if (fullHistory.size < 5) return RunResult(emptyList(), null)

        val closedTrades = mutableListOf<SimulatedTrade>()
        var openTrade: SimulatedTrade? = null
        val runningHistory = mutableListOf<PriceSnapshot>()

        // Adaptive window: if data is hourly, a 5-min window makes no sense.
        val gaps = fullHistory.zipWithNext { a, b -> (b.timestampMillis - a.timestampMillis) / 60000.0 }
        val medianGap = if (gaps.isEmpty()) 5.0 else gaps.sorted()[gaps.size / 2]
        val adaptiveWindow = maxOf(pumpSettings.windowMinutes.toDouble(), medianGap * 4).toInt()
        val effectivePumpSettings = pumpSettings.copy(windowMinutes = adaptiveWindow)

        for (snapshot in fullHistory) {
            runningHistory.add(snapshot)
            if (runningHistory.size > 120) runningHistory.removeAt(0)

            val coinSoFar = CoinTrack(
                id = coinId, symbol = symbol, name = name,
                imageUrl = null, history = runningHistory,
                change24hPercent = null
            )

            // 1. Check LONG signal
            val longSignal = PumpDetector.evaluate(runningHistory, effectivePumpSettings)
            val isLong = longSignal?.isPump == true

            // 2. Check SHORT signal (only in FUTURES mode if allowed)
            val isShort = if (mode == MarketMode.FUTURES && tradeSettings.allowShort) {
                PumpDetector.evaluateShort(
                    runningHistory, effectivePumpSettings,
                    tradeSettings.reversalDropPercent, tradeSettings.reversalDownTicks
                )
            } else false

            val entrySignal = when {
                isLong -> TradeDirection.LONG
                isShort -> TradeDirection.SHORT
                else -> null
            }

            val (result, event) = TradingEngine.tick(
                coin = coinSoFar,
                openTrade = openTrade,
                entrySignal = entrySignal,
                settings = tradeSettings.copy(enabled = true, maxConcurrentTrades = 1)
            )

            openTrade = result
            if (event is TradeEvent.Closed) closedTrades.add(event.trade)
        }

        return RunResult(closedTrades, openTrade)
    }

    fun summarize(run: RunResult, stakeAmount: Double = 100.0): BacktestResult {
        val trades = run.closedTrades
        val wins = trades.filter { (it.closedPnlPercent ?: 0.0) > 0 }
        val losses = trades.filter { (it.closedPnlPercent ?: 0.0) <= 0 }

        val grossProfit = wins.sumOf { it.closedProfitAmount ?: 0.0 }
        val grossLoss = kotlin.math.abs(losses.sumOf { it.closedProfitAmount ?: 0.0 })
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else Double.POSITIVE_INFINITY

        val maxDrawdown = calculateMaxDrawdown(trades, stakeAmount)

        val avgHoldTime = trades.mapNotNull { t ->
            t.exitTimeMillis?.let { (it - t.entryTimeMillis) / 60000.0 }
        }.takeIf { it.isNotEmpty() }?.average() ?: 0.0

        val exitBreakdown = trades.groupingBy { it.exitReason!! }.eachCount()

        val winRate = if (trades.isNotEmpty()) wins.size.toDouble() / trades.size * 100.0 else 0.0
        val totalPnl = trades.sumOf { it.closedProfitAmount ?: 0.0 }

        return BacktestResult(
            trades = trades,
            totalTrades = trades.size,
            winCount = wins.size,
            lossCount = losses.size,
            winRatePercent = winRate,
            totalPnlAmount = totalPnl,
            profitFactor = profitFactor,
            maxDrawdownPercent = maxDrawdown,
            avgHoldTimeMinutes = avgHoldTime,
            exitReasonBreakdown = exitBreakdown,
            stillOpenAtEndCount = if (run.stillOpenTrade != null) 1 else 0
        )
    }

    private fun calculateMaxDrawdown(trades: List<SimulatedTrade>, initialCapital: Double): Double {
        var equity = initialCapital
        var peak = initialCapital
        var maxDrawdown = 0.0

        for (trade in trades.sortedBy { it.exitTimeMillis }) {
            equity += trade.closedProfitAmount ?: 0.0
            peak = maxOf(peak, equity)
            val drawdown = (peak - equity) / peak * 100.0
            maxDrawdown = maxOf(maxDrawdown, drawdown)
        }
        return maxDrawdown
    }
}
