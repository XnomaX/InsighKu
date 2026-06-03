package com.example.insightku.ui.components.settings

import com.example.insightku.ui.components.settings.InputMode
import com.example.insightku.ui.theme.InsightTone
import com.example.insightku.ui.theme.VisualDensity

sealed class SettingsEvent {
    object LoadSettings : SettingsEvent()

    // Dialogs and Errors
    object ShowLogoutDialog : SettingsEvent()
    object HideLogoutDialog : SettingsEvent()
    object ConfirmLogout : SettingsEvent()
    object ClearError : SettingsEvent()

    // Appearance
    data class OnThemeChange(val isDarkMode: Boolean) : SettingsEvent()

    // Comfort — softens animations/density app-wide
    data class OnComfortModeToggle(val enabled: Boolean) : SettingsEvent()

    // Insight tone — adapts reflective copy app-wide
    data class OnInsightToneChange(val tone: InsightTone) : SettingsEvent()

    // Global personalization controls
    data class OnAccentChange(val hex: String) : SettingsEvent()
    data class OnVisualDensityChange(val density: VisualDensity) : SettingsEvent()
    data class OnHideAmountsToggle(val hidden: Boolean) : SettingsEvent()

    // Habit goal — friendly stepper over the (previously hidden) streakGoal preference
    data class OnHabitGoalChange(val goal: Int) : SettingsEvent()

    // Currency
    data class OnCurrencyChange(val currencyCode: String) : SettingsEvent()

    // Transaction Input
    data class OnDefaultInputChange(val mode: InputMode) : SettingsEvent()

    // Notifications
    data class OnPushNotificationsToggle(val enabled: Boolean) : SettingsEvent()

    // Smart capture — honest consent
    data class OnSmartCaptureToggle(val enabled: Boolean) : SettingsEvent()
    data class OnCategoryLearningToggle(val enabled: Boolean) : SettingsEvent()
    /** Forget one learned merchant→category memory (user control over what's learned). */
    data class OnForgetMemory(val merchant: String) : SettingsEvent()
}
