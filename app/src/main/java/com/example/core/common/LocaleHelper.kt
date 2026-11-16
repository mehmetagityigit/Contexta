package com.example.core.common

import android.content.Context
import android.content.res.Configuration
import com.example.core.model.AppLanguage
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "contexta_locale_prefs"
    private const val KEY_LANGUAGE = "app_language_code"

    fun getSavedLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, AppLanguage.TURKISH.code) ?: AppLanguage.TURKISH.code
    }

    fun saveLanguageCode(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).commit()
    }

    fun applyLocale(base: Context): Context {
        val code = getSavedLanguageCode(base)
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
