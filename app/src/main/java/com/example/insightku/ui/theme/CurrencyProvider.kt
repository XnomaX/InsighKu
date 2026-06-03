package com.example.insightku.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.insightku.utils.CurrencyUtils

/** The mask shown in place of any amount when global privacy (hide amounts) is on. */
const val MASKED_AMOUNT = "••••••"

/**
 * CompositionLocal yang menyimpan kode mata uang aktif (mis. "IDR", "USD").
 *
 * Cara pakai di composable mana saja:
 * ```kotlin
 * val currencyCode = LocalCurrencyCode.current
 * Text(CurrencyUtils.formatAmount(amount, currencyCode))
 *
 * // Atau pakai helper:
 * Text(formatCurrency(amount))
 * ```
 */
val LocalCurrencyCode = compositionLocalOf { "IDR" }

/**
 * Wrapper yang menyediakan [currencyCode] ke seluruh composable tree di bawahnya.
 * Dipanggil di [InsightKuTheme] agar currency tersedia secara global.
 */
@Composable
fun ProvideCurrency(currencyCode: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCurrencyCode provides currencyCode) {
        content()
    }
}

/**
 * Shorthand helper — format [amount] menggunakan currency yang aktif saat ini.
 *
 * Contoh:
 * ```kotlin
 * Text(formatCurrency(uiState.totalBalance))  // otomatis pakai Rp / $ / € dll.
 * ```
 */
@Composable
fun formatCurrency(amount: Double): String {
    // Global privacy: when hide-amounts is on, every figure routed through this helper masks
    // automatically — one chokepoint, consistent across the whole app.
    if (LocalHideAmounts.current) return MASKED_AMOUNT
    val code = LocalCurrencyCode.current
    return CurrencyUtils.formatAmount(amount, code)
}
