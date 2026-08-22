package com.pumpwatch.app.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.data.repository.TradeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(
    private val tradeRepository: TradeRepository,
    private val initialCapital: Double = 1000.0
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun refresh() {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = tradeRepository.getStatistics()
                val closedTrades = tradeRepository.getClosedTrades()
                val equityCurve = buildEquityCurve(closedTrades, initialCapital)

                _uiState.value = StatsUiState.Success(
                    stats = stats,
                    trades = closedTrades,
                    equityCurve = equityCurve
                )
            } catch (e: Exception) {
                _uiState.value = StatsUiState.Error(
                    e.message ?: "Failed to load statistics"
                )
            }
        }
    }
}
