package com.example.insightku.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val context: Context
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
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("notification_enabled")
        private val FIRST_TIME_USER_KEY = booleanPreferencesKey("first_time_user")
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

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED_KEY] ?: false
    }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_ENABLED_KEY] ?: true
    }

    val isFirstTimeUser: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_TIME_USER_KEY] ?: true
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
}
