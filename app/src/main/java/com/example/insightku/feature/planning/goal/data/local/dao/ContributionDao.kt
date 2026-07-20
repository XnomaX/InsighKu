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

    @Query("SELECT * FROM contributions WHERE transactionId LIKE :transactionIdPrefix || '%' AND type = :type")
    suspend fun getContributionsByTransactionType(transactionIdPrefix: String, type: String): List<ContributionEntity>

    @Query("SELECT * FROM contributions WHERE id = :id")
    suspend fun getContributionById(id: String): ContributionEntity?

    /** Net amount saved toward a goal (contributions minus withdrawals). */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId")
    suspend fun getTotalContributed(goalId: String): Double

    /** Batch query: get net total for ALL goals in a single query (avoids N+1). */
    @Query("SELECT goalId, COALESCE(SUM(amount), 0.0) as total FROM contributions GROUP BY goalId")
    suspend fun getAllGoalTotals(): List<GoalAllocation>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM contributions WHERE goalId = :goalId")
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

    // ── Allocation Queries (envelope model: net amounts — withdrawals reduce set-aside) ──

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE accountId = :accountId AND goalId = :goalId
    """)
    suspend fun getAllocatedFromAccountToGoal(accountId: String, goalId: String): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE accountId = :accountId
    """)
    suspend fun getTotalAllocatedFromAccount(accountId: String): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM contributions
        WHERE accountId = :accountId
    """)
    fun getTotalAllocatedFromAccountFlow(accountId: String): Flow<Double>

    @Query("""
        SELECT goalId, COALESCE(SUM(amount), 0.0) as total
        FROM contributions
        WHERE accountId = :accountId
        GROUP BY goalId
        HAVING SUM(amount) > 0
    """)
    suspend fun getGoalAllocationsFromAccount(accountId: String): List<GoalAllocation>

    /** Net funds held per account for a goal (for funds dialogs/transfers). */
    @Query("""
        SELECT accountId, COALESCE(SUM(amount), 0.0) as total
        FROM contributions
        WHERE goalId = :goalId
        GROUP BY accountId
        HAVING SUM(amount) > 0
    """)
    suspend fun getNetAllocationsByGoal(goalId: String): List<AccountAllocationRow>

    /** Funds set aside in an account for non-completed goals (envelope validation guard). */
    @Query("""
        SELECT COALESCE(SUM(c.amount), 0.0)
        FROM contributions c
        INNER JOIN goals g ON c.goalId = g.id
        WHERE c.accountId = :accountId AND g.status != 'completed'
    """)
    suspend fun getSetAsideByAccount(accountId: String): Double

    @Query("""
        SELECT COALESCE(SUM(c.amount), 0.0)
        FROM contributions c
        INNER JOIN goals g ON c.goalId = g.id
        WHERE c.accountId = :accountId AND g.status != 'completed'
    """)
    fun getSetAsideByAccountFlow(accountId: String): Flow<Double>

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

    // ── Legacy Data Repair ─────────────────────────────────────────────────────

    @Query("SELECT * FROM contributions WHERE createdAt < :cutoffMillis")
    suspend fun getContributionsWithEpochTimestamps(cutoffMillis: Long): List<ContributionEntity>

    @Query("UPDATE contributions SET createdAt = :newTimestamp WHERE id = :id")
    suspend fun updateContributionTimestamp(id: String, newTimestamp: Long)
}

data class GoalAllocation(
    val goalId: String,
    val total: Double
)

data class AccountAllocationRow(
    val accountId: String,
    val total: Double
)
