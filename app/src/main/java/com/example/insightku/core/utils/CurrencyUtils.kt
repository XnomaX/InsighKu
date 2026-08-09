package com.example.insightku.core.utils

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

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

    val SUPPORTED_CURRENCIES: List<CurrencyOption> = listOf(
        CurrencyOption("IDR", "Rp",  "IDR – Rupiah (Rp)",            Locale.forLanguageTag("in-ID"), fractionDigits = 0, flag = "🇮🇩", region = "Indonesian Rupiah"),
        CurrencyOption("USD", "$",   "USD – Dollar ($)",             Locale.US,          fractionDigits = 2, flag = "🇺🇸", region = "US Dollar"),
        CurrencyOption("EUR", "€",   "EUR – Euro (€)",               Locale.GERMANY,     fractionDigits = 2, flag = "🇪🇺", region = "Euro"),
        CurrencyOption("SGD", "S\$", "SGD – Singapore Dollar (S\$)", Locale.forLanguageTag("en-SG"), fractionDigits = 2, flag = "🇸🇬", region = "Singapore Dollar"),
        CurrencyOption("MYR", "RM",  "MYR – Ringgit (RM)",           Locale.forLanguageTag("ms-MY"), fractionDigits = 2, flag = "🇲🇾", region = "Malaysian Ringgit"),
        CurrencyOption("JPY", "¥",   "JPY – Yen (¥)",                Locale.JAPAN,       fractionDigits = 0, flag = "🇯🇵", region = "Japanese Yen"),
    )

    fun getOption(code: String): CurrencyOption =
        SUPPORTED_CURRENCIES.firstOrNull { it.code == code } ?: SUPPORTED_CURRENCIES.first()

    fun formatAmount(amount: Double, currencyCode: String = "IDR"): String {
        val option = getOption(currencyCode)
        return try {
            val fmt = NumberFormat.getCurrencyInstance(option.locale)
            fmt.currency = Currency.getInstance(option.code)
            fmt.maximumFractionDigits = option.fractionDigits
            fmt.minimumFractionDigits = option.fractionDigits
            fmt.format(amount)
        } catch (_: Exception) {
            val fmt = NumberFormat.getNumberInstance(option.locale)
            fmt.maximumFractionDigits = option.fractionDigits
            fmt.minimumFractionDigits = option.fractionDigits
            "${option.symbol} ${fmt.format(amount)}"
        }
    }

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

    private fun formatScale(value: Double): String =
        if (value == kotlin.math.floor(value)) value.toLong().toString()
        else String.format(Locale.getDefault(), "%.1f", value)

    fun formatInputThousands(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val number = digits.toLongOrNull() ?: return digits
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("in-ID")).apply {
            maximumFractionDigits = 0; minimumFractionDigits = 0; isGroupingUsed = true
        }.format(number)
    }

    fun stripThousands(formatted: String): String = formatted.filter { it.isDigit() }


}

