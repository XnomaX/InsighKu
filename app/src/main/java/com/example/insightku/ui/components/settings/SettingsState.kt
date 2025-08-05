package com.example.insightku.ui.components.settings

data class SettingsState(
    val userEmail: String = "",
    val userName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showLogoutDialog: Boolean = false,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val currencyCode: String = "IDR"
)
