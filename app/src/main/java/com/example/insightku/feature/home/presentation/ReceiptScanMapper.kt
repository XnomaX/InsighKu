package com.example.insightku.feature.home.presentation

import com.example.insightku.feature.home.domain.receipt.ReceiptScanData

/** CORD/Donut special tokens like `<s_itemname>`, `</s_itemname>`, `<s>`, `</s>`. */
private val CORD_TOKEN_REGEX = Regex("""</?s[a-z_]*>""")

/** Strip any stray CORD special tokens from a value (defense-in-depth). */
private fun String.stripCordTokens(): String = replace(CORD_TOKEN_REGEX, "").trim()

/**
 * Map Donut/CORD-v2 scan output to the add-transaction form.
 *
 * CORD-v2 has no store-name field, so `merchant` is left blank for the user to
 * type. `total` -> `amount` (digits only — strips currency symbols/separators,
 * consistent with [String.toAmountOrZero]); the parsed `items` -> `description`;
 * `isIncome` is carried from the user's chosen mode.
 *
 * Every value is scrubbed of stray CORD special tokens so nothing like
 * `<s_itemname>` ever shows in the form, even if the server missed one.
 */
internal fun ReceiptScanData.toTransactionFormData(isIncome: Boolean): TransactionFormData {
    val amount = total.stripCordTokens().filter { it.isDigit() }
    val cleanItems = items.map { it.copy(name = it.name.stripCordTokens(), qty = it.qty.stripCordTokens(), price = it.price.stripCordTokens()) }
    val description = if (cleanItems.isEmpty()) {
        rawText.stripCordTokens()
    } else {
        cleanItems.joinToString("\n") { item ->
            buildString {
                append(item.name)
                if (item.qty.isNotBlank()) append(" x").append(item.qty)
                if (item.price.isNotBlank()) append(" = ").append(item.price)
            }
        }.ifBlank { rawText.stripCordTokens() }
    }
    return TransactionFormData(
        merchant = merchant ?: "",
        amount = amount,
        category = "",
        description = description,
        isIncome = isIncome
    )
}
