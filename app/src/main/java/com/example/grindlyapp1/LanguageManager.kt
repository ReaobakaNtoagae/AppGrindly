package com.example.grindlyapp1

import android.content.Context
import java.util.Locale

object LanguageManager {

    private const val PREF_PREFIX = "prefs_"
    private const val KEY_LANGUAGE = "app_language"

    fun saveLanguage(context: Context, userId: String, languageCode: String) {
        val prefs = context.getSharedPreferences("${PREF_PREFIX}$userId", Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getSavedLanguage(context: Context, userId: String): String {
        val prefs = context.getSharedPreferences("${PREF_PREFIX}$userId", Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun applyLanguage(context: Context, userId: String): Context {
        val languageCode = getSavedLanguage(context, userId)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}