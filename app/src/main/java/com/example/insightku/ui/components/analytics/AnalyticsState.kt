package com.example.insightku.ui.components.analytics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.insightku.data.model.Transaction
import com.example.insightku.domain.usecase.analytics.BehavioralPattern
import com.example.insightku.domain.usecase.analytics.BigDecision
import com.example.insightku.domain.usecase.analytics.HeatmapCell
import com.example.insightku.domain.usecase.analytics.MoodData
import com.example.insightku.domain.usecase.analytics.RhythmCaption
import com.example.insightku.domain.usecase.analytics.SpendingPersonality
import com.example.insightku.domain.usecase.analytics.SpotlightData
import com.example.insightku.domain.usecase.analytics.StreakData

/**
 * Behavioral state for the Analytics screen — the single source of truth for the UI.
 *
 * Holds the pure domain insight models directly; the only UI-enriched type is [CategoryBubble],
 * which the ViewModel builds from `CategorySlice` by attaching a color + icon via the app-wide
 * `CategoryIconResolver` (the same system Home/Budgeting/Edit use).
 *
 * [expandedBubble] tracks the open category; [expandedDay] tracks the tapped heatmap day (its
 * `dayOfMonth`); [patternsExpanded] toggles progressive disclosure of deeper patterns.
 */
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false,
    val personality: SpendingPersonality = SpendingPersonality.ONBOARDING,
    val patterns: List<BehavioralPattern> = emptyList(),
    val patternsExpanded: Boolean = false,
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val rhythm: RhythmCaption = RhythmCaption(""),
    val expandedDay: Int? = null,
    val categoryBubbles: List<CategoryBubble> = emptyList(),
    val expandedBubble: String? = null,
    val bigDecisions: List<BigDecision> = emptyList(),
    val streak: StreakData = StreakData(0, 0, 0),
    val spotlight: SpotlightData = SpotlightData(),
    val mood: MoodData? = null,
    /** True while previewing synthetic data via the temporary debug mode. */
    val debugLabel: String? = null
)

/**
 * UI-facing category model — a `CategorySlice` enriched with its display color and icon resolved by
 * the app-wide `CategoryIconResolver`. [proportion] (0f..1f, largest == 1f) sizes the bubble;
 * [topTransactions] are revealed on tap.
 */
data class CategoryBubble(
    val name: String,
    val amount: Double,
    val proportion: Float,
    val color: Color,
    val icon: ImageVector,
    val topTransactions: List<Transaction>
)
