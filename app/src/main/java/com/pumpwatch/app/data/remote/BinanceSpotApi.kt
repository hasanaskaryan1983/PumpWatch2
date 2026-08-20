
package com.pumpwatch.app.data.remote

import com.google.gson.JsonArray
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceSpotApi {

    @GET("api/v3/klines")
    suspend fun klines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 200
    ): List<JsonArray>

    companion object {
        const val BASE_URL = "https://api.binance.com/"
    }
}
