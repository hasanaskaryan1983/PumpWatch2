package com.pumpwatch.app.presentation.stats

import com.pumpwatch.app.data.repository.TradeStatistics
import com.pumpwatch.app.domain.SimulatedTrade

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(
        val stats: TradeStatistics,
        val trades: List<SimulatedTrade>,
        val equityCurve: List<EquityPoint>
    ) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

data class EquityPoint(
    val tradeIndex: Int,
    val equity: Double,
    val timestampMillis: Long
)

fun buildEquityCurve(
    trades: List<SimulatedTrade>,
    initialCapital: Double = 1000.0
): List<EquityPoint> {
    if (trades.isEmpty()) return emptyList()

    var equity = initialCapital
    val points = mutableListOf(
        EquityPoint(0, initialCapital, trades.first().entryTimeMillis)
    )

    trades.sortedBy { it.exitTimeMillis }.forEachIndexed { index, trade ->
        equity += trade.closedProfitAmount ?: 0.0
        points.add(
            EquityPoint(
                tradeIndex = index + 1,
                equity = equity,
                timestampMillis = trade.exitTimeMillis ?: trade.entryTimeMillis
            )
        )
    }
    return points
}
