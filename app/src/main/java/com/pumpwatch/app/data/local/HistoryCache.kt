package com.pumpwatch.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pumpwatch.app.domain.PriceSnapshot
import java.io.File

object HistoryCache {

    private const val TTL_MILLIS = 6 * 3600_000L
    private val gson = Gson()

    fun read(context: Context, coinId: String, days: Int): List<PriceSnapshot>? {
        val file = File(context.cacheDir, "hist_${coinId}_$days.json")
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > TTL_MILLIS) return null
        return try {
            gson.fromJson(
                file.readText(),
                object : TypeToken<List<PriceSnapshot>>() {}.type
            )
        } catch (e: Exception) { null }
    }

    fun write(context: Context, coinId: String, days: Int, data: List<PriceSnapshot>) {
        try {
            File(context.cacheDir, "hist_${coinId}_$days.json")
                .writeText(gson.toJson(data))
        } catch (_: Exception) {}
    }
}
