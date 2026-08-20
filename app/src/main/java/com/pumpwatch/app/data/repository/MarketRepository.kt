
package com.pumpwatch.app.data.repository

import android.content.Context
import com.pumpwatch.app.data.local.HistoryCache
import com.pumpwatch.app.data.remote.BinanceSpotApi
import com.pumpwatch.app.data.remote.CoinGeckoApi
import com.pumpwatch.app.data.remote.CoinMarketDto
import com.pumpwatch.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MarketRepository(private val context: Context) {

    private val coinGeckoApi: CoinGeckoApi by lazy {
        Retrofit.Builder()
            .baseUrl(CoinGeckoApi.BASE_URL)
            .client(OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApi::class.java)
    }

    private val binanceApi: BinanceSpotApi by lazy {
        Retrofit.Builder()
            .baseUrl(BinanceSpotApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BinanceSpotApi::class.java)
    }

    private val _coins = MutableStateFlow<List<CoinTrack>>(emptyList())
    val coins: StateFlow<List<CoinTrack>> = _coins.asStateFlow()

    private val _btcTrend = MutableStateFlow<BtcTrend?>(null)
    val btcTrend: StateFlow<BtcTrend?> = _btcTrend.asStateFlow()

    suspend fun refresh(minRank: Int, maxRank: Int) {
        withContext(Dispatchers.IO) {
            try {
                val markets = coinGeckoApi.getMarkets(
                    perPage = 250,
                    page = 1,
                    priceChangePercentage = "1h"
                )

                val filtered = markets.filter { m ->
                    val rank = m.marketCapRank ?: return@filter false
                    rank in minRank..maxRank &&
                    (m.totalVolume ?: 0.0) >= 100_000.0
                }

                val tracks = filtered.map { m ->
                    CoinTrack(
                        id = m.id ?: "",
                        symbol = m.symbol ?: "",
                        name = m.name ?: "",
                        imageUrl = m.image,
                        history = emptyList(),
                        change24hPercent = m.priceChangePercentage24h,
                        marketCapRank = m.marketCapRank
                    )
                }

                _coins.value = tracks

                // BTC trend
                val btc = markets.firstOrNull { it.id == "bitcoin" }
                _btcTrend.value = btc?.let {
                    val change1h = it.priceChangePercentage24h ?: 0.0
                    BtcTrend(
                        priceChangePercent = change1h,
                        isBearish = change1h < -3.0
                    )
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    suspend fun fetchHistoricalSnapshots(coinId: String, days: Int): List<PriceSnapshot> {
        return withContext(Dispatchers.IO) {
            HistoryCache.read(context, coinId, days)?.let { return@withContext it }

            try {
                val chart = coinGeckoApi.getMarketChart(coinId, "usd", days)
                val snapshots = chart.prices.mapIndexed { i, priceData ->
                    val volumeData = chart.totalVolumes.getOrNull(i)
                    PriceSnapshot(
                        timestampMillis = priceData[0].toLong(),
                        price = priceData[1],
                        totalVolume = volumeData?.getOrNull(1) ?: 0.0
                    )
                }

                HistoryCache.write(context, coinId, days, snapshots)
                snapshots
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun fetchCandles(symbol: String, interval: String, limit: Int = 200): List<Candle> {
        return withContext(Dispatchers.IO) {
            try {
                val klines = binanceApi.klines(symbol.uppercase(), interval, limit)
                klines.map { row ->
                    Candle(
                        time = row[0].asLong,
                        open = row[1].asDouble,
                        high = row[2].asDouble,
                        low = row[3].asDouble,
                        close = row[4].asDouble,
                        volume = row[5].asDouble
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
