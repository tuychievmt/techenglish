package com.techenglish.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tech_english_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

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
    val aiConsentAccepted: Boolean = false,
    val isInitialSetupCompleted: Boolean = false,
    val localUsername: String = "",
    val developerRole: String = "Middle",
    val pendingProfileSync: Boolean = false,
    val cachedMinSupportedVersionCode: Int = 0,
    val cachedLatestVersionCode: Int = 0,
    val cachedLatestVersionName: String = "",
    val cachedLatestApkUrl: String = "",
    val cachedApkSha256: String = "",
    val cachedChangelogRu: String = "",
    val optionalUpdateSnoozedUntilMs: Long = 0L
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
        val INITIAL_SETUP_COMPLETED = booleanPreferencesKey("initial_setup_completed")
        val LOCAL_USERNAME = stringPreferencesKey("local_username")
        val DEVELOPER_ROLE = stringPreferencesKey("developer_role")
        val PENDING_PROFILE_SYNC = booleanPreferencesKey("pending_profile_sync")
        val CACHED_MIN_SUPPORTED_VERSION_CODE = intPreferencesKey("cached_min_supported_version_code")
        val CACHED_LATEST_VERSION_CODE = intPreferencesKey("cached_latest_version_code")
        val CACHED_LATEST_VERSION_NAME = stringPreferencesKey("cached_latest_version_name")
        val CACHED_LATEST_APK_URL = stringPreferencesKey("cached_latest_apk_url")
        val CACHED_APK_SHA256 = stringPreferencesKey("cached_apk_sha256")
        val CACHED_CHANGELOG_RU = stringPreferencesKey("cached_changelog_ru")
        val OPTIONAL_UPDATE_SNOOZED_UNTIL_MS = longPreferencesKey("optional_update_snoozed_until_ms")
    }

    val preferencesFlow: Flow<StoredUserPreferences> = context.dataStore.data
        .catch { err ->
            if (err is IOException) emit(emptyPreferences()) else throw err
        }
        .map { prefs ->
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
                aiConsentAccepted = prefs[Keys.AI_CONSENT_ACCEPTED] ?: false,
                isInitialSetupCompleted = prefs[Keys.INITIAL_SETUP_COMPLETED] ?: false,
                localUsername = prefs[Keys.LOCAL_USERNAME].orEmpty(),
                developerRole = prefs[Keys.DEVELOPER_ROLE] ?: "Middle",
                pendingProfileSync = prefs[Keys.PENDING_PROFILE_SYNC] ?: false,
                cachedMinSupportedVersionCode = prefs[Keys.CACHED_MIN_SUPPORTED_VERSION_CODE] ?: 0,
                cachedLatestVersionCode = prefs[Keys.CACHED_LATEST_VERSION_CODE] ?: 0,
                cachedLatestVersionName = prefs[Keys.CACHED_LATEST_VERSION_NAME].orEmpty(),
                cachedLatestApkUrl = prefs[Keys.CACHED_LATEST_APK_URL].orEmpty(),
                cachedApkSha256 = prefs[Keys.CACHED_APK_SHA256].orEmpty(),
                cachedChangelogRu = prefs[Keys.CACHED_CHANGELOG_RU].orEmpty(),
                optionalUpdateSnoozedUntilMs = prefs[Keys.OPTIONAL_UPDATE_SNOOZED_UNTIL_MS] ?: 0L
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

    suspend fun setInitialSetupCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.INITIAL_SETUP_COMPLETED] = completed }
    }

    suspend fun setLocalUsername(username: String) {
        context.dataStore.edit { it[Keys.LOCAL_USERNAME] = username.trim().removePrefix("@") }
    }

    suspend fun setDeveloperRole(role: String) {
        context.dataStore.edit { it[Keys.DEVELOPER_ROLE] = role.trim().ifEmpty { "Middle" } }
    }

    suspend fun setPendingProfileSync(pending: Boolean) {
        context.dataStore.edit { it[Keys.PENDING_PROFILE_SYNC] = pending }
    }

    suspend fun completeInitialSetup(
        localUsername: String,
        displayName: String,
        developerRole: String,
        dailyGoal: Int,
        translationLanguage: String,
        themeMode: String,
        pendingProfileSync: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCAL_USERNAME] = localUsername.trim().removePrefix("@")
            prefs[Keys.USER_NAME] = displayName.trim().ifEmpty { localUsername.ifBlank { "Разработчик" } }
            prefs[Keys.DEVELOPER_ROLE] = developerRole.trim().ifEmpty { "Middle" }
            prefs[Keys.DAILY_GOAL] = when (dailyGoal) {
                10, 20, 30 -> dailyGoal
                else -> dailyGoal.coerceIn(5, 100)
            }
            prefs[Keys.TRANSLATION_LANGUAGE] = translationLanguage
            prefs[Keys.THEME_MODE] = themeMode
            prefs[Keys.PENDING_PROFILE_SYNC] = pendingProfileSync
            prefs[Keys.AGE_VERIFIED_16_PLUS] = true
            prefs[Keys.INITIAL_SETUP_COMPLETED] = true
        }
    }

    suspend fun saveCachedUpdateMetadata(
        minSupportedVersionCode: Int,
        latestVersionCode: Int,
        latestVersionName: String,
        latestApkUrl: String,
        apkSha256: String,
        changelogRu: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHED_MIN_SUPPORTED_VERSION_CODE] = minSupportedVersionCode
            prefs[Keys.CACHED_LATEST_VERSION_CODE] = latestVersionCode
            prefs[Keys.CACHED_LATEST_VERSION_NAME] = latestVersionName.trim()
            prefs[Keys.CACHED_LATEST_APK_URL] = latestApkUrl.trim()
            prefs[Keys.CACHED_APK_SHA256] = apkSha256.trim().lowercase()
            prefs[Keys.CACHED_CHANGELOG_RU] = changelogRu.trim()
        }
    }

    suspend fun setOptionalUpdateSnoozedUntil(untilMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPTIONAL_UPDATE_SNOOZED_UNTIL_MS] = untilMs
        }
    }
}
