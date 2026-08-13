package com.pumpwatch.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.data.repository.PumpSettingsStore
import com.pumpwatch.app.domain.PumpSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = PumpSettingsStore(application)

    val settings: StateFlow<PumpSettings> = store.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PumpSettings())

    fun save(settings: PumpSettings) {
        viewModelScope.launch { store.update(settings) }
    }
}
