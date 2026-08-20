package com.pumpwatch.app.domain

/**
 * Aggregate stats for a backtest run.
 */
data class BacktestResult(
    val trades: List<SimulatedTrade>,
    val totalTrades: Int,
    val winCount: Int,
    val lossCount: Int,
    val winRatePercent: Double,
    val totalPnlAmount: Double,
    val profitFactor: Double,
    val maxDrawdownPercent: Double,
    val avgHoldTimeMinutes: Double,
    val exitReasonBreakdown: Map<ExitReason, Int>,
    val stillOpenAtEndCount: Int
)
