
package com.example.insightku.ui.theme

import androidx.compose.ui.unit.dp

object Dimens {
    val PaddingSmall = 4.dp
    val PaddingMedium = 8.dp
    val PaddingLarge = 16.dp
    val PaddingExtraLarge = 24.dp

    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp

    val ElevationSmall = 1.dp
    val ElevationMedium = 3.dp

    val IconSizeSmall = 14.dp
    val IconSizeMedium = 18.dp
    val IconSizeLarge = 24.dp
    val IconSizeExtraLarge = 48.dp

    val HeaderHeight = 120.dp
    val HeaderVerticalOffset = (-20).dp

    val StatCardPaddingHorizontal = 18.dp
    val StatCardPaddingVertical = 16.dp
    val StatCardIconBoxSize = 48.dp
    val StatCardIconSize = 24.dp

    val MonthSelectorButtonSize = 28.dp
    val MonthSelectorPadding = 10.dp

    val LinearProgressHeight = 6.dp

    // ─── Responsive / Adaptive ─────────────────────────────────────────────────
    // Ukuran yang disesuaikan berdasarkan lebar layar:
    // Compact  = HP biasa  (< 600dp)
    // Medium   = HP besar / foldable (600–840dp)
    // Expanded = Tablet    (> 840dp)

    /** Padding horizontal halaman auth (Login / SignUp) */
    val AuthScreenPaddingCompact   = 20.dp
    val AuthScreenPaddingMedium    = 48.dp
    val AuthScreenPaddingExpanded  = 96.dp

    /** Tinggi logo icon di halaman auth */
    val AuthLogoSizeCompact        = 64.dp
    val AuthLogoSizeMedium         = 80.dp
    val AuthLogoSizeExpanded       = 96.dp

    /** Tinggi tombol utama (Login / Create Account) */
    val ButtonHeightCompact        = 48.dp
    val ButtonHeightMedium         = 54.dp
    val ButtonHeightExpanded       = 56.dp

    /** Padding dalam Card form */
    val CardInnerPaddingCompact    = 16.dp
    val CardInnerPaddingMedium     = 24.dp
    val CardInnerPaddingExpanded   = 32.dp

    /** Spasi antar elemen di form */
    val FormSpacingCompact         = 12.dp
    val FormSpacingMedium          = 16.dp
    val FormSpacingExpanded        = 20.dp
}
