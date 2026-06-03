package com.example.insightku.utils

import java.text.NumberFormat
import java.util.*

/**
 * Mewakili satu pilihan mata uang yang tersedia di aplikasi.
 *
 * @param code    Kode ISO 4217 (mis. "IDR")
 * @param symbol  Simbol tampilan (mis. "Rp")
 * @param displayName Nama lengkap untuk UI dropdown (mis. "IDR – Rupiah (Rp)")
 * @param locale  Locale Java yang dipakai NumberFormat (mengatur separator + posisi simbol)
 * @param fractionDigits Jumlah angka desimal untuk currency ini. Locale mengatur SEPARATOR &
 *   POSISI SIMBOL; presisi desimal dikonfigurasi di sini (IDR/JPY = 0, lainnya = 2) karena
 *   kebiasaan lokal sering berbeda dari default ISO 4217.
 * @param flag    Emoji bendera negara/region untuk UI picker
 * @param region  Nama negara/region untuk UI picker (mis. "Indonesia")
 */
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String,
    val locale: Locale,
    val fractionDigits: Int = 2,
    val flag: String = "",
    val region: String = ""
)

object CurrencyUtils {

    // ─── Daftar mata uang yang didukung ───────────────────────────────────────
    val SUPPORTED_CURRENCIES: List<CurrencyOption> = listOf(
        CurrencyOption("IDR", "Rp",  "IDR – Rupiah (Rp)",            Locale("in", "ID"), fractionDigits = 0, flag = "🇮🇩", region = "Indonesian Rupiah"),
        CurrencyOption("USD", "$",   "USD – Dollar ($)",             Locale.US,          fractionDigits = 2, flag = "🇺🇸", region = "US Dollar"),
        CurrencyOption("EUR", "€",   "EUR – Euro (€)",               Locale.GERMANY,     fractionDigits = 2, flag = "🇪🇺", region = "Euro"),
        CurrencyOption("SGD", "S\$", "SGD – Singapore Dollar (S\$)", Locale("en", "SG"), fractionDigits = 2, flag = "🇸🇬", region = "Singapore Dollar"),
        CurrencyOption("MYR", "RM",  "MYR – Ringgit (RM)",           Locale("ms", "MY"), fractionDigits = 2, flag = "🇲🇾", region = "Malaysian Ringgit"),
        CurrencyOption("JPY", "¥",   "JPY – Yen (¥)",                Locale.JAPAN,       fractionDigits = 0, flag = "🇯🇵", region = "Japanese Yen"),
    )

    /** Cari CurrencyOption berdasarkan kode. Fallback ke IDR. */
    fun getOption(code: String): CurrencyOption =
        SUPPORTED_CURRENCIES.firstOrNull { it.code == code }
            ?: SUPPORTED_CURRENCIES.first()

    // ─── Formatter ───────────────────────────────────────────────────────────

    /**
     * Format [amount] menggunakan kode mata uang [currencyCode]. SATU jalur untuk semua currency:
     * `NumberFormat.getCurrencyInstance(locale)` membuat separator ribuan/desimal + posisi simbol
     * mengikuti locale secara otomatis (mis. EUR → "1.500.000,00 €", USD → "$1,500,000.00"). Presisi
     * desimal di-override dari [CurrencyOption.fractionDigits] (IDR/JPY = 0). TIDAK ada simbol atau
     * separator yang di-hardcode di sini.
     */
    fun formatAmount(amount: Double, currencyCode: String = "IDR"): String {
        val option = getOption(currencyCode)
        return try {
            val fmt = NumberFormat.getCurrencyInstance(option.locale)
            fmt.currency = Currency.getInstance(option.code)
            fmt.maximumFractionDigits = option.fractionDigits
            fmt.minimumFractionDigits = option.fractionDigits
            fmt.format(amount)
        } catch (e: Exception) {
            // Fallback masih locale-aware untuk separator; simbol dari konfigurasi option.
            val fmt = NumberFormat.getNumberInstance(option.locale)
            fmt.maximumFractionDigits = option.fractionDigits
            fmt.minimumFractionDigits = option.fractionDigits
            "${option.symbol} ${fmt.format(amount)}"
        }
    }

    /** Format tanpa simbol mata uang, dua desimal. */
    fun formatAmountWithoutSymbol(amount: Double): String =
        String.format("%.2f", amount)

    /**
     * Format ringkas dengan skala yang sesuai bahasa/currency:
     *
     * IDR  → Rp. 1,5 Jt / Rp. 2,3 M / Rp. 1,2 T  (ribu/juta/miliar/triliun)
     * Lain → $1.5M / $2.3K / ¥1.2B                 (thousand/million/billion/trillion)
     *
     * Aturan pembulatan: 1 desimal jika tidak bulat, tanpa desimal jika bulat.
     * Contoh IDR: 1.500.000 → "Rp. 1,5 Jt", 2.000.000 → "Rp. 2 Jt"
     */
    fun formatAmountCompact(amount: Double, currencyCode: String = "IDR"): String {
        val symbol = getOption(currencyCode).symbol
        val absAmount = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""

        return if (currencyCode == "IDR") {
            when {
                absAmount >= 1_000_000_000_000.0 -> "$sign$symbol${formatScale(absAmount / 1_000_000_000_000.0)} T"
                absAmount >= 1_000_000_000.0     -> "$sign$symbol${formatScale(absAmount / 1_000_000_000.0)} M"
                absAmount >= 1_000_000.0         -> "$sign$symbol${formatScale(absAmount / 1_000_000.0)} Jt"
                absAmount >= 1_000.0             -> "$sign$symbol${formatScale(absAmount / 1_000.0)} Rb"
                else                             -> formatAmount(amount, currencyCode)
            }
        } else {
            when {
                absAmount >= 1_000_000_000_000.0 -> "$sign$symbol${formatScale(absAmount / 1_000_000_000_000.0)}T"
                absAmount >= 1_000_000_000.0     -> "$sign$symbol${formatScale(absAmount / 1_000_000_000.0)}B"
                absAmount >= 1_000_000.0         -> "$sign$symbol${formatScale(absAmount / 1_000_000.0)}M"
                absAmount >= 1_000.0             -> "$sign$symbol${formatScale(absAmount / 1_000.0)}K"
                else                             -> formatAmount(amount, currencyCode)
            }
        }
    }

    /**
     * Format angka skala: hilangkan desimal jika bulat, tampilkan 1 desimal jika tidak.
     * Contoh: 1.0 → "1", 1.5 → "1,5", 2.35 → "2,4"
     */
    private fun formatScale(value: Double): String {
        return if (value == kotlin.math.floor(value)) {
            value.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }

    /**
     * Format raw digit string with thousand separators for display in input fields.
     * Input:  "50000"  → Output: "50.000"
     * Input:  "1000000" → Output: "1.000.000"
     * Only digits are accepted — non-digit characters are stripped first.
     */
    fun formatInputThousands(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val number = digits.toLongOrNull() ?: return digits
        return NumberFormat.getNumberInstance(Locale("in", "ID")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
            isGroupingUsed = true
        }.format(number)
    }

    /**
     * Strip thousand separators from a formatted input string back to raw digits.
     * Input: "50.000" → Output: "50000"
     */
    fun stripThousands(formatted: String): String = formatted.filter { it.isDigit() }

    fun parseAmount(amountString: String): Double? {
        return try {
            val cleanString = amountString
                .replace(Regex("^[A-Za-z$€¥₩Rp.\\s]+"), "")  // strip currency prefix
                .replace(".", "")   // strip IDR thousands separator
                .replace(",", ".")  // normalize decimal separator
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
