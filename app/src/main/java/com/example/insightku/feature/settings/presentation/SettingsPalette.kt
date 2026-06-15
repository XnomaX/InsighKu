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
    val IncomeGreen = Color(0xFF10B981)
    val ExpenseRed = Color(0xFFEF4444)

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val background: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF0F0A1E) else Color(0xFFFAF9FE)

    val card: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF1A1030) else Color.White

    /** A second elevation layer for nested panels (previews, learning rows). */
    val cardElevated: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF241840) else Color(0xFFF7F4FE)

    val cardBorder: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF2D2050) else Color(0xFFECE7F6)

    val textPrimary: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFFEDE9FE) else Color(0xFF1A1A2E)

    val textMuted: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFFAB8FD4) else Color(0xFF9E9E9E)

    /** Soft fill behind icon boxes / chips — the accent at low alpha. */
    fun tint(accent: Color): Color = accent.copy(alpha = 0.12f)
}


