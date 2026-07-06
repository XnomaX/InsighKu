package com.example.insightku.feature.settings.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.insightku.core.ui.theme.LocalAccent

/**
 * Theme-aware palette for the Settings screen — same approach as `AnalyticsPalette`.
 *
 * Settings needs dark-mode support AND must honor the design-system rule of fixed-hex tokens (not
 * `MaterialTheme.colorScheme` for screen UI). This resolves the tension the old screen got wrong:
 * darkness is read from the **resolved** `MaterialTheme.colorScheme` (the app drives its theme from
 * DataStore in `MainActivity`, NOT the system), so we must NOT call `isSystemInDarkTheme()`.
 *
 * Light values match the app design system (bg `0xFFFAF9FE`, white cards, `0xFF1A1A2E` text); brand
 * accents are theme-invariant.
 */
object SettingsPalette {
    /** Brand accent — now driven by the global [LocalAccent] so the accent picker recolors Settings. */
    val Purple: Color
        @Composable @ReadOnlyComposable
        get() = LocalAccent.current
    val IncomeGreen: Color = com.example.insightku.core.ui.theme.AppPalette.success
    val ExpenseRed: Color = com.example.insightku.core.ui.theme.AppPalette.error

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val background: Color @Composable @ReadOnlyComposable get() =
        com.example.insightku.core.ui.theme.AppPalette.background

    val card: Color @Composable @ReadOnlyComposable get() =
        com.example.insightku.core.ui.theme.AppPalette.card

    /** A second elevation layer for nested panels (previews, learning rows). */
    val cardElevated: Color @Composable @ReadOnlyComposable get() =
        com.example.insightku.core.ui.theme.AppPalette.cardElevated

    val cardBorder: Color @Composable @ReadOnlyComposable get() =
        com.example.insightku.core.ui.theme.AppPalette.cardBorder

    val textPrimary: Color @Composable @ReadOnlyComposable get() =
        com.example.insightku.core.ui.theme.AppPalette.textPrimary

    val textMuted: Color @Composable @ReadOnlyComposable get() =
        com.example.insightku.core.ui.theme.AppPalette.textMuted

    /** Soft fill behind icon boxes / chips — the accent at low alpha. */
    fun tint(accent: Color): Color = accent.copy(alpha = 0.12f)
}


