package com.pumpwatch.app.worker

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import com.pumpwatch.app.data.repository.MarketRepository
import com.pumpwatch.app.data.repository.PumpSettingsStore
import com.pumpwatch.app.data.repository.TradeSettingsStore
import com.pumpwatch.app.domain.TradeEvent
import com.pumpwatch.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps polling CoinGecko while the user has monitoring turned on, even if
 * they navigate away from the app. Runs as a foreground service (required by
 * Android for reliable background execution) with a low-priority ongoing
 * notification, per the manifest's dataSync foreground service type.
 *
 * Also drives the paper-trading engine each cycle: opens/updates/closes
 * simulated trades based on the latest signals. No real order is ever placed.
 */
class MonitoringService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var settingsStore: PumpSettingsStore
    private lateinit var tradeSettingsStore: TradeSettingsStore
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        settingsStore = PumpSettingsStore(applicationContext)
        tradeSettingsStore = TradeSettingsStore(applicationContext)
        NotificationHelper.ensureChannels(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildMonitoringNotification(this, "شروع پایش…")
        ServiceCompat.startForeground(
            this,
            NotificationHelper.MONITORING_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        MarketRepository.setMonitoring(true)
        startLoop()
        return START_STICKY
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (true) {
                val settings = settingsStore.settingsFlow.first()
                val tradeSettings = tradeSettingsStore.settingsFlow.first()
                val result = MarketRepository.refresh(settings, tradeSettings)

                result.pumpSignals.forEach { signal ->
                    val coin = MarketRepository.coins.value.find { it.id == signal.coinId }
                    if (coin != null) {
                        NotificationHelper.notifyPump(applicationContext, coin.name, coin.symbol, signal)
                    }
                }

                result.tradeEvents.forEach { event ->
                    when (event) {
                        is TradeEvent.Opened -> NotificationHelper.notifyTradeOpened(applicationContext, event.trade)
                        is TradeEvent.Closed -> NotificationHelper.notifyTradeClosed(applicationContext, event.trade)
                    }
                }

                val status = if (result.pumpSignals.isEmpty()) "بدون پامپ | بررسی بعدی طبق فاصله تنظیم‌شده"
                             else "${result.pumpSignals.size} کوین با سیگنال پامپ شناسایی شد"
                val notification = NotificationHelper.buildMonitoringNotification(this@MonitoringService, status)
                (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager)
                    .notify(NotificationHelper.MONITORING_NOTIFICATION_ID, notification)

                delay(settings.pollIntervalMinutes * 60_000L)
            }
        }
    }

    override fun onDestroy() {
        MarketRepository.setMonitoring(false)
        loopJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
