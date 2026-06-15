package com.example.insightku.feature.analytics.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.insightku.core.ui.theme.LocalAccent

/**
 * Theme-aware palette for the Analytics screen.
 *
 * Analytics is a documented exception (decided 2026-05-31) to the design-system rule against using
 * theme-driven colors for screen UI: it adapts light/dark. In **light** mode the values match the
 * design-system palette (airy `0xFFFAF9FE` background, white cards, `0xFF1A1A2E` text). Brand accents
 * (purple/green/red) are theme-invariant.
 *
 * Darkness is derived from the **resolved** `MaterialTheme.colorScheme` (the app drives its theme from
 * its own DataStore setting in `MainActivity`, NOT the system setting) — so we must NOT call
 * `isSystemInDarkTheme()` here, or Analytics would render dark while the rest of the app is light.
 */
object AnalyticsPalette {
    // Brand accents — fixed across themes
    /** Brand accent — driven by the global [LocalAccent] so the accent picker recolors Analytics. */
    val Purple: Color
        @Composable @ReadOnlyComposable
        get() = LocalAccent.current
    val IncomeGreen = Color(0xFF10B981)
    val ExpenseRed = Color(0xFFEF4444)

    /** True when the active Material theme is dark — read from the resolved scheme, not the system. */
    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    /** App canvas — soft lavender-white in light, deep plum in dark. */
    val background: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF0F0A1E) else Color(0xFFFAF9FE)

    /** Card surface — pure white in light (airy), subtle raised plum in dark. */
    val card: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF1A1030) else Color.White

    /** A second elevation layer for nested panels (tap-reveal detail), kept very subtle. */
    val cardElevated: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF241840) else Color(0xFFF7F4FE)

    val cardBorder: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF2D2050) else Color(0xFFECE7F6)

    val textPrimary: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFFEDE9FE) else Color(0xFF1A1A2E)

    val textMuted: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFFAB8FD4) else Color(0xFF9E9E9E)

    /** Soft fill behind icon boxes / chips — the section accent at low alpha. */
    fun tint(accent: Color): Color = accent.copy(alpha = 0.12f)
}


