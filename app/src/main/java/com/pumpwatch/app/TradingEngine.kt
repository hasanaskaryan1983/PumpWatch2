package com.pumpwatch.app.domain

/**
 * Events emitted by the engine on state transitions.
 */
sealed class TradeEvent {
    data class Opened(val trade: SimulatedTrade) : TradeEvent()
    data class Closed(val trade: SimulatedTrade) : TradeEvent()
}

/**
 * Pure, stateless decision core for simulated trades.
 * Supports both LONG (spot) and SHORT (futures) directions.
 * Side-effect free — easy to unit test independently of Android.
 */
object TradingEngine {

    /**
     * Called once per coin per poll cycle.
     *
     * @param coin          current coin state with rolling price history
     * @param openTrade     existing open trade for this coin, or null
     * @param entrySignal   LONG for pump, SHORT for dump / dead-pump, null = no entry
     * @param settings      trade settings (stops, fees, stake)
     * @return (updated trade, event) — event is non-null on open/close
     */
    fun tick(
        coin: CoinTrack,
        openTrade: SimulatedTrade?,
        entrySignal: TradeDirection?,
        settings: TradeSettings
    ): Pair<SimulatedTrade?, TradeEvent?> {
        val currentPrice = coin.history.lastOrNull()?.price ?: return openTrade to null

        // ---------- ENTRY ----------
        if (openTrade == null) {
            if (!settings.enabled || entrySignal == null) return null to null
            if (entrySignal == TradeDirection.SHORT && !settings.allowShort) return null to null
            val trade = openNewTrade(coin, currentPrice, entrySignal, settings)
            return trade to TradeEvent.Opened(trade)
        }

        // ---------- UPDATE + EXIT CHECK ----------
        val updated = updateTrailing(openTrade, currentPrice, settings)
        val exitReason = decideExit(coin, updated, currentPrice, settings)

        return if (exitReason != null) {
            val closed = updated.copy(
                status = TradeStatus.CLOSED,
                exitPrice = currentPrice,
                exitTimeMillis = System.currentTimeMillis(),
                exitReason = exitReason
            )
            closed to TradeEvent.Closed(closed)
        } else {
            updated to null
        }
    }

    // ---------- entry ----------
    private fun openNewTrade(
        coin: CoinTrack,
        price: Double,
        dir: TradeDirection,
        s: TradeSettings
    ): SimulatedTrade {
        val long = dir == TradeDirection.LONG
        return SimulatedTrade(
            id = "${coin.id}-${System.currentTimeMillis()}",
            coinId = coin.id,
            symbol = coin.symbol,
            name = coin.name,
            direction = dir,
            entryPrice = price,
            entryTimeMillis = System.currentTimeMillis(),
            highestPriceSinceEntry = price,
            lowestPriceSinceEntry = price,
            trailingStopPrice = if (long) price * (1 - s.trailingStopPercent / 100.0)
                                else price * (1 + s.trailingStopPercent / 100.0),
            hardStopLossPrice = if (long) price * (1 - s.hardStopLossPercent / 100.0)
                                else price * (1 + s.hardStopLossPercent / 100.0),
            takeProfitPrice = if (long) price * (1 + s.takeProfitPercent / 100.0)
                              else price * (1 - s.takeProfitPercent / 100.0),
            status = TradeStatus.OPEN,
            feePercent = s.feePercent,
            stakeAmount = s.initialStake
        )
    }

    // ---------- trailing stop ratchets in the right direction ----------
    private fun updateTrailing(t: SimulatedTrade, price: Double, s: TradeSettings): SimulatedTrade =
        if (t.direction == TradeDirection.LONG) {
            val newHigh = maxOf(t.highestPriceSinceEntry, price)
            t.copy(
                highestPriceSinceEntry = newHigh,
                trailingStopPrice = maxOf(
                    t.trailingStopPrice,
                    newHigh * (1 - s.trailingStopPercent / 100.0)
                )
            )
        } else {
            val newLow = minOf(t.lowestPriceSinceEntry, price)
            t.copy(
                lowestPriceSinceEntry = newLow,
                trailingStopPrice = minOf(
                    t.trailingStopPrice,
                    newLow * (1 + s.trailingStopPercent / 100.0)
                )
            )
        }

    // ---------- exit rules, evaluated in priority order ----------
    private fun decideExit(
        coin: CoinTrack,
        t: SimulatedTrade,
        price: Double,
        s: TradeSettings
    ): ExitReason? {
        val long = t.direction == TradeDirection.LONG

        // signed: positive = loss, negative = gain
        val lossPct = if (long)
            (t.entryPrice - price) / t.entryPrice * 100.0
        else
            (price - t.entryPrice) / t.entryPrice * 100.0
        val gainPct = -lossPct

        // 1) hard stop — absolute floor
        if (lossPct >= s.hardStopLossPercent) return ExitReason.HARD_STOP_LOSS

        // 2) take profit
        if (s.takeProfitPercent > 0 && gainPct >= s.takeProfitPercent) return ExitReason.TAKE_PROFIT

        // 3) trailing stop
        val trailingHit = if (long) price <= t.trailingStopPrice else price >= t.trailingStopPrice
        if (trailingHit) return ExitReason.TRAILING_STOP

        // 4) reversal: adverse pullback from peak + consecutive adverse ticks
        val pullbackFromExtreme = if (long)
            (t.highestPriceSinceEntry - price) / t.highestPriceSinceEntry * 100.0
        else
            (price - t.lowestPriceSinceEntry) / t.lowestPriceSinceEntry * 100.0
        val adverseTicks = if (long) countTicks(coin, down = true) else countTicks(coin, down = false)
        if (pullbackFromExtreme >= s.reversalDropPercent && adverseTicks >= s.reversalDownTicks)
            return ExitReason.REVERSAL_DETECTED

        // 5) timeout — stale position
        val holdMin = (System.currentTimeMillis() - t.entryTimeMillis) / 60_000.0
        if (s.maxHoldTimeMinutes > 0 && holdMin >= s.maxHoldTimeMinutes)
            return ExitReason.TIMEOUT

        return null
    }

    private fun countTicks(coin: CoinTrack, down: Boolean): Int {
        val prices = coin.history.map { it.price }
        var n = 0
        for (i in prices.size - 1 downTo 1) {
            val d = prices[i] - prices[i - 1]
            if ((down && d < 0) || (!down && d > 0)) n++ else break
        }
        return n
    }
}
