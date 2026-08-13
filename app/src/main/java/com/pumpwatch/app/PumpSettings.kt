package com.pumpwatch.app.domain

/**
 * User-configurable thresholds. All three signals are checked together;
 * a coin is flagged only when enough of them fire at once (see PumpDetector).
 */
data class PumpSettings(
    val pollIntervalMinutes: Int = 3,
    val priceChangeThresholdPercent: Double = 5.0,   // price move within the tracked window
    val volumeGrowthThresholdPercent: Double = 40.0, // volume acceleration vs recent baseline
    val minConsecutiveUpTicks: Int = 3,               // sustained upward momentum
    val windowMinutes: Int = 15,                      // how far back "recent" looks
    val minSignalsRequired: Int = 2,                  // how many of the 3 signals must fire
    val minMarketCapRank: Int = 300,                  // only scan coins ranked between min..max
    val maxMarketCapRank: Int = 600                   // by market cap (e.g. 300-600)
)
