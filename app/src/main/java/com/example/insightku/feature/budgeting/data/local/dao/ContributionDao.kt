package com.example.insightku.feature.budgeting.data.local.dao

import androidx.room.*
import com.example.insightku.feature.budgeting.data.model.ContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContributionDao {

    @Query("SELECT * FROM contributions ORDER BY createdAt DESC")
    fun getAllContributions(): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getContributionsByGoal(goalId: String): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getContributionsByAccount(accountId: String): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE transactionId = :transactionId")
    suspend fun getContributionByTransaction(transactionId: String): ContributionEntity?

    @Query("SELECT * FROM contributions WHERE id = :id")
    suspend fun getContributionById(id: String): ContributionEntity?

    /**
     * Get total amount contributed (positive contributions only) for a goal.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId AND amount > 0")
    suspend fun getTotalContributed(goalId: String): Double

    /**
     * Get total amount contributed as a Flow for reactive updates.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId AND amount > 0")
    fun getTotalContributedFlow(goalId: String): Flow<Double>

    /**
     * Get total withdrawals for a goal.
     */
    @Query("SELECT COALESCE(ABS(SUM(amount)), 0.0) FROM contributions WHERE goalId = :goalId AND amount < 0")
    suspend fun getTotalWithdrawn(goalId: String): Double

    /**
     * Get net amount for a goal (contributions - withdrawals).
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId")
    suspend fun getNetAmount(goalId: String): Double

    /**
     * Get net amount as Flow for reactive updates.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId")
    fun getNetAmountFlow(goalId: String): Flow<Double>

    /**
     * Get today's total contributions across all accounts (for daily target).
     * Only counts positive contributions (additions, not withdrawals).
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE amount > 0
        AND createdAt >= :dayStartMillis
        AND createdAt < :dayEndMillis
    """)
    suspend fun getTodayContributions(dayStartMillis: Long, dayEndMillis: Long): Double

    /**
     * Get today's contributions as Flow.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE amount > 0
        AND createdAt >= :dayStartMillis
        AND createdAt < :dayEndMillis
    """)
    fun getTodayContributionsFlow(dayStartMillis: Long, dayEndMillis: Long): Flow<Double>

    /**
     * Get contributions count for a goal.
     */
    @Query("SELECT COUNT(*) FROM contributions WHERE goalId = :goalId")
    suspend fun getContributionCount(goalId: String): Int

    /**
     * Get average daily contribution for a goal (last 30 days).
     */
    @Query("""
        SELECT COALESCE(AVG(daily_total), 0.0)
        FROM (
            SELECT SUM(amount) as daily_total
            FROM contributions
            WHERE goalId = :goalId
            AND amount > 0
            AND createdAt >= :startMillis
            GROUP BY date(createdAt / 1000, 'unixepoch')
        )
    """)
    suspend fun getAverageDailyContribution(goalId: String, startMillis: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: ContributionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<ContributionEntity>)

    @Update
    suspend fun updateContribution(contribution: ContributionEntity)

    @Delete
    suspend fun deleteContribution(contribution: ContributionEntity)

    @Query("DELETE FROM contributions WHERE goalId = :goalId")
    suspend fun deleteContributionsByGoal(goalId: String)
}
