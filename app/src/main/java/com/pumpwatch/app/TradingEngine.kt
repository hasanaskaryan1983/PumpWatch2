package com.pumpwatch.app.domain

sealed class TradeEvent {
    data class Opened(val trade: SimulatedTrade) : TradeEvent()
    data class Closed(val trade: SimulatedTrade) : TradeEvent()
}

/**
 * Pure decision logic for paper trades. Holds no state itself — the caller
 * (TradeRepository) owns the list of trades and persists the result of each
 * call. Kept side-effect free so the entry/exit rules are easy to reason
 * about and unit-test independently of Android/coroutines.
 */
object TradingEngine {

    /**
     * Called once per coin per poll cycle.
     *
     * @param openTrade the existing open trade for this coin, if any
     * @param isPumpSignal whether PumpDetector currently flags this coin
     * @return an updated trade (open or freshly closed) and an event describing what happened, or null if nothing changed
     */
    fun tick(
        coin: CoinTrack,
        openTrade: SimulatedTrade?,
        isPumpSignal: Boolean,
        settings: TradeSettings
    ): Pair<SimulatedTrade?, TradeEvent?> {
        val currentPrice = coin.history.lastOrNull()?.price ?: return openTrade to null

        if (openTrade == null) {
            if (!settings.enabled || !isPumpSignal) return null to null
            val trade = openNewTrade(coin, currentPrice, settings)
            return trade to TradeEvent.Opened(trade)
        }

        // Update the running peak; trailing stop and locked-in profit only ever move up.
        val newHighest = maxOf(openTrade.highestPriceSinceEntry, currentPrice)
        val newTrailingStop = newHighest * (1 - settings.trailingStopPercent / 100.0)
        val updated = openTrade.copy(
            highestPriceSinceEntry = newHighest,
            trailingStopPrice = maxOf(openTrade.trailingStopPrice, newTrailingStop)
        )

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

    private fun openNewTrade(coin: CoinTrack, currentPrice: Double, settings: TradeSettings): SimulatedTrade {
        return SimulatedTrade(
            id = "${coin.id}-${System.currentTimeMillis()}",
            coinId = coin.id,
            symbol = coin.symbol,
            name = coin.name,
            entryPrice = currentPrice,
            entryTimeMillis = System.currentTimeMillis(),
            highestPriceSinceEntry = currentPrice,
            trailingStopPrice = currentPrice * (1 - settings.trailingStopPercent / 100.0),
            hardStopLossPrice = currentPrice * (1 - settings.hardStopLossPercent / 100.0),
            status = TradeStatus.OPEN
        )
    }

    private fun decideExit(
        coin: CoinTrack,
        trade: SimulatedTrade,
        currentPrice: Double,
        settings: TradeSettings
    ): ExitReason? {
        if (currentPrice <= trade.hardStopLossPrice && trade.highestPriceSinceEntry == trade.entryPrice) {
            // Never moved in our favor at all before reversing — treat as the hard safety stop.
            return ExitReason.HARD_STOP_LOSS
        }

        if (currentPrice <= trade.trailingStopPrice) {
            return ExitReason.TRAILING_STOP
        }

        // Early reversal check, independent of the trailing stop: a sharp drop from the
        // recent peak plus consecutive down-ticks can exit before the trailing stop is reached.
        val dropFromPeakPercent = (trade.highestPriceSinceEntry - currentPrice) / trade.highestPriceSinceEntry * 100.0
        val downTicks = countConsecutiveDownTicks(coin.history.map { it.price })

        if (dropFromPeakPercent >= settings.reversalDropPercent && downTicks >= settings.reversalDownTicks) {
            return ExitReason.REVERSAL_DETECTED
        }

        return null
    }

    private fun countConsecutiveDownTicks(prices: List<Double>): Int {
        var count = 0
        for (i in prices.size - 1 downTo 1) {
            if (prices[i] < prices[i - 1]) count++ else break
        }
        return count
    }
}
