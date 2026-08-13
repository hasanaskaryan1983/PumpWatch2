package com.pumpwatch.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApi {

    /**
     * Fetches the top [perPage] coins by market cap in a single call.
     * Free, no API key required (subject to CoinGecko's public rate limits).
     */
    @GET("api/v3/coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("price_change_percentage") priceChangePercentage: String = "1h,24h"
    ): List<CoinMarketDto>

    /**
     * Historical prices/volumes for one coin, used for backtesting.
     * CoinGecko auto-picks granularity by [days]: ~5-minutely for 1 day,
     * hourly for 2-90 days, daily beyond that (free tier behavior).
     */
    @GET("api/v3/coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id") id: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: Int = 7
    ): MarketChartDto

    companion object {
        const val BASE_URL = "https://api.coingecko.com/"
    }
}
