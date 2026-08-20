
package com.pumpwatch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TradeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: SimulatedTradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trades: List<SimulatedTradeEntity>)

    @Query("SELECT * FROM simulated_trades WHERE status = 'OPEN' ORDER BY entryTimeMillis DESC")
    suspend fun getOpenTrades(): List<SimulatedTradeEntity>

    @Query("SELECT * FROM simulated_trades WHERE status = 'CLOSED' ORDER BY exitTimeMillis DESC")
    suspend fun getClosedTrades(): List<SimulatedTradeEntity>

    @Query("SELECT * FROM simulated_trades ORDER BY entryTimeMillis DESC")
    suspend fun getAllTrades(): List<SimulatedTradeEntity>

    @Query("DELETE FROM simulated_trades")
    suspend fun clearAll()

    @Query("DELETE FROM simulated_trades WHERE status = 'CLOSED'")
    suspend fun clearClosed()
}
