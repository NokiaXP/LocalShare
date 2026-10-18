package com.defname.localshare

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Applies the language selected inside LocalShare without changing the device language. */
object LocaleManager {
    private const val PREFERENCES_NAME = "localshare_preferences"
    private const val LANGUAGE_KEY = "language"
    const val ENGLISH = "en"
    const val SPANISH = "es"

    fun currentLanguage(context: Context): String =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, SPANISH)
            ?.takeIf { it == ENGLISH || it == SPANISH } ?: SPANISH

    fun setLanguage(context: Context, language: String) {
        val supportedLanguage = if (language == SPANISH) SPANISH else ENGLISH
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit().putString(LANGUAGE_KEY, supportedLanguage).apply()
    }

    fun wrap(context: Context): Context {
        val locale = Locale.forLanguageTag(currentLanguage(context))
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
