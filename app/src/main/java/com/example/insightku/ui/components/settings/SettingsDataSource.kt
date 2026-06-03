
package com.example.insightku.ui.components.settings

/**
 * Provides default or dummy data for the Settings screen.
 * In a real app, this would interact with a preferences repository (DataStore/SharedPreferences).
 */
object SettingsDataSource {

    fun getDefaultSettings(): SettingsUiState {
        return SettingsUiState(
            isLoading = false,
            userEmail = "user@insightku.com",
            userName = "Andi",
            isDarkMode = false,
            defaultInputMode = InputMode.MANUAL,
            pushNotificationsEnabled = true
        )
    }
}
