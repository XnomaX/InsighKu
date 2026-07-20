package com.example.insightku.feature.planning.goal.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.insightku.R
import com.example.insightku.core.data.local.database.InsightKuDatabase
import com.example.insightku.core.notification.AutoAllocationNotificationHelper
import com.example.insightku.core.worker.SyncGoalWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.utils.AppConstants
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.planning.goal.data.local.dao.*
import com.example.insightku.feature.planning.goal.data.model.*
import com.example.insightku.feature.planning.goal.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val contributionDao: ContributionDao,
    private val goalAccountDao: GoalAccountDao,
    private val reservedBalanceDao: ReservedBalanceDao,
    private val autoAllocationRuleDao: AutoAllocationRuleDao,
    private val dailyTargetDao: DailyTargetDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    private val database: InsightKuDatabase,
    private val notificationHelper: AutoAllocationNotificationHelper
) {
    companion object {
        private const val TAG = "GoalRepository"
    }

    // ── Per-account Mutex to prevent concurrent auto-allocation overdrafts ──
    private val accountMutexes = ConcurrentHashMap<String, Mutex>()
    private fun mutexForAccount(accountId: String): Mutex =
        accountMutexes.computeIfAbsent(accountId) { Mutex() }

    fun getActiveGoals(): Flow<List<Goal>> {
        return combine(
            goalDao.getAllActiveGoals(),
            contributionDao.getTotalAllocatedFromAccountFlow("")  // invalidation trigger
        ) { entities, _ ->
            val totalsMap = contributionDao.getAllGoalTotals().associate { it.goalId to it.total }
            entities.map { entity ->
                Goal.fromEntity(entity, totalsMap[entity.id] ?: 0.0)
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getGoalById(goalId: String): Goal? {
        val entity = goalDao.getGoalById(goalId) ?: return null
        val currentAmount = contributionDao.getTotalContributed(goalId)
        return Goal.fromEntity(entity, currentAmount)
    }

    fun getGoalByIdFlow(goalId: String): Flow<Goal?> {
        return combine(
            goalDao.getGoalByIdFlow(goalId),
            contributionDao.getNetAmountFlow(goalId)
        ) { entity, amount ->
            entity?.let { Goal.fromEntity(it, amount) }
        }
    }

    suspend fun createGoal(goal: Goal): Result<Goal> {
        return try {
            val entity = goal.toEntity().copy(isSynced = false)
            goalDao.insertGoal(entity)
            scheduleGoalSync()
            Result.success(goal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            val entity = goal.toEntity().copy(isSynced = false)
            goalDao.updateGoal(entity)
            scheduleGoalSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveGoal(goalId: String): Result<Unit> {
        return try {
            goalDao.archiveGoal(goalId)
            val entity = goalDao.getGoalById(goalId)
            if (entity != null) {
                goalDao.updateGoal(entity.copy(isSynced = false))
            }
            scheduleGoalSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getArchivedGoals(): Flow<List<Goal>> {
        return combine(
            goalDao.getArchivedGoals(),
            contributionDao.getTotalAllocatedFromAccountFlow("")  // invalidation trigger
        ) { entities, _ ->
            val totalsMap = contributionDao.getAllGoalTotals().associate { it.goalId to it.total }
            entities.map { entity ->
                Goal.fromEntity(entity, totalsMap[entity.id] ?: 0.0)
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun restoreGoal(goalId: String): Result<Unit> {
        return try {
            goalDao.restoreGoal(goalId)
            val entity = goalDao.getGoalById(goalId)
            if (entity != null) {
                goalDao.updateGoal(entity.copy(isSynced = false))
            }
            scheduleGoalSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoalPermanently(goalId: String): Result<Unit> {
        return try {
            database.withTransaction {
                // Remove linked account allocations
                goalAccountDao.unlinkAllAccountsFromGoal(goalId)
                // Remove all contributions for this goal
                contributionDao.deleteContributionsByGoal(goalId)
                // Remove goal-related transactions (contributions/withdrawals)
                transactionDao.deleteTransactionsByGoalId(goalId)
                // Remove auto-allocation rules targeting this goal
                autoAllocationRuleDao.deleteRulesByGoal(goalId)
                // Remove the goal itself
                goalDao.deleteGoalById(goalId)
            }
            scheduleGoalSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Net funds this goal holds per account (for the Complete/Delete funds dialogs). */
    suspend fun getGoalFundsByAccount(goalId: String): Map<String, Double> =
        contributionDao.getNetAllocationsByGoal(goalId).associate { it.accountId to it.total }

    /** Available cash in an account: balance minus funds set aside in non-completed goals. */
    suspend fun getAvailableCash(accountId: String): Double {
        val account = accountDao.getAccountById(accountId) ?: return 0.0
        return (account.balance - contributionDao.getSetAsideByAccount(accountId)).coerceAtLeast(0.0)
    }

    /**
     * Complete a goal and physically transfer its set-aside funds to another account.
     * Atomic: all per-account transfers + status change in one transaction.
     */
    suspend fun completeGoalWithTransfer(goalId: String, targetAccountId: String): Result<Unit> {
        return try {
            val goal = goalDao.getGoalById(goalId) ?: return Result.failure(IllegalArgumentException("Goal not found"))
            val funds = contributionDao.getNetAllocationsByGoal(goalId)
            database.withTransaction {
                transferFunds(goal, funds, targetAccountId)
                goalDao.updateGoalStatus(goalId, GoalStatus.COMPLETED.value)
                val updated = goalDao.getGoalById(goalId)
                if (updated != null) goalDao.updateGoal(updated.copy(isSynced = false))
            }
            scheduleGoalSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a goal after physically transferring its set-aside funds to another account.
     * Atomic: transfers + full cleanup in one transaction.
     */
    suspend fun deleteGoalWithTransfer(goalId: String, targetAccountId: String): Result<Unit> {
        return try {
            val goal = goalDao.getGoalById(goalId) ?: return Result.failure(IllegalArgumentException("Goal not found"))
            val funds = contributionDao.getNetAllocationsByGoal(goalId)
            database.withTransaction {
                transferFunds(goal, funds, targetAccountId)
                goalAccountDao.unlinkAllAccountsFromGoal(goalId)
                contributionDao.deleteContributionsByGoal(goalId)
                transactionDao.deleteTransactionsByGoalId(goalId)
                autoAllocationRuleDao.deleteRulesByGoal(goalId)
                goalDao.deleteGoalById(goalId)
            }
            scheduleGoalSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Move each account's net allocation to the target account (real balance movement + transfer transaction pair). */
    private suspend fun transferFunds(goal: GoalEntity, funds: List<AccountAllocationRow>, targetAccountId: String) {
        val now = System.currentTimeMillis()
        for (row in funds) {
            if (row.accountId == targetAccountId || row.total <= 0) continue
            // Skip sources that no longer exist or are deactivated (e.g. a deleted account
            // whose contributions remain as goal history) — their funds can't be moved.
            val source = accountDao.getAccountById(row.accountId)
            if (source == null || !source.isActive) continue
            accountDao.updateBalance(row.accountId, -row.total)
            accountDao.updateBalance(targetAccountId, row.total)
            val transferId = java.util.UUID.randomUUID().toString()
            transactionDao.insertTransaction(Transaction(
                title = "Transfer: ${goal.name}", amount = row.total, category = "Transfer",
                date = now, type = TransactionType.TRANSFER_OUT,
                description = "Funds from completed goal \"${goal.name}\"",
                accountId = row.accountId, relatedAccountId = targetAccountId,
                goalId = goal.id, goalName = goal.name,
                transferId = transferId, sourceModule = "transfer", isSynced = false
            ))
            transactionDao.insertTransaction(Transaction(
                title = "Transfer: ${goal.name}", amount = row.total, category = "Transfer",
                date = now, type = TransactionType.TRANSFER_IN,
                description = "Funds from completed goal \"${goal.name}\"",
                accountId = targetAccountId, relatedAccountId = row.accountId,
                goalId = goal.id, goalName = goal.name,
                transferId = transferId, sourceModule = "transfer", isSynced = false
            ))
        }
    }

    suspend fun updateGoalStatus(goalId: String, status: GoalStatus): Result<Unit> {
        return try {
            goalDao.updateGoalStatus(goalId, status.value)
            val entity = goalDao.getGoalById(goalId)
            if (entity != null) {
                goalDao.updateGoal(entity.copy(isSynced = false))
            }
            scheduleGoalSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setAutoAllocate(goalId: String, enabled: Boolean): Result<Unit> {
        return try {
            goalDao.updateAutoAllocate(goalId, enabled)
            val entity = goalDao.getGoalById(goalId)
            if (entity != null) {
                goalDao.updateGoal(entity.copy(isSynced = false))
            }
            scheduleGoalSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveGoalCount(): Int = goalDao.getActiveGoalCount()

    suspend fun contribute(
        goalId: String,
        accountId: String,
        amount: Double,
        type: ContributionType = ContributionType.MANUAL,
        transactionId: String? = null,
        notes: String = ""
    ): Result<Contribution> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be positive"))

            // ── Mutex: serialize auto-allocation per account to prevent overdrafts ──
            if (type == ContributionType.AUTO_ALLOCATION) {
                return mutexForAccount(accountId).withLock {
                    contributeInternal(goalId, accountId, amount, type, transactionId, notes)
                }
            }

            contributeInternal(goalId, accountId, amount, type, transactionId, notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun contributeInternal(
        goalId: String,
        accountId: String,
        amount: Double,
        type: ContributionType,
        transactionId: String?,
        notes: String
    ): Result<Contribution> {
        // ── Idempotency: skip if this transaction already triggered an allocation ──
        if (type == ContributionType.AUTO_ALLOCATION && !transactionId.isNullOrBlank()) {
            val existing = contributionDao.getContributionByTransaction(transactionId)
            if (existing != null) {
                Log.d(TAG, "[Idempotency] Skipping — contribution already exists for transactionId=$transactionId")
                return Result.success(Contribution.fromEntity(existing))
            }
        }

        val goal = goalDao.getGoalById(goalId) ?: return Result.failure(IllegalArgumentException("Goal not found"))
        val account = accountDao.getAccountById(accountId) ?: return Result.failure(IllegalArgumentException("Account not found"))
        if (!account.isActive) return Result.failure(IllegalArgumentException("Account is no longer active"))
        // Envelope model: contributions set money aside — check available cash, not raw balance
        val availableCash = account.balance - contributionDao.getSetAsideByAccount(accountId)
        if (availableCash < amount) return Result.failure(IllegalArgumentException("Insufficient available cash"))

        val contribution = atomicContribute(goalId, accountId, amount, type, transactionId, notes, goal)
        scheduleGoalSync()
        return Result.success(Contribution.fromEntity(contribution))
    }

    private suspend fun atomicContribute(
        goalId: String, accountId: String, amount: Double, type: ContributionType,
        transactionId: String?, notes: String, goal: GoalEntity
    ): ContributionEntity {
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val contribution = ContributionEntity(
                goalId = goalId, accountId = accountId, amount = amount,
                type = type.value, transactionId = transactionId, notes = notes, isSynced = false,
                createdAt = now
            )
            contributionDao.insertContribution(contribution)
            // Envelope model: balance is the total pool — contributions only set money aside.

            val currentAmount = contributionDao.getTotalContributed(goalId)
            if (currentAmount >= goal.targetAmount && goal.goalStatus != GoalStatus.COMPLETED) {
                goalDao.updateGoalStatus(goalId, GoalStatus.COMPLETED.value)
                val updatedGoal = goalDao.getGoalById(goalId)
                if (updatedGoal != null) goalDao.updateGoal(updatedGoal.copy(isSynced = false))
                notificationHelper.showGoalCompletedNotification(goal.name)
            }

            val txType = if (type == ContributionType.AUTO_ALLOCATION) TransactionType.AUTO_ALLOCATION else TransactionType.GOAL_CONTRIBUTION
            val txTitle = if (type == ContributionType.AUTO_ALLOCATION) "Auto: ${goal.name}" else "Goal: ${goal.name}"
            val tx = Transaction(
                title = txTitle, amount = -amount, category = txType.name.replace('_', ' '),
                date = now, type = txType,
                description = notes.ifBlank { "Contribution to ${goal.name}" },
                accountId = accountId, goalId = goalId, goalName = goal.name,
                sourceModule = if (type == ContributionType.AUTO_ALLOCATION) "auto_allocation" else "goal",
                referenceId = contribution.id, isAuto = type == ContributionType.AUTO_ALLOCATION
            )
            transactionDao.insertTransaction(tx.copy(isSynced = false))
            contribution
        }
    }

    /**
     * Reverse all auto-allocation contributions triggered by a specific transaction.
     * Called when an income transaction is edited or deleted after allocation.
     *
     * @return number of reversed contributions
     */
    suspend fun reverseAllocationsForTransaction(transactionId: String): Int {
        val contributions = contributionDao.getContributionsByTransactionType(
            transactionId, ContributionType.AUTO_ALLOCATION.value
        )
        if (contributions.isEmpty()) return 0

        var reversedCount = 0
        for (contribution in contributions) {
            try {
                mutexForAccount(contribution.accountId).withLock {
                    database.withTransaction {
                        // 1. Delete the contribution (envelope model: set-aside shrinks automatically)
                        contributionDao.deleteContribution(contribution)

                        // 2. Delete the auto-allocation transaction record
                        transactionDao.deleteTransactionByReferenceId(contribution.id)

                        // 3. Re-evaluate goal completion status
                        val goal = goalDao.getGoalById(contribution.goalId)
                        if (goal != null) {
                            val currentAmount = contributionDao.getTotalContributed(contribution.goalId)
                            if (currentAmount < goal.targetAmount && goal.goalStatus == GoalStatus.COMPLETED) {
                                goalDao.updateGoalStatus(contribution.goalId, GoalStatus.ACTIVE.value)
                                goalDao.updateGoal(goal.copy(isSynced = false))
                            }
                        }
                    }
                    reversedCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reverse contribution ${contribution.id}: ${e.message}", e)
            }
        }

        if (reversedCount > 0) {
            scheduleGoalSync()
            Log.d(TAG, "Reversed $reversedCount allocation(s) for transactionId=$transactionId")
        }
        return reversedCount
    }

    suspend fun withdraw(goalId: String, accountId: String, amount: Double, notes: String = ""): Result<Contribution> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be positive"))
            val currentAmount = contributionDao.getNetAmount(goalId)
            if (currentAmount < amount) return Result.failure(IllegalArgumentException("Insufficient goal balance"))

            val contribution = atomicWithdraw(goalId, accountId, amount, notes)
            scheduleGoalSync()
            Result.success(Contribution.fromEntity(contribution))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun atomicWithdraw(goalId: String, accountId: String, amount: Double, notes: String): ContributionEntity {
        return database.withTransaction {
            val goal = goalDao.getGoalById(goalId)
            val now = System.currentTimeMillis()
            val contribution = ContributionEntity(
                goalId = goalId, accountId = accountId, amount = -amount,
                type = ContributionType.WITHDRAWAL.value, notes = notes, isSynced = false,
                createdAt = now
            )
            contributionDao.insertContribution(contribution)
            // Envelope model: withdrawing frees set-aside — balance (the pool) never changed.

            val tx = Transaction(
                title = context.getString(R.string.goal_withdraw_title, goal?.name ?: context.getString(R.string.goals_title)), amount = amount,
                category = "Goal Withdrawal", date = now,
                type = TransactionType.GOAL_WITHDRAWAL,
                description = notes.ifBlank { "Withdrawal from ${goal?.name ?: "Goal"}" },
                accountId = accountId, goalId = goalId, goalName = goal?.name,
                sourceModule = "goal", referenceId = contribution.id
            )
            transactionDao.insertTransaction(tx.copy(isSynced = false))
            contribution
        }
    }

    fun getContributionsByGoal(goalId: String): Flow<List<Contribution>> {
        return contributionDao.getContributionsByGoal(goalId).map { entities ->
            entities.map { Contribution.fromEntity(it) }
        }
    }

    suspend fun getGoalProgress(goalId: String): GoalProgress {
        val goal = goalDao.getGoalById(goalId) ?: return GoalProgress.empty(goalId)
        val currentAmount = contributionDao.getTotalContributed(goalId)
        val totalWithdrawn = contributionDao.getTotalWithdrawn(goalId)
        val contributionCount = contributionDao.getContributionCount(goalId)
        val thirtyDaysAgo = LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val avgDaily = contributionDao.getAverageDailyContribution(goalId, thirtyDaysAgo)
        val deadline = goal.deadline?.let {
            val deadlineDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate).toInt().coerceAtLeast(0)
        }
        return GoalProgress(
            goalId = goalId, currentAmount = currentAmount, targetAmount = goal.targetAmount,
            totalWithdrawn = totalWithdrawn,
            progressPercent = if (goal.targetAmount > 0) ((currentAmount / goal.targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0,
            remainingAmount = (goal.targetAmount - currentAmount).coerceAtLeast(0.0),
            daysRemaining = deadline, isOnTrack = true,
            averageDailyContribution = avgDaily, contributionCount = contributionCount
        )
    }

    suspend fun linkAccountToGoal(goalId: String, accountId: String, allocationPercent: Double = 100.0, isPrimary: Boolean = false): Result<Unit> {
        return try {
            if (isPrimary) goalAccountDao.clearPrimaryForGoal(goalId)
            goalAccountDao.insertGoalAccount(GoalAccountEntity(goalId, accountId, allocationPercent, isPrimary))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun unlinkAccountFromGoal(goalId: String, accountId: String): Result<Unit> {
        return try { goalAccountDao.unlinkAccountFromGoal(goalId, accountId); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    fun getLinkedAccounts(goalId: String): Flow<List<GoalAccountEntity>> = goalAccountDao.getGoalAccountsByGoal(goalId)

    /** All goal↔account links as a stream (re-emits on any link change). */
    fun getAllGoalAccountLinks(): Flow<List<GoalAccountEntity>> = goalAccountDao.getAllGoalAccounts()

    fun getLinkedAccountsWithInfo(goalId: String): Flow<List<Pair<GoalAccountEntity, Account?>>> {
        return goalAccountDao.getGoalAccountsByGoal(goalId).map { entities ->
            entities.map { entity -> entity to accountDao.getAccountById(entity.accountId) }
        }
    }

    fun getDailyTarget(): Flow<DailyTarget> {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        val dayStart = today.toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).toInstant().toEpochMilli()
        return combine(dailyTargetDao.getGlobalDailyTarget(), contributionDao.getTodayContributionsFlow(dayStart, dayEnd)) { entity, todayAmount ->
            DailyTarget.fromEntity(entity, todayAmount)
        }
    }

    suspend fun setDailyTarget(amount: Double): Result<Unit> {
        return try { dailyTargetDao.insertOrUpdateDailyTarget(DailyTargetEntity(targetAmount = amount)); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun clearDailyTarget(): Result<Unit> {
        return try { dailyTargetDao.clearDailyTarget(); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    fun getAutoAllocationRules(): Flow<List<AutoAllocationRule>> {
        return combine(
            autoAllocationRuleDao.getAllRules(),
            goalDao.getAllGoalsIncludingArchived()
        ) { entities, goals ->
            val goalMap = goals.associateBy { it.id }
            entities.map { entity -> AutoAllocationRule.fromEntity(entity, goalMap[entity.goalId]?.name ?: "") }
        }.flowOn(Dispatchers.IO)
    }

    fun getEnabledRules(): Flow<List<AutoAllocationRule>> {
        return combine(
            autoAllocationRuleDao.getEnabledRules(),
            goalDao.getAllGoalsIncludingArchived()
        ) { entities, goals ->
            val goalMap = goals.associateBy { it.id }
            entities.map { entity -> AutoAllocationRule.fromEntity(entity, goalMap[entity.goalId]?.name ?: "") }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Allocation rules for a single goal, as domain models.
     * Lets the presentation layer observe rules without depending on the DAO directly.
     */
    fun getRulesByGoalFlow(goalId: String): Flow<List<AutoAllocationRule>> {
        return autoAllocationRuleDao.getRulesByGoal(goalId).map { entities ->
            entities.map { AutoAllocationRule.fromEntity(it) }
        }
    }

    /**
     * Record that an auto-allocation rule fired, so it is not re-triggered.
     */
    suspend fun markRuleExecuted(ruleId: String, timestamp: Long) {
        autoAllocationRuleDao.setLastExecutedAt(ruleId, timestamp)
    }

    suspend fun addAutoAllocationRule(rule: AutoAllocationRule): Result<Unit> {
        return try { goalDao.updateAutoAllocate(rule.goalId, true); autoAllocationRuleDao.insertRule(rule.toEntity()); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateAutoAllocationRule(rule: AutoAllocationRule): Result<Unit> {
        return try { autoAllocationRuleDao.updateRule(rule.toEntity()); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteAutoAllocationRule(ruleId: String): Result<Unit> {
        return try {
            autoAllocationRuleDao.deleteRuleById(ruleId)
            if (autoAllocationRuleDao.getEnabledRuleCount() == 0) {
                goalDao.getAllActiveGoals().first().forEach { goalDao.updateAutoAllocate(it.id, false) }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Result<Unit> {
        return try { autoAllocationRuleDao.setRuleEnabled(ruleId, enabled); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    fun getReservedBalance(accountId: String): Flow<Double> = reservedBalanceDao.getTotalReservedForAccountFlow(accountId)
    suspend fun getAvailableBalance(accountId: String): Double? = reservedBalanceDao.getAvailableBalance(accountId)

    suspend fun reserveFunds(accountId: String, amount: Double, goalId: String?): Result<Unit> {
        return try { reservedBalanceDao.increaseReservation(accountId, amount, goalId); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun releaseReservedFunds(accountId: String, amount: Double): Result<Unit> {
        return try { reservedBalanceDao.decreaseReservation(accountId, amount); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    fun getGoalsSummary(): Flow<GoalSummary> {
        return combine(
            goalDao.getAllActiveGoals(),
            contributionDao.getTotalAllocatedFromAccountFlow("")  // invalidation trigger
        ) { entities, _ ->
            val activeGoals = entities.filter { it.goalStatus != GoalStatus.ARCHIVED }
            val totalsMap = contributionDao.getAllGoalTotals().associate { it.goalId to it.total }
            val totalSaved = activeGoals.sumOf { totalsMap[it.id] ?: 0.0 }
            val totalTarget = activeGoals.sumOf { it.targetAmount }
            GoalSummary(entities.size, activeGoals.count { it.goalStatus == GoalStatus.ACTIVE },
                activeGoals.count { it.goalStatus == GoalStatus.COMPLETED }, totalSaved, totalTarget,
                if (totalTarget > 0) (totalSaved / totalTarget) * 100 else 0.0)
        }.flowOn(Dispatchers.IO)
    }

    fun hasUnsyncedGoalsFlow(): Flow<Boolean> = goalDao.getUnsyncedGoalsFlow().map { it.isNotEmpty() }

    private fun scheduleGoalSync() {
        val request = OneTimeWorkRequestBuilder<SyncGoalWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(context).enqueueUniqueWork("SyncGoalRecovery", ExistingWorkPolicy.KEEP, request)
    }

    suspend fun syncUnsyncedGoals(userId: String) {
        val unsyncedGoals = goalDao.getUnsyncedGoals()
        if (unsyncedGoals.isEmpty()) return
        for (goalEntity in unsyncedGoals) {
            try {
                val goalDomain = Goal.fromEntity(goalEntity, contributionDao.getTotalContributed(goalEntity.id))
                firestore.collection("users").document(userId).collection("goals").document(goalEntity.id)
                    .set(goalDomain.toFirestoreMap()).await()
                goalDao.markAsSynced(goalEntity.id)
            } catch (e: Exception) { Log.w(TAG, "syncUnsyncedGoals: failed ${goalEntity.id}", e) }
        }
    }

    suspend fun syncUnsyncedContributions(userId: String) {
        val unsynced = contributionDao.getUnsyncedContributions()
        if (unsynced.isEmpty()) return
        for (contribution in unsynced) {
            try {
                firestore.collection("users").document(userId).collection("contributions").document(contribution.id)
                    .set(contribution).await()
                contributionDao.markAsSynced(contribution.id)
            } catch (e: Exception) { Log.w(TAG, "syncUnsyncedContributions: failed ${contribution.id}", e) }
        }
    }

    /**
     * One-time migration: fix contributions with epoch (0L) timestamps.
     * Attempts to recover the real timestamp from the linked Transaction's
     * [Transaction.date] field via [Transaction.referenceId].
     */
    private var legacyTimestampFixRun = false

    suspend fun fixLegacyContributionTimestamps() {
        if (legacyTimestampFixRun) return
        legacyTimestampFixRun = true
        try {
            val cutoff = com.example.insightku.core.utils.AppConstants.EPOCH_CUTOFF_MS
            val broken = contributionDao.getContributionsWithEpochTimestamps(cutoff)
            if (broken.isEmpty()) return
            Log.d(TAG, "Fixing ${broken.size} contribution(s) with epoch timestamps")
            var fixed = 0
            for (contribution in broken) {
                val newTimestamp = resolveContributionTimestamp(contribution)
                contributionDao.updateContributionTimestamp(contribution.id, newTimestamp)
                fixed++
            }
            Log.d(TAG, "Fixed $fixed contribution timestamp(s)")
        } catch (e: Exception) {
            Log.w(TAG, "fixLegacyContributionTimestamps failed", e)
        }
    }

    private suspend fun resolveContributionTimestamp(contribution: ContributionEntity): Long {
        // 1) If transactionId is set, use the Transaction's date
        if (!contribution.transactionId.isNullOrBlank()) {
            val tx = transactionDao.getTransactionById(contribution.transactionId)
            if (tx != null && tx.date > AppConstants.EPOCH_CUTOFF_MS) return tx.date
        }
        // 2) If referenceId links a Transaction to this contribution, use that Transaction's date
        //    (Transaction.referenceId == contribution.id for linked records)
        val linkedTx = transactionDao.getTransactionById(contribution.id)
        if (linkedTx != null && linkedTx.date > AppConstants.EPOCH_CUTOFF_MS) return linkedTx.date
        // 3) Use the goal's createdAt as best-effort
        val goal = goalDao.getGoalById(contribution.goalId)
        if (goal != null && goal.createdAt > AppConstants.EPOCH_CUTOFF_MS) return goal.createdAt
        // 4) Final fallback: now
        return System.currentTimeMillis()
    }

    suspend fun refreshGoalsFromFirestore(userId: String) {
        try {
            val snapshot = firestore.collection("users").document(userId).collection("goals").get().await()
            val remoteGoals = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { data ->
                    GoalEntity(id = doc.id, name = data["name"] as? String ?: "",
                        targetAmount = (data["targetAmount"] as? Number)?.toDouble() ?: 0.0,
                        deadline = (data["deadline"] as? Number)?.toLong(),
                        status = data["status"] as? String ?: "active",
                        autoAllocate = data["autoAllocate"] as? Boolean ?: false,
                        allocationPriority = (data["allocationPriority"] as? Number)?.toInt() ?: 0,
                        iconName = data["iconName"] as? String ?: "savings",
                        color = data["color"] as? String ?: "#7C4DFF",
                        notes = data["notes"] as? String ?: "",
                        reminderEnabled = data["reminderEnabled"] as? Boolean ?: false,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L, isSynced = true)
                }
            }
            goalDao.insertGoalsFromRemote(remoteGoals)

            val contribSnapshot = firestore.collection("users").document(userId).collection("contributions").get().await()
            val remoteContributions = contribSnapshot.documents.mapNotNull { doc ->
                doc.data?.let { data ->
                    ContributionEntity(id = doc.id, goalId = data["goalId"] as? String ?: "",
                        accountId = data["accountId"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        type = data["type"] as? String ?: "manual",
                        transactionId = data["transactionId"] as? String,
                        notes = data["notes"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(), isSynced = true)
                }
            }
            contributionDao.insertContributionsFromRemote(remoteContributions)
        } catch (e: Exception) { Log.w(TAG, "refreshGoalsFromFirestore: failed", e) }
    }
}

private fun Goal.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "targetAmount" to targetAmount,
    "deadline" to deadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    "status" to status.value, "autoAllocate" to autoAllocate, "allocationPriority" to allocationPriority,
    "iconName" to iconName, "color" to color, "notes" to notes, "reminderEnabled" to reminderEnabled,
    "createdAt" to createdAt.toEpochMilli(), "updatedAt" to updatedAt.toEpochMilli()
)
