package com.pumpwatch.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pumpwatch.app.domain.SimulatedTrade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trades")

class TradeStore(private val context: Context) {
    
    private val gson = Gson()
    private val TRADES_KEY = stringPreferencesKey("simulated_trades")

    val allTrades: Flow<List<SimulatedTrade>> = context.dataStore.data.map { preferences ->
        val json = preferences[TRADES_KEY] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<SimulatedTrade>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveTrades(trades: List<SimulatedTrade>) {
        context.dataStore.edit { preferences ->
            val json = gson.toJson(trades)
            preferences[TRADES_KEY] = json
        }
    }

    suspend fun addTrade(trade: SimulatedTrade) {
        val currentTrades = allTrades.value ?: emptyList()
        saveTrades(currentTrades + trade)
    }

    suspend fun updateTrade(trade: SimulatedTrade) {
        val currentTrades = allTrades.value ?: emptyList()
        val updatedTrades = currentTrades.map { if (it.id == trade.id) trade else it }
        saveTrades(updatedTrades)
    }

    suspend fun deleteTrade(tradeId: String) {
        val currentTrades = allTrades.value ?: emptyList()
        saveTrades(currentTrades.filter { it.id != tradeId })
    }

    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.remove(TRADES_KEY)
        }
    }

    fun getOpenTrades(): Flow<List<SimulatedTrade>> = allTrades.map { trades ->
        trades.filter { it.status.name == "OPEN" }
    }

    fun getClosedTrades(): Flow<List<SimulatedTrade>> = allTrades.map { trades ->
        trades.filter { it.status.name == "CLOSED" }
    }
}
