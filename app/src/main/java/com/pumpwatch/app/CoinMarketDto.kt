package com.pumpwatch.app.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Maps the response of CoinGecko's /coins/markets endpoint.
 * Docs: https://www.coingecko.com/en/api/documentation
 */
data class CoinMarketDto(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String?,

    @SerializedName("current_price")
    val currentPrice: Double,

    @SerializedName("total_volume")
    val totalVolume: Double,

    @SerializedName("market_cap")
    val marketCap: Double?,

    @SerializedName("market_cap_rank")
    val marketCapRank: Int?,

    @SerializedName("price_change_percentage_1h_in_currency")
    val priceChangePercentage1h: Double?,

    @SerializedName("price_change_percentage_24h_in_currency")
    val priceChangePercentage24h: Double?
)
