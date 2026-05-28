package com.example.insightku.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
        private val DEFAULT_INPUT_MODE_KEY = stringPreferencesKey("default_input_mode")
        private val WHATSAPP_ENABLED_KEY = booleanPreferencesKey("whatsapp_enabled")
        private val LAST_TRANSACTION_DATE_KEY = longPreferencesKey("last_transaction_date")
        private val CURRENT_STREAK_KEY = intPreferencesKey("current_streak")
        private val BEST_STREAK_KEY = intPreferencesKey("best_streak")
        private val TOTAL_DAYS_KEY = intPreferencesKey("total_days")
        private val FREEZE_COUNT_KEY = intPreferencesKey("streak_freeze_count")
        private val PERFECT_STREAK_KEY = booleanPreferencesKey("streak_perfect")
        private val STREAK_GOAL_KEY = intPreferencesKey("streak_goal")
        private val LAST_FREEZE_DATE_KEY = stringPreferencesKey("last_freeze_date")
        private val REPAIR_AVAILABLE_KEY = booleanPreferencesKey("streak_repair_available")
        private val REPAIR_EXPIRY_KEY = longPreferencesKey("streak_repair_expiry")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("notification_enabled")
        private val FIRST_TIME_USER_KEY = booleanPreferencesKey("first_time_user")
        private val CURRENCY_CODE_KEY = stringPreferencesKey("currency_code")
    }

    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "light"
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE_KEY] ?: false
    }

    val defaultInputMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_INPUT_MODE_KEY] ?: "ocr"
    }

    val whatsappEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WHATSAPP_ENABLED_KEY] ?: false
    }

    val lastTransactionDate: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_TRANSACTION_DATE_KEY] ?: 0L
    }

    val currentStreak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_STREAK_KEY] ?: 0
    }

    val bestStreak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BEST_STREAK_KEY] ?: 0
    }

    val totalDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TOTAL_DAYS_KEY] ?: 0
    }

    val freezeCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[FREEZE_COUNT_KEY] ?: 0
    }

    val isPerfectStreak: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PERFECT_STREAK_KEY] ?: false
    }

    val streakGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[STREAK_GOAL_KEY] ?: 7
    }

    val lastFreezeDate: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_FREEZE_DATE_KEY] ?: ""
    }

    val repairAvailable: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REPAIR_AVAILABLE_KEY] ?: false
    }

    val repairExpiry: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[REPAIR_EXPIRY_KEY] ?: 0L
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED_KEY] ?: false
    }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_ENABLED_KEY] ?: true
    }

    val isFirstTimeUser: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_TIME_USER_KEY] ?: true
    }

    /** Kode mata uang yang aktif (ISO 4217), default: IDR */
    val currencyCode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_CODE_KEY] ?: "IDR"
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE_KEY] = isDarkMode
        }
    }

    suspend fun setDefaultInputMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_INPUT_MODE_KEY] = mode
        }
    }

    suspend fun setWhatsappEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WHATSAPP_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateLastTransactionDate(date: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_TRANSACTION_DATE_KEY] = date
        }
    }

    suspend fun updateCurrentStreak(streak: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_STREAK_KEY] = streak
        }
    }

    suspend fun updateBestStreak(streak: Int) {
        context.dataStore.edit { preferences ->
            preferences[BEST_STREAK_KEY] = streak
        }
    }

    suspend fun updateTotalDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[TOTAL_DAYS_KEY] = days
        }
    }

    suspend fun updateFreezeCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[FREEZE_COUNT_KEY] = count.coerceIn(0, 2)
        }
    }

    suspend fun setPerfectStreak(perfect: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PERFECT_STREAK_KEY] = perfect
        }
    }

    suspend fun setStreakGoal(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[STREAK_GOAL_KEY] = goal
        }
    }

    suspend fun setLastFreezeDate(dateKey: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_FREEZE_DATE_KEY] = dateKey
        }
    }

    suspend fun setRepairAvailable(available: Boolean, expiryMs: Long = 0L) {
        context.dataStore.edit { preferences ->
            preferences[REPAIR_AVAILABLE_KEY] = available
            preferences[REPAIR_EXPIRY_KEY]    = expiryMs
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun setFirstTimeUser(isFirstTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_TIME_USER_KEY] = isFirstTime
        }
    }

    /** Simpan pilihan mata uang user (mis. "IDR", "USD", "EUR") */
    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_CODE_KEY] = code
        }
    }
}
