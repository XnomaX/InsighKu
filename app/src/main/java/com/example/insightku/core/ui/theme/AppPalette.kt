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

    // ── Semantic UI colors (shared across screens) ────────────────────────

    /** Brand accent — purple. */
    val accent: Color = Color(0xFF7C4DFF)
    /** Primary brand color (dark purple). */
    val primary: Color = Color(0xFF5A2A82)
    /** Success / positive indicator. */
    val success: Color = Color(0xFF10B981)
    /** Error / danger indicator. */
    val error: Color = Color(0xFFEF4444)
    /** Warning / amber indicator. */
    val warning: Color = Color(0xFFF59E0B)
    /** Cyan accent (travel, dental, water). */
    val cyan: Color = Color(0xFF06B6D4)
    /** Neutral gray (housing, taxes). */
    val gray: Color = Color(0xFF6B7280)
    /** Indigo (parking, photography, service). */
    val indigo: Color = Color(0xFF6366F1)
    /** Muted dialog text. */
    val textDialogMuted: Color = Color(0xFF6B6B8A)
    /** Placeholder / hint text. */
    val placeholder: Color = Color(0xFFBDBDBD)
    /** Delete action red. */
    val deleteRed: Color = Color(0xFFE57373)
    /** Delete action background. */
    val deleteBg: Color = Color(0xFFFFF5F5)
    /** Success chip background. */
    val successChipBg: Color = Color(0xFF064E3B)
    /** Error chip background. */
    val errorChipBg: Color = Color(0xFF450A0A)

    // Semantic info-tile colors
    val notesPurple: Color = Color(0xFF8B5CF6)
    val locationPink: Color = Color(0xFFEC4899)
    val defaultBlue: Color = Color(0xFF3B82F6)

    // Goal feature colors
    val GoalColors = listOf(
        "#7C3AED", // Purple
        "#EC4899", // Pink
        "#EF4444", // Red
        "#F59E0B", // Orange
        "#10B981", // Green
        "#06B6D4", // Cyan
        "#3B82F6", // Blue
        "#8B5CF6", // Violet
        "#6366F1", // Indigo
        "#14B8A6", // Teal
        "#F97316", // Deep Orange
        "#84CC16"  // Lime
    )
}
