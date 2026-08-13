package com.pumpwatch.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pumpwatch.app.domain.TradeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tradeDataStore by preferencesDataStore(name = "trade_settings")

class TradeSettingsStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent_trades")
        val HARD_STOP = doublePreferencesKey("hard_stop_loss_percent")
        val TRAILING_STOP = doublePreferencesKey("trailing_stop_percent")
        val REVERSAL_DROP = doublePreferencesKey("reversal_drop_percent")
        val REVERSAL_TICKS = intPreferencesKey("reversal_down_ticks")
    }

    val settingsFlow: Flow<TradeSettings> = context.tradeDataStore.data.map { prefs ->
        val d = TradeSettings()
        TradeSettings(
            enabled = prefs[Keys.ENABLED] ?: d.enabled,
            maxConcurrentTrades = prefs[Keys.MAX_CONCURRENT] ?: d.maxConcurrentTrades,
            hardStopLossPercent = prefs[Keys.HARD_STOP] ?: d.hardStopLossPercent,
            trailingStopPercent = prefs[Keys.TRAILING_STOP] ?: d.trailingStopPercent,
            reversalDropPercent = prefs[Keys.REVERSAL_DROP] ?: d.reversalDropPercent,
            reversalDownTicks = prefs[Keys.REVERSAL_TICKS] ?: d.reversalDownTicks
        )
    }

    suspend fun update(settings: TradeSettings) {
        context.tradeDataStore.edit { prefs ->
            prefs[Keys.ENABLED] = settings.enabled
            prefs[Keys.MAX_CONCURRENT] = settings.maxConcurrentTrades
            prefs[Keys.HARD_STOP] = settings.hardStopLossPercent
            prefs[Keys.TRAILING_STOP] = settings.trailingStopPercent
            prefs[Keys.REVERSAL_DROP] = settings.reversalDropPercent
            prefs[Keys.REVERSAL_TICKS] = settings.reversalDownTicks
        }
    }
}
