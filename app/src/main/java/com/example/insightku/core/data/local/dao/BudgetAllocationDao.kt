package com.example.insightku.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for querying budget allocations from an account's spending.
 * Budget allocations are derived from expense transactions in budget categories.
 */
@Dao
interface BudgetAllocationDao {

    /**
     * Get total spent from a specific account on budgets within a time period.
     * Joins transactions with categories to get categoryId, then filters to expense categories.
     */
    @Query("""
        SELECT COALESCE(SUM(ABS(t.amount)), 0.0)
        FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.accountId = :accountId
        AND t.type = 'EXPENSE'
        AND c.categoryType = 'EXPENSE'
        AND t.date >= :periodStart
        AND t.date <= :periodEnd
    """)
    fun getTotalBudgetSpentFromAccountFlow(
        accountId: String,
        periodStart: Long,
        periodEnd: Long
    ): Flow<Double>

    /**
     * Get spending breakdown by category for an account within a budget period.
     * Returns categoryId and totalSpent for each category with active budgets.
     */
    @Query("""
        SELECT
            c.id as categoryId,
            c.name as categoryName,
            c.color as categoryColor,
            c.icon as categoryIcon,
            COALESCE(SUM(ABS(t.amount)), 0.0) as totalSpent
        FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.accountId = :accountId
        AND t.type = 'EXPENSE'
        AND c.categoryType = 'EXPENSE'
        AND t.date >= :periodStart
        AND t.date <= :periodEnd
        GROUP BY c.id
        HAVING totalSpent > 0
        ORDER BY totalSpent DESC
    """)
    fun getBudgetAllocationsByCategoryFlow(
        accountId: String,
        periodStart: Long,
        periodEnd: Long
    ): Flow<List<BudgetAllocationByCategory>>
}

/**
 * Result class for budget allocation queries grouped by category.
 */
data class BudgetAllocationByCategory(
    val categoryId: String,
    val categoryName: String,
    val categoryColor: String?,
    val categoryIcon: String?,
    val totalSpent: Double
)
