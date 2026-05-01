
package com.example.insightku.ui.components.settings

enum class InputMode {
    OCR,
    MANUAL
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userEmail: String = "",
    val userName: String = "",
    val showLogoutDialog: Boolean = false,

    // Appearance
    val isDarkMode: Boolean = false,

    // Currency
    val currencyCode: String = "IDR",

    // Transaction Input
    val defaultInputMode: InputMode = InputMode.MANUAL,

    // WhatsApp Integration
    val whatsappEnabled: Boolean = false,

    // Notifications
    val pushNotificationsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,

    // Security
    val biometricEnabled: Boolean = false
)
