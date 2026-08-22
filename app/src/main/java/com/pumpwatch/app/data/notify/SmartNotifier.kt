package com.pumpwatch.app.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pumpwatch.app.domain.SimulatedTrade
import com.pumpwatch.app.domain.TradeDirection

class SmartNotifier(private val context: Context) {

    companion object {
        private const val CHANNEL_SIGNALS = "pump_signals"
        private const val CHANNEL_TRADES = "trade_updates"
        private const val CHANNEL_ALERTS = "critical_alerts"

        private const val NOTIF_ID_SIGNAL = 1000
        private const val NOTIF_ID_TRADE = 2000
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val recentSignals = mutableSetOf<String>()
    private val lastSignalTime = mutableMapOf<String, Long>()

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val signalChannel = NotificationChannel(
                CHANNEL_SIGNALS,
                "Pump Signals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New pump/dump detection signals"
                enableVibration(true)
            }

            val tradeChannel = NotificationChannel(
                CHANNEL_TRADES,
                "Trade Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Trade open/close notifications"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Critical Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hard stop loss and critical events"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(signalChannel, tradeChannel, alertChannel)
            )
        }
    }

    /**
     * Send pump/dump signal notification with deduplication.
     * Same coin won't be notified more than once per 15 minutes.
     */
    fun notifySignal(
        coinSymbol: String,
        coinName: String,
        direction: TradeDirection,
        priceChangePercent: Double,
        volumeGrowthPercent: Double,
        score: Int
    ) {
        val now = System.currentTimeMillis()
        val lastTime = lastSignalTime[coinSymbol] ?: 0L
        val cooldownMs = 15 * 60_000L

        if (now - lastTime < cooldownMs) return

        lastSignalTime[coinSymbol] = now

        val emoji = if (direction == TradeDirection.LONG) "🚀" else "📉"
        val title = "$emoji ${coinSymbol.uppercase()} - ${direction.name}"
        val text = "Price: +${"%.1f".format(priceChangePercent)}% | Volume: +${"%.0f".format(volumeGrowthPercent)}% | Score: $score"

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SIGNALS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_SIGNAL + coinSymbol.hashCode(), notification)
    }

    /**
     * Send trade open/close notification.
     */
    fun notifyTrade(trade: SimulatedTrade, isOpen: Boolean) {
        val emoji = if (isOpen) "" else "✅"
        val title = if (isOpen) {
            "$emoji ${trade.direction.name} Opened: ${trade.symbol.uppercase()}"
        } else {
            "$emoji ${trade.direction.name} Closed: ${trade.symbol.uppercase()}"
        }

        val text = if (isOpen) {
            "Entry: $${"%.4f".format(trade.entryPrice)} | Stake: $${"%.0f".format(trade.stakeAmount)}"
        } else {
            val pnl = trade.closedPnlPercent ?: 0.0
            val profit = trade.closedProfitAmount ?: 0.0
            "Exit: $${"%.4f".format(trade.exitPrice)} | PnL: ${if (pnl >= 0) "+" else ""}%.2f%% ($${"%.2f".format(profit)})\nReason: ${trade.exitReason?.name?.replace("_", " ")}"
        }

        val channel = if (!isOpen && trade.exitReason == com.pumpwatch.app.domain.ExitReason.HARD_STOP_LOSS) {
            CHANNEL_ALERTS
        } else {
            CHANNEL_TRADES
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (channel == CHANNEL_ALERTS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_TRADE + trade.id.hashCode(), notification)
    }

    /**
     * Send critical alert (e.g., BTC crash filter triggered).
     */
    fun notifyCriticalAlert(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_TRADE + 9999, notification)
    }
}
