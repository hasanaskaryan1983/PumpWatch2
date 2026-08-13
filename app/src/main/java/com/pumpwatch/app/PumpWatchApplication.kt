package com.pumpwatch.app

import android.app.Application
import com.pumpwatch.app.data.repository.CrashLogStore
import com.pumpwatch.app.notification.NotificationHelper
import java.io.PrintWriter
import java.io.StringWriter

class PumpWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        installCrashLogger()
    }

    /**
     * Catches any otherwise-fatal exception, saves the full stack trace to
     * disk (synchronously, so it survives the crash), then hands off to the
     * system's default handler so the OS still closes the app normally.
     * MainActivity checks for a saved crash on next launch and shows it
     * front-and-center instead of a silent blank screen.
     */
    private fun installCrashLogger() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                CrashLogStore.save(applicationContext, writer.toString())
            } catch (_: Exception) {
                // best-effort only
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
