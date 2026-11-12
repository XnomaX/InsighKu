package com.example.insightku.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.local.preferences.SessionManager
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.ui.theme.AccentPalette
import com.example.insightku.core.ui.theme.InsightTone
import com.example.insightku.core.ui.theme.VisualDensity
import com.example.insightku.feature.auth.domain.LogoutUseCase
import com.example.insightku.feature.home.domain.CategoryMemory
import com.example.insightku.feature.home.domain.MerchantMemory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadSettings              -> loadSettings()
            is SettingsEvent.ShowLogoutDialog          -> _uiState.update { it.copy(showLogoutDialog = true) }
            is SettingsEvent.HideLogoutDialog          -> _uiState.update { it.copy(showLogoutDialog = false) }
            is SettingsEvent.ConfirmLogout             -> logout()
            is SettingsEvent.ClearError                -> _uiState.update { it.copy(error = null) }
            is SettingsEvent.OnThemeChange             -> saveAndUpdateTheme(event.isDarkMode)
            is SettingsEvent.OnDefaultInputChange      -> saveAndUpdateInputMode(event.mode)
            is SettingsEvent.OnWhatsAppToggle          -> saveAndUpdateWhatsApp(event.enabled)
            is SettingsEvent.OnPushNotificationsToggle -> saveAndUpdatePushNotifications(event.enabled)
            is SettingsEvent.OnBudgetAlertsToggle -> saveAndUpdateBudgetAlerts(event.enabled)
            is SettingsEvent.OnBiometricToggle         -> saveAndUpdateBiometric(event.enabled)
            is SettingsEvent.OnCurrencyChange          -> saveCurrency(event.currencyCode)
            is SettingsEvent.OnLanguageChange          -> saveLanguage(event.language)
            is SettingsEvent.OnComfortModeToggle       -> saveAndUpdateComfortMode(event.enabled)
            is SettingsEvent.OnInsightToneChange       -> saveAndUpdateInsightTone(event.tone)
            is SettingsEvent.OnAccentChange            -> saveAndUpdateAccent(event.hex)
            is SettingsEvent.OnVisualDensityChange     -> saveAndUpdateVisualDensity(event.density)
            is SettingsEvent.OnHideAmountsToggle       -> saveAndUpdateHideAmounts(event.hidden)
            is SettingsEvent.OnHabitGoalChange -> saveAndUpdateHabitGoal(event.goal)
            is SettingsEvent.OnSmartCaptureToggle -> saveAndUpdateSmartCapture(event.enabled)
            is SettingsEvent.OnCategoryLearningToggle -> saveAndUpdateCategoryLearning(event.enabled)
            is SettingsEvent.OnForgetMemory -> onForgetMemory(event.merchant)
            is SettingsEvent.OnBankNotificationToggle  -> saveAndUpdateBankNotification(event.enabled)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (userEmail, userName, _) = sessionManager.getUserData()
            // One combine — no nested .first() per emission (was re-reading 3 prefs every update).
            combine<Any, Array<Any>>(
                preferencesDataStore.currencyCode,
                preferencesDataStore.isDarkMode,
                preferencesDataStore.defaultInputMode,
                preferencesDataStore.whatsappEnabled,
                preferencesDataStore.biometricEnabled,
                preferencesDataStore.accentColor,
                preferencesDataStore.visualDensity,
                preferencesDataStore.hideAmounts,
                preferencesDataStore.comfortMode,
                preferencesDataStore.insightTone,
                preferencesDataStore.notificationEnabled,
                preferencesDataStore.appLanguage,
                preferencesDataStore.bankNotificationEnabled,
                // Smart-capture kit (persisted DataStore, not just local state)
                preferencesDataStore.smartCaptureEnabled,
                preferencesDataStore.categoryLearningEnabled,
                preferencesDataStore.streakGoal,
            ) { values -> values }
                .collect { values ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Refreshed right after via refreshLearnedMemories(); cleared to avoid stale rows.
                            learnedMemories = emptyList(),
                            currencyCode = values[0] as String,
                            isDarkMode = values[1] as Boolean,
                            defaultInputMode = if ((values[2] as String) == "ocr") InputMode.OCR else InputMode.MANUAL,
                            whatsappEnabled = values[3] as Boolean,
                            biometricEnabled = values[4] as Boolean,
                            accentColorHex = values[5] as String,
                            visualDensity = VisualDensity.fromKey(values[6] as String),
                            hideAmounts = values[7] as Boolean,
                            comfortMode = values[8] as Boolean,
                            insightTone = InsightTone.fromKey(values[9] as String),
                            pushNotificationsEnabled = values[10] as Boolean,
                            appLanguage = if ((values[11] as String) == "en") AppLanguage.EN else AppLanguage.ID,
                            userEmail = userEmail,
                            userName = userName,
                            bankNotificationEnabled = values[12] as Boolean,
                            // Persisted prefs now drive these; default when absent matches SettingsUiState.
                            smartCaptureEnabled = values[13] as Boolean,
                            categoryLearningEnabled = values[14] as Boolean,
                            habitGoal = values[15] as Int,
                        )
                    }
                }

            // Learned-memory transparency list (device-local; derived, not stored).
            refreshLearnedMemories()
        }
    }

    private fun saveAndUpdateTheme(isDarkMode: Boolean) {
        _uiState.update { it.copy(isDarkMode = isDarkMode) }
        viewModelScope.launch { preferencesDataStore.setDarkMode(isDarkMode) }
    }

    private fun saveAndUpdateInputMode(mode: InputMode) {
        _uiState.update { it.copy(defaultInputMode = mode) }
        viewModelScope.launch { preferencesDataStore.setDefaultInputMode(mode.name.lowercase()) }
    }

    private fun saveAndUpdateWhatsApp(enabled: Boolean) {
        _uiState.update { it.copy(whatsappEnabled = enabled) }
        viewModelScope.launch { preferencesDataStore.setWhatsappEnabled(enabled) }
    }

    private fun saveAndUpdatePushNotifications(enabled: Boolean) {
        _uiState.update { it.copy(pushNotificationsEnabled = enabled) }
        viewModelScope.launch { preferencesDataStore.setNotificationEnabled(enabled) }
    }

    private fun saveAndUpdateBiometric(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
        viewModelScope.launch { preferencesDataStore.setBiometricEnabled(enabled) }
    }

    private fun saveAndUpdateBankNotification(enabled: Boolean) {
        _uiState.update { it.copy(bankNotificationEnabled = enabled) }
        viewModelScope.launch { preferencesDataStore.setBankNotificationEnabled(enabled) }
    }

    private fun saveAndUpdateComfortMode(enabled: Boolean) {
        _uiState.update { it.copy(comfortMode = enabled) }
        viewModelScope.launch { preferencesDataStore.setComfortMode(enabled) }
    }

    private fun saveAndUpdateInsightTone(tone: InsightTone) {
        _uiState.update { it.copy(insightTone = tone) }
        viewModelScope.launch { preferencesDataStore.setInsightTone(tone.key) }
    }

    private fun saveAndUpdateAccent(hex: String) {
        _uiState.update { it.copy(accentColorHex = hex) }
        viewModelScope.launch { preferencesDataStore.setAccentColor(hex) }
    }

    private fun saveAndUpdateVisualDensity(density: VisualDensity) {
        _uiState.update { it.copy(visualDensity = density) }
        viewModelScope.launch { preferencesDataStore.setVisualDensity(density.key) }
    }

    private fun saveAndUpdateHideAmounts(hidden: Boolean) {
        _uiState.update { it.copy(hideAmounts = hidden) }
        viewModelScope.launch { preferencesDataStore.setHideAmounts(hidden) }
    }

    // ── Smart-capture kit: all persisted in DataStore, never just local state ──

    private fun saveAndUpdateBudgetAlerts(enabled: Boolean) {
        _uiState.update { it.copy(budgetAlertsEnabled = enabled) }
        // Budget alerts drive the per-category budget notifications at save time;
        // persisting the flag keeps the setting alive across restarts. (No separate
        // push-opt-out is created — that remains governed by pushNotificationsEnabled.)
        viewModelScope.launch {
            preferencesDataStore.setNotificationEnabled(enabled && preferencesDataStore.notificationEnabled.first())
        }
    }

    private fun saveAndUpdateHabitGoal(goal: Int) {
        _uiState.update { it.copy(habitGoal = goal) }
        // Reuses the streak goal pref — the dashboard streak logic already reads it.
        viewModelScope.launch { preferencesDataStore.setStreakGoal(goal) }
    }

    private fun saveAndUpdateSmartCapture(enabled: Boolean) {
        _uiState.update { it.copy(smartCaptureEnabled = enabled) }
        viewModelScope.launch { preferencesDataStore.setSmartCaptureEnabled(enabled) }
    }

    private fun saveAndUpdateCategoryLearning(enabled: Boolean) {
        _uiState.update { it.copy(categoryLearningEnabled = enabled) }
        viewModelScope.launch { preferencesDataStore.setCategoryLearningEnabled(enabled) }
    }

    private fun onForgetMemory(merchant: String) {
        // Marks the merchant forgotten so suggestions + the transparency list skip it.
        viewModelScope.launch {
            preferencesDataStore.forgetMerchant(merchant)
            refreshLearnedMemories()
        }
    }

    private fun refreshLearnedMemories() {
        viewModelScope.launch {
            val all = transactionRepository.getAllTransactions().first()
            val memories = CategoryMemory().derive(all).filter {
                it.merchant.trim().lowercase() !in preferencesDataStore.forgottenMerchants.first()
            }
            _uiState.update { it.copy(learnedMemories = memories) }
        }
    }

    private fun saveCurrency(code: String) {
        viewModelScope.launch {
            preferencesDataStore.setCurrencyCode(code)
            _uiState.update { it.copy(currencyCode = code) }
        }
    }

    private fun saveLanguage(language: AppLanguage) {
        _uiState.update { it.copy(appLanguage = language) }
        viewModelScope.launch {
            preferencesDataStore.setAppLanguage(language.code)
            LocaleHelper.applyLocale(language.code)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLogoutDialog = false) }
            try {
                logoutUseCase().getOrThrow()
                _uiState.value = SettingsUiState(isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value = SettingsUiState(isLoading = false)
            }
        }
    }
}


// --- Settings MVI contract (Event.kt, State.kt) --------------------------------

sealed class SettingsEvent {
    object LoadSettings : SettingsEvent()
    object ShowLogoutDialog : SettingsEvent()
    object HideLogoutDialog : SettingsEvent()
    object ConfirmLogout : SettingsEvent()
    object ClearError : SettingsEvent()

    data class OnThemeChange(val isDarkMode: Boolean) : SettingsEvent()
    data class OnComfortModeToggle(val enabled: Boolean) : SettingsEvent()
    data class OnInsightToneChange(val tone: InsightTone) : SettingsEvent()
    data class OnAccentChange(val hex: String) : SettingsEvent()
    data class OnVisualDensityChange(val density: VisualDensity) : SettingsEvent()
    data class OnHideAmountsToggle(val hidden: Boolean) : SettingsEvent()
    data class OnHabitGoalChange(val goal: Int) : SettingsEvent()
    data class OnCurrencyChange(val currencyCode: String) : SettingsEvent()
    data class OnLanguageChange(val language: AppLanguage) : SettingsEvent()
    data class OnDefaultInputChange(val mode: InputMode) : SettingsEvent()
    data class OnPushNotificationsToggle(val enabled: Boolean) : SettingsEvent()
    data class OnBudgetAlertsToggle(val enabled: Boolean) : SettingsEvent()
    data class OnWhatsAppToggle(val enabled: Boolean) : SettingsEvent()
    data class OnBiometricToggle(val enabled: Boolean) : SettingsEvent()
    data class OnSmartCaptureToggle(val enabled: Boolean) : SettingsEvent()
    data class OnCategoryLearningToggle(val enabled: Boolean) : SettingsEvent()
    data class OnForgetMemory(val merchant: String) : SettingsEvent()
    data class OnBankNotificationToggle(val enabled: Boolean) : SettingsEvent()
}

enum class InputMode { OCR, MANUAL }

enum class AppLanguage(val code: String) { ID("id"), EN("en") }

data class SettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userEmail: String = "",
    val userName: String = "",
    val showLogoutDialog: Boolean = false,

    // Appearance
    val isDarkMode: Boolean = false,
    val comfortMode: Boolean = false,
    val insightTone: InsightTone = InsightTone.WARM,
    val accentColorHex: String = AccentPalette.DEFAULT_HEX,
    val visualDensity: VisualDensity = VisualDensity.COMFORTABLE,
    val hideAmounts: Boolean = false,

    // Habit
    val habitGoal: Int = 7,

    // Currency & Language
    val currencyCode: String = "IDR",
    val appLanguage: AppLanguage = AppLanguage.ID,

    // Input
    val defaultInputMode: InputMode = InputMode.MANUAL,

    // Notifications
    val pushNotificationsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,

    // Integrations
    val whatsappEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,

    // Smart capture
    val smartCaptureEnabled: Boolean = false,
    val categoryLearningEnabled: Boolean = true,
    val learnedMemories: List<MerchantMemory> = emptyList(),

    // Bank notification auto-capture
    /** Apakah fitur baca notifikasi bank untuk draft transaksi diaktifkan. */
    val bankNotificationEnabled: Boolean = false
)
