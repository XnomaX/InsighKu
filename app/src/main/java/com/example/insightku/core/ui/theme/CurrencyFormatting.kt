package com.example.insightku.core.ui.theme

import androidx.compose.runtime.Composable
import com.example.insightku.core.utils.CurrencyUtils

/** Shorthand to format a Double using the current LocalCurrencyCode. */
@Composable
fun formatCurrency(amount: Double): String {
    val code = LocalCurrencyCode.current
    return CurrencyUtils.formatAmount(amount, code)
}

/** Shorthand for compact format using the current LocalCurrencyCode. */
@Composable
fun formatCurrencyCompact(amount: Double): String {
    val code = LocalCurrencyCode.current
    return CurrencyUtils.formatAmountCompact(amount, code)
}

