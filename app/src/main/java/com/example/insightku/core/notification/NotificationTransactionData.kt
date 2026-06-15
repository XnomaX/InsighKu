package com.example.insightku.core.notification

/**
 * Pre-filled transaction data extracted from a bank notification deep link.
 * Passed through the composable tree to AddTransactionDialog.
 * No category — user always picks manually.
 */
data class NotificationTransactionData(
    val amount: Double,
    val title: String,
    val bankName: String,
    /** "INCOME" or "EXPENSE" — pre-selects type but user can change */
    val typeHint: String,
    val timestamp: Long,
    val description: String,
    /** id draft sumber; dipakai untuk menghapus draft setelah transaksi dikonfirmasi & disimpan */
    val draftId: String? = null
)
