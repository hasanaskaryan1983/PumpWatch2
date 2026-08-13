package com.pumpwatch.app.domain

enum class TradeStatus { OPEN, CLOSED }

enum class ExitReason {
    TRAILING_STOP,      // price pulled back to the trailing stop level
    REVERSAL_DETECTED,  // quick drop + consecutive down-ticks fired before trailing stop was hit
    HARD_STOP_LOSS,      // initial fixed stop loss (protects against an immediate reversal right after entry)
    MANUAL
}

/**
 * A fully simulated ("paper") position — no real order is ever sent anywhere.
 * highestPriceSinceEntry drives the trailing stop: it only ever moves up,
 * which is what lets both the effective stop-loss and the "locked-in" profit
 * ratchet upward with price, per the user's "شناور بالا ببره" requirement.
 */
data class SimulatedTrade(
    val id: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val entryPrice: Double,
    val entryTimeMillis: Long,
    val highestPriceSinceEntry: Double,
    val trailingStopPrice: Double,
    val hardStopLossPrice: Double,
    val status: TradeStatus,
    val exitPrice: Double? = null,
    val exitTimeMillis: Long? = null,
    val exitReason: ExitReason? = null
) {
    fun pnlPercentAt(currentPrice: Double): Double =
        (currentPrice - entryPrice) / entryPrice * 100.0

    val closedPnlPercent: Double?
        get() = exitPrice?.let { (it - entryPrice) / entryPrice * 100.0 }
}
