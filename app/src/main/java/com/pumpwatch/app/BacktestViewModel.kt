package com.pumpwatch.app.presentation.backtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BacktestInputs(
    val coinId: String = "",
    val coinSymbol: String = "",
    val mode: MarketMode = MarketMode.SPOT,
    val days: Int = 30
)

sealed class BacktestUiState {
    object Idle : BacktestUiState()
    object Running : BacktestUiState()
    data class Result(
        val result: BacktestResult,
        val insights: List<Insight> = emptyList()
    ) : BacktestUiState()
    data class Error(val message: String) : BacktestUiState()
}

class BacktestViewModel(
    private val backtester: Backtester,
    private val coinRepository: CoinRepository
) : ViewModel() {

    private val _inputs = MutableStateFlow(BacktestInputs())
    val inputs: StateFlow<BacktestInputs> = _inputs.asStateFlow()

    private val _uiState = MutableStateFlow<BacktestUiState>(BacktestUiState.Idle)
    val uiState: StateFlow<BacktestUiState> = _uiState.asStateFlow()

    val availableCoins: StateFlow<List<CoinTrack>> = coinRepository.coins

    fun updateInputs(update: BacktestInputs.() -> BacktestInputs) {
        _inputs.update(update)
    }

    fun runBacktest() {
        val currentInputs = _inputs.value
        if (currentInputs.coinId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = BacktestUiState.Running

            try {
                val result = backtester.runBacktest(
                    coinId = currentInputs.coinId,
                    mode = currentInputs.mode,
                    days = currentInputs.days
                )

                val insights = BacktestAdvisor.analyze(result)

                _uiState.value = BacktestUiState.Result(
                    result = result,
                    insights = insights
                )
            } catch (e: Exception) {
                _uiState.value = BacktestUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        _uiState.value = BacktestUiState.Idle
    }
}
