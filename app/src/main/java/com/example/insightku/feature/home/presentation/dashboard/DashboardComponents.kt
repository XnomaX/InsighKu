package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.insightku.core.ui.theme.LocalAccent
import java.util.Calendar

internal fun getTimeGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..11  -> "Good morning"
    in 12..16 -> "Good afternoon"
    else      -> "Good evening"
}

internal fun String.firstName(): String = split(" ").firstOrNull()?.ifBlank { this } ?: this

internal fun getContextualSubtitle(savings: Double, streak: Int): String = when {
    streak >= 30  -> "Remarkable consistency. Keep the momentum."
    streak >= 7   -> "You're building healthy spending habits."
    savings > 0   -> "Your savings are staying on track."
    else          -> "Small mindful steps create strong finances."
}

// NavPurple — matches bottom nav primary action color exactly. Now driven by the global accent
// (LocalAccent) so the accent picker recolors the dashboard live. Composable getter — used only in
// composable scope (modifiers/tints), never inside a Canvas DrawScope.
internal val NavPurple: Color
    @Composable get() = LocalAccent.current
