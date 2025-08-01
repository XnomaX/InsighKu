package com.example.insightku.utils

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    fun formatAmount(amount: Double): String {
        return currencyFormat.format(amount)
    }
    
    fun formatAmountWithoutSymbol(amount: Double): String {
        return String.format("%.2f", amount)
    }
    
    fun formatAmountCompact(amount: Double): String {
        return when {
            amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000)
            amount >= 1_000 -> String.format("%.1fK", amount / 1_000)
            else -> formatAmount(amount)
        }
    }
    
    fun parseAmount(amountString: String): Double? {
        return try {
            // Remove currency symbols and whitespace, then parse
            val cleanString = amountString
                .replace(Constants.CURRENCY_SYMBOL, "")
                .replace(",", "")
                .trim()
            
            cleanString.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    fun formatAmountWithSign(amount: Double, isIncome: Boolean): String {
        val formattedAmount = formatAmount(Math.abs(amount))
        return if (isIncome) "+$formattedAmount" else "-$formattedAmount"
    }
    
    fun getAmountColor(amount: Double, isIncome: Boolean): String {
        return if (isIncome) "#10B981" else "#EF4444" // Green for income, red for expense
    }
    
    fun calculatePercentage(value: Double, total: Double): Double {
        return if (total != 0.0) (value / total) * 100 else 0.0
    }
    
    fun formatPercentage(percentage: Double): String {
        return String.format("%.1f%%", percentage)
    }
    
    fun roundToTwoDecimals(amount: Double): Double {
        return Math.round(amount * 100.0) / 100.0
    }
    
    fun isValidAmount(amount: String): Boolean {
        return parseAmount(amount) != null && parseAmount(amount)!! >= 0
    }
}