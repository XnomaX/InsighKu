package com.example.insightku.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.core.notification.AutoAllocationNotificationHelper
import com.example.insightku.core.notification.DraftTransactionManager
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.engine.AutoAllocationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * ScheduledAllocationWorker — Periodic worker for scheduled auto-allocation rules.
 *
 * Runs every 6 hours and evaluates:
 * - Daily, Weekly, Biweekly, Monthly allocation rules
 * - Balance-above rules
 *
 * Auto-executes rules with AUTO confirmation mode via GoalRepository.contribute().
 * For CONFIRMATION_REQUIRED rules, creates notification suggestions.
 * Updates lastExecutedAt on the rule after successful execution to prevent duplicate fires.
 */
@HiltWorker
class ScheduledAllocationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val autoAllocationEngine: AutoAllocationEngine,
    private val goalRepository: GoalRepository,
    private val notificationHelper: AutoAllocationNotificationHelper,
    private val draftRepository: DraftTransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "ScheduledAllocationWorker"
        private const val TAG = "ScheduledAllocationWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting scheduled allocation processing")

        return try {
            // Process scheduled allocations (daily/weekly/biweekly/monthly)
            val scheduledResult = autoAllocationEngine.processScheduledAllocations()
            processResult(scheduledResult)

            // Process balance-above rules
            val balanceResult = autoAllocationEngine.processBalanceAboveRules()
            processResult(balanceResult)

            // Process category-based periodic rules (after_daily_total, after_monthly_total)
            val categoryPeriodicResult = autoAllocationEngine.processCategoryBasedPeriodicRules()
            processResult(categoryPeriodicResult)

            Log.d(TAG, "Scheduled allocation processing complete")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scheduled allocations", e)
            Result.retry()
        }
    }

    private suspend fun processResult(result: com.example.insightku.feature.planning.goal.domain.model.AutoAllocationResult) {
        Log.d(TAG, "processResult: ${result.autoExecuted.size} auto-executed, ${result.suggestions.size} suggestions needing confirmation")

        // Auto-execute allocations via GoalRepository
        for (suggestion in result.autoExecuted) {
            Log.d(TAG, "[AutoExec] Rule=${suggestion.ruleId} Goal=${suggestion.goalName} Amount=${suggestion.amount}")
            try {
                // Generate a deterministic idempotency key from ruleId + execution timestamp
                // to prevent duplicate allocations across worker retries
                val idempotencyKey = "scheduled|${suggestion.ruleId}|${System.currentTimeMillis() / 60_000}"
                val contributionResult = goalRepository.contribute(
                    goalId = suggestion.goalId,
                    accountId = suggestion.sourceAccountId,
                    amount = suggestion.amount,
                    type = ContributionType.AUTO_ALLOCATION,
                    transactionId = idempotencyKey
                )
                if (contributionResult.isSuccess) {
                    Log.d(TAG, "[AutoExec] SUCCESS — ${suggestion.amount} allocated to ${suggestion.goalName}")
                    notificationHelper.showAllocationSuccessNotification(
                        goalName = suggestion.goalName,
                        amount = suggestion.amount
                    )
                    // Update lastExecutedAt on the fired rule to prevent duplicate fires
                    updateLastExecutedForRule(suggestion.ruleId)
                } else {
                    Log.e(TAG, "[AutoExec] FAILED — ${contributionResult.exceptionOrNull()?.message}")
                    notificationHelper.showAllocationSkippedNotification(
                        goalName = suggestion.goalName,
                        accountName = suggestion.sourceAccountName
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "[AutoExec] EXCEPTION — ${e.message}", e)
            }
        }

        // Create DraftTransaction for suggestions requiring confirmation
        for (suggestion in result.suggestions) {
            Log.d(TAG, "[ConfirmFirst] Creating draft — Rule=${suggestion.ruleId} Goal=${suggestion.goalName} Amount=${suggestion.amount}")
            try {
                val draftId = DraftTransactionManager.get().createAllocationDraft(
                    context = applicationContext,
                    ruleId = suggestion.ruleId,
                    goalId = suggestion.goalId,
                    goalName = suggestion.goalName,
                    sourceAccountId = suggestion.sourceAccountId,
                    sourceAccountName = suggestion.sourceAccountName,
                    allocationAmount = suggestion.amount,
                    triggerType = "scheduled",
                    triggerDescription = suggestion.triggerDescription,
                    draftRepository = draftRepository
                )
                if (draftId != null) {
                    Log.d(TAG, "[ConfirmFirst] DRAFT CREATED — id=$draftId for ${suggestion.goalName}")
                } else {
                    Log.w(TAG, "[ConfirmFirst] DRAFT DUPLICATE — already pending for rule ${suggestion.ruleId}")
                }
                // Update lastExecutedAt to prevent duplicate fires even for confirmation rules
                updateLastExecutedForRule(suggestion.ruleId)
            } catch (e: Exception) {
                Log.e(TAG, "[ConfirmFirst] EXCEPTION creating draft — ${e.message}", e)
            }
        }
    }

    /**
     * Update lastExecutedAt on a specific rule to prevent duplicate fires.
     */
    private suspend fun updateLastExecutedForRule(ruleId: String?) {
        if (ruleId == null) return
        try {
            val now = System.currentTimeMillis()
            goalRepository.markRuleExecuted(ruleId, now)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update lastExecutedAt for rule $ruleId: ${e.message}")
        }
    }
}
