package com.example.insightku.feature.home.presentation.dashboard

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.insightku.R
import com.example.insightku.core.ui.theme.LocalAccent

internal fun String.firstName(): String = split(" ").firstOrNull()?.ifBlank { this } ?: this

@StringRes
internal fun getContextualSubtitleRes(savings: Double, streak: Int): Int = when {
    streak >= 30  -> R.string.dashboard_subtitle_remarkable_streak
    streak >= 7   -> R.string.dashboard_subtitle_building_habits
    savings > 0   -> R.string.dashboard_subtitle_savings_on_track
    else          -> R.string.dashboard_subtitle_small_steps
}

// NavPurple — matches bottom nav primary action color exactly. Now driven by the global accent
// (LocalAccent) so the accent picker recolors the dashboard live. Composable getter — used only in
// composable scope (modifiers/tints), never inside a Canvas DrawScope.
internal val NavPurple: Color
    @Composable get() = LocalAccent.current
