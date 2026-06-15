package com.example.insightku.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ResponsiveDimens — satu set dimensi yang nilainya otomatis menyesuaikan lebar layar.
 *
 * Cara pakai di screen mana saja:
 * ```kotlin
 * val dimens = LocalResponsiveDimens.current
 * Modifier.padding(horizontal = dimens.screenHorizontalPadding)
 * Column(modifier = Modifier.padding(dimens.contentPadding))
 * ```
 *
 * Tidak perlu ubah tiap screen secara manual — cukup wrap di Theme.kt,
 * semua Composable di bawahnya otomatis dapat nilai yang tepat.
 */
data class ResponsiveDimens(
    /** Padding horizontal utama halaman (kiri & kanan) */
    val screenHorizontalPadding: Dp,

    /** Padding vertikal konten utama */
    val screenVerticalPadding: Dp,

    /** Padding dalam Card / container */
    val contentPadding: Dp,

    /** Jarak antar elemen dalam form / list */
    val itemSpacing: Dp,

    /** Tinggi tombol utama */
    val buttonHeight: Dp,

    /** Tinggi header gradient di halaman (Dashboard, Budgeting, Settings) */
    val headerHeight: Dp,

    /** Ukuran icon di header / avatar */
    val headerIconSize: Dp,

    /** Ukuran font relatif (untuk scaling teks jika diperlukan) */
    val isCompact: Boolean
)

val LocalResponsiveDimens = compositionLocalOf {
    // Default: Compact (HP normal)
    ResponsiveDimens(
        screenHorizontalPadding = 16.dp,
        screenVerticalPadding   = 16.dp,
        contentPadding          = 16.dp,
        itemSpacing             = 12.dp,
        buttonHeight            = 48.dp,
        headerHeight            = 180.dp,
        headerIconSize          = 48.dp,
        isCompact               = true
    )
}

/**
 * Buat ResponsiveDimens berdasarkan lebar layar saat ini.
 * Dipanggil sekali di Theme.kt agar tersedia di seluruh app.
 */
@Composable
fun rememberResponsiveDimens(): ResponsiveDimens {
    val config = LocalConfiguration.current
    return remember(config.screenWidthDp) {
        when {
            config.screenWidthDp < 600 -> ResponsiveDimens(
                // Compact — HP normal (< 600dp)
                screenHorizontalPadding = 16.dp,
                screenVerticalPadding   = 16.dp,
                contentPadding          = 16.dp,
                itemSpacing             = 12.dp,
                buttonHeight            = 48.dp,
                headerHeight            = 180.dp,
                headerIconSize          = 48.dp,
                isCompact               = true
            )
            config.screenWidthDp < 840 -> ResponsiveDimens(
                // Medium — HP besar / foldable (600–840dp)
                screenHorizontalPadding = 32.dp,
                screenVerticalPadding   = 24.dp,
                contentPadding          = 24.dp,
                itemSpacing             = 16.dp,
                buttonHeight            = 54.dp,
                headerHeight            = 220.dp,
                headerIconSize          = 64.dp,
                isCompact               = false
            )
            else -> ResponsiveDimens(
                // Expanded — Tablet (> 840dp)
                screenHorizontalPadding = 64.dp,
                screenVerticalPadding   = 32.dp,
                contentPadding          = 32.dp,
                itemSpacing             = 20.dp,
                buttonHeight            = 56.dp,
                headerHeight            = 260.dp,
                headerIconSize          = 80.dp,
                isCompact               = false
            )
        }
    }
}

/**
 * Wrapper agar ResponsiveDimens tersedia sebagai CompositionLocal.
 * Gunakan ini di InsightKuTheme (Theme.kt) untuk wrap seluruh app.
 */
@Composable
fun ProvideResponsiveDimens(content: @Composable () -> Unit) {
    val dimens = rememberResponsiveDimens()
    CompositionLocalProvider(LocalResponsiveDimens provides dimens) {
        content()
    }
}
