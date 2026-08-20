package com.pumpwatch.app.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Multi-signal confluence engine for detecting momentum anomalies.
 *
 * LONG signals (pumps):
 *   - price acceleration
 *   - volume acceleration (median-based, robust to outliers)
 *   - consecutive up-ticks
 *
 * SHORT signals (futures only):
 *   - dump: mirror of pump (crash + volume + down-ticks)
 *   - dead pump: coin pumped hard, now reversing from the top
 *
 * All filters (BTC trend, cooldown, liquidity) applied BEFORE signals fire.
 */
object PumpDetector {

    /** Minimum 24h volume in USD to consider a coin. */
    private const val MIN_VOLUME_USD_24H = 100_000.0

    // ---------- LONG (pump) ----------

    fun evaluate(
        history: List<PriceSnapshot>,
        settings: PumpSettings,
        btcTrend: BtcTrend? = null,
        lastSignalTimeMillis: Long? = null,
        coinVolumeUsd24h: Double = 0.0
    ): PumpSignal? {
        if (history.size < 3) return null
        if (coinVolumeUsd24h < MIN_VOLUME_USD_24H) return null

        // Cooldown
        if (lastSignalTimeMillis != null) {
            val cooldownMs = settings.cooldownMinutes * 60_000L
            if (System.currentTimeMillis() - lastSignalTimeMillis < cooldownMs) return null
        }

        // BTC bearish filter
        if (btcTrend?.isBearish == true) return null

        val windowStart = history.last().timestampMillis - settings.windowMinutes * 60_000L
        val windowed = history.filter { it.timestampMillis >= windowStart }
        if (windowed.size < 3) return null

        val first = windowed.first()
        val last = windowed.last()

        // 1) Price change
        val priceChangePercent = if (first.price > 0)
            (last.price - first.price) / first.price * 100.0 else 0.0

        // 2) Volume acceleration (median-based)
        val volumeDeltas = windowed.zipWithNext { a, b -> b.totalVolume - a.totalVolume }
        val historicalDeltas = if (volumeDeltas.size > 1) volumeDeltas.dropLast(1) else emptyList()
        val avgDelta = median(historicalDeltas)
        val latestDelta = volumeDeltas.lastOrNull() ?: 0.0
        val volumeGrowthPercent = when {
            avgDelta > 0 -> (latestDelta - avgDelta) / avgDelta * 100.0
            latestDelta > 0 -> 100.0
            else -> 0.0
        }

        // 3) Consecutive up-ticks
        var upTicks = 0
        for (i in windowed.size - 1 downTo 1) {
            if (windowed[i].price > windowed[i - 1].price) upTicks++ else break
        }

        val priceFired = priceChangePercent >= settings.priceChangeThresholdPercent
        val volumeFired = volumeGrowthPercent >= settings.volumeGrowthThresholdPercent
        val momentumFired = upTicks >= settings.minConsecutiveUpTicks

        val firedCount = listOf(priceFired, volumeFired, momentumFired).count { it }
        val isPump = firedCount >= settings.minSignalsRequired

        val score = computeScore(
            priceChangePercent, volumeGrowthPercent, upTicks, settings
        )

        return PumpSignal(
            coinId = "", // caller fills in
            priceChangePercent = priceChangePercent,
            volumeGrowthPercent = volumeGrowthPercent,
            consecutiveUpTicks = upTicks,
            firedSignalCount = firedCount,
            isPump = isPump,
            score = score
        )
    }

    // ---------- SHORT (dump / dead pump) ----------

    /**
     * Returns true if a SHORT entry signal is present.
     * Two setups:
     *   1) DUMP: crash + volume spike + down-ticks (mirror of pump)
     *   2) DEAD PUMP: coin pumped hard, now reversing from the top
     */
    fun evaluateShort(
        history: List<PriceSnapshot>,
        settings: PumpSettings,
        reversalDropPercent: Double,
        reversalDownTicks: Int
    ): Boolean {
        if (history.size < 3) return false

        val windowStart = history.last().timestampMillis - settings.windowMinutes * 60_000L
        val w = history.filter { it.timestampMillis >= windowStart }
        if (w.size < 3) return false

        val first = w.first()
        val last = w.last()

        val priceChange = if (first.price > 0)
            (last.price - first.price) / first.price * 100.0 else 0.0

        val deltas = w.zipWithNext { a, b -> b.totalVolume - a.totalVolume }
        val hist = if (deltas.size > 1) deltas.dropLast(1) else emptyList()
        val medianDelta = median(hist)
        val latestDelta = deltas.lastOrNull() ?: 0.0
        val volGrowth = when {
            medianDelta > 0 -> (latestDelta - medianDelta) / medianDelta * 100.0
            latestDelta > 0 -> 100.0
            else -> 0.0
        }

        var downTicks = 0
        for (i in w.size - 1 downTo 1) {
            if (w[i].price < w[i - 1].price) downTicks++ else break
        }

        // Setup 1: pure dump
        if (priceChange <= -settings.priceChangeThresholdPercent &&
            volGrowth >= settings.volumeGrowthThresholdPercent &&
            downTicks >= settings.minConsecutiveUpTicks) return true

        // Setup 2: dead pump (rose hard, now reversing)
        val peak = w.maxOf { it.price }
        val rise = (peak - first.price) / first.price * 100.0
        val drop = (peak - last.price) / peak * 100.0
        if (rise >= settings.priceChangeThresholdPercent * 2 &&
            drop >= reversalDropPercent &&
            downTicks >= reversalDownTicks) return true

        return false
    }

    // ---------- helpers ----------

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        return if (sorted.size % 2 == 0)
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        else
            sorted[sorted.size / 2]
    }

    private fun computeScore(
        priceChangePercent: Double,
        volumeGrowthPercent: Double,
        upTicks: Int,
        settings: PumpSettings
    ): Int {
        val priceScore = min(100.0, max(0.0,
            priceChangePercent / settings.priceChangeThresholdPercent * 40))
        val volumeScore = min(100.0, max(0.0,
            volumeGrowthPercent / settings.volumeGrowthThresholdPercent * 40))
        val momentumScore = min(100.0, max(0.0,
            upTicks.toDouble() / settings.minConsecutiveUpTicks * 20))
        return (priceScore + volumeScore + momentumScore).roundToInt().coerceIn(0, 100)
    }
}
