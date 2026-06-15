package com.example.insightku.feature.settings.presentation

import com.example.insightku.core.ui.theme.InsightTone
import com.example.insightku.core.ui.theme.VisualDensity

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
