package com.pumpwatch.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pumpwatch.app.domain.PumpSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pump_settings")

class PumpSettingsStore(private val context: Context) {

    private object Keys {
        val POLL_INTERVAL = intPreferencesKey("poll_interval_minutes")
        val PRICE_THRESHOLD = doublePreferencesKey("price_change_threshold")
        val VOLUME_THRESHOLD = doublePreferencesKey("volume_growth_threshold")
        val MIN_UP_TICKS = intPreferencesKey("min_consecutive_up_ticks")
        val WINDOW_MINUTES = intPreferencesKey("window_minutes")
        val MIN_SIGNALS = intPreferencesKey("min_signals_required")
        val MIN_RANK = intPreferencesKey("min_market_cap_rank")
        val MAX_RANK = intPreferencesKey("max_market_cap_rank")
    }

    val settingsFlow: Flow<PumpSettings> = context.dataStore.data.map { prefs ->
        val defaults = PumpSettings()
        PumpSettings(
            pollIntervalMinutes = prefs[Keys.POLL_INTERVAL] ?: defaults.pollIntervalMinutes,
            priceChangeThresholdPercent = prefs[Keys.PRICE_THRESHOLD] ?: defaults.priceChangeThresholdPercent,
            volumeGrowthThresholdPercent = prefs[Keys.VOLUME_THRESHOLD] ?: defaults.volumeGrowthThresholdPercent,
            minConsecutiveUpTicks = prefs[Keys.MIN_UP_TICKS] ?: defaults.minConsecutiveUpTicks,
            windowMinutes = prefs[Keys.WINDOW_MINUTES] ?: defaults.windowMinutes,
            minSignalsRequired = prefs[Keys.MIN_SIGNALS] ?: defaults.minSignalsRequired,
            minMarketCapRank = prefs[Keys.MIN_RANK] ?: defaults.minMarketCapRank,
            maxMarketCapRank = prefs[Keys.MAX_RANK] ?: defaults.maxMarketCapRank
        )
    }

    suspend fun update(settings: PumpSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POLL_INTERVAL] = settings.pollIntervalMinutes
            prefs[Keys.PRICE_THRESHOLD] = settings.priceChangeThresholdPercent
            prefs[Keys.VOLUME_THRESHOLD] = settings.volumeGrowthThresholdPercent
            prefs[Keys.MIN_UP_TICKS] = settings.minConsecutiveUpTicks
            prefs[Keys.WINDOW_MINUTES] = settings.windowMinutes
            prefs[Keys.MIN_SIGNALS] = settings.minSignalsRequired
            prefs[Keys.MIN_RANK] = settings.minMarketCapRank
            prefs[Keys.MAX_RANK] = settings.maxMarketCapRank
        }
    }
}
