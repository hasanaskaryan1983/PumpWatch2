package com.pumpwatch.app.data.repository

import com.pumpwatch.app.domain.CoinTrack
import com.pumpwatch.app.domain.TradeEvent
import com.pumpwatch.app.domain.TradeSettings
import com.pumpwatch.app.domain.SimulatedTrade
import com.pumpwatch.app.domain.TradeStatus
import com.pumpwatch.app.domain.TradingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory only, same rationale as MarketRepository: this is a test/paper-trading tool. */
object TradeRepository {

    private val openTradesByCoin = mutableMapOf<String, SimulatedTrade>()
    private val closedTrades = mutableListOf<SimulatedTrade>()

    private val _trades = MutableStateFlow<List<SimulatedTrade>>(emptyList())
    val trades: StateFlow<List<SimulatedTrade>> = _trades.asStateFlow()

    /**
     * Runs the trading engine for every currently tracked coin. [pumpingCoinIds] are the
     * coin ids PumpDetector flagged this cycle (candidates for a brand-new entry).
     */
    fun processTick(
        coins: List<CoinTrack>,
        pumpingCoinIds: Set<String>,
        settings: TradeSettings
    ): List<TradeEvent> {
        val events = mutableListOf<TradeEvent>()

        for (coin in coins) {
            val existing = openTradesByCoin[coin.id]

            if (existing == null && openTradesByCoin.size >= settings.maxConcurrentTrades) {
                continue // at the concurrent-trade cap; only manage existing positions
            }

            val (result, event) = TradingEngine.tick(
                coin = coin,
                openTrade = existing,
                isPumpSignal = coin.id in pumpingCoinIds,
                settings = settings
            )

            when {
                result == null -> Unit
                result.status == TradeStatus.OPEN -> openTradesByCoin[coin.id] = result
                result.status == TradeStatus.CLOSED -> {
                    openTradesByCoin.remove(coin.id)
                    closedTrades.add(0, result)
                }
            }
            event?.let { events.add(it) }
        }

        _trades.value = openTradesByCoin.values.sortedByDescending { it.entryTimeMillis } +
            closedTrades.take(50)

        return events
    }
}
