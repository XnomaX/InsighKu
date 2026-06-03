package com.example.insightku.ui.components.analytics

/**
 * Analytics interactions. The interaction budget was deliberately widened (governance amended
 * 2026-05-31) to support richer, progressively-disclosed reflection: tapping a heatmap day reveals
 * a contextual panel, tapping the personality card reveals deeper patterns, bubbles expand inline.
 * Still no destructive actions, no navigation away, no filters — read-only reflection only.
 */
sealed class AnalyticsEvent {
    object LoadAnalytics : AnalyticsEvent()
    object RefreshData : AnalyticsEvent()

    /** Toggle a category bubble's inline detail. Passing the open name (or null) collapses it. */
    data class ExpandBubble(val categoryName: String?) : AnalyticsEvent()

    /** Toggle a heatmap day's contextual panel, keyed by dayOfMonth. Passing the open day collapses it. */
    data class ExpandDay(val dayOfMonth: Int?) : AnalyticsEvent()

    /** Toggle progressive disclosure of deeper behavioral patterns on the personality card. */
    object TogglePatterns : AnalyticsEvent()

    /** Uncover a "covered until curious" section (Quiet Reveal). One-way: invitation → revealed. */
    data class Uncover(val sectionId: String) : AnalyticsEvent()

    /** Temporary: cycle the debug/preview scenario (removed before final polish). */
    object CycleDebugScenario : AnalyticsEvent()

    /** Temporary: leave debug/preview mode and return to live data. */
    object ExitDebug : AnalyticsEvent()
}
