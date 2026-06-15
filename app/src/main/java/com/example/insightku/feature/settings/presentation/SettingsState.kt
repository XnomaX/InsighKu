
package com.example.insightku.feature.settings.presentation

import androidx.compose.ui.graphics.Color
import com.example.insightku.feature.home.domain.MerchantMemory
import com.example.insightku.core.ui.theme.InsightTone
import com.example.insightku.core.ui.theme.VisualDensity

enum class InputMode { OCR, MANUAL }

enum class AppLanguage(val code: String) { ID("id"), EN("en") }

data class SettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userEmail: String = "",
    val userName: String = "",
    val showLogoutDialog: Boolean = false,

    // Appearance
    val isDarkMode: Boolean = false,
    val comfortMode: Boolean = false,
    val insightTone: InsightTone = InsightTone.WARM,
    val accentColor: Color = Color(0xFF7C4DFF),
    val visualDensity: VisualDensity = VisualDensity.COMFORTABLE,
    val hideAmounts: Boolean = false,

    // Habit
    val habitGoal: Int = 7,

    // Currency & Language
    val currencyCode: String = "IDR",
    val appLanguage: AppLanguage = AppLanguage.ID,

    // Input
    val defaultInputMode: InputMode = InputMode.MANUAL,

    // Notifications
    val pushNotificationsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,

    // Integrations
    val whatsappEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,

    // Smart capture
    val smartCaptureEnabled: Boolean = false,
    val categoryLearningEnabled: Boolean = true,
    val learnedMemories: List<MerchantMemory> = emptyList(),

    // Bank notification auto-capture
    /** Apakah fitur baca notifikasi bank untuk draft transaksi diaktifkan. */
    val bankNotificationEnabled: Boolean = false
)


