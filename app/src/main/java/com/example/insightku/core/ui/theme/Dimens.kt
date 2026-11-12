
package com.example.insightku.core.ui.theme

import androidx.compose.ui.unit.dp

object Dimens {
    val PaddingSmall = 4.dp
    val PaddingMedium = 8.dp
    val PaddingLarge = 16.dp
    val PaddingExtraLarge = 24.dp

    // ─── Design System Tokens (DESIGN.md) ─────────────────────────────────────
    val ScreenHorizontalPadding = 18.dp
    val ContentBottomPadding = 104.dp
    val SectionSpacing = 24.dp
    val CardSpacing = 12.dp
    val CardInnerPadding = 16.dp
    val CardInnerPaddingLarge = 20.dp

    // Corner radius
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp
    val CardRadius = 20.dp
    val CardRadiusLarge = 24.dp
    val ButtonRadius = 16.dp
    val ButtonRadiusSmall = 14.dp
    val ChipRadius = 10.dp
    val BottomSheetRadius = 28.dp

    // ─── Section-header add pill ──────────────────────────────────────────────
    /**
     * Fixed width of the section-header add pill.
     *
     * Sized from the widest shipped label — Indonesian "Tambah Kategori" at `labelMedium`
     * measures ~100dp — plus the pill chrome that insets the label: 12dp padding x2, 14dp
     * icon, 4dp gap = 42dp. At 144dp the label budget is 102dp, so that string renders at
     * full size in both locales, and the longest Indonesian section title ("Anggaran
     * Pengeluaran", ~167dp) still fits beside the pill on a 360dp screen instead of
     * wrapping. The 1dp border is drawn on the edge and does not consume content width.
     *
     * See `AddPillChrome` / `AddPillLabelBudget` in SharedComponents.kt, which must be kept
     * in step with this value.
     */
    val AddPillWidth = 144.dp

    // ─── Bottom Sheet tokens ────────────────────────────────────────────
    /** Corner radius for all bottom sheets. */
    // val BottomSheetRadius = 28.dp  // already defined above
    /** Drag handle width. */
    val BottomSheetHandleWidth = 40.dp
    /** Drag handle height. */
    val BottomSheetHandleHeight = 4.dp
    /** Drag handle corner radius (pill shape). */
    val BottomSheetHandleRadius = 50.dp
    /** Horizontal content padding inside sheet. */
    val BottomSheetContentPadding = 24.dp
    /** Spacing between header divider and content. */
    val BottomSheetHeaderDividerSpacing = 8.dp
    /** Bottom padding for sticky footer area. */
    val BottomSheetFooterPadding = 24.dp
    /** Spacing between footer buttons. */
    val BottomSheetFooterSpacing = 10.dp
    /** Small sheet height (45–55% of screen). */
    val BottomSheetHeightSmall = 0.50f
    /** Medium sheet height (65–75% of screen). */
    val BottomSheetHeightMedium = 0.70f
    /** Large sheet height (max 90% of screen). */
    val BottomSheetHeightLarge = 0.90f

    // Elevation
    val ElevationSmall = 1.dp
    val ElevationMedium = 2.dp
    val ElevationLarge = 3.dp
    val ElevationDialog = 24.dp

    // Icon sizes
    val IconSizeSmall = 14.dp
    val IconSizeMedium = 20.dp
    val IconSizeLarge = 24.dp
    val IconSizeExtraLarge = 48.dp

    // Button heights
    val ButtonHeightPrimary = 52.dp
    val ButtonHeightSecondary = 44.dp
    val ButtonHeightCompact = 48.dp
    val ButtonHeightMedium = 54.dp
    val ButtonHeightExpanded = 56.dp

    // Navigation
    val BottomNavHeight = 64.dp

    // Header
    val HeaderHeight = 120.dp
    val HeaderVerticalOffset = (-20).dp

    // Stat card
    val StatCardPaddingHorizontal = 18.dp
    val StatCardPaddingVertical = 16.dp
    val StatCardIconBoxSize = 48.dp
    val StatCardIconSize = 24.dp

    // Misc
    val MonthSelectorButtonSize = 28.dp
    val MonthSelectorPadding = 10.dp
    val LinearProgressHeight = 6.dp
    val ProgressBarHeight = 8.dp

    // Auth screen responsive
    val AuthScreenPaddingCompact   = 20.dp
    val AuthScreenPaddingMedium    = 48.dp
    val AuthScreenPaddingExpanded  = 96.dp
    val AuthLogoSizeCompact        = 64.dp
    val AuthLogoSizeMedium         = 80.dp
    val AuthLogoSizeExpanded       = 96.dp
    val CardInnerPaddingCompact    = 16.dp
    val CardInnerPaddingMedium     = 24.dp
    val CardInnerPaddingExpanded   = 32.dp
    val FormSpacingCompact         = 12.dp
    val FormSpacingMedium          = 16.dp
    val FormSpacingExpanded        = 20.dp
}
