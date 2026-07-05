package com.example.insightku.core.ui.theme

import androidx.compose.runtime.Composable
import com.example.insightku.core.utils.CurrencyUtils
import java.text.NumberFormat
import java.util.Locale

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

/**
 * Compact IDR format with English-style suffixes (Rp, M, K).
 * Used by GoalCard, GoalDetailScreen, and other budgeting screens.
 */
fun formatCurrencyCompactIDR(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> {
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount / 1_000_000_000)
            "Rp$formatted M"
        }
        amount >= 1_000_000 -> {
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount / 1_000_000)
            "Rp$formatted M"
        }
        amount >= 1_000 -> {
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount / 1_000)
            "Rp$formatted K"
        }
        else -> "Rp${NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())}"
    }
}

