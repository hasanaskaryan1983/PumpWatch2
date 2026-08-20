package com.pumpwatch.app.domain

/** One polled data point for a coin. */
data class PriceSnapshot(
    val timestampMillis: Long,
    val price: Double,
    val totalVolume: Double
)

/** Latest known state for a tracked coin. */
data class CoinTrack(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val history: List<PriceSnapshot>,
    val change24hPercent: Double?,
    val marketCapRank: Int? = null
)

/** Result of running the multi-signal pump check. */
data class PumpSignal(
    val coinId: String,
    val priceChangePercent: Double,
    val volumeGrowthPercent: Double,
    val consecutiveUpTicks: Int,
    val firedSignalCount: Int,
    val isPump: Boolean,
    val score: Int
)

/** BTC trend for market-wide filter. */
data class BtcTrend(
    val priceChangePercent: Double,
    val isBearish: Boolean
)

/** Pump detection configuration. */
data class PumpSettings(
    val enabled: Boolean = true,
    val minMarketCapRank: Int = 302,
    val maxMarketCapRank: Int = 495,
    val windowMinutes: Int = 5,
    val priceChangeThresholdPercent: Double = 5.0,
    val volumeGrowthThresholdPercent: Double = 29.0,
    val minConsecutiveUpTicks: Int = 3,
    val minSignalsRequired: Int = 3,
    val cooldownMinutes: Int = 15
)

/** Market mode selector — drives universe and strategy. */
enum class MarketMode(val label: String, val emoji: String) {
    SPOT("Spot", "🟠"),
    FUTURES("Futures", "🔵");

    val rankRange: IntRange get() = when (this) {
        SPOT -> 302..495
        FUTURES -> 1..100
    }

    fun defaultPumpSettings(): PumpSettings = when (this) {
        SPOT -> PumpSettings(
            minMarketCapRank = 302, maxMarketCapRank = 495,
            windowMinutes = 5,
            priceChangeThresholdPercent = 5.0,
            volumeGrowthThresholdPercent = 29.0,
            minConsecutiveUpTicks = 3,
            minSignalsRequired = 3,
            cooldownMinutes = 15
        )
        FUTURES -> PumpSettings(
            minMarketCapRank = 1, maxMarketCapRank = 100,
            windowMinutes = 5,
            priceChangeThresholdPercent = 2.0,
            volumeGrowthThresholdPercent = 20.0,
            minConsecutiveUpTicks = 3,
            minSignalsRequired = 3,
            cooldownMinutes = 10
        )
    }

    fun defaultTradeSettings(): TradeSettings = when (this) {
        SPOT -> TradeSettings(
            maxConcurrentTrades = 3,
            initialStake = 100.0,
            feePercent = 0.1,
            trailingStopPercent = 3.0,
            hardStopLossPercent = 8.0,
            takeProfitPercent = 15.0,
            reversalDropPercent = 4.0,
            reversalDownTicks = 2,
            maxHoldTimeMinutes = 120,
            allowShort = false
        )
        FUTURES -> TradeSettings(
            maxConcurrentTrades = 5,
            initialStake = 100.0,
            feePercent = 0.05,
            trailingStopPercent = 1.5,
            hardStopLossPercent = 4.0,
            takeProfitPercent = 6.0,
            reversalDropPercent = 2.0,
            reversalDownTicks = 2,
            maxHoldTimeMinutes = 60,
            allowShort = true
        )
    }
}
