package com.pumpwatch.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.data.repository.MarketRepository
import com.pumpwatch.app.data.repository.PumpSettingsStore
import com.pumpwatch.app.data.repository.TradeSettingsStore
import com.pumpwatch.app.domain.CoinTrack
import com.pumpwatch.app.domain.PumpSignal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val coins: List<CoinTrack> = emptyList(),
    val signals: Map<String, PumpSignal> = emptyMap(),
    val isMonitoring: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = PumpSettingsStore(application)
    private val tradeSettingsStore = TradeSettingsStore(application)

    val uiState: StateFlow<DashboardUiState> = combine(
        MarketRepository.coins,
        MarketRepository.signals,
        MarketRepository.isMonitoring,
        MarketRepository.lastError
    ) { coins, signals, monitoring, error ->
        DashboardUiState(coins, signals, monitoring, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    /** Manual one-off refresh, e.g. pull-to-refresh, even when the service isn't running. */
    fun refreshOnce() {
        viewModelScope.launch {
            val settings = settingsStore.settingsFlow.first()
            val tradeSettings = tradeSettingsStore.settingsFlow.first()
            MarketRepository.refresh(settings, tradeSettings)
        }
    }
}
