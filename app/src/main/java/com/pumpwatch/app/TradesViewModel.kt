package com.pumpwatch.app.ui.trades

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.data.repository.TradeRepository
import com.pumpwatch.app.data.repository.TradeSettingsStore
import com.pumpwatch.app.domain.SimulatedTrade
import com.pumpwatch.app.domain.TradeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TradesViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TradeSettingsStore(application)

    val trades: StateFlow<List<SimulatedTrade>> = TradeRepository.trades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<TradeSettings> = store.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TradeSettings())

    fun saveSettings(settings: TradeSettings) {
        viewModelScope.launch { store.update(settings) }
    }
}
