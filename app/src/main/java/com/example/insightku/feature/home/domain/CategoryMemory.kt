package com.example.insightku.feature.home.domain

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.home.domain.CategoryMemory.Companion.MIN_SIGHTINGS

/**
 * One learned association: when the user logs [merchant], they most often file it under [category].
 * [timesSeen] is how many times that merchant appeared (confidence/transparency), and [totalForMerchant]
 * is the merchant's total transaction count, so the UI can show "X of Y times".
 */
data class MerchantMemory(
    val merchant: String,
    val category: String,
    val timesSeen: Int,
    val totalForMerchant: Int
)

/**
 * Pure, backend-free "category learning". The app already records, for every transaction, the merchant
 * title and the category the user chose — so the most-frequent category per merchant IS a genuine
 * learned memory, derivable without any new storage or ML. This powers two real effects:
 *   1. a transparency surface in Settings ("what I've learned about you"), and
 *   2. a pre-selected category suggestion in the add-transaction flow.
 *
 * No Android/Compose types here, so it is JVM unit-testable (mirrors the InsightEngine style). Every
 * function is TOTAL: any input (including empty) yields a valid, non-throwing result.
 */
class CategoryMemory {

    private companion object {
        const val MIN_SIGHTINGS = 2          // a single sighting isn't a "pattern" worth surfacing
        const val MAX_MEMORIES_SURFACED = 8  // keep the transparency list calm, not a data dump
    }

    /**
     * Build the learned memories from all expense transactions. A merchant is only included once it
     * has been seen at least [MIN_SIGHTINGS] times with a non-blank category, so we never surface a
     * low-confidence guess as if it were a real pattern. Sorted by how often the merchant appears.
     */
    fun derive(all: List<Transaction>): List<MerchantMemory> {
        val expenses = all.filter { it.type == TransactionType.EXPENSE && it.title.isNotBlank() }
        if (expenses.isEmpty()) return emptyList()

        return expenses
            .groupBy { it.title.trim() }
            .mapNotNull { (merchant, txns) ->
                if (merchant.isEmpty() || txns.size < MIN_SIGHTINGS) return@mapNotNull null
                val topCategory = txns
                    .filter { it.category.isNotBlank() }
                    .groupingBy { it.category }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?: return@mapNotNull null
                MerchantMemory(
                    merchant = merchant,
                    category = topCategory.key,
                    timesSeen = topCategory.value,
                    totalForMerchant = txns.size
                )
            }
            .sortedByDescending { it.totalForMerchant }
            .take(MAX_MEMORIES_SURFACED)
    }

}


