package com.example.insightku.core.i18n

import com.example.insightku.core.i18n.NumberFormatter.formatCompact
import com.example.insightku.core.i18n.NumberFormatter.formatCurrencyCompact
import com.example.insightku.core.utils.CurrencyUtils
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * Centralized locale-aware number and currency formatter.
 *
 * This is the **single source of truth** for all numeric formatting in InsightKu.
 * Every screen should use these methods instead of custom formatting logic.
 *
 * Supports:
 *   - Indonesian formatting (1.234.567,89 with . thousands separator, , decimal)
 *   - English formatting (1,234,567.89 with , thousands separator, . decimal)
 *   - Currency formatting with proper symbol placement
 *   - Compact numbers (1K, 1M, 1Jt, etc.)
 *   - Input formatting (thousands separator while typing)
 *   - Negative values, zero values, large numbers
 *   - Configurable decimal precision
 */
object NumberFormatter {

    // ══════════════════════════════════════════════════════════════════════════════
    // NUMBER FORMATTING
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a number with locale-aware thousands separators and decimal places.
     *
     * Indonesian: 1.234.567,89
     * English:    1,234,567.89
     *
     * @param value The number to format
     * @param decimalPlaces Number of decimal places (default: 0)
     * @param locale The locale to use (default: current app locale)
     */
    fun formatNumber(
        value: Double,
        decimalPlaces: Int = 0,
        locale: Locale = Locale.getDefault()
    ): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val fmt = NumberFormat.getNumberInstance(locale) as DecimalFormat
        fmt.maximumFractionDigits = decimalPlaces
        fmt.minimumFractionDigits = decimalPlaces
        return fmt.format(value)
    }

    /**
     * Format an integer with locale-aware thousands separators.
     *
     * Indonesian: 1.234.567
     * English:    1,234,567
     */
    fun formatInteger(value: Long, locale: Locale = Locale.getDefault()): String {
        val fmt = NumberFormat.getIntegerInstance(locale)
        return fmt.format(value)
    }

    /**
     * Format a number for input fields (thousands separator while typing).
     * Uses the given locale (default: Indonesian for backward compat).
     */
    fun formatInputThousands(
        raw: String,
        locale: Locale = Locale.forLanguageTag("in-ID")
    ): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val number = digits.toLongOrNull() ?: return digits
        return NumberFormat.getIntegerInstance(locale).format(number)
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // CURRENCY FORMATTING
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a currency amount with locale-aware formatting.
     *
     * Indonesian IDR: Rp12.500 (no decimals)
     * English USD:    $12,500.00
     * English IDR:    Rp 12,500 (no decimals)
     *
     * @param amount The amount to format
     * @param currencyCode ISO 4217 currency code (e.g., "IDR", "USD")
     * @param locale The locale to use (default: current app locale)
     */
    fun formatCurrency(
        amount: Double,
        currencyCode: String = "IDR",
        locale: Locale = Locale.getDefault()
    ): String {
        if (amount.isNaN() || amount.isInfinite()) {
            val numFmt = NumberFormat.getNumberInstance(locale) as DecimalFormat
            val option = CurrencyUtils.getOption(currencyCode)
            numFmt.maximumFractionDigits = option.fractionDigits
            numFmt.minimumFractionDigits = option.fractionDigits
            return "${option.symbol}${numFmt.format(0.0)}"
        }

        val option = CurrencyUtils.getOption(currencyCode)

        return try {
            val fmt = NumberFormat.getCurrencyInstance(locale) as DecimalFormat
            fmt.currency = Currency.getInstance(option.code)
            fmt.maximumFractionDigits = option.fractionDigits
            fmt.minimumFractionDigits = option.fractionDigits
            fmt.format(amount)
        } catch (_: Exception) {
            val numFmt = NumberFormat.getNumberInstance(locale) as DecimalFormat
            numFmt.maximumFractionDigits = option.fractionDigits
            numFmt.minimumFractionDigits = option.fractionDigits
            "${option.symbol}${numFmt.format(amount)}"
        }
    }

    /**
     * Format a currency amount with sign (+ for income, - for expense).
     *
     * Indonesian: +Rp1.250.000 / -Rp1.250.000
     * English:    +$1,250,000.00 / -$1,250,000.00
     */
    fun formatCurrencyWithSign(
        amount: Double,
        isIncome: Boolean,
        currencyCode: String = "IDR",
        locale: Locale = Locale.getDefault()
    ): String {
        val formatted = formatCurrency(abs(amount), currencyCode, locale)
        return if (isIncome) "+$formatted" else "-$formatted"
    }

    /**
     * Format currency in compact form (e.g., Rp12 Jt, $12M, Rp1,2 Rb, $1.2K).
     *
     * Indonesian IDR: Rp12 Jt, Rp1,2 Rb
     * English:        $12M, $1.2K
     */
    fun formatCurrencyCompact(
        amount: Double,
        currencyCode: String = "IDR",
        locale: Locale = Locale.getDefault()
    ): String {
        if (amount.isNaN() || amount.isInfinite()) {
            return formatCurrency(0.0, currencyCode, locale)
        }

        val absAmount = abs(amount)
        val sign = if (amount < 0) "-" else ""

        // For amounts < 1000, fall back to standard currency formatting
        // to respect the currency's fraction digits (e.g., USD shows $500.00)
        if (absAmount < 1_000.0) {
            return "$sign${formatCurrency(absAmount, currencyCode, locale)}"
        }

        val option = CurrencyUtils.getOption(currencyCode)
        val symbol = option.symbol
        val isIndonesian = currencyCode == "IDR" || locale.language == "id"

        val result = resolveCompact(absAmount, locale, isIndonesian, minDecimals = 1)
        return "$sign$symbol${result.scaled} ${result.suffix}"
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // COMPACT NUMBER FORMATTING
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a number in compact/abbreviated form without currency.
     *
     * Indonesian abbreviations:
     *   950 → 950
     *   1.200 → 1,2 Rb
     *   15.000 → 15 Rb
     *   1.500.000 → 1,5 Jt
     *   25.000.000 → 25 Jt
     *   1.500.000.000 → 1,5 M
     *   1.500.000.000.000 → 1,5 T
     *
     * English abbreviations:
     *   950 → 950
     *   1,200 → 1.2K
     *   15,000 → 15K
     *   1,500,000 → 1.5M
     *   1,500,000,000 → 1.5B
     *   1,500,000,000,000 → 1.5T
     *
     * @param value The number to format
     * @param locale The locale to use (default: current app locale)
     */
    fun formatCompact(
        value: Double,
        locale: Locale = Locale.getDefault()
    ): String {
        if (value.isNaN() || value.isInfinite()) return "0"

        val absValue = abs(value)
        val sign = if (value < 0) "-" else ""
        val isIndonesian = locale.language == "id"

        val result = resolveCompact(absValue, locale, isIndonesian, minDecimals = 0)
        return if (result.suffix.isEmpty()) {
            "$sign${result.scaled}"
        } else {
            "$sign${result.scaled}${result.suffix}"
        }
    }

    /**
     * Compact formatting for Long values (avoids unnecessary .toDouble() at call sites).
     */
    fun formatCompact(
        value: Long,
        locale: Locale = Locale.getDefault()
    ): String = formatCompact(value.toDouble(), locale)

    /**
     * Get the current currency symbol (e.g., "Rp" for IDR, "$" for USD).
     * Useful for input field prefixes where only the symbol is needed.
     */
    fun getCurrencySymbol(currencyCode: String = "IDR"): String {
        return CurrencyUtils.getOption(currencyCode).symbol
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a scaled value for display with max 1 decimal digit.
     * @param minDecimals Minimum fraction digits (1 for currency compact, 0 for standalone compact)
     */
    private fun formatScale(value: Double, locale: Locale = Locale.getDefault(), minDecimals: Int = 1): String {
        return if (value == floor(value)) {
            formatInteger(value.toLong(), locale)
        } else {
            val fmt = NumberFormat.getNumberInstance(locale) as DecimalFormat
            fmt.maximumFractionDigits = 1
            fmt.minimumFractionDigits = minDecimals
            fmt.format(value)
        }
    }

    /**
     * Data class holding the result of compact scale resolution.
     */
    private data class CompactResult(val scaled: String, val suffix: String)

    /**
     * Resolve the scaled value and suffix for compact formatting.
     * Shared by [formatCompact] and [formatCurrencyCompact].
     */
    private fun resolveCompact(absValue: Double, locale: Locale, isIndonesian: Boolean, minDecimals: Int = 0): CompactResult {
        return when {
            absValue >= 1_000_000_000_000.0 -> CompactResult(formatScale(absValue / 1_000_000_000_000.0, locale, minDecimals), "T")
            absValue >= 1_000_000_000.0 -> CompactResult(formatScale(absValue / 1_000_000_000.0, locale, minDecimals), if (isIndonesian) "M" else "B")
            absValue >= 1_000_000.0 -> CompactResult(formatScale(absValue / 1_000_000.0, locale, minDecimals), if (isIndonesian) "Jt" else "M")
            absValue >= 1_000.0 -> CompactResult(formatScale(absValue / 1_000.0, locale, minDecimals), if (isIndonesian) "Rb" else "K")
            else -> CompactResult(formatScale(absValue, locale, minDecimals), "")
        }
    }

}
