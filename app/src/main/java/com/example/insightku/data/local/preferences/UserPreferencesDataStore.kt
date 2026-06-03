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
        // Analytics "did you notice?" — ids of insights already surfaced, so new ones rotate in over time.
        private val SEEN_NOTICINGS_KEY = stringSetPreferencesKey("analytics_seen_noticings")
        // Personalization (Settings redesign) — comfort + insight tone propagate app-wide via
        // CompositionLocals; smart-capture keys gate the real category-learning + future capture consent.
        private val COMFORT_MODE_KEY = booleanPreferencesKey("comfort_mode")
        private val INSIGHT_TONE_KEY = stringPreferencesKey("insight_tone")
        private val SMART_CAPTURE_KEY = booleanPreferencesKey("smart_capture_enabled")
        private val CATEGORY_LEARNING_KEY = booleanPreferencesKey("category_learning_enabled")
        // Merchants the user asked the app to forget — filtered out of category learning + suggestions.
        private val FORGOTTEN_MERCHANTS_KEY = stringSetPreferencesKey("forgotten_merchants")
        // Global personalization — each propagates app-wide via a CompositionLocal in InsightKuTheme.
        private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
        private val VISUAL_DENSITY_KEY = stringPreferencesKey("visual_density")
        private val HIDE_AMOUNTS_KEY = booleanPreferencesKey("hide_amounts")
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

    /**
     * Ids of Analytics "did you notice?" insights already shown to the user. The Analytics screen
     * surfaces the first *unseen* noticing so observations rotate in gradually over time; once all
     * have been seen the set resets and the cycle begins again.
     */
    val seenNoticings: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[SEEN_NOTICINGS_KEY] ?: emptySet()
    }

    /** Comfort mode — softens animations and density app-wide (propagated via LocalComfortMode). */
    val comfortMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[COMFORT_MODE_KEY] ?: false
    }

    /** Insight tone — "gentle" / "warm" / "direct"; Analytics copy adapts (via LocalInsightTone). */
    val insightTone: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[INSIGHT_TONE_KEY] ?: "warm"
    }

    /** Master consent for smart transaction capture (forward-looking; off by default). */
    val smartCaptureEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SMART_CAPTURE_KEY] ?: false
    }

    /** Gates the real merchant→category learning (suggestions + transparency surface). On by default. */
    val categoryLearningEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CATEGORY_LEARNING_KEY] ?: true
    }

    /** Lowercased merchant names the user asked to forget — excluded from learning + suggestions. */
    val forgottenMerchants: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FORGOTTEN_MERCHANTS_KEY] ?: emptySet()
    }

    /** Brand accent color (hex string) — recolors the app via LocalAccent. Default brand purple. */
    val accentColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACCENT_COLOR_KEY] ?: "#7C4DFF"
    }

    /** Visual density — "comfortable" / "cozy" / "compact" spacing scale. */
    val visualDensity: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[VISUAL_DENSITY_KEY] ?: "comfortable"
    }

    /** Hide amounts — global balance privacy; masks balances on Home + Budgeting. */
    val hideAmounts: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_AMOUNTS_KEY] ?: false
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

    /** Mark an Analytics noticing as seen (idempotent — adds to the stored set). */
    suspend fun markNoticingSeen(id: String) {
        context.dataStore.edit { preferences ->
            preferences[SEEN_NOTICINGS_KEY] = (preferences[SEEN_NOTICINGS_KEY] ?: emptySet()) + id
        }
    }

    /** Clear all seen noticings so the rotation can begin again. */
    suspend fun resetSeenNoticings() {
        context.dataStore.edit { preferences ->
            preferences[SEEN_NOTICINGS_KEY] = emptySet()
        }
    }

    suspend fun setComfortMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[COMFORT_MODE_KEY] = enabled
        }
    }

    suspend fun setInsightTone(tone: String) {
        context.dataStore.edit { preferences ->
            preferences[INSIGHT_TONE_KEY] = tone
        }
    }

    suspend fun setSmartCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_CAPTURE_KEY] = enabled
        }
    }

    suspend fun setCategoryLearningEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CATEGORY_LEARNING_KEY] = enabled
        }
    }

    /** Ask the app to forget a merchant's learned category (idempotent; stored lowercased). */
    suspend fun forgetMerchant(merchant: String) {
        val key = merchant.trim().lowercase()
        if (key.isEmpty()) return
        context.dataStore.edit { preferences ->
            preferences[FORGOTTEN_MERCHANTS_KEY] = (preferences[FORGOTTEN_MERCHANTS_KEY] ?: emptySet()) + key
        }
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { preferences -> preferences[ACCENT_COLOR_KEY] = hex }
    }

    suspend fun setVisualDensity(density: String) {
        context.dataStore.edit { preferences -> preferences[VISUAL_DENSITY_KEY] = density }
    }

    suspend fun setHideAmounts(hidden: Boolean) {
        context.dataStore.edit { preferences -> preferences[HIDE_AMOUNTS_KEY] = hidden }
    }
}
