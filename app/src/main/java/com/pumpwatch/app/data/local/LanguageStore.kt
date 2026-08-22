package com.pumpwatch.app.data.local

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

class LanguageStore(context: Context) {
    private val prefs = context.getSharedPreferences("language", Context.MODE_PRIVATE)

    fun getSavedLanguage(): String =
        prefs.getString("language", Locale.getDefault().language) ?: "en"

    fun saveLanguage(languageCode: String) {
        prefs.edit().putString("language", languageCode).apply()
        applyLanguage(languageCode)
    }

    companion object {
        fun applyLanguage(languageCode: String) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode)
            )
        }
    }
}

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String
)

object Languages {
    val available = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("fa", "Persian", "فارسی"),
        LanguageOption("ar", "Arabic", "العربية"),
        LanguageOption("zh", "Chinese", "中文"),
        LanguageOption("ja", "Japanese", "日本語"),
        LanguageOption("ru", "Russian", "Русский"),
        LanguageOption("it", "Italian", "Italiano"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("de", "German", "Deutsch")
    )
}
