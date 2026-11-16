package com.example.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.common.LocaleHelper
import com.example.core.model.AppLanguage
import com.example.core.model.CefrLevel
import com.example.core.model.TargetExam
import com.example.core.model.ThemeMode
import com.example.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "contexta_settings")

class DataStoreSettingsRepository(context: Context) : SettingsRepository {
    private val appContext = context.applicationContext
    private val overlayPrefs = appContext.getSharedPreferences("contexta_overlay_pos", Context.MODE_PRIVATE)

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val TARGET_EXAM = stringPreferencesKey("target_exam")
        val CEFR_LEVEL = stringPreferencesKey("cefr_level")
        val OVERLAY_SCALE = floatPreferencesKey("overlay_scale")
        val OVERLAY_ALPHA = floatPreferencesKey("overlay_alpha")
        val READING_FONT_SIZE = intPreferencesKey("reading_font_size_sp")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LAST_STUDY_ROUTE = stringPreferencesKey("last_study_route")
    }

    override val settingsFlow: Flow<UserSettings> = appContext.dataStore.data.map { prefs ->
        UserSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = ThemeMode.fromCode(prefs[Keys.THEME_MODE]),
            dailyGoalMinutes = (prefs[Keys.DAILY_GOAL_MINUTES] ?: 20).coerceIn(5, 240),
            targetExam = TargetExam.fromCode(prefs[Keys.TARGET_EXAM]),
            cefrLevel = CefrLevel.fromCode(prefs[Keys.CEFR_LEVEL]),
            overlayScale = (prefs[Keys.OVERLAY_SCALE] ?: 1.0f).coerceIn(0.7f, 1.5f),
            overlayAlpha = (prefs[Keys.OVERLAY_ALPHA] ?: 0.92f).coerceIn(0.4f, 1.0f),
            readingFontSizeSp = (prefs[Keys.READING_FONT_SIZE] ?: 17).coerceIn(14, 28),
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    override val lastStudyRouteFlow: Flow<String?> = appContext.dataStore.data.map { prefs ->
        prefs[Keys.LAST_STUDY_ROUTE]
    }

    override suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        appContext.dataStore.edit { prefs ->
            val current = UserSettings(
                language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
                themeMode = ThemeMode.fromCode(prefs[Keys.THEME_MODE]),
                dailyGoalMinutes = (prefs[Keys.DAILY_GOAL_MINUTES] ?: 20).coerceIn(5, 240),
                targetExam = TargetExam.fromCode(prefs[Keys.TARGET_EXAM]),
                cefrLevel = CefrLevel.fromCode(prefs[Keys.CEFR_LEVEL]),
                overlayScale = (prefs[Keys.OVERLAY_SCALE] ?: 1.0f).coerceIn(0.7f, 1.5f),
                overlayAlpha = (prefs[Keys.OVERLAY_ALPHA] ?: 0.92f).coerceIn(0.4f, 1.0f),
                readingFontSizeSp = (prefs[Keys.READING_FONT_SIZE] ?: 17).coerceIn(14, 28),
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
            )
            val updated = transform(current)
            prefs[Keys.LANGUAGE] = updated.language.code
            prefs[Keys.THEME_MODE] = updated.themeMode.code
            prefs[Keys.DAILY_GOAL_MINUTES] = updated.dailyGoalMinutes.coerceIn(5, 240)
            prefs[Keys.TARGET_EXAM] = updated.targetExam.code
            prefs[Keys.CEFR_LEVEL] = updated.cefrLevel.code
            prefs[Keys.OVERLAY_SCALE] = updated.overlayScale.coerceIn(0.7f, 1.5f)
            prefs[Keys.OVERLAY_ALPHA] = updated.overlayAlpha.coerceIn(0.4f, 1.0f)
            prefs[Keys.READING_FONT_SIZE] = updated.readingFontSizeSp.coerceIn(14, 28)
            prefs[Keys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            LocaleHelper.saveLanguageCode(appContext, updated.language)
        }
    }

    override suspend fun saveLastStudyRoute(route: String) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.LAST_STUDY_ROUTE] = route
        }
    }

    override fun getOverlayPosition(): Pair<Int, Int> {
        val x = overlayPrefs.getInt("overlay_x", 24)
        val y = overlayPrefs.getInt("overlay_y", 240)
        return x to y
    }

    override fun saveOverlayPosition(x: Int, y: Int) {
        overlayPrefs.edit().putInt("overlay_x", x).putInt("overlay_y", y).apply()
    }
}
