package com.pumpwatch.app.data.repository

import com.pumpwatch.app.data.local.TradeStore
import com.pumpwatch.app.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TradeRepository(private val tradeStore: TradeStore) {

    private val _openTrades = MutableStateFlow<List<SimulatedTrade>>(emptyList())
    val openTrades: StateFlow<List<SimulatedTrade>> = _openTrades.asStateFlow()

    private val _closedTrades = MutableStateFlow<List<SimulatedTrade>>(emptyList())
    val closedTrades: StateFlow<List<SimulatedTrade>> = _closedTrades.asStateFlow()

    suspend fun loadTrades() {
        tradeStore.allTrades.collect { trades ->
            _openTrades.value = trades.filter { it.status == TradeStatus.OPEN }
            _closedTrades.value = trades.filter { it.status == TradeStatus.CLOSED }
        }
    }

    fun processTick(
        coins: List<CoinTrack>,
        entrySignals: Map<String, TradeDirection>,
        settings: TradeSettings
    ): List<TradeEvent> {
        val events = mutableListOf<TradeEvent>()
        val updatedOpenTrades = mutableListOf<SimulatedTrade>()

        // Update existing trades
        _openTrades.value.forEach { openTrade ->
            val coin = coins.find { it.id == openTrade.coinId }
            if (coin != null) {
                val (updated, event) = TradingEngine.tick(
                    coin = coin,
                    openTrade = openTrade,
                    entrySignal = null,
                    settings = settings
                )

                updated?.let { updatedOpenTrades.add(it) }
                event?.let { events.add(it) }

                if (event is TradeEvent.Closed) {
                    _closedTrades.value = _closedTrades.value + event.trade
                }
            }
        }

        // Check for new entries
        coins.forEach { coin ->
            val signal = entrySignals[coin.id]
            if (signal != null) {
                val hasOpenTrade = updatedOpenTrades.any { it.coinId == coin.id }
                if (!hasOpenTrade) {
                    if (updatedOpenTrades.size < settings.maxConcurrentTrades) {
                        val (newTrade, event) = TradingEngine.tick(
                            coin = coin,
                            openTrade = null,
                            entrySignal = signal,
                            settings = settings
                        )

                        newTrade?.let { updatedOpenTrades.add(it) }
                        event?.let { events.add(it) }
                    }
                }
            }
        }

        _openTrades.value = updatedOpenTrades
        return events
    }

    suspend fun saveTrade(trade: SimulatedTrade) {
        tradeStore.addTrade(trade)
    }

    suspend fun updateTrade(trade: SimulatedTrade) {
        tradeStore.updateTrade(trade)
    }

    fun getStatistics(): TradeStatistics {
        val closed = _closedTrades.value
        val wins = closed.filter { (it.closedPnlPercent ?: 0.0) > 0 }
        val losses = closed.filter { (it.closedPnlPercent ?: 0.0) <= 0 }

        val grossProfit = wins.sumOf { it.closedProfitAmount ?: 0.0 }
        val grossLoss = kotlin.math.abs(losses.sumOf { it.closedProfitAmount ?: 0.0 })
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else Double.POSITIVE_INFINITY

        val totalPnl = closed.sumOf { it.closedProfitAmount ?: 0.0 }
        val winRate = if (closed.isNotEmpty()) wins.size * 100.0 / closed.size else 0.0

        val avgWin = wins.mapNotNull { it.closedPnlPercent }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgLoss = losses.mapNotNull { it.closedPnlPercent }.average().takeIf { !it.isNaN() } ?: 0.0

        val largestWin = wins.maxOfOrNull { it.closedProfitAmount ?: 0.0 } ?: 0.0
        val largestLoss = losses.minOfOrNull { it.closedProfitAmount ?: 0.0 } ?: 0.0

        return TradeStatistics(
            totalTrades = closed.size,
            winningTrades = wins.size,
            losingTrades = losses.size,
            winRatePercent = winRate,
            totalProfit = totalPnl,
            averageWinPercent = avgWin,
            averageLossPercent = avgLoss,
            largestWin = largestWin,
            largestLoss = largestLoss,
            profitFactor = profitFactor
        )
    }

    fun clear() {
        _openTrades.value = emptyList()
        _closedTrades.value = emptyList()
    }
}
