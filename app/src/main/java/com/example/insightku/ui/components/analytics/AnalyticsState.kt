package com.example.insightku.ui.components.analytics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.insightku.data.model.Transaction
import com.example.insightku.domain.usecase.analytics.BehavioralPattern
import com.example.insightku.domain.usecase.analytics.BigDecision
import com.example.insightku.domain.usecase.analytics.HeatmapCell
import com.example.insightku.domain.usecase.analytics.MoodData
import com.example.insightku.domain.usecase.analytics.Noticing
import com.example.insightku.domain.usecase.analytics.RhythmCaption
import com.example.insightku.domain.usecase.analytics.SpendingPersonality
import com.example.insightku.domain.usecase.analytics.SpotlightData
import com.example.insightku.domain.usecase.analytics.StreakData
import com.example.insightku.domain.usecase.analytics.TwoYousData

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
    /** The centerpiece comparison, color-enriched for the morph. Not-confident → card shows a greeting. */
    val twoYous: TwoYousUi = TwoYousUi.NOT_CONFIDENT,
    val patterns: List<BehavioralPattern> = emptyList(),
    val patternsExpanded: Boolean = false,
    /** The single "did you notice?" teaching moment to surface (first unseen), or null if none. */
    val noticing: Noticing? = null,
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val rhythm: RhythmCaption = RhythmCaption(""),
    val expandedDay: Int? = null,
    val categoryBubbles: List<CategoryBubble> = emptyList(),
    val expandedBubble: String? = null,
    val bigDecisions: List<BigDecision> = emptyList(),
    val streak: StreakData = StreakData(0, 0, 0),
    val spotlight: SpotlightData = SpotlightData(),
    val mood: MoodData? = null,
    /**
     * Ids of "covered until curious" sections the user has uncovered this session. Covered sections
     * (consistency, biggest-moves, spotlight, mood) render an invitation until their id is in here.
     */
    val uncoveredSections: Set<String> = emptySet(),
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

/**
 * UI-facing "Two Yous" model — the pure [TwoYousData] with each self's category dots enriched with a
 * resolved display [Color] (via the app-wide `CategoryIconResolver`, the same system bubbles use).
 * The morph lerps between [left] (Weekday You) and [right] (Weekend You); [divergence] sizes how far
 * the magnitude tint pushes. [gapHeadline] is the constant relationship sentence (never a pole).
 */
data class TwoYousUi(
    val left: SpendingSelfUi,
    val right: SpendingSelfUi,
    val divergence: Float,
    val gapHeadline: String,
    val isConfident: Boolean
) {
    companion object {
        val NOT_CONFIDENT = TwoYousUi(
            left = SpendingSelfUi("Weekday You", "🌤️", 0f, emptyList()),
            right = SpendingSelfUi("Weekend You", "🌙", 0f, emptyList()),
            divergence = 0f,
            gapHeadline = "",
            isConfident = false
        )
    }
}

/**
 * One self for the morph: orientation label + emoji target + its color-enriched category dots.
 * [intensity] (0f..1f) is this self's per-day spend normalized so the heavier self is 1f — it drives
 * the purple magnitude tint as the user drags toward this pole (magnitude, never judgment).
 */
data class SpendingSelfUi(
    val label: String,
    val emoji: String,
    val intensity: Float,
    val categories: List<SelfCategoryUi>
)

/** A category dot within a self — name, its 0f..1f weight within that self, and its resolved color. */
data class SelfCategoryUi(
    val name: String,
    val proportion: Float,
    val color: Color
)
