package com.example.insightku.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.ui.components.settings.SettingsEvent
import com.example.insightku.ui.components.settings.SettingsState
import com.example.insightku.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private var _uiState = mutableStateOf(SettingsState())
    val uiState: SettingsState by _uiState

    init {
        loadUserData()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadUserData -> {
                loadUserData()
            }
            is SettingsEvent.ShowLogoutDialog -> {
                _uiState.value = _uiState.value.copy(showLogoutDialog = true)
            }
            is SettingsEvent.HideLogoutDialog -> {
                _uiState.value = _uiState.value.copy(showLogoutDialog = false)
            }
            is SettingsEvent.ConfirmLogout -> {
                logout()
            }
            is SettingsEvent.ToggleDarkMode -> {
                _uiState.value = _uiState.value.copy(isDarkMode = event.enabled)
                // TODO: Save to preferences
            }
            is SettingsEvent.ToggleNotifications -> {
                _uiState.value = _uiState.value.copy(notificationsEnabled = event.enabled)
                // TODO: Save to preferences
            }
            is SettingsEvent.ToggleBiometric -> {
                _uiState.value = _uiState.value.copy(biometricEnabled = event.enabled)
                // TODO: Save to preferences
            }
            is SettingsEvent.ToggleAutoBackup -> {
                _uiState.value = _uiState.value.copy(autoBackupEnabled = event.enabled)
                // TODO: Save to preferences
            }
            is SettingsEvent.ChangeCurrency -> {
                _uiState.value = _uiState.value.copy(currencyCode = event.currencyCode)
                // TODO: Save to preferences
            }
            is SettingsEvent.ClearError -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val (email, name, _) = sessionManager.getUserData()
                _uiState.value = _uiState.value.copy(
                    userEmail = email,
                    userName = name,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load user data: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                sessionManager.clearSession()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showLogoutDialog = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Logout failed: ${e.message}",
                    isLoading = false,
                    showLogoutDialog = false
                )
            }
        }
    }
}
