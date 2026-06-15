package com.example.insightku.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.datastore.SessionManager
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.feature.auth.domain.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
            is SettingsEvent.OnComfortModeToggle       -> _uiState.update { it.copy(comfortMode = event.enabled) }
            is SettingsEvent.OnInsightToneChange       -> _uiState.update { it.copy(insightTone = event.tone) }
            is SettingsEvent.OnAccentChange            -> _uiState.update { it.copy(accentColor = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(event.hex))) }
            is SettingsEvent.OnVisualDensityChange     -> _uiState.update { it.copy(visualDensity = event.density) }
            is SettingsEvent.OnHideAmountsToggle       -> _uiState.update { it.copy(hideAmounts = event.hidden) }
            is SettingsEvent.OnHabitGoalChange         -> _uiState.update { it.copy(habitGoal = event.goal) }
            is SettingsEvent.OnSmartCaptureToggle      -> _uiState.update { it.copy(smartCaptureEnabled = event.enabled) }
            is SettingsEvent.OnCategoryLearningToggle  -> _uiState.update { it.copy(categoryLearningEnabled = event.enabled) }
            is SettingsEvent.OnForgetMemory            -> { /* TODO: remove from DataStore */ }
            is SettingsEvent.OnBankNotificationToggle  -> saveAndUpdateBankNotification(event.enabled)
            else                                       -> {}
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (userEmail, userName, _) = sessionManager.getUserData()
            combine(
                preferencesDataStore.currencyCode,
                preferencesDataStore.isDarkMode,
                preferencesDataStore.defaultInputMode,
                preferencesDataStore.whatsappEnabled,
                preferencesDataStore.biometricEnabled
            ) { cur, dark, input, wa, bio -> arrayOf(cur, dark, input, wa, bio) }
                .collect { values ->
                    val notification        = preferencesDataStore.notificationEnabled.first()
                    val langCode            = preferencesDataStore.appLanguage.first()
                    val bankNotifEnabled    = preferencesDataStore.bankNotificationEnabled.first()
                    _uiState.update {
                        it.copy(
                            isLoading                = false,
                            currencyCode             = values[0] as String,
                            isDarkMode               = values[1] as Boolean,
                            defaultInputMode         = if ((values[2] as String) == "ocr") InputMode.OCR else InputMode.MANUAL,
                            whatsappEnabled          = values[3] as Boolean,
                            biometricEnabled         = values[4] as Boolean,
                            pushNotificationsEnabled = notification,
                            appLanguage              = if (langCode == "en") AppLanguage.EN else AppLanguage.ID,
                            userEmail                = userEmail,
                            userName                 = userName,
                            bankNotificationEnabled  = bankNotifEnabled
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

    private fun saveCurrency(code: String) {
        viewModelScope.launch {
            preferencesDataStore.setCurrencyCode(code)
            _uiState.update { it.copy(currencyCode = code) }
        }
    }

    private fun saveLanguage(language: AppLanguage) {
        _uiState.update { it.copy(appLanguage = language) }
        viewModelScope.launch { preferencesDataStore.setAppLanguage(language.code) }
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
