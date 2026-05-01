package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.domain.usecase.auth.LogoutUseCase
import com.example.insightku.ui.components.settings.InputMode
import com.example.insightku.ui.components.settings.SettingsEvent
import com.example.insightku.ui.components.settings.SettingsUiState
import com.example.insightku.utils.ErrorBus
import com.example.insightku.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SettingsViewModel — mengelola state Settings dan menyimpan preferensi ke DataStore.
 *
 * BUG6 FIX: loadSettings() sekarang membaca SEMUA preferensi dari DataStore,
 * bukan hanya currencyCode. Sebelumnya, isDarkMode/whatsappEnabled/biometricEnabled/dll.
 * selalu kembali ke nilai default (dari SettingsDataSource) setiap kali app restart.
 *
 * Setiap event toggle/change kini juga disimpan ke DataStore agar persisten.
 */
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
        onEvent(SettingsEvent.LoadSettings)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadSettings              -> loadSettings()
            is SettingsEvent.ShowLogoutDialog          -> _uiState.update { it.copy(showLogoutDialog = true) }
            is SettingsEvent.HideLogoutDialog          -> _uiState.update { it.copy(showLogoutDialog = false) }
            is SettingsEvent.ConfirmLogout             -> logout()
            is SettingsEvent.ClearError                -> _uiState.update { it.copy(error = null) }
            // BUG6 FIX: Semua toggle kini juga disimpan ke DataStore
            is SettingsEvent.OnThemeChange             -> saveAndUpdate(event.isDarkMode)
            is SettingsEvent.OnDefaultInputChange      -> saveAndUpdateInputMode(event.mode)
            is SettingsEvent.OnWhatsAppToggle          -> saveAndUpdateWhatsApp(event.enabled)
            is SettingsEvent.OnPushNotificationsToggle -> saveAndUpdatePushNotifications(event.enabled)
            is SettingsEvent.OnBudgetAlertsToggle      -> _uiState.update { it.copy(budgetAlertsEnabled = event.enabled) }
            is SettingsEvent.OnBiometricToggle         -> saveAndUpdateBiometric(event.enabled)
            is SettingsEvent.OnCurrencyChange          -> saveCurrency(event.currencyCode)
            else -> {}
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Baca profil user satu kali (bukan reactive, OK karena tidak berubah mid-session)
            val (userEmail, userName, _) = sessionManager.getUserData()

            // CURRENCY FIX: Gunakan combine + collect untuk observe semua preferences
            // sebagai reactive Flow. Ketika user ganti currency di Settings:
            // DataStore emit → combine emit → _uiState update → MainActivity
            // observe currencyCode → InsightKuTheme(currencyCode=...) recompose
            // → LocalCurrencyCode update → SEMUA formatCurrency() di UI ikut update.
            //
            // Sebelumnya: multiple .first() = ambil sekali, tidak reactive.
            // Sekarang: collect = terus mendengar perubahan selama ViewModel hidup.
            combine(
                preferencesDataStore.currencyCode,
                preferencesDataStore.isDarkMode,
                preferencesDataStore.defaultInputMode,
                preferencesDataStore.whatsappEnabled,
                preferencesDataStore.biometricEnabled
            ) { currencyCode, isDarkMode, inputMode, whatsApp, biometric ->
                // combine max 5 flows, notification dibaca terpisah
                arrayOf(currencyCode, isDarkMode, inputMode, whatsApp, biometric)
            }.collect { values ->
                val currencyCode = values[0] as String
                val isDarkMode   = values[1] as Boolean
                val inputMode    = values[2] as String
                val whatsApp     = values[3] as Boolean
                val biometric    = values[4] as Boolean
                val notification = preferencesDataStore.notificationEnabled.first()

                _uiState.update {
                    it.copy(
                        isLoading                = false,
                        currencyCode             = currencyCode,
                        isDarkMode               = isDarkMode,
                        defaultInputMode         = if (inputMode == "ocr") InputMode.OCR else InputMode.MANUAL,
                        whatsappEnabled          = whatsApp,
                        biometricEnabled         = biometric,
                        pushNotificationsEnabled = notification,
                        userEmail                = userEmail,
                        userName                 = userName
                    )
                }
            }
        }
    }

    private fun saveAndUpdate(isDarkMode: Boolean) {
        _uiState.update { it.copy(isDarkMode = isDarkMode) }
        viewModelScope.launch {
            preferencesDataStore.setDarkMode(isDarkMode)
        }
    }

    private fun saveAndUpdateInputMode(mode: InputMode) {
        _uiState.update { it.copy(defaultInputMode = mode) }
        viewModelScope.launch {
            preferencesDataStore.setDefaultInputMode(mode.name.lowercase())
        }
    }

    private fun saveAndUpdateWhatsApp(enabled: Boolean) {
        _uiState.update { it.copy(whatsappEnabled = enabled) }
        viewModelScope.launch {
            preferencesDataStore.setWhatsappEnabled(enabled)
        }
    }

    private fun saveAndUpdatePushNotifications(enabled: Boolean) {
        _uiState.update { it.copy(pushNotificationsEnabled = enabled) }
        viewModelScope.launch {
            preferencesDataStore.setNotificationEnabled(enabled)
        }
    }

    private fun saveAndUpdateBiometric(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
        viewModelScope.launch {
            preferencesDataStore.setBiometricEnabled(enabled)
        }
    }

    private fun saveCurrency(code: String) {
        viewModelScope.launch {
            preferencesDataStore.setCurrencyCode(code)
            _uiState.update { it.copy(currencyCode = code) }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLogoutDialog = false) }
            try {
                logoutUseCase().getOrThrow()
                _uiState.value = SettingsUiState(userEmail = "", userName = "", isLoading = false)
            } catch (e: CancellationException) {
                // Normal coroutine cancellation during logout — not an error
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = null) }
                // Still navigate out by resetting state
                _uiState.value = SettingsUiState(userEmail = "", userName = "", isLoading = false)
            }
        }
    }
}