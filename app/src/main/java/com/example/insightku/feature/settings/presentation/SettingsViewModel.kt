package com.example.insightku.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.data.local.preferences.SessionManager
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.ui.theme.InsightTone
import com.example.insightku.core.ui.theme.VisualDensity
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.feature.auth.domain.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val errorBus: ErrorBus,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sessionManager: SessionManager
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
            is SettingsEvent.OnBudgetAlertsToggle      -> _uiState.update { it.copy(budgetAlertsEnabled = event.enabled) }
            is SettingsEvent.OnBiometricToggle         -> saveAndUpdateBiometric(event.enabled)
            is SettingsEvent.OnCurrencyChange          -> saveCurrency(event.currencyCode)
            is SettingsEvent.OnLanguageChange          -> saveLanguage(event.language)
            is SettingsEvent.OnComfortModeToggle       -> saveAndUpdateComfortMode(event.enabled)
            is SettingsEvent.OnInsightToneChange       -> saveAndUpdateInsightTone(event.tone)
            is SettingsEvent.OnAccentChange            -> saveAndUpdateAccent(event.hex)
            is SettingsEvent.OnVisualDensityChange     -> saveAndUpdateVisualDensity(event.density)
            is SettingsEvent.OnHideAmountsToggle       -> saveAndUpdateHideAmounts(event.hidden)
            is SettingsEvent.OnHabitGoalChange         -> _uiState.update { it.copy(habitGoal = event.goal) }
            is SettingsEvent.OnSmartCaptureToggle      -> _uiState.update { it.copy(smartCaptureEnabled = event.enabled) }
            is SettingsEvent.OnCategoryLearningToggle  -> _uiState.update { it.copy(categoryLearningEnabled = event.enabled) }
            is SettingsEvent.OnForgetMemory            -> { /* TODO: remove from DataStore */ }
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
            ) { values -> values }
                .collect { values ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
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
                        )
                    }
                }
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
            } catch (e: Exception) {
                _uiState.value = SettingsUiState(isLoading = false)
            }
        }
    }
}
