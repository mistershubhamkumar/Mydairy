package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.diaryDataStore: DataStore<Preferences> by preferencesDataStore(name = "my_diary_prefs")

enum class LockMode { NONE, PIN, PASSWORD }

data class AppPreferences(
    val onboardingCompleted: Boolean = false,
    val themeId: String = "pink_princess",
    val fontId: String = "quicksand",
    val mascotId: String = "white_cat",
    val animationsEnabled: Boolean = true,
    val language: String = "English",
    val soundVibrationEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val lockMode: LockMode = LockMode.NONE,
    val lockHash: String = "",
    val lockSalt: String = "",
    val biometricEnabled: Boolean = false,
    val secretModeEnabled: Boolean = false,
    val autoLockMinutes: Int = 5,
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val reminderDaysCsv: String = "Mon,Tue,Wed,Thu,Fri,Sat,Sun",
    val moodReminderEnabled: Boolean = false,
    val gratitudeReminderEnabled: Boolean = false,
    val streakReminderEnabled: Boolean = true,
    val autoCleanupRecycleBin: Boolean = true,
    val autoCloudBackupEnabled: Boolean = true
)

class LocalPreferencesStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_ID = stringPreferencesKey("theme_id")
        val FONT_ID = stringPreferencesKey("font_id")
        val MASCOT_ID = stringPreferencesKey("mascot_id")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val SOUND_VIBRATION = booleanPreferencesKey("sound_vibration")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LOCK_MODE = stringPreferencesKey("lock_mode")
        val LOCK_HASH = stringPreferencesKey("lock_hash")
        val LOCK_SALT = stringPreferencesKey("lock_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SECRET_MODE = booleanPreferencesKey("secret_mode")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_DAYS = stringPreferencesKey("reminder_days")
        val MOOD_REMINDER = booleanPreferencesKey("mood_reminder")
        val GRATITUDE_REMINDER = booleanPreferencesKey("gratitude_reminder")
        val STREAK_REMINDER = booleanPreferencesKey("streak_reminder")
        val AUTO_CLEANUP_BIN = booleanPreferencesKey("auto_cleanup_bin")
        val AUTO_CLOUD_BACKUP = booleanPreferencesKey("auto_cloud_backup")
    }

    val preferencesFlow: Flow<AppPreferences> = context.diaryDataStore.data.map { p ->
        val rawMode = p[Keys.LOCK_MODE] ?: LockMode.NONE.name
        val mode = LockMode.entries.firstOrNull { it.name == rawMode } ?: LockMode.NONE
        AppPreferences(
            onboardingCompleted = p[Keys.ONBOARDING_COMPLETED] ?: false,
            themeId = p[Keys.THEME_ID] ?: "pink_princess",
            fontId = p[Keys.FONT_ID] ?: "quicksand",
            mascotId = p[Keys.MASCOT_ID] ?: "white_cat",
            animationsEnabled = p[Keys.ANIMATIONS_ENABLED] ?: true,
            language = p[Keys.LANGUAGE] ?: "English",
            soundVibrationEnabled = p[Keys.SOUND_VIBRATION] ?: true,
            appLockEnabled = p[Keys.APP_LOCK_ENABLED] ?: false,
            lockMode = mode,
            lockHash = p[Keys.LOCK_HASH] ?: "",
            lockSalt = p[Keys.LOCK_SALT] ?: "",
            biometricEnabled = p[Keys.BIOMETRIC_ENABLED] ?: false,
            secretModeEnabled = p[Keys.SECRET_MODE] ?: false,
            autoLockMinutes = p[Keys.AUTO_LOCK_MINUTES] ?: 5,
            notificationsEnabled = p[Keys.NOTIFICATIONS_ENABLED] ?: true,
            dailyReminderEnabled = p[Keys.DAILY_REMINDER_ENABLED] ?: false,
            reminderHour = p[Keys.REMINDER_HOUR] ?: 21,
            reminderMinute = p[Keys.REMINDER_MINUTE] ?: 0,
            reminderDaysCsv = p[Keys.REMINDER_DAYS] ?: "Mon,Tue,Wed,Thu,Fri,Sat,Sun",
            moodReminderEnabled = p[Keys.MOOD_REMINDER] ?: false,
            gratitudeReminderEnabled = p[Keys.GRATITUDE_REMINDER] ?: false,
            streakReminderEnabled = p[Keys.STREAK_REMINDER] ?: true,
            autoCleanupRecycleBin = p[Keys.AUTO_CLEANUP_BIN] ?: true,
            autoCloudBackupEnabled = p[Keys.AUTO_CLOUD_BACKUP] ?: true
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.diaryDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setTheme(themeId: String) {
        context.diaryDataStore.edit { it[Keys.THEME_ID] = themeId }
    }

    suspend fun setFont(fontId: String) {
        context.diaryDataStore.edit { it[Keys.FONT_ID] = fontId }
    }

    suspend fun setMascot(mascotId: String) {
        context.diaryDataStore.edit { it[Keys.MASCOT_ID] = mascotId }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.diaryDataStore.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun setLanguage(language: String) {
        context.diaryDataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setSoundVibration(enabled: Boolean) {
        context.diaryDataStore.edit { it[Keys.SOUND_VIBRATION] = enabled }
    }

    suspend fun setAppLock(mode: LockMode, secret: String) {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val hashHex = sha256Hex("$saltHex:$secret")
        context.diaryDataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = true
            it[Keys.LOCK_MODE] = mode.name
            it[Keys.LOCK_SALT] = saltHex
            it[Keys.LOCK_HASH] = hashHex
        }
    }

    suspend fun disableAppLock() {
        context.diaryDataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = false
            it[Keys.LOCK_MODE] = LockMode.NONE.name
            it[Keys.LOCK_HASH] = ""
            it[Keys.LOCK_SALT] = ""
            it[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    fun verifySecret(prefs: AppPreferences, candidate: String): Boolean {
        if (prefs.lockHash.isBlank() || prefs.lockSalt.isBlank()) return false
        return sha256Hex("${prefs.lockSalt}:$candidate") == prefs.lockHash
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.diaryDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setSecretModeEnabled(enabled: Boolean) {
        context.diaryDataStore.edit { it[Keys.SECRET_MODE] = enabled }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.diaryDataStore.edit { it[Keys.AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun updateReminders(
        notificationsEnabled: Boolean? = null,
        dailyEnabled: Boolean? = null,
        hour: Int? = null,
        minute: Int? = null,
        daysCsv: String? = null,
        moodReminder: Boolean? = null,
        gratitudeReminder: Boolean? = null,
        streakReminder: Boolean? = null
    ) {
        context.diaryDataStore.edit { p ->
            notificationsEnabled?.let { p[Keys.NOTIFICATIONS_ENABLED] = it }
            dailyEnabled?.let { p[Keys.DAILY_REMINDER_ENABLED] = it }
            hour?.let { p[Keys.REMINDER_HOUR] = it }
            minute?.let { p[Keys.REMINDER_MINUTE] = it }
            daysCsv?.let { p[Keys.REMINDER_DAYS] = it }
            moodReminder?.let { p[Keys.MOOD_REMINDER] = it }
            gratitudeReminder?.let { p[Keys.GRATITUDE_REMINDER] = it }
            streakReminder?.let { p[Keys.STREAK_REMINDER] = it }
        }
    }

    suspend fun setAutoCleanupRecycleBin(enabled: Boolean) {
        context.diaryDataStore.edit { it[Keys.AUTO_CLEANUP_BIN] = enabled }
    }

    suspend fun setAutoCloudBackup(enabled: Boolean) {
        context.diaryDataStore.edit { it[Keys.AUTO_CLOUD_BACKUP] = enabled }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
