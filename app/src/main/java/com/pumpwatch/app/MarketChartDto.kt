package com.pumpwatch.app.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Maps CoinGecko's /coins/{id}/market_chart response.
 * Each inner list is [timestampMillis, value] as raw JSON numbers.
 */
data class MarketChartDto(
    val prices: List<List<Double>>,

    @SerializedName("total_volumes")
    val totalVolumes: List<List<Double>>
)
