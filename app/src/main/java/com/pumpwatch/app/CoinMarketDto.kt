package com.pumpwatch.app.data.remote

import com.google.gson.annotations.SerializedName

// ---------- Markets ----------

data class CoinMarketDto(
    val id: String?,
    val symbol: String?,
    val name: String?,
    val image: String?,
    @SerializedName("current_price") val currentPrice: Double,
    @SerializedName("market_cap") val marketCap: Double?,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("fully_diluted_valuation") val fullyDilutedValuation: Double?,
    @SerializedName("total_volume") val totalVolume: Double?,
    @SerializedName("high_24h") val high24h: Double?,
    @SerializedName("low_24h") val low24h: Double?,
    @SerializedName("price_change_24h") val priceChange24h: Double?,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: Double?,
    @SerializedName("market_cap_change_24h") val marketCapChange24h: Double?,
    @SerializedName("market_cap_change_percentage_24h") val marketCapChangePercentage24h: Double?,
    @SerializedName("circulating_supply") val circulatingSupply: Double?,
    @SerializedName("total_supply") val totalSupply: Double?,
    @SerializedName("max_supply") val maxSupply: Double?,
    val ath: Double?,
    @SerializedName("ath_change_percentage") val athChangePercentage: Double?,
    @SerializedName("ath_date") val athDate: String?,
    val atl: Double?,
    @SerializedName("atl_change_percentage") val atlChangePercentage: Double?,
    @SerializedName("atl_date") val atlDate: String?,
    @SerializedName("last_updated") val lastUpdated: String?
)

// ---------- Market Chart (historical) ----------

data class MarketChartDto(
    val prices: List<List<Double>>,
    @SerializedName("market_caps") val marketCaps: List<List<Double>>,
    @SerializedName("total_volumes") val totalVolumes: List<List<Double>>
)

// ---------- Trending ----------

data class TrendingDto(
    val coins: List<TrendingCoinWrapper>?,
    val exchanges: List<Any>?
)

data class TrendingCoinWrapper(
    val item: TrendingItem?
)

data class TrendingItem(
    val id: String?,
    val name: String?,
    val symbol: String?,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    val thumb: String?,
    val small: String?,
    val large: String?,
    val slug: String?,
    @SerializedName("price_btc") val priceBtc: Double?,
    val score: Int?
)

// ---------- Coin Details (for Scam Radar) ----------

data class CoinDetailsDto(
    val id: String?,
    val symbol: String?,
    val name: String?,
    @SerializedName("genesis_date") val genesisDate: String?,
    val links: LinksDto?,
    @SerializedName("developer_data") val developerData: DeveloperDataDto?,
    @SerializedName("market_data") val marketData: MarketDataDto?,
    val tickers: List<TickerDto>?
)

data class LinksDto(
    val homepage: List<String>?,
    val whitepaper: String?,
    @SerializedName("twitter_screen_name") val twitter: String?,
    @SerializedName("telegram_channel_identifier") val telegram: String?
)

data class DeveloperDataDto(
    @SerializedName("commit_count_4_weeks") val commits4w: Int?,
    @SerializedName("stars") val stars: Int?,
    @SerializedName("subscribers") val subscribers: Int?
)

data class MarketDataDto(
    @SerializedName("market_cap") val marketCap: UsdValue?,
    @SerializedName("total_volume") val totalVolume: UsdValue?,
    @SerializedName("circulating_supply") val circulatingSupply: Double?,
    @SerializedName("total_supply") val totalSupply: Double?,
    @SerializedName("max_supply") val maxSupply: Double?
)

data class UsdValue(
    val usd: Double?
)

data class TickerDto(
    val base: String?,
    val target: String?,
    val market: TickerMarket?,
    @SerializedName("trust_score") val trustScore: String?,
    @SerializedName("converted_volume") val convertedVolume: UsdValue?,
    @SerializedName("bid_ask_spread_percentage") val spreadPercent: Double?,
    val last: Double?
)

data class TickerMarket(
    val name: String?,
    val identifier: String?
)
