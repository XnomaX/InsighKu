package com.example.insightku.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp

/**
 * Tiga ukuran breakpoint layar, sama dengan Material3 WindowSizeClass.
 * Compact  = HP normal           (lebar < 600dp)
 * Medium   = HP besar/foldable   (lebar 600–840dp)
 * Expanded = Tablet              (lebar > 840dp)
 *
 * Kenapa tidak pakai WindowSizeClass langsung?
 * WindowSizeClass butuh Activity context dan dependency tambahan.
 * Ini versi ringan yang cukup untuk kebutuhan auth screen.
 */
enum class WindowSize { Compact, Medium, Expanded }

@Composable
fun rememberWindowSize(): WindowSize {
    val config = LocalConfiguration.current
    return remember(config.screenWidthDp) {
        when {
            config.screenWidthDp < 600  -> WindowSize.Compact
            config.screenWidthDp < 840  -> WindowSize.Medium
            else                        -> WindowSize.Expanded
        }
    }
}

/**
 * Helper: pilih nilai berdasarkan ukuran layar.
 *
 * Contoh:
 * ```
 * val padding = windowSize.adaptive(
 *     compact  = Dimens.AuthScreenPaddingCompact,
 *     medium   = Dimens.AuthScreenPaddingMedium,
 *     expanded = Dimens.AuthScreenPaddingExpanded
 * )
 * ```
 */
fun <T> WindowSize.adaptive(compact: T, medium: T, expanded: T): T = when (this) {
    WindowSize.Compact  -> compact
    WindowSize.Medium   -> medium
    WindowSize.Expanded -> expanded
}

/** Versi khusus untuk Dp agar lebih ringkas. */
@Composable
fun adaptiveDp(compact: Dp, medium: Dp = compact, expanded: Dp = medium): Dp {
    val windowSize = rememberWindowSize()
    return windowSize.adaptive(compact, medium, expanded)
}
