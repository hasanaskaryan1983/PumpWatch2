package com.pumpwatch.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApi {

    @GET("api/v3/coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("price_change_percentage") priceChangePercentage: String = "1h,24h",
        @Query("ids") ids: String? = null
    ): List<CoinMarketDto>

    @GET("api/v3/coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id") id: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: Int = 7
    ): MarketChartDto

    @GET("api/v3/search/trending")
    suspend fun getTrending(): TrendingDto

    @GET("api/v3/coins/{id}")
    suspend fun getCoinDetails(
        @Path("id") id: String,
        @Query("localization") localization: Boolean = false,
        @Query("community_data") communityData: Boolean = false,
        @Query("sparkline") sparkline: Boolean = false
    ): CoinDetailsDto

    companion object {
        const val BASE_URL = "https://api.coingecko.com/"
    }
}
