package com.pumpwatch.app.data.repository

import com.pumpwatch.app.data.remote.CoinGeckoApi
import com.pumpwatch.app.domain.CoinTrack
import com.pumpwatch.app.domain.PriceSnapshot
import com.pumpwatch.app.domain.PumpDetector
import com.pumpwatch.app.domain.PumpSettings
import com.pumpwatch.app.domain.PumpSignal
import com.pumpwatch.app.domain.TradeEvent
import com.pumpwatch.app.domain.TradeSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class RefreshResult(
    val pumpSignals: List<PumpSignal>,
    val tradeEvents: List<TradeEvent>
)

/**
 * App-wide singleton so both the UI (Activity/Compose) and the background
 * MonitoringService observe and update the exact same in-memory state.
 * History is intentionally kept in memory only (capped per coin) — this is
 * a monitoring/testing tool; simulated trades (see TradeRepository) are the
 * only "trading" state, and nothing here ever touches a real exchange.
 */
object MarketRepository {

    private const val MAX_HISTORY_PER_COIN = 60 // ~ a few hours at a few-minute poll interval
    private const val PAGE_SIZE = 250            // CoinGecko's per_page max

    private val api: CoinGeckoApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(CoinGeckoApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApi::class.java)
    }

    private val historyById = mutableMapOf<String, MutableList<PriceSnapshot>>()
    private val metaById = mutableMapOf<String, Triple<String, String, String?>>() // symbol, name, image
    private val change24hById = mutableMapOf<String, Double?>()

    private val _coins = MutableStateFlow<List<CoinTrack>>(emptyList())
    val coins: StateFlow<List<CoinTrack>> = _coins.asStateFlow()

    private val _signals = MutableStateFlow<Map<String, PumpSignal>>(emptyMap())
    val signals: StateFlow<Map<String, PumpSignal>> = _signals.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    fun setMonitoring(active: Boolean) {
        _isMonitoring.value = active
    }

    /**
     * Fetches enough pages to cover [PumpSettings.maxMarketCapRank], keeps only coins whose
     * market_cap_rank falls within [minMarketCapRank, maxMarketCapRank], updates rolling
     * history, runs pump detection, then feeds the results into the paper-trading engine.
     */
    suspend fun refresh(settings: PumpSettings, tradeSettings: TradeSettings): RefreshResult {
        val pagesNeeded = ((settings.maxMarketCapRank + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        val markets = try {
            val all = mutableListOf<com.pumpwatch.app.data.remote.CoinMarketDto>()
            for (page in 1..pagesNeeded) {
                if (page > 1) delay(1500)
                all += api.getMarkets(perPage = PAGE_SIZE, page = page)
            }
            _lastError.value = null
            all
        } catch (e: Exception) {
            _lastError.value = e.message ?: "Network error"
            return RefreshResult(emptyList(), emptyList())
        }

        val inRange = markets.filter { dto ->
            val rank = dto.marketCapRank ?: return@filter false
            rank in settings.minMarketCapRank..settings.maxMarketCapRank
        }

        val now = System.currentTimeMillis()
        val newSignals = mutableMapOf<String, PumpSignal>()
        val trackedIds = inRange.map { it.id }.toSet()

        for (m in inRange) {
            metaById[m.id] = Triple(m.symbol, m.name, m.image)
            change24hById[m.id] = m.priceChangePercentage24h

            val list = historyById.getOrPut(m.id) { mutableListOf() }
            list.add(PriceSnapshot(now, m.currentPrice, m.totalVolume))
            while (list.size > MAX_HISTORY_PER_COIN) list.removeAt(0)

            val signal = PumpDetector.evaluate(list, settings)
            if (signal != null) {
                newSignals[m.id] = signal.copy(coinId = m.id)
            }
        }

        // Drop history for coins that fell out of the configured rank window.
        historyById.keys.retainAll(trackedIds)

        _signals.value = newSignals
        val trackedCoins = historyById.keys.mapNotNull { id ->
            val meta = metaById[id] ?: return@mapNotNull null
            CoinTrack(
                id = id,
                symbol = meta.first,
                name = meta.second,
                imageUrl = meta.third,
                history = historyById[id].orEmpty(),
                change24hPercent = change24hById[id]
            )
        }.sortedByDescending { newSignals[it.id]?.score ?: -1 }

        _coins.value = trackedCoins

        val pumpingIds = newSignals.filterValues { it.isPump }.keys
        val tradeEvents = TradeRepository.processTick(trackedCoins, pumpingIds, tradeSettings)

        return RefreshResult(
            pumpSignals = newSignals.values.filter { it.isPump },
            tradeEvents = tradeEvents
        )
    }

    /** Fetches historical price/volume points for one coin, used by the backtester. */
    suspend fun fetchHistoricalSnapshots(coinId: String, days: Int): List<PriceSnapshot> {
        val chart = api.getMarketChart(id = coinId, days = days)
        // Volumes and prices are usually aligned by index at the same cadence CoinGecko
        // picked for the requested range; pairing by index is the standard approach here.
        val count = minOf(chart.prices.size, chart.totalVolumes.size)
        return (0 until count).map { i ->
            PriceSnapshot(
                timestampMillis = chart.prices[i][0].toLong(),
                price = chart.prices[i][1],
                totalVolume = chart.totalVolumes[i][1]
            )
        }
    }
}
