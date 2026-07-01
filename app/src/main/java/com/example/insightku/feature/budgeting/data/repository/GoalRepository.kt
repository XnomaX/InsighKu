package com.example.insightku.feature.budgeting.data.repository

import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.feature.budgeting.data.local.dao.*
import com.example.insightku.feature.budgeting.data.model.*
import com.example.insightku.feature.budgeting.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for goal-related operations.
 * Orchestrates Room (offline-first) + Firestore (cloud sync).
 */
@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val contributionDao: ContributionDao,
    private val goalAccountDao: GoalAccountDao,
    private val reservedBalanceDao: ReservedBalanceDao,
    private val autoAllocationRuleDao: AutoAllocationRuleDao,
    private val dailyTargetDao: DailyTargetDao,
    private val accountDao: AccountDao,
    private val firestore: FirebaseFirestore
) {
    // ── Goal Operations ─────────────────────────────────────────────────────────

    /**
     * Get all active goals with their current amounts.
     */
    fun getActiveGoals(): Flow<List<Goal>> {
        return goalDao.getAllActiveGoals().map { entities ->
            entities.map { entity ->
                val currentAmount = contributionDao.getTotalContributed(entity.id)
                Goal.fromEntity(entity, currentAmount)
            }
        }
    }

    /**
     * Get a single goal by ID with its current amount.
     */
    suspend fun getGoalById(goalId: String): Goal? {
        val entity = goalDao.getGoalById(goalId) ?: return null
        val currentAmount = contributionDao.getTotalContributed(goalId)
        return Goal.fromEntity(entity, currentAmount)
    }

    /**
     * Get a goal as a Flow for reactive updates.
     */
    fun getGoalByIdFlow(goalId: String): Flow<Goal?> {
        return combine(
            goalDao.getGoalByIdFlow(goalId),
            contributionDao.getNetAmountFlow(goalId)
        ) { entity, amount ->
            entity?.let { Goal.fromEntity(it, amount) }
        }
    }

    /**
     * Create a new goal.
     */
    suspend fun createGoal(goal: Goal): Result<Goal> {
        return try {
            val entity = goal.toEntity()
            goalDao.insertGoal(entity)
            // TODO: Sync to Firestore
            Result.success(goal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing goal.
     */
    suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            goalDao.updateGoal(goal.toEntity())
            // TODO: Sync to Firestore
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Archive a goal (soft delete).
     */
    suspend fun archiveGoal(goalId: String): Result<Unit> {
        return try {
            goalDao.archiveGoal(goalId)
            // TODO: Sync to Firestore
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update goal status.
     */
    suspend fun updateGoalStatus(goalId: String, status: GoalStatus): Result<Unit> {
        return try {
            goalDao.updateGoalStatus(goalId, status.value)
            // TODO: Sync to Firestore
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle auto-allocate for a goal.
     */
    suspend fun setAutoAllocate(goalId: String, enabled: Boolean): Result<Unit> {
        return try {
            goalDao.updateAutoAllocate(goalId, enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get goal count (for limiting max goals).
     */
    suspend fun getActiveGoalCount(): Int {
        return goalDao.getActiveGoalCount()
    }

    // ── Contribution Operations ─────────────────────────────────────────────────

    /**
     * Contribute to a goal.
     * Creates contribution, updates goal amount, and updates account balance.
     */
    suspend fun contribute(
        goalId: String,
        accountId: String,
        amount: Double,
        type: ContributionType = ContributionType.MANUAL,
        transactionId: String? = null,
        notes: String = ""
    ): Result<Contribution> {
        return try {
            // Validate amount
            if (amount <= 0) {
                return Result.failure(IllegalArgumentException("Amount must be positive"))
            }

            // Validate goal exists
            val goal = goalDao.getGoalById(goalId)
                ?: return Result.failure(IllegalArgumentException("Goal not found"))

            // Validate account exists and has sufficient balance
            val account = accountDao.getAccountById(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))

            if (account.balance < amount) {
                return Result.failure(IllegalArgumentException("Insufficient balance"))
            }

            // Create contribution
            val contribution = ContributionEntity(
                goalId = goalId,
                accountId = accountId,
                amount = amount,
                type = type.value,
                transactionId = transactionId,
                notes = notes
            )
            contributionDao.insertContribution(contribution)

            // Deduct from account balance
            accountDao.updateBalance(accountId, -amount)

            // Check if goal is now complete
            val currentAmount = contributionDao.getTotalContributed(goalId)
            if (currentAmount >= goal.targetAmount && goal.goalStatus != GoalStatus.COMPLETED) {
                goalDao.updateGoalStatus(goalId, GoalStatus.COMPLETED.value)
            }

            Result.success(Contribution.fromEntity(contribution))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Withdraw from a goal.
     * Creates negative contribution and returns funds to account.
     */
    suspend fun withdraw(
        goalId: String,
        accountId: String,
        amount: Double,
        notes: String = ""
    ): Result<Contribution> {
        return try {
            // Validate amount
            if (amount <= 0) {
                return Result.failure(IllegalArgumentException("Amount must be positive"))
            }

            // Check current goal balance
            val currentAmount = contributionDao.getNetAmount(goalId)
            if (currentAmount < amount) {
                return Result.failure(IllegalArgumentException("Insufficient goal balance"))
            }

            // Create withdrawal (negative contribution)
            val contribution = ContributionEntity(
                goalId = goalId,
                accountId = accountId,
                amount = -amount, // Negative for withdrawal
                type = ContributionType.WITHDRAWAL.value,
                notes = notes
            )
            contributionDao.insertContribution(contribution)

            // Return funds to account
            accountDao.updateBalance(accountId, amount)

            Result.success(Contribution.fromEntity(contribution))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get contributions for a goal.
     */
    fun getContributionsByGoal(goalId: String): Flow<List<Contribution>> {
        return contributionDao.getContributionsByGoal(goalId).map { entities ->
            entities.map { Contribution.fromEntity(it) }
        }
    }

    /**
     * Get goal progress metrics.
     */
    suspend fun getGoalProgress(goalId: String): GoalProgress {
        val goal = goalDao.getGoalById(goalId) ?: return GoalProgress.empty(goalId)
        val currentAmount = contributionDao.getTotalContributed(goalId)
        val totalWithdrawn = contributionDao.getTotalWithdrawn(goalId)
        val contributionCount = contributionDao.getContributionCount(goalId)

        val thirtyDaysAgo = LocalDate.now().minusDays(30)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val avgDaily = contributionDao.getAverageDailyContribution(goalId, thirtyDaysAgo)

        val deadline = goal.deadline?.let {
            val deadlineDate = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate).toInt().coerceAtLeast(0)
        }

        return GoalProgress(
            goalId = goalId,
            currentAmount = currentAmount,
            targetAmount = goal.targetAmount,
            totalWithdrawn = totalWithdrawn,
            progressPercent = if (goal.targetAmount > 0)
                ((currentAmount / goal.targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0,
            remainingAmount = (goal.targetAmount - currentAmount).coerceAtLeast(0.0),
            daysRemaining = deadline,
            isOnTrack = true, // TODO: Calculate properly
            averageDailyContribution = avgDaily,
            contributionCount = contributionCount
        )
    }

    // ── Account Linking ────────────────────────────────────────────────────────

    /**
     * Link an account to a goal.
     */
    suspend fun linkAccountToGoal(
        goalId: String,
        accountId: String,
        allocationPercent: Double = 100.0,
        isPrimary: Boolean = false
    ): Result<Unit> {
        return try {
            // If this is primary, clear existing primary
            if (isPrimary) {
                goalAccountDao.clearPrimaryForGoal(goalId)
            }

            val entity = GoalAccountEntity(
                goalId = goalId,
                accountId = accountId,
                allocationPercent = allocationPercent,
                isPrimary = isPrimary
            )
            goalAccountDao.insertGoalAccount(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unlink an account from a goal.
     */
    suspend fun unlinkAccountFromGoal(goalId: String, accountId: String): Result<Unit> {
        return try {
            goalAccountDao.unlinkAccountFromGoal(goalId, accountId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get linked accounts for a goal.
     */
    fun getLinkedAccounts(goalId: String): Flow<List<GoalAccountEntity>> {
        return goalAccountDao.getGoalAccountsByGoal(goalId)
    }

    /**
     * Get linked accounts with full account info.
     */
    fun getLinkedAccountsWithInfo(goalId: String): Flow<List<Pair<GoalAccountEntity, Account?>>> {
        return goalAccountDao.getGoalAccountsByGoal(goalId).map { entities ->
            entities.map { entity ->
                val account = accountDao.getAccountById(entity.accountId)
                entity to account
            }
        }
    }

    // ── Daily Target Operations ─────────────────────────────────────────────────

    /**
     * Get the global daily target.
     */
    fun getDailyTarget(): Flow<DailyTarget> {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        val dayStart = today.toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).toInstant().toEpochMilli()

        return combine(
            dailyTargetDao.getGlobalDailyTarget(),
            contributionDao.getTodayContributionsFlow(dayStart, dayEnd)
        ) { entity, todayAmount ->
            DailyTarget.fromEntity(entity, todayAmount)
        }
    }

    /**
     * Set the daily saving target.
     */
    suspend fun setDailyTarget(amount: Double): Result<Unit> {
        return try {
            val entity = DailyTargetEntity(targetAmount = amount)
            dailyTargetDao.insertOrUpdateDailyTarget(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear the daily target.
     */
    suspend fun clearDailyTarget(): Result<Unit> {
        return try {
            dailyTargetDao.clearDailyTarget()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Auto-Allocation Operations ───────────────────────────────────────────────

    /**
     * Get all auto-allocation rules.
     */
    fun getAutoAllocationRules(): Flow<List<AutoAllocationRule>> {
        return autoAllocationRuleDao.getAllRules().map { entities ->
            entities.map { entity ->
                val goalName = goalDao.getGoalById(entity.goalId)?.name ?: ""
                AutoAllocationRule.fromEntity(entity, goalName)
            }
        }
    }

    /**
     * Get enabled auto-allocation rules.
     */
    fun getEnabledRules(): Flow<List<AutoAllocationRule>> {
        return autoAllocationRuleDao.getEnabledRules().map { entities ->
            entities.map { entity ->
                val goalName = goalDao.getGoalById(entity.goalId)?.name ?: ""
                AutoAllocationRule.fromEntity(entity, goalName)
            }
        }
    }

    /**
     * Add a new auto-allocation rule.
     */
    suspend fun addAutoAllocationRule(rule: AutoAllocationRule): Result<Unit> {
        return try {
            // Enable auto-allocate on the goal
            goalDao.updateAutoAllocate(rule.goalId, true)

            autoAllocationRuleDao.insertRule(rule.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an auto-allocation rule.
     */
    suspend fun updateAutoAllocationRule(rule: AutoAllocationRule): Result<Unit> {
        return try {
            autoAllocationRuleDao.updateRule(rule.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an auto-allocation rule.
     */
    suspend fun deleteAutoAllocationRule(ruleId: String): Result<Unit> {
        return try {
            autoAllocationRuleDao.deleteRuleById(ruleId)

            // Check if there are other enabled rules
            val hasOtherRules = autoAllocationRuleDao.getEnabledRuleCount() > 0
            if (!hasOtherRules) {
                // Disable auto-allocate on all goals
                goalDao.getAllActiveGoals().first().forEach { goal ->
                    goalDao.updateAutoAllocate(goal.id, false)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle an auto-allocation rule.
     */
    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Result<Unit> {
        return try {
            autoAllocationRuleDao.setRuleEnabled(ruleId, enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reserved Balance Operations ─────────────────────────────────────────────

    /**
     * Get total reserved for an account.
     */
    fun getReservedBalance(accountId: String): Flow<Double> {
        return reservedBalanceDao.getTotalReservedForAccountFlow(accountId)
    }

    /**
     * Get available balance (total - reserved).
     */
    suspend fun getAvailableBalance(accountId: String): Double? {
        return reservedBalanceDao.getAvailableBalance(accountId)
    }

    /**
     * Reserve funds for a goal.
     */
    suspend fun reserveFunds(accountId: String, amount: Double, goalId: String?): Result<Unit> {
        return try {
            reservedBalanceDao.increaseReservation(accountId, amount, goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Release reserved funds.
     */
    suspend fun releaseReservedFunds(accountId: String, amount: Double): Result<Unit> {
        return try {
            reservedBalanceDao.decreaseReservation(accountId, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Summary Operations ─────────────────────────────────────────────────────

    /**
     * Get goals summary statistics.
     */
    fun getGoalsSummary(): Flow<GoalSummary> {
        return goalDao.getAllActiveGoals().map { entities ->
            val activeGoals = entities.filter { it.goalStatus != GoalStatus.ARCHIVED }
            val totalSaved = activeGoals.sumOf { contributionDao.getTotalContributed(it.id) }
            val totalTarget = activeGoals.sumOf { it.targetAmount }

            GoalSummary(
                totalGoals = entities.size,
                activeGoals = activeGoals.count { it.goalStatus == GoalStatus.ACTIVE },
                completedGoals = activeGoals.count { it.goalStatus == GoalStatus.COMPLETED },
                totalSaved = totalSaved,
                totalTarget = totalTarget,
                overallProgress = if (totalTarget > 0) (totalSaved / totalTarget) * 100 else 0.0
            )
        }
    }
}
