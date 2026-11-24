package com.techenglish.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tech_english_prefs")

data class StoredUserPreferences(
    val userName: String = "Разработчик",
    val dailyGoal: Int = 20,
    val themeMode: String = "SYSTEM",
    val dynamicColors: Boolean = true,
    val readingFontSizeSp: Float = 16f,
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val hapticFeedbackEnabled: Boolean = true,
    val reduceMotionEnabled: Boolean = false,
    val translationLanguage: String = "RU",
    val ageVerified16Plus: Boolean = false,
    val allowFriendPings: Boolean = true,
    val aiConsentAccepted: Boolean = false
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val READING_FONT_SIZE = floatPreferencesKey("reading_font_size")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val REDUCE_MOTION_ENABLED = booleanPreferencesKey("reduce_motion_enabled")
        val TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        val AGE_VERIFIED_16_PLUS = booleanPreferencesKey("age_verified_16_plus")
        val ALLOW_FRIEND_PINGS = booleanPreferencesKey("allow_friend_pings")
        val AI_CONSENT_ACCEPTED = booleanPreferencesKey("ai_consent_accepted")
    }

    val preferencesFlow: Flow<StoredUserPreferences> = context.dataStore.data.map { prefs ->
        StoredUserPreferences(
            userName = prefs[Keys.USER_NAME] ?: "Разработчик",
            dailyGoal = prefs[Keys.DAILY_GOAL] ?: 20,
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            dynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: true,
            readingFontSizeSp = prefs[Keys.READING_FONT_SIZE] ?: 16f,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 19,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK_ENABLED] ?: true,
            reduceMotionEnabled = prefs[Keys.REDUCE_MOTION_ENABLED] ?: false,
            translationLanguage = prefs[Keys.TRANSLATION_LANGUAGE] ?: "RU",
            ageVerified16Plus = prefs[Keys.AGE_VERIFIED_16_PLUS] ?: false,
            allowFriendPings = prefs[Keys.ALLOW_FRIEND_PINGS] ?: true,
            aiConsentAccepted = prefs[Keys.AI_CONSENT_ACCEPTED] ?: false
        )
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim().ifEmpty { "Разработчик" } }
    }

    suspend fun setDailyGoal(goal: Int) {
        val sanitized = when (goal) {
            10, 20, 30 -> goal
            else -> goal.coerceIn(5, 100)
        }
        context.dataStore.edit { it[Keys.DAILY_GOAL] = sanitized }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }

    suspend fun setReadingFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.READING_FONT_SIZE] = sizeSp.coerceIn(13f, 24f) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK_ENABLED] = enabled }
    }

    suspend fun setReduceMotionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_MOTION_ENABLED] = enabled }
    }

    suspend fun setTranslationLanguage(language: String) {
        context.dataStore.edit { it[Keys.TRANSLATION_LANGUAGE] = language }
    }

    suspend fun setAgeVerified16Plus(verified: Boolean) {
        context.dataStore.edit { it[Keys.AGE_VERIFIED_16_PLUS] = verified }
    }

    suspend fun setAllowFriendPings(allowed: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_FRIEND_PINGS] = allowed }
    }

    suspend fun setAiConsentAccepted(accepted: Boolean) {
        context.dataStore.edit { it[Keys.AI_CONSENT_ACCEPTED] = accepted }
    }
}
