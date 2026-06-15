package com.example.insightku.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class InsightTone {
    GENTLE, WARM, DIRECT;
    companion object {
        fun fromKey(key: String): InsightTone = when (key.lowercase()) {
            "gentle" -> GENTLE
            "direct" -> DIRECT
            else -> WARM
        }
    }
    val key: String get() = name.lowercase()
}

enum class VisualDensity(val spacing: Float) {
    COMFORTABLE(1.0f), COZY(0.8f), COMPACT(0.6f);
    companion object {
        fun fromKey(key: String): VisualDensity = when (key.lowercase()) {
            "cozy" -> COZY
            "compact" -> COMPACT
            else -> COMFORTABLE
        }
    }
    val key: String get() = name.lowercase()
}

val LocalComfortMode = compositionLocalOf { false }
val LocalInsightTone = compositionLocalOf { InsightTone.WARM }
val LocalAccent = compositionLocalOf { Color(0xFF7C4DFF) }
val LocalVisualDensity = compositionLocalOf { VisualDensity.COMFORTABLE }
val LocalHideAmounts = compositionLocalOf { false }
val LocalCurrencyCode = compositionLocalOf { "IDR" }
