package com.pumpwatch.app.domain

data class TradeSettings(
    val enabled: Boolean = false,
    val maxConcurrentTrades: Int = 3,
    val initialStake: Double = 100.0,
    val feePercent: Double = 0.1,
    val trailingStopPercent: Double = 3.0,
    val hardStopLossPercent: Double = 8.0,
    val takeProfitPercent: Double = 15.0,
    val reversalDropPercent: Double = 4.0,
    val reversalDownTicks: Int = 2,
    val maxHoldTimeMinutes: Int = 120,
    val allowShort: Boolean = false
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (trailingStopPercent <= 0) errors.add("Trailing stop must be > 0")
        if (hardStopLossPercent <= 0) errors.add("Hard stop must be > 0")
        if (trailingStopPercent >= hardStopLossPercent)
            errors.add("Trailing must be < hard stop")
        if (reversalDropPercent <= 0) errors.add("Reversal drop must be > 0")
        if (reversalDownTicks < 1) errors.add("Reversal ticks must be >= 1")
        if (initialStake <= 0) errors.add("Stake must be > 0")
        if (feePercent < 0) errors.add("Fee cannot be negative")
        return errors
    }
}
