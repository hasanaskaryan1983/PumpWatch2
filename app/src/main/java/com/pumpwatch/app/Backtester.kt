package com.pumpwatch.app.domain

/** Aggregate stats for one or more coins run through the backtester. */
data class BacktestResult(
    val trades: List<SimulatedTrade>,
    val totalTrades: Int,
    val winCount: Int,
    val lossCount: Int,
    val winRatePercent: Double,
    val avgPnlPercent: Double,
    val bestTradePercent: Double,
    val worstTradePercent: Double,
    val stillOpenAtEndCount: Int
)

/**
 * Replays historical price/volume snapshots through the exact same
 * PumpDetector + TradingEngine logic used live, so results reflect the real
 * strategy rather than a separate approximation. Purely a simulation over
 * past data — it does not place or suggest any real order.
 *
 * Limitation worth surfacing to the user: CoinGecko's free historical
 * endpoint only gives fine-grained (~5 min) data for the last 1 day; longer
 * lookback windows come back as hourly candles, which coarsens short-window
 * (e.g. 15 minute) pump detection and makes the backtest an approximation.
 */
object Backtester {

    fun run(
        coinId: String,
        symbol: String,
        name: String,
        fullHistory: List<PriceSnapshot>,
        pumpSettings: PumpSettings,
        tradeSettings: TradeSettings
    ): List<SimulatedTrade> {
        if (fullHistory.size < 5) return emptyList()

        val closedTrades = mutableListOf<SimulatedTrade>()
        var openTrade: SimulatedTrade? = null
        val runningHistory = mutableListOf<PriceSnapshot>()

        for (snapshot in fullHistory) {
            runningHistory.add(snapshot)

            val coinSoFar = CoinTrack(
                id = coinId,
                symbol = symbol,
                name = name,
                imageUrl = null,
                history = runningHistory,
                change24hPercent = null
            )

            val signal = PumpDetector.evaluate(runningHistory, pumpSettings)
            val isPumping = signal?.isPump == true

            val (result, event) = TradingEngine.tick(
                coin = coinSoFar,
                openTrade = openTrade,
                isPumpSignal = isPumping,
                settings = tradeSettings.copy(enabled = true, maxConcurrentTrades = 1)
            )

            openTrade = result
            if (event is TradeEvent.Closed) {
                closedTrades.add(event.trade)
            }
        }

        return closedTrades
    }

    fun summarize(trades: List<SimulatedTrade>, openCount: Int): BacktestResult {
        val pnls = trades.mapNotNull { it.closedPnlPercent }
        val wins = pnls.count { it > 0 }
        val losses = pnls.count { it <= 0 }
        return BacktestResult(
            trades = trades,
            totalTrades = trades.size,
            winCount = wins,
            lossCount = losses,
            winRatePercent = if (trades.isNotEmpty()) wins.toDouble() / trades.size * 100.0 else 0.0,
            avgPnlPercent = if (pnls.isNotEmpty()) pnls.average() else 0.0,
            bestTradePercent = pnls.maxOrNull() ?: 0.0,
            worstTradePercent = pnls.minOrNull() ?: 0.0,
            stillOpenAtEndCount = openCount
        )
    }
}
