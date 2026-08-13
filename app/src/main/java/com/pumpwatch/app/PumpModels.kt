package com.pumpwatch.app.domain

/** One polled data point for a coin, kept in a short rolling history. */
data class PriceSnapshot(
    val timestampMillis: Long,
    val price: Double,
    val totalVolume: Double
)

/** Latest known state for a tracked coin, including its rolling history. */
data class CoinTrack(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val history: List<PriceSnapshot>,
    val change24hPercent: Double?
)

/** Result of running the multi-signal pump check on a coin. */
data class PumpSignal(
    val coinId: String,
    val priceChangePercent: Double,
    val volumeGrowthPercent: Double,
    val consecutiveUpTicks: Int,
    val firedSignalCount: Int,
    val isPump: Boolean,
    val score: Int // 0-100, rough composite strength for sorting/UI
)
