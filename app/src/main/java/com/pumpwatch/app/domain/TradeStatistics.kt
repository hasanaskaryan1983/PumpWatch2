
package com.pumpwatch.app.domain

data class TradeStatistics(
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRatePercent: Double,
    val totalProfit: Double,
    val averageWinPercent: Double,
    val averageLossPercent: Double,
    val largestWin: Double,
    val largestLoss: Double,
    val profitFactor: Double
)
