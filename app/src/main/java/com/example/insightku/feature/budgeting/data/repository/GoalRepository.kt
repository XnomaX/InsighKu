package com.example.insightku.feature.budgeting.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
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
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.budgeting.data.local.dao.*
import com.example.insightku.feature.budgeting.data.model.*
import com.example.insightku.feature.budgeting.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for goal-related operations.
 * Orchestrates Room (offline-first single source of truth) + Firestore (background sync).
 *
 * All writes go to Room immediately → UI updates instantly via Flow.
 * Firestore sync happens in background via WorkManager.
 * Users never wait for Firebase before seeing changes.
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
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    private val database: InsightKuDatabase,
    private val notificationHelper: AutoAllocationNotificationHelper
) {
    companion object {
        private const val TAG = "GoalRepository"
    }

    // ── Goal Operations ─────────────────────────────────────────────────────────

    /**
     * Get all active goals with their current amounts.
     * UI observes Room Flow — instant updates after any local write.
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
     * Combines goal entity + contribution sum — both from Room.
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
     * 1. Save to Room immediately (isSynced=false) → UI updates
     * 2. Try Firestore sync in background
     */
    suspend fun createGoal(goal: Goal): Result<Goal> {
        return try {
            val entity = goal.toEntity().copy(isSynced = false)
            goalDao.insertGoal(entity)

            // Background Firestore sync
            scheduleGoalSync()

            Result.success(goal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing goal.
     * 1. Update Room immediately (isSynced=false) → UI updates
     * 2. Try Firestore sync in background
     */
    suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            val entity = goal.toEntity().copy(isSynced = false)
            goalDao.updateGoal(entity)

            // Background Firestore sync
            scheduleGoalSync()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Archive a goal (soft delete).
     * 1. Update Room immediately (isSynced=false) → UI updates
     * 2. Background Firestore sync
     */
    suspend fun archiveGoal(goalId: String): Result<Unit> {
        return try {
            goalDao.archiveGoal(goalId)
            // Mark as unsynced for Firestore
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

    /**
     * Update goal status.
     */
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

    // ── Contribution Operations (Offline-First) ────────────────────────────────

    /**
     * Contribute to a goal — ATOMIC Room transaction.
     *
     * Flow:
     * 1. Validate inputs
     * 2. Write ALL changes to Room in a single transaction (isSynced=false)
     *    → Room Flow emits → UI updates instantly
     * 3. Queue Firestore sync in background
     *
     * If any step fails, Room transaction rolls back — no partial state.
     * User never waits for Firebase.
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
            // Validate
            if (amount <= 0) {
                return Result.failure(IllegalArgumentException("Amount must be positive"))
            }
            val goal = goalDao.getGoalById(goalId)
                ?: return Result.failure(IllegalArgumentException("Goal not found"))
            val account = accountDao.getAccountById(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))
            if (account.balance < amount) {
                return Result.failure(IllegalArgumentException("Insufficient balance"))
            }

            // Atomic Room transaction — all writes succeed or all fail
            // Includes contribution + account balance + unified ledger Transaction
            val contribution = atomicContribute(goalId, accountId, amount, type, transactionId, notes, goal)

            // Queue background Firestore sync
            scheduleGoalSync()

            Result.success(Contribution.fromEntity(contribution))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun atomicContribute(
        goalId: String,
        accountId: String,
        amount: Double,
        type: ContributionType,
        transactionId: String?,
        notes: String,
        goal: GoalEntity
    ): ContributionEntity {
        return database.withTransaction {
            // 1. Create contribution (isSynced=false)
            val contribution = ContributionEntity(
                goalId = goalId,
                accountId = accountId,
                amount = amount,
                type = type.value,
                transactionId = transactionId,
                notes = notes,
                isSynced = false
            )
            contributionDao.insertContribution(contribution)

            // 2. Deduct from account balance
            accountDao.updateBalance(accountId, -amount)

            // 3. Check if goal is now complete
            val currentAmount = contributionDao.getTotalContributed(goalId)
            if (currentAmount >= goal.targetAmount && goal.goalStatus != GoalStatus.COMPLETED) {
                goalDao.updateGoalStatus(goalId, GoalStatus.COMPLETED.value)
                val updatedGoal = goalDao.getGoalById(goalId)
                if (updatedGoal != null) {
                    goalDao.updateGoal(updatedGoal.copy(isSynced = false))
                }
                // Notify user about goal completion
                notificationHelper.showGoalCompletedNotification(goal.name)
            }

            // 4. Create unified ledger Transaction (inside atomic block)
            val txType = if (type == ContributionType.AUTO_ALLOCATION) TransactionType.AUTO_ALLOCATION else TransactionType.GOAL_CONTRIBUTION
            val txTitle = if (type == ContributionType.AUTO_ALLOCATION) "Auto: ${goal.name}" else "Goal: ${goal.name}"
            val tx = Transaction(
                title = txTitle,
                amount = -amount,
                category = txType.name.replace('_', ' '),
                date = System.currentTimeMillis(),
                type = txType,
                description = notes.ifBlank { "Contribution to ${goal.name}" },
                accountId = accountId,
                goalId = goalId,
                goalName = goal.name,
                sourceModule = if (type == ContributionType.AUTO_ALLOCATION) "auto_allocation" else "goal",
                referenceId = contribution.id,
                isAuto = type == ContributionType.AUTO_ALLOCATION
            )
            transactionDao.insertTransaction(tx.copy(isSynced = false))

            contribution
        }
    }

    /**
     * Withdraw from a goal — ATOMIC Room transaction.
     *
     * Flow:
     * 1. Validate inputs
     * 2. Write ALL changes to Room in a single transaction (isSynced=false)
     *    → Room Flow emits → UI updates instantly
     * 3. Queue Firestore sync in background
     */
    suspend fun withdraw(
        goalId: String,
        accountId: String,
        amount: Double,
        notes: String = ""
    ): Result<Contribution> {
        return try {
            // Validate
            if (amount <= 0) {
                return Result.failure(IllegalArgumentException("Amount must be positive"))
            }
            val currentAmount = contributionDao.getNetAmount(goalId)
            if (currentAmount < amount) {
                return Result.failure(IllegalArgumentException("Insufficient goal balance"))
            }

            // Atomic Room transaction — includes withdrawal + balance + ledger Transaction
            val contribution = atomicWithdraw(goalId, accountId, amount, notes)

            // Queue background Firestore sync
            scheduleGoalSync()

            Result.success(Contribution.fromEntity(contribution))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomic Room transaction for withdrawal.
     * Uses RoomDatabase.withTransaction to ensure all writes are atomic.
     */
    private suspend fun atomicWithdraw(
        goalId: String,
        accountId: String,
        amount: Double,
        notes: String
    ): ContributionEntity {
        return database.withTransaction {
            val goal = goalDao.getGoalById(goalId)

            // 1. Create withdrawal (negative contribution, isSynced=false)
            val contribution = ContributionEntity(
                goalId = goalId,
                accountId = accountId,
                amount = -amount,
                type = ContributionType.WITHDRAWAL.value,
                notes = notes,
                isSynced = false
            )
            contributionDao.insertContribution(contribution)

            // 2. Return funds to account
            accountDao.updateBalance(accountId, amount)

            // 3. Create unified ledger Transaction (inside atomic block)
            val tx = Transaction(
                title = "Withdraw: ${goal?.name ?: "Goal"}",
                amount = amount,
                category = "Goal Withdrawal",
                date = System.currentTimeMillis(),
                type = TransactionType.GOAL_WITHDRAWAL,
                description = notes.ifBlank { "Withdrawal from ${goal?.name ?: "Goal"}" },
                accountId = accountId,
                goalId = goalId,
                goalName = goal?.name,
                sourceModule = "goal",
                referenceId = contribution.id
            )
            transactionDao.insertTransaction(tx.copy(isSynced = false))

            contribution
        }
    }

    /**
     * Get contributions for a goal.
     * UI observes Room Flow — instant updates.
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
            isOnTrack = true,
            averageDailyContribution = avgDaily,
            contributionCount = contributionCount
        )
    }

    // ── Account Linking ────────────────────────────────────────────────────────

    suspend fun linkAccountToGoal(
        goalId: String,
        accountId: String,
        allocationPercent: Double = 100.0,
        isPrimary: Boolean = false
    ): Result<Unit> {
        return try {
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

    suspend fun unlinkAccountFromGoal(goalId: String, accountId: String): Result<Unit> {
        return try {
            goalAccountDao.unlinkAccountFromGoal(goalId, accountId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLinkedAccounts(goalId: String): Flow<List<GoalAccountEntity>> {
        return goalAccountDao.getGoalAccountsByGoal(goalId)
    }

    fun getLinkedAccountsWithInfo(goalId: String): Flow<List<Pair<GoalAccountEntity, Account?>>> {
        return goalAccountDao.getGoalAccountsByGoal(goalId).map { entities ->
            entities.map { entity ->
                val account = accountDao.getAccountById(entity.accountId)
                entity to account
            }
        }
    }

    // ── Daily Target Operations ─────────────────────────────────────────────────

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

    suspend fun setDailyTarget(amount: Double): Result<Unit> {
        return try {
            val entity = DailyTargetEntity(targetAmount = amount)
            dailyTargetDao.insertOrUpdateDailyTarget(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearDailyTarget(): Result<Unit> {
        return try {
            dailyTargetDao.clearDailyTarget()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Auto-Allocation Operations ───────────────────────────────────────────────

    fun getAutoAllocationRules(): Flow<List<AutoAllocationRule>> {
        return autoAllocationRuleDao.getAllRules().map { entities ->
            entities.map { entity ->
                val goalName = goalDao.getGoalById(entity.goalId)?.name ?: ""
                AutoAllocationRule.fromEntity(entity, goalName)
            }
        }
    }

    fun getEnabledRules(): Flow<List<AutoAllocationRule>> {
        return autoAllocationRuleDao.getEnabledRules().map { entities ->
            entities.map { entity ->
                val goalName = goalDao.getGoalById(entity.goalId)?.name ?: ""
                AutoAllocationRule.fromEntity(entity, goalName)
            }
        }
    }

    suspend fun addAutoAllocationRule(rule: AutoAllocationRule): Result<Unit> {
        return try {
            goalDao.updateAutoAllocate(rule.goalId, true)
            autoAllocationRuleDao.insertRule(rule.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAutoAllocationRule(rule: AutoAllocationRule): Result<Unit> {
        return try {
            autoAllocationRuleDao.updateRule(rule.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAutoAllocationRule(ruleId: String): Result<Unit> {
        return try {
            autoAllocationRuleDao.deleteRuleById(ruleId)
            val hasOtherRules = autoAllocationRuleDao.getEnabledRuleCount() > 0
            if (!hasOtherRules) {
                goalDao.getAllActiveGoals().first().forEach { goal ->
                    goalDao.updateAutoAllocate(goal.id, false)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Result<Unit> {
        return try {
            autoAllocationRuleDao.setRuleEnabled(ruleId, enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reserved Balance Operations ─────────────────────────────────────────────

    fun getReservedBalance(accountId: String): Flow<Double> {
        return reservedBalanceDao.getTotalReservedForAccountFlow(accountId)
    }

    suspend fun getAvailableBalance(accountId: String): Double? {
        return reservedBalanceDao.getAvailableBalance(accountId)
    }

    suspend fun reserveFunds(accountId: String, amount: Double, goalId: String?): Result<Unit> {
        return try {
            reservedBalanceDao.increaseReservation(accountId, amount, goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun releaseReservedFunds(accountId: String, amount: Double): Result<Unit> {
        return try {
            reservedBalanceDao.decreaseReservation(accountId, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Summary Operations ─────────────────────────────────────────────────────

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

    // ── Offline-First Sync Support ──────────────────────────────────────────────

    /**
     * Flow that emits whenever there are unsynced goal changes.
     * UI can observe this to show/hide pending sync indicator.
     */
    fun hasUnsyncedGoalsFlow(): Flow<Boolean> {
        return goalDao.getUnsyncedGoalsFlow().map { it.isNotEmpty() }
    }

    /**
     * Queue a one-shot background sync for all unsynced goal data.
     * Uses WorkManager with NetworkConstraint so it only runs when online.
     */
    private fun scheduleGoalSync() {
        val request = OneTimeWorkRequestBuilder<SyncGoalWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncGoalRecovery",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Sync unsynced goals to Firestore.
     * Called by SyncGoalWorker in background.
     */
    suspend fun syncUnsyncedGoals(userId: String) {
        val unsyncedGoals = goalDao.getUnsyncedGoals()
        if (unsyncedGoals.isEmpty()) return

        Log.d(TAG, "syncUnsyncedGoals: syncing ${unsyncedGoals.size} goals")
        for (goalEntity in unsyncedGoals) {
            try {
                val goalDomain = Goal.fromEntity(goalEntity, contributionDao.getTotalContributed(goalEntity.id))
                firestore.collection("users").document(userId)
                    .collection("goals").document(goalEntity.id)
                    .set(goalDomain.toFirestoreMap())
                    .await()
                goalDao.markAsSynced(goalEntity.id)
                Log.d(TAG, "syncUnsyncedGoals: synced goal ${goalEntity.id}")
            } catch (e: Exception) {
                Log.w(TAG, "syncUnsyncedGoals: failed to sync goal ${goalEntity.id}", e)
                // Will retry on next sync attempt
            }
        }
    }

    /**
     * Sync unsynced contributions to Firestore.
     * Called by SyncGoalWorker in background.
     */
    suspend fun syncUnsyncedContributions(userId: String) {
        val unsyncedContributions = contributionDao.getUnsyncedContributions()
        if (unsyncedContributions.isEmpty()) return

        Log.d(TAG, "syncUnsyncedContributions: syncing ${unsyncedContributions.size} contributions")
        for (contribution in unsyncedContributions) {
            try {
                firestore.collection("users").document(userId)
                    .collection("contributions").document(contribution.id)
                    .set(contribution)
                    .await()
                contributionDao.markAsSynced(contribution.id)
                Log.d(TAG, "syncUnsyncedContributions: synced contribution ${contribution.id}")
            } catch (e: Exception) {
                Log.w(TAG, "syncUnsyncedContributions: failed to sync contribution ${contribution.id}", e)
            }
        }
    }

    /**
     * Refresh goals from Firestore into Room.
     * Uses IGNORE strategy so local unsynced data is never overwritten.
     */
    suspend fun refreshGoalsFromFirestore(userId: String) {
        try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("goals").get().await()
            val remoteGoals = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { data ->
                    GoalEntity(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        targetAmount = (data["targetAmount"] as? Number)?.toDouble() ?: 0.0,
                        deadline = (data["deadline"] as? Number)?.toLong(),
                        status = data["status"] as? String ?: "active",
                        autoAllocate = data["autoAllocate"] as? Boolean ?: false,
                        allocationPriority = (data["allocationPriority"] as? Number)?.toInt() ?: 0,
                        iconName = data["iconName"] as? String ?: "savings",
                        color = data["color"] as? String ?: "#7C4DFF",
                        notes = data["notes"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
                        isSynced = true // Remote data is by definition synced
                    )
                }
            }
            goalDao.insertGoalsFromRemote(remoteGoals)

            // Also refresh contributions
            val contribSnapshot = firestore.collection("users").document(userId)
                .collection("contributions").get().await()
            val remoteContributions = contribSnapshot.documents.mapNotNull { doc ->
                doc.data?.let { data ->
                    ContributionEntity(
                        id = doc.id,
                        goalId = data["goalId"] as? String ?: "",
                        accountId = data["accountId"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        type = data["type"] as? String ?: "manual",
                        transactionId = data["transactionId"] as? String,
                        notes = data["notes"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                        isSynced = true
                    )
                }
            }
            contributionDao.insertContributionsFromRemote(remoteContributions)

            Log.d(TAG, "refreshGoalsFromFirestore: synced ${remoteGoals.size} goals, ${remoteContributions.size} contributions")
        } catch (e: Exception) {
            Log.w(TAG, "refreshGoalsFromFirestore: failed", e)
        }
    }
}

/**
 * Extension to convert Goal to a Firestore-compatible map.
 */
private fun Goal.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "name" to name,
        "targetAmount" to targetAmount,
        "deadline" to deadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        "status" to status.value,
        "autoAllocate" to autoAllocate,
        "allocationPriority" to allocationPriority,
        "iconName" to iconName,
        "color" to color,
        "notes" to notes,
        "createdAt" to createdAt.toEpochMilli(),
        "updatedAt" to System.currentTimeMillis()
    )
}
