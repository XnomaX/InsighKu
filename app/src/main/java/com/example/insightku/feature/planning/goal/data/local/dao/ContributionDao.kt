package com.example.insightku.feature.planning.goal.data.local.dao

import androidx.room.*
import com.example.insightku.feature.planning.goal.data.model.ContributionEntity
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

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId AND amount > 0")
    suspend fun getTotalContributed(goalId: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId AND amount > 0")
    fun getTotalContributedFlow(goalId: String): Flow<Double>

    @Query("SELECT COALESCE(ABS(SUM(amount)), 0.0) FROM contributions WHERE goalId = :goalId AND amount < 0")
    suspend fun getTotalWithdrawn(goalId: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId")
    suspend fun getNetAmount(goalId: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId")
    fun getNetAmountFlow(goalId: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE amount > 0
        AND createdAt >= :dayStartMillis
        AND createdAt < :dayEndMillis
    """)
    suspend fun getTodayContributions(dayStartMillis: Long, dayEndMillis: Long): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE amount > 0
        AND createdAt >= :dayStartMillis
        AND createdAt < :dayEndMillis
    """)
    fun getTodayContributionsFlow(dayStartMillis: Long, dayEndMillis: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM contributions WHERE goalId = :goalId")
    suspend fun getContributionCount(goalId: String): Int

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

    // ── Allocation Queries ────────────────────────────────────────────────────

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE accountId = :accountId AND goalId = :goalId
    """)
    suspend fun getAllocatedFromAccountToGoal(accountId: String, goalId: String): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE accountId = :accountId AND amount > 0
    """)
    suspend fun getTotalAllocatedFromAccount(accountId: String): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE accountId = :accountId AND amount > 0
    """)
    fun getTotalAllocatedFromAccountFlow(accountId: String): Flow<Double>

    @Query("""
        SELECT goalId, COALESCE(SUM(amount), 0.0) as total
        FROM contributions
        WHERE accountId = :accountId AND amount > 0
        GROUP BY goalId
    """)
    suspend fun getGoalAllocationsFromAccount(accountId: String): List<GoalAllocation>

    @Query("""
        SELECT c.* FROM contributions c
        INNER JOIN goals g ON c.goalId = g.id
        WHERE c.accountId = :accountId
        ORDER BY c.createdAt DESC
    """)
    fun getContributionsFromAccount(accountId: String): Flow<List<ContributionEntity>>

    // ── Offline-First Sync Support ──────────────────────────────────────────────

    @Query("UPDATE contributions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT * FROM contributions WHERE isSynced = 0")
    suspend fun getUnsyncedContributions(): List<ContributionEntity>

    @Query("SELECT * FROM contributions WHERE goalId = :goalId AND isSynced = 0")
    suspend fun getUnsyncedContributionsByGoal(goalId: String): List<ContributionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContributionFromRemote(contribution: ContributionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContributionsFromRemote(contributions: List<ContributionEntity>)
}

data class GoalAllocation(
    val goalId: String,
    val total: Double
)
