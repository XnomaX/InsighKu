package com.example.insightku.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.notification.DraftTransactionManager
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.engine.AutoAllocationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * AllocationSafetyNetWorker — ensures auto-allocation is processed for a specific transaction.
 *
 * This handles the case where the app is killed between addTransaction() and the
 * allocation execution in AddTransactionUseCase. The worker re-evaluates the
 * allocation engine; idempotency checks in GoalRepository.contribute() prevent
 * duplicate execution.
 *
 * Scheduled as a OneTimeWorkRequest with a 5-second delay after each transaction save.
 * Each transaction gets a unique work name to avoid collisions.
 */
@HiltWorker
class AllocationSafetyNetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val autoAllocationEngine: AutoAllocationEngine,
    private val goalRepository: GoalRepository,
    private val draftRepository: DraftTransactionRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AllocationSafetyNet"
        const val KEY_TRANSACTION_ID = "transactionId"
    }

    override suspend fun doWork(): Result {
        val transactionId = inputData.getString(KEY_TRANSACTION_ID)
        if (transactionId.isNullOrBlank()) {
            Log.d(TAG, "No transactionId provided — skipping")
            return Result.success()
        }

        Log.d(TAG, "Processing safety-net allocation for txId=$transactionId")

        return try {
            // Fetch the original transaction
            val transaction = transactionRepository.getTransactionById(transactionId)
            if (transaction == null) {
                Log.d(TAG, "Transaction not found (may have been deleted) — skipping")
                return Result.success()
            }

            // Only process INCOME and EXPENSE transactions (same gate as AddTransactionUseCase)
            if (transaction.type != TransactionType.INCOME && transaction.type != TransactionType.EXPENSE) {
                Log.d(TAG, "Transaction type=${transaction.type} — no allocation needed")
                return Result.success()
            }

            // Re-evaluate allocation engine (engine has built-in dedup check)
            val result = autoAllocationEngine.processTransaction(transaction)

            // Execute AUTO-mode allocations (per-rule idempotency in contribute() prevents duplicates)
            for (suggestion in result.autoExecuted) {
                try {
                    val idempotencyKey = "$transactionId|${suggestion.ruleId}"
                    goalRepository.contribute(
                        goalId = suggestion.goalId,
                        accountId = suggestion.sourceAccountId,
                        amount = suggestion.amount,
                        type = ContributionType.AUTO_ALLOCATION,
                        transactionId = idempotencyKey
                    )
                    Log.d(TAG, "[SafetyNet] Allocated ${suggestion.amount} to ${suggestion.goalName}")
                } catch (e: Exception) {
                    Log.e(TAG, "[SafetyNet] Failed to allocate: ${e.message}")
                }
            }

            // Create drafts for CONFIRMATION_REQUIRED suggestions (safety-net covers these too)
            for (suggestion in result.suggestions) {
                try {
                    DraftTransactionManager.get().createAllocationDraft(
                        context = applicationContext,
                        ruleId = suggestion.ruleId.ifBlank { suggestion.id },
                        goalId = suggestion.goalId,
                        goalName = suggestion.goalName,
                        sourceAccountId = suggestion.sourceAccountId,
                        sourceAccountName = suggestion.sourceAccountName,
                        allocationAmount = suggestion.amount,
                        triggerType = "safety_net",
                        triggerDescription = suggestion.triggerDescription,
                        draftRepository = draftRepository
                    )
                    Log.d(TAG, "[SafetyNet] Created draft for ${suggestion.goalName}")
                } catch (e: Exception) {
                    Log.e(TAG, "[SafetyNet] Failed to create draft: ${e.message}")
                }
            }

            Log.d(TAG, "Safety-net allocation complete for txId=$transactionId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Safety-net allocation error: ${e.message}", e)
            Result.retry()
        }
    }
}
