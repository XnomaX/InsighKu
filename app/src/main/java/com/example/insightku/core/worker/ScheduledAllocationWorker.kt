package com.example.insightku.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.insightku.core.notification.AutoAllocationNotificationHelper
import com.example.insightku.feature.planning.goal.data.local.dao.AutoAllocationRuleDao
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
    private val autoAllocationRuleDao: AutoAllocationRuleDao,
    private val notificationHelper: AutoAllocationNotificationHelper
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

            Log.d(TAG, "Scheduled allocation processing complete")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scheduled allocations", e)
            Result.retry()
        }
    }

    private suspend fun processResult(result: com.example.insightku.feature.planning.goal.domain.model.AutoAllocationResult) {
        // Auto-execute allocations via GoalRepository
        for (suggestion in result.autoExecuted) {
            try {
                val contributionResult = goalRepository.contribute(
                    goalId = suggestion.goalId,
                    accountId = suggestion.sourceAccountId,
                    amount = suggestion.amount,
                    type = ContributionType.AUTO_ALLOCATION
                )
                if (contributionResult.isSuccess) {
                    Log.d(TAG, "Auto-allocated ${suggestion.amount} to ${suggestion.goalName}")
                    notificationHelper.showAllocationSuccessNotification(
                        goalName = suggestion.goalName,
                        amount = suggestion.amount
                    )
                    // Update lastExecutedAt on matching scheduled rules to prevent duplicate fires
                    updateLastExecutedForGoal(suggestion.goalId)
                } else {
                    Log.e(TAG, "Auto-allocation failed: ${contributionResult.exceptionOrNull()?.message}")
                    notificationHelper.showAllocationSkippedNotification(
                        goalName = suggestion.goalName,
                        accountName = suggestion.sourceAccountName
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-execute allocation: ${e.message}", e)
            }
        }

        // Notify about suggestions requiring confirmation
        for (suggestion in result.suggestions) {
            notificationHelper.showAllocationSuggestionNotification(
                goalName = suggestion.goalName,
                amount = suggestion.amount,
                triggerDescription = suggestion.triggerDescription
            )
        }
    }

    /**
     * Update lastExecutedAt on all scheduled rules for a goal to prevent duplicate fires.
     */
    private suspend fun updateLastExecutedForGoal(goalId: String) {
        try {
            val now = System.currentTimeMillis()
            val rules = autoAllocationRuleDao.getEnabledRulesSync().filter {
                it.goalId == goalId
            }
            for (rule in rules) {
                autoAllocationRuleDao.setLastExecutedAt(rule.id, now)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update lastExecutedAt: ${e.message}")
        }
    }
}
