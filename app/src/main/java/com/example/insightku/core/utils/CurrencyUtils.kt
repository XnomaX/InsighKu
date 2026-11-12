package com.example.insightku.core.utils

import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.i18n.NumberFormatter
import java.text.NumberFormat
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

    /**
     * Full amount with currency symbol, in the *app* language.
     *
     * Delegates to [NumberFormatter.formatCurrency]. This function used to format with
     * `option.locale` — a per-currency constant (`in-ID` for IDR) — so the same balance read
     * `Rp1.234.567` here while the dashboard's locale-aware formatter showed `IDR1,234,567`
     * in English mode. One formatter, one locale rule; see `formatInputThousands` below,
     * which had the same `in-ID` hardcoding fixed earlier for the same reason.
     */
    fun formatAmount(
        amount: Double,
        currencyCode: String = "IDR",
        locale: Locale = LocaleHelper.currentLocale()
    ): String = NumberFormatter.formatCurrency(amount, currencyCode, locale)

    /**
     * Compact amount with currency symbol, in the *app* language.
     *
     * Delegates to [NumberFormatter.formatCurrencyCompact] rather than carrying its own
     * branch: this function used to hardcode `currencyCode == "IDR"` → `Rp` + `Rb/Jt/M/T`
     * with no locale parameter at all, so an English-locale app showed Indonesian units
     * ("Rp1,2 Jt") on the contribution and allocation sheets while the dashboard — same
     * amount, different formatter — showed "IDR1.2M". One formatter, one rule set.
     *
     * Output parity for the Indonesian case is preserved: symbol "Rp", units Rb/Jt/M/T,
     * one decimal for fractional scales, no decimals for whole ones.
     */
    fun formatAmountCompact(amount: Double, currencyCode: String = "IDR"): String =
        NumberFormatter.formatCurrencyCompact(amount, currencyCode)

    /**
     * Group a digits-only string with thousands separators for display in an
     * amount input field.
     *
     * The locale defaults to [Locale.getDefault] to match the rest of the app's
     * formatting (`NumberFormatter.formatCurrency`, `DateFormatter`), so what the
     * user types and what the app displays elsewhere use the same separators.
     * This previously hardcoded `in-ID`, which showed `1.500` to an English-locale
     * user while every other amount on screen read `1,500`.
     *
     * Input is digits-only by contract: `maximumFractionDigits = 0` here and
     * [stripThousands] on the way back out, so no decimal separator can reach the
     * stored value regardless of locale.
     */
    fun formatInputThousands(raw: String, locale: Locale = LocaleHelper.currentLocale()): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val number = digits.toLongOrNull() ?: return digits
        return NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 0; minimumFractionDigits = 0; isGroupingUsed = true
        }.format(number)
    }

    fun stripThousands(formatted: String): String = formatted.filter { it.isDigit() }


}

