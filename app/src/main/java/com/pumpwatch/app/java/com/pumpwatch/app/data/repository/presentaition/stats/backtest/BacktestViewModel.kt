
package com.pumpwatch.app.presentation.backtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.data.repository.MarketRepository
import com.pumpwatch.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class BacktestUiState {
    object Idle : BacktestUiState()
    object Running : BacktestUiState()
    data class Result(
        val coinSymbol: String,
        val result: BacktestResult,
        val insights: List<AdvisorInsight>,
        val shareText: String
    ) : BacktestUiState()
    data class Error(val message: String) : BacktestUiState()
}

data class BacktestInputs(
    val coinId: String = "",
    val coinSymbol: String = "",
    val days: Int = 30,
    val mode: MarketMode = MarketMode.SPOT,
    val pumpSettings: PumpSettings = MarketMode.SPOT.defaultPumpSettings(),
    val tradeSettings: TradeSettings = MarketMode.SPOT.defaultTradeSettings()
)

class BacktestViewModel(
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _inputs = MutableStateFlow(BacktestInputs())
    val inputs: StateFlow<BacktestInputs> = _inputs.asStateFlow()

    private val _uiState = MutableStateFlow<BacktestUiState>(BacktestUiState.Idle)
    val uiState: StateFlow<BacktestUiState> = _uiState.asStateFlow()

    private val _availableCoins = MutableStateFlow<List<CoinTrack>>(emptyList())
    val availableCoins: StateFlow<List<CoinTrack>> = _availableCoins.asStateFlow()

    init {
        loadAvailableCoins()
    }

    private fun loadAvailableCoins() {
        viewModelScope.launch {
            marketRepository.refresh(1, 500)
            _availableCoins.value = marketRepository.coins.value
        }
    }

    fun updateInputs(update: BacktestInputs.() -> BacktestInputs) {
        _inputs.value = _inputs.value.update()
    }

    fun runBacktest() {
        val current = _inputs.value
        if (current.coinId.isBlank()) {
            _uiState.value = BacktestUiState.Error("لطفاً یه ارز انتخاب کن")
            return
        }

        viewModelScope.launch {
            _uiState.value = BacktestUiState.Running
            try {
                val history = withContext(Dispatchers.IO) {
                    marketRepository.fetchHistoricalSnapshots(current.coinId, current.days)
                }

                if (history.isEmpty()) {
                    _uiState.value = BacktestUiState.Error("داده تاریخی برای این ارز پیدا نشد")
                    return@launch
                }

                val run = Backtester.run(
                    coinId = current.coinId,
                    symbol = current.coinSymbol,
                    name = current.coinSymbol,
                    fullHistory = history,
                    pumpSettings = current.pumpSettings,
                    tradeSettings = current.tradeSettings,
                    mode = current.mode
                )

                val result = Backtester.summarize(run, current.tradeSettings.initialStake)
                val insights = PerformanceAdvisor.analyze(result.trades)
                val shareText = ShareFormatter.format(current.coinSymbol, result, insights)

                _uiState.value = BacktestUiState.Result(
                    coinSymbol = current.coinSymbol,
                    result = result,
                    insights = insights,
                    shareText = shareText
                )
            } catch (e: Exception) {
                _uiState.value = BacktestUiState.Error(e.message ?: "خطای ناشناخته")
            }
        }
    }

    fun reset() {
        _uiState.value = BacktestUiState.Idle
    }
}
