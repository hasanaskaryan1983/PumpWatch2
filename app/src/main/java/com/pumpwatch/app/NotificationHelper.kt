package com.pumpwatch.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pumpwatch.app.MainActivity
import com.pumpwatch.app.domain.PumpSignal
import com.pumpwatch.app.domain.SimulatedTrade
import android.app.PendingIntent
import android.content.Intent

object NotificationHelper {

    const val MONITORING_CHANNEL_ID = "monitoring_channel"
    const val ALERT_CHANNEL_ID = "pump_alert_channel"
    const val MONITORING_NOTIFICATION_ID = 1001
    private var nextAlertId = 2000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val monitoring = NotificationChannel(
            MONITORING_CHANNEL_ID,
            "وضعیت پایش بازار",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "نمایش وضعیت فعال بودن پایش رمزارزها" }

        val alerts = NotificationChannel(
            ALERT_CHANNEL_ID,
            "هشدار پامپ",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "هشدار زمانی که رفتار پامپ‌مانند تشخیص داده شود" }

        manager.createNotificationChannel(monitoring)
        manager.createNotificationChannel(alerts)
    }

    fun buildMonitoringNotification(context: Context, statusText: String) =
        NotificationCompat.Builder(context, MONITORING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("PumpWatch در حال پایش است")
            .setContentText(statusText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun notifyPump(context: Context, coinName: String, symbol: String, signal: PumpSignal) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "قیمت %+.1f%% | حجم %+.0f%% | %d تیک صعودی | امتیاز %d".format(
            signal.priceChangePercent, signal.volumeGrowthPercent, signal.consecutiveUpTicks, signal.score
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("⚠️ احتمال پامپ: ${symbol.uppercase()} ($coinName)")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(nextAlertId++, notification)
    }

    fun notifyTradeOpened(context: Context, trade: SimulatedTrade) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val text = "قیمت ورود: $${"%,.4f".format(trade.entryPrice)} | استاپ اولیه: $${"%,.4f".format(trade.hardStopLossPrice)}"

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("🟢 معامله شبیه‌سازی باز شد: ${trade.symbol.uppercase()}")
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(nextAlertId++, notification)
    }

    fun notifyTradeClosed(context: Context, trade: SimulatedTrade) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val pnl = trade.closedPnlPercent ?: 0.0
        val reasonText = when (trade.exitReason) {
            com.pumpwatch.app.domain.ExitReason.TRAILING_STOP -> "برخورد با استاپ شناور"
            com.pumpwatch.app.domain.ExitReason.REVERSAL_DETECTED -> "تشخیص برگشت روند"
            com.pumpwatch.app.domain.ExitReason.HARD_STOP_LOSS -> "حد ضرر اولیه"
            else -> "بستن دستی"
        }
        val emoji = if (pnl >= 0) "✅" else "🔴"
        val text = "علت خروج: $reasonText | سود/زیان: %+.2f%%".format(pnl)

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("$emoji معامله بسته شد: ${trade.symbol.uppercase()}")
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(nextAlertId++, notification)
    }
}
