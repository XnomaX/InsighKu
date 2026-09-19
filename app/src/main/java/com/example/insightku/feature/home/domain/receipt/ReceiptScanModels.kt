package com.example.insightku.feature.home.domain.receipt

/** One line item extracted from a receipt (Donut/CORD-v2 output). */
data class ReceiptScanItem(
    val name: String,
    val qty: String,
    val price: String
)

/**
 * Structured result of scanning one receipt.
 *
 * Note: `merchant` is always null — the CORD-v2 schema this model was trained
 * on has no store-name field, only `items[]` and `total`.
 */
data class ReceiptScanData(
    val merchant: String?,
    val total: String,
    val items: List<ReceiptScanItem>,
    val rawText: String
)

/**
 * Online-only scan outcome. `Offline` and `Error` never carry a cached or
 * partial result — there is no offline path.
 */
sealed interface ReceiptScanResult {
    data class Success(val data: ReceiptScanData) : ReceiptScanResult
    data object Offline : ReceiptScanResult
    data class Error(val message: String) : ReceiptScanResult
}
