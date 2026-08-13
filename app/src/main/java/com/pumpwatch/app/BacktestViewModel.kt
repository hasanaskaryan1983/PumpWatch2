package com.pumpwatch.app.ui.backtest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.data.repository.MarketRepository
import com.pumpwatch.app.data.repository.PumpSettingsStore
import com.pumpwatch.app.data.repository.TradeSettingsStore
import com.pumpwatch.app.domain.BacktestResult
import com.pumpwatch.app.domain.Backtester
import com.pumpwatch.app.domain.CoinTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BacktestUiState(
    val isRunning: Boolean = false,
    val progressText: String = "",
    val perCoinResults: Map<String, BacktestResult> = emptyMap(),
    val combined: BacktestResult? = null,
    val error: String? = null
)

class BacktestViewModel(application: Application) : AndroidViewModel(application) {

    private val pumpSettingsStore = PumpSettingsStore(application)
    private val tradeSettingsStore = TradeSettingsStore(application)

    val candidateCoins: StateFlow<List<CoinTrack>> = MarketRepository.coins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(BacktestUiState())
    val uiState: StateFlow<BacktestUiState> = _uiState

    fun run(selectedCoinIds: List<String>, days: Int) {
        if (selectedCoinIds.isEmpty()) {
            _uiState.value = BacktestUiState(error = "حداقل یک کوین را انتخاب کن")
            return
        }

        viewModelScope.launch {
            _uiState.value = BacktestUiState(isRunning = true, progressText = "در حال دریافت داده تاریخی…")

            val pumpSettings = pumpSettingsStore.settingsFlow.first()
            val tradeSettings = tradeSettingsStore.settingsFlow.first()
            val coinsById = candidateCoins.value.associateBy { it.id }

            val perCoin = mutableMapOf<String, BacktestResult>()
            val allTrades = mutableListOf<com.pumpwatch.app.domain.SimulatedTrade>()

            for ((index, coinId) in selectedCoinIds.withIndex()) {
                val meta = coinsById[coinId]
                _uiState.value = _uiState.value.copy(
                    progressText = "در حال پردازش ${meta?.name ?: coinId} (${index + 1}/${selectedCoinIds.size})"
                )
                try {
                    val history = MarketRepository.fetchHistoricalSnapshots(coinId, days)
                    val trades = Backtester.run(
                        coinId = coinId,
                        symbol = meta?.symbol ?: coinId,
                        name = meta?.name ?: coinId,
                        fullHistory = history,
                        pumpSettings = pumpSettings,
                        tradeSettings = tradeSettings
                    )
                    allTrades += trades
                    perCoin[coinId] = Backtester.summarize(trades, openCount = 0)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = "خطا در دریافت ${meta?.name ?: coinId}: ${e.message}")
                }
            }

            _uiState.value = BacktestUiState(
                isRunning = false,
                perCoinResults = perCoin,
                combined = Backtester.summarize(allTrades, openCount = 0),
                error = _uiState.value.error
            )
        }
    }
}
