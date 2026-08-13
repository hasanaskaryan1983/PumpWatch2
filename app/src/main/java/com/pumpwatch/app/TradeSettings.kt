package com.pumpwatch.app.domain

data class TradeSettings(
    val enabled: Boolean = false,                 // simulated trading is off by default; user opts in
    val maxConcurrentTrades: Int = 3,
    val hardStopLossPercent: Double = 8.0,         // fixed safety-net stop right after entry
    val trailingStopPercent: Double = 6.0,         // distance kept below the highest price seen
    val reversalDropPercent: Double = 4.0,         // quick drop from peak that alone signals reversal
    val reversalDownTicks: Int = 2                 // consecutive down-ticks that alone signal reversal
)
