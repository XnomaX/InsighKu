package com.example.insightku.feature.analytics.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent

/**
 * Theme-aware palette for the Analytics screen.
 *
 * Delegates all color decisions to the central [AppPalette]. This object is kept for backward
 * compatibility with existing Analytics composables that reference `AnalyticsPalette.xxx`.
 */
object AnalyticsPalette {
    // Brand accents — fixed across themes
    /** Brand accent — driven by the global [LocalAccent] so the accent picker recolors Analytics. */
    val Purple: Color
        @Composable @ReadOnlyComposable
        get() = LocalAccent.current
    val IncomeGreen @Composable get() = AppPalette.success
    val ExpenseRed @Composable get() = AppPalette.error

    val background: Color @Composable get() = AppPalette.background
    val card: Color @Composable get() = AppPalette.card
    val cardElevated: Color @Composable get() = AppPalette.cardElevated
    val cardBorder: Color @Composable get() = AppPalette.cardBorder
    val textPrimary: Color @Composable get() = AppPalette.textPrimary
    val textMuted: Color @Composable get() = AppPalette.textMuted

    /** Soft fill behind icon boxes / chips — the section accent at low alpha. */
    fun tint(accent: Color): Color = accent.copy(alpha = 0.12f)
}


