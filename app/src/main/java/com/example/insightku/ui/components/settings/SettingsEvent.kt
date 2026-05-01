package com.example.insightku.ui.components.settings

import com.example.insightku.ui.components.settings.InputMode

sealed class SettingsEvent {
    object LoadSettings : SettingsEvent()

    // Dialogs and Errors
    object ShowLogoutDialog : SettingsEvent()
    object HideLogoutDialog : SettingsEvent()
    object ConfirmLogout : SettingsEvent()
    object ClearError : SettingsEvent()

    // Appearance
    data class OnThemeChange(val isDarkMode: Boolean) : SettingsEvent()

    // Currency
    data class OnCurrencyChange(val currencyCode: String) : SettingsEvent()

    // Transaction Input
    data class OnDefaultInputChange(val mode: InputMode) : SettingsEvent()

    // WhatsApp Integration
    data class OnWhatsAppToggle(val enabled: Boolean) : SettingsEvent()
    object OnWhatsAppSetup : SettingsEvent()

    // Notifications
    data class OnPushNotificationsToggle(val enabled: Boolean) : SettingsEvent()
    data class OnBudgetAlertsToggle(val enabled: Boolean) : SettingsEvent()

    // Security
    data class OnBiometricToggle(val enabled: Boolean) : SettingsEvent()
}