package com.example.insightku.utils

import java.text.NumberFormat
import java.util.*

/**
 * Mewakili satu pilihan mata uang yang tersedia di aplikasi.
 *
 * @param code    Kode ISO 4217 (mis. "IDR")
 * @param symbol  Simbol tampilan (mis. "Rp")
 * @param displayName Nama lengkap untuk UI dropdown (mis. "IDR – Rupiah (Rp)")
 * @param locale  Locale Java yang dipakai NumberFormat
 */
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String,
    val locale: Locale
)

object CurrencyUtils {

    // ─── Daftar mata uang yang didukung ───────────────────────────────────────
    val SUPPORTED_CURRENCIES: List<CurrencyOption> = listOf(
        CurrencyOption("IDR", "Rp",  "IDR – Rupiah (Rp)",         Locale("in", "ID")),
        CurrencyOption("USD", "$",   "USD – Dollar ($)",           Locale.US),
        CurrencyOption("EUR", "€",   "EUR – Euro (€)",             Locale.GERMANY),
        CurrencyOption("SGD", "S\$", "SGD – Singapore Dollar (S\$)", Locale("en", "SG")),
        CurrencyOption("MYR", "RM",  "MYR – Ringgit (RM)",        Locale("ms", "MY")),
        CurrencyOption("JPY", "¥",   "JPY – Yen (¥)",             Locale.JAPAN),
    )

    /** Cari CurrencyOption berdasarkan kode. Fallback ke IDR. */
    fun getOption(code: String): CurrencyOption =
        SUPPORTED_CURRENCIES.firstOrNull { it.code == code }
            ?: SUPPORTED_CURRENCIES.first()

    // ─── Formatter ───────────────────────────────────────────────────────────

    /**
     * Format [amount] menggunakan kode mata uang [currencyCode].
     * Otomatis memilih locale yang tepat sehingga separator ribuan/desimal sesuai.
     */
    fun formatAmount(amount: Double, currencyCode: String = "IDR"): String {
        val option = getOption(currencyCode)
        return try {
            when (currencyCode) {
                "IDR" -> {
                    // Format: Rp. 1.000.000 (titik ribuan, tanpa desimal)
                    val fmt = NumberFormat.getNumberInstance(option.locale)
                    fmt.maximumFractionDigits = 0
                    fmt.minimumFractionDigits = 0
                    "Rp. ${fmt.format(amount)}"
                }
                "JPY" -> {
                    // Yen tanpa desimal
                    val fmt = NumberFormat.getNumberInstance(option.locale)
                    fmt.maximumFractionDigits = 0
                    "¥${fmt.format(amount)}"
                }
                else -> {
                    val fmt = NumberFormat.getCurrencyInstance(option.locale)
                    fmt.currency = Currency.getInstance(option.code)
                    fmt.format(amount)
                }
            }
        } catch (e: Exception) {
            "${option.symbol} ${String.format("%.2f", amount)}"
        }
    }

    /** Format tanpa simbol mata uang, dua desimal. */
    fun formatAmountWithoutSymbol(amount: Double): String =
        String.format("%.2f", amount)

    /** Format ringkas: 1.5M / 2.3K, dengan simbol dari [currencyCode]. */
    fun formatAmountCompact(amount: Double, currencyCode: String = "IDR"): String {
        val symbol = getOption(currencyCode).symbol
        return when {
            amount >= 1_000_000 -> "$symbol${String.format("%.1f", amount / 1_000_000)}M"
            amount >= 1_000     -> "$symbol${String.format("%.1f", amount / 1_000)}K"
            else                -> formatAmount(amount, currencyCode)
        }
    }

    fun parseAmount(amountString: String): Double? {
        return try {
            val cleanString = amountString
                .replace(Constants.CURRENCY_SYMBOL, "")
                .replace(",", "")
                .trim()
            cleanString.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun formatAmountWithSign(amount: Double, isIncome: Boolean, currencyCode: String = "IDR"): String {
        val formattedAmount = formatAmount(kotlin.math.abs(amount), currencyCode)
        return if (isIncome) "+$formattedAmount" else "-$formattedAmount"
    }

    fun getAmountColor(amount: Double, isIncome: Boolean): String =
        if (isIncome) "#10B981" else "#EF4444"

    fun calculatePercentage(value: Double, total: Double): Double =
        if (total != 0.0) (value / total) * 100 else 0.0

    fun formatPercentage(percentage: Double): String =
        String.format("%.1f%%", percentage)

    fun roundToTwoDecimals(amount: Double): Double =
        Math.round(amount * 100.0) / 100.0

    fun isValidAmount(amount: String): Boolean =
        parseAmount(amount) != null && parseAmount(amount)!! >= 0
}
