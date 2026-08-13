package com.pumpwatch.app.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Combines three independent signals computed purely from locally-stored
 * poll history (no exchange orderbook needed):
 *
 *  1. Price change %      -> price now vs. price at the start of the window
 *  2. Volume growth %     -> latest per-poll volume delta vs. the average
 *                             per-poll delta over the window (acceleration)
 *  3. Consecutive up-ticks -> how many polls in a row moved price upward
 *
 * A coin is flagged as a pump only when at least [PumpSettings.minSignalsRequired]
 * of these fire past their thresholds, which avoids single-metric false positives
 * (e.g. one noisy volume spike alone won't trigger an alert).
 */
object PumpDetector {

    fun evaluate(history: List<PriceSnapshot>, settings: PumpSettings): PumpSignal? {
        if (history.size < 3) return null

        val windowStart = history.last().timestampMillis - settings.windowMinutes * 60_000L
        val windowed = history.filter { it.timestampMillis >= windowStart }
        if (windowed.size < 3) return null

        val first = windowed.first()
        val last = windowed.last()

        // 1) Price change over the window
        val priceChangePercent = if (first.price > 0) {
            (last.price - first.price) / first.price * 100.0
        } else 0.0

        // 2) Volume acceleration: latest delta vs. average delta
        val volumeDeltas = windowed.zipWithNext { a, b -> b.totalVolume - a.totalVolume }
        val avgDelta = volumeDeltas.dropLast(1).ifEmpty { volumeDeltas }.average()
        val latestDelta = volumeDeltas.lastOrNull() ?: 0.0
        val volumeGrowthPercent = when {
            avgDelta > 0 -> (latestDelta - avgDelta) / avgDelta * 100.0
            latestDelta > 0 -> 100.0
            else -> 0.0
        }

        // 3) Consecutive upward ticks (most recent streak)
        var upTicks = 0
        for (i in windowed.size - 1 downTo 1) {
            if (windowed[i].price > windowed[i - 1].price) upTicks++ else break
        }

        val priceFired = priceChangePercent >= settings.priceChangeThresholdPercent
        val volumeFired = volumeGrowthPercent >= settings.volumeGrowthThresholdPercent
        val momentumFired = upTicks >= settings.minConsecutiveUpTicks

        val firedCount = listOf(priceFired, volumeFired, momentumFired).count { it }
        val isPump = firedCount >= settings.minSignalsRequired

        val score = computeScore(priceChangePercent, volumeGrowthPercent, upTicks, settings)

        return PumpSignal(
            coinId = "", // filled in by the caller, which knows the coin id
            priceChangePercent = priceChangePercent,
            volumeGrowthPercent = volumeGrowthPercent,
            consecutiveUpTicks = upTicks,
            firedSignalCount = firedCount,
            isPump = isPump,
            score = score
        )
    }

    private fun computeScore(
        priceChangePercent: Double,
        volumeGrowthPercent: Double,
        upTicks: Int,
        settings: PumpSettings
    ): Int {
        val priceScore = min(100.0, max(0.0, priceChangePercent / settings.priceChangeThresholdPercent * 40))
        val volumeScore = min(100.0, max(0.0, volumeGrowthPercent / settings.volumeGrowthThresholdPercent * 40))
        val momentumScore = min(100.0, max(0.0, upTicks.toDouble() / settings.minConsecutiveUpTicks * 20))
        return (priceScore + volumeScore + momentumScore).roundToInt().coerceIn(0, 100)
    }
}
