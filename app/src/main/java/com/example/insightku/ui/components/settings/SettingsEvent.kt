package com.example.insightku.ui.components.settings

sealed class SettingsEvent {
    object LoadUserData : SettingsEvent()
    object ShowLogoutDialog : SettingsEvent()
    object HideLogoutDialog : SettingsEvent()
    object ConfirmLogout : SettingsEvent()
    data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent()
    data class ToggleNotifications(val enabled: Boolean) : SettingsEvent()
    data class ToggleBiometric(val enabled: Boolean) : SettingsEvent()
    data class ToggleAutoBackup(val enabled: Boolean) : SettingsEvent()
    data class ChangeCurrency(val currencyCode: String) : SettingsEvent()
    object ClearError : SettingsEvent()
}
