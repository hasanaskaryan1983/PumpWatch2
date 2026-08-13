package com.pumpwatch.app.data.repository

import android.content.Context

/**
 * Deliberately plain (synchronous) SharedPreferences, not DataStore: this is
 * written from an uncaught-exception handler right before the process may
 * die, so it needs a blocking, guaranteed-to-finish write (commit()), not a
 * suspend function that might not get scheduled in time.
 */
object CrashLogStore {
    private const val PREFS_NAME = "crash_log"
    private const val KEY_LAST_CRASH = "last_crash_text"

    fun save(context: Context, text: String) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_CRASH, text)
                .commit() // synchronous on purpose
        } catch (_: Exception) {
            // If even crash logging fails, there's nothing more we can safely do here.
        }
    }

    fun read(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LAST_CRASH, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_LAST_CRASH).apply()
    }
}
