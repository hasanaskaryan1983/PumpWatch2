package com.pumpwatch.app.domain

enum class TradeStatus { OPEN, CLOSED }

enum class ExitReason {
    TRAILING_STOP,
    TAKE_PROFIT,
    REVERSAL_DETECTED,
    HARD_STOP_LOSS,
    TIMEOUT,
    MANUAL
}

enum class TradeDirection { LONG, SHORT }

/**
 * A fully simulated ("paper") position — no real order is ever sent.
 * Supports both LONG and SHORT directions.
 */
data class SimulatedTrade(
    val id: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val direction: TradeDirection = TradeDirection.LONG,
    val entryPrice: Double,
    val entryTimeMillis: Long,
    val highestPriceSinceEntry: Double,
    val lowestPriceSinceEntry: Double,
    val trailingStopPrice: Double,
    val hardStopLossPrice: Double,
    val takeProfitPrice: Double?,
    val status: TradeStatus,
    val feePercent: Double = 0.0,
    val stakeAmount: Double = 0.0,
    val exitPrice: Double? = null,
    val exitTimeMillis: Long? = null,
    val exitReason: ExitReason? = null
) {
    /** Raw price change, direction-aware, no fees. */
    fun rawPnlPercentAt(currentPrice: Double): Double {
        val raw = if (direction == TradeDirection.LONG)
            (currentPrice - entryPrice) / entryPrice
        else
            (entryPrice - currentPrice) / entryPrice
        return raw * 100.0
    }

    /** Net PnL: raw change minus entry + exit fees. */
    fun pnlPercentAt(currentPrice: Double): Double =
        rawPnlPercentAt(currentPrice) - feePercent * 2.0

    /** Net profit in stake currency for a closed trade. */
    val closedProfitAmount: Double?
        get() = exitPrice?.let { stakeAmount * pnlPercentAt(it) / 100.0 }

    val closedPnlPercent: Double?
        get() = exitPrice?.let { pnlPercentAt(it) }
}
