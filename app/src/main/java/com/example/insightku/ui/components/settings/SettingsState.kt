
package com.example.insightku.ui.components.settings

import androidx.compose.ui.graphics.Color
import com.example.insightku.domain.usecase.learning.MerchantMemory
import com.example.insightku.ui.theme.InsightTone
import com.example.insightku.ui.theme.VisualDensity

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

    // Comfort — softens animations/density app-wide (real, via LocalComfortMode)
    val comfortMode: Boolean = false,

    // How insights talk to you — adapts reflective copy app-wide (real, via LocalInsightTone)
    val insightTone: InsightTone = InsightTone.WARM,

    // Global appearance/behavior personalization (each propagates via a CompositionLocal)
    val accentColor: Color = Color(0xFF7C4DFF),
    val visualDensity: VisualDensity = VisualDensity.COMFORTABLE,
    /** Global balance privacy — masks amounts on Home + Budgeting. */
    val hideAmounts: Boolean = false,

    // Habit goal — surfaces the previously-hidden streakGoal preference
    val habitGoal: Int = 7,

    // Currency
    val currencyCode: String = "IDR",

    // Transaction Input
    val defaultInputMode: InputMode = InputMode.MANUAL,

    // Notifications
    val pushNotificationsEnabled: Boolean = true,

    // Smart capture — honest consent framing
    val smartCaptureEnabled: Boolean = false,
    /** Gates the REAL merchant→category learning (suggestions + transparency below). */
    val categoryLearningEnabled: Boolean = true,
    /** What the app has genuinely learned from the user's own transactions (transparency surface). */
    val learnedMemories: List<MerchantMemory> = emptyList()
)
