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
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.planning.goal.data.local.dao.*
import com.example.insightku.feature.planning.goal.data.model.*
import com.example.insightku.feature.planning.goal.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
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
            contributionDao.getAllContributions()
        ) { entities, _ ->
            entities.map { entity ->
                val currentAmount = contributionDao.getTotalContributed(entity.id)
                Goal.fromEntity(entity, currentAmount)
            }
        }
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
        if (account.balance < amount) return Result.failure(IllegalArgumentException("Insufficient balance"))

        val contribution = atomicContribute(goalId, accountId, amount, type, transactionId, notes, goal)
        scheduleGoalSync()
        return Result.success(Contribution.fromEntity(contribution))
    }

    private suspend fun atomicContribute(
        goalId: String, accountId: String, amount: Double, type: ContributionType,
        transactionId: String?, notes: String, goal: GoalEntity
    ): ContributionEntity {
        return database.withTransaction {
            val contribution = ContributionEntity(
                goalId = goalId, accountId = accountId, amount = amount,
                type = type.value, transactionId = transactionId, notes = notes, isSynced = false
            )
            contributionDao.insertContribution(contribution)
            accountDao.updateBalance(accountId, -amount)

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
                date = System.currentTimeMillis(), type = txType,
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
                        // 1. Credit back to account
                        accountDao.updateBalance(contribution.accountId, contribution.amount)

                        // 2. Delete the contribution
                        contributionDao.deleteContribution(contribution)

                        // 3. Delete the auto-allocation transaction record
                        transactionDao.deleteTransactionByReferenceId(contribution.id)

                        // 4. Re-evaluate goal completion status
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
            val contribution = ContributionEntity(
                goalId = goalId, accountId = accountId, amount = -amount,
                type = ContributionType.WITHDRAWAL.value, notes = notes, isSynced = false
            )
            contributionDao.insertContribution(contribution)
            accountDao.updateBalance(accountId, amount)

            val tx = Transaction(
                title = context.getString(R.string.goal_withdraw_title, goal?.name ?: context.getString(R.string.goals_title)), amount = amount,
                category = "Goal Withdrawal", date = System.currentTimeMillis(),
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
        return autoAllocationRuleDao.getAllRules().map { entities ->
            entities.map { entity -> AutoAllocationRule.fromEntity(entity, goalDao.getGoalById(entity.goalId)?.name ?: "") }
        }
    }

    fun getEnabledRules(): Flow<List<AutoAllocationRule>> {
        return autoAllocationRuleDao.getEnabledRules().map { entities ->
            entities.map { entity -> AutoAllocationRule.fromEntity(entity, goalDao.getGoalById(entity.goalId)?.name ?: "") }
        }
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
        return combine(goalDao.getAllActiveGoals(), contributionDao.getAllContributions()) { entities, _ ->
            val activeGoals = entities.filter { it.goalStatus != GoalStatus.ARCHIVED }
            val totalSaved = activeGoals.sumOf { contributionDao.getTotalContributed(it.id) }
            val totalTarget = activeGoals.sumOf { it.targetAmount }
            GoalSummary(entities.size, activeGoals.count { it.goalStatus == GoalStatus.ACTIVE },
                activeGoals.count { it.goalStatus == GoalStatus.COMPLETED }, totalSaved, totalTarget,
                if (totalTarget > 0) (totalSaved / totalTarget) * 100 else 0.0)
        }
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
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L, isSynced = true)
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
    "iconName" to iconName, "color" to color, "notes" to notes,
    "createdAt" to createdAt.toEpochMilli(), "updatedAt" to System.currentTimeMillis()
)
