package com.example.insightku.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance


object AppPalette {
    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    /** App canvas — soft lavender-white in light, deep plum in dark. */
    val background: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF0F0A1E) else Color(0xFFFAF9FE)

    /** Card surface — pure white in light, raised plum in dark. */
    val card: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF1A1030) else Color.White

    /** Nested / secondary surface (chips, inner panels). */
    val cardElevated: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF241840) else Color(0xFFF7F4FE)

    /** Hairline borders and dividers. */
    val cardBorder: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFF2D2050) else Color(0xFFECE7F6)

    val textPrimary: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFFEDE9FE) else Color(0xFF1A1A2E)

    val textMuted: Color @Composable @ReadOnlyComposable get() =
        if (isDark) Color(0xFFAB8FD4) else Color(0xFF9E9E9E)
}
