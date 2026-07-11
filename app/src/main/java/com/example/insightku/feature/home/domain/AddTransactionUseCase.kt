package com.example.insightku.feature.home.domain

import android.content.Context
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.core.notification.DraftTransactionManager
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.engine.AutoAllocationEngine
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationResult
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.insightku.core.worker.AllocationSafetyNetWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * AddTransactionUseCase — menambah transaksi baru ke Room dan Firestore.
 *
 * Enhanced with Smart Auto Allocation:
 * After a transaction is saved, the AutoAllocationEngine evaluates all enabled
 * auto-allocation rules against the new transaction. Matching rules create
 * allocation drafts (for CONFIRMATION_REQUIRED) or auto-execute allocations (for AUTO).
 *
 * Flow:
 * 1. Save transaction to Room + Firestore
 * 2. Run AutoAllocationEngine.processTransaction()
 * 3. For AUTO-mode rules: execute allocations immediately via GoalRepository.contribute()
 * 4. For CONFIRMATION_REQUIRED rules: create DraftTransaction via DraftTransactionManager
 * 5. Return AutoAllocationResult for ViewModel to handle
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository,
    private val autoAllocationEngine: AutoAllocationEngine,
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val draftRepository: DraftTransactionRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AddTransactionUseCase"
    }

    /**
     * Add a transaction and process auto-allocation rules.
     *
     * @return Result containing the AutoAllocationResult (suggestions + auto-executed allocations)
     */
    suspend operator fun invoke(transaction: Transaction): Result<AutoAllocationResult> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User tidak login"))
        return try {
            // Step 1: Save transaction to Room + Firestore
            transactionRepository.addTransaction(transaction, userId)

            // Step 2: Run auto-allocation engine for INCOME and EXPENSE transactions
            val allocationResult = when (transaction.type) {
                TransactionType.INCOME -> {
                    // Income-based allocation (Pay Yourself First)
                    autoAllocationEngine.processTransaction(transaction)
                }
                TransactionType.EXPENSE -> {
                    // Round-up savings from expenses
                    autoAllocationEngine.processTransaction(transaction)
                }
                else -> AutoAllocationResult(
                    suggestions = emptyList(),
                    autoExecuted = emptyList(),
                    processedTransactionId = transaction.id,
                    processedAmount = transaction.amount
                )
            }

            // Step 3: Execute AUTO-mode allocations immediately
            // Pass transactionId|ruleId for per-rule idempotency — prevents duplicate allocations
            // while allowing multiple rules to fire on the same transaction
            val executedSuggestions = mutableListOf<com.example.insightku.feature.planning.goal.domain.model.AllocationSuggestion>()
            for (suggestion in allocationResult.autoExecuted) {
                try {
                    val idempotencyKey = "${transaction.id}|${suggestion.ruleId}"
                    goalRepository.contribute(
                        goalId = suggestion.goalId,
                        accountId = suggestion.sourceAccountId,
                        amount = suggestion.amount,
                        type = ContributionType.AUTO_ALLOCATION,
                        transactionId = idempotencyKey
                    ).onSuccess {
                        executedSuggestions.add(suggestion)
                    }.onFailure { _ ->
                        // Allocation failed — skip this suggestion
                    }
                } catch (_: Exception) {
                    // Allocation execution error — skip this suggestion
                }
            }

            // Step 4: Create DraftTransaction for CONFIRMATION_REQUIRED suggestions
            for (suggestion in allocationResult.suggestions) {
                try {
                    DraftTransactionManager.get().createAllocationDraft(
                        context = context,
                        ruleId = suggestion.ruleId.ifBlank { suggestion.id },
                        goalId = suggestion.goalId,
                        goalName = suggestion.goalName,
                        sourceAccountId = suggestion.sourceAccountId,
                        sourceAccountName = suggestion.sourceAccountName,
                        allocationAmount = suggestion.amount,
                        triggerType = "transaction",
                        triggerDescription = suggestion.triggerDescription,
                        draftRepository = draftRepository
                    )
                } catch (e: Exception) {
                    // Failed to create allocation draft — log and continue
                    android.util.Log.e(TAG, "Failed to create allocation draft: ${e.message}")
                }
            }

            // ── Safety-net: schedule a one-shot worker to re-process allocations ──
            // If the app is killed between addTransaction() and contribute(),
            // this worker will re-evaluate allocations. The idempotency check
            // in GoalRepository.contribute() ensures no duplicate execution.
            if (transaction.type == TransactionType.INCOME || transaction.type == TransactionType.EXPENSE) {
                scheduleAllocationSafetyNet(transaction.id)
            }

            // Return result with executed suggestions moved to autoExecuted
            Result.success(allocationResult.copy(
                autoExecuted = executedSuggestions,
                suggestions = emptyList() // Suggestions are now drafts, not in-memory
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Schedule a one-shot safety-net worker to ensure allocations are processed.
     * This handles the case where the app is killed between transaction save
     * and allocation execution. The worker re-evaluates the allocation engine;
     * idempotency checks in GoalRepository.contribute() prevent duplicates.
     */
    private fun scheduleAllocationSafetyNet(transactionId: String) {
        val request = OneTimeWorkRequestBuilder<AllocationSafetyNetWorker>()
            .setInputData(androidx.work.workDataOf("transactionId" to transactionId))
            .setConstraints(Constraints.Builder().build()) // No network needed
            .setInitialDelay(5, java.util.concurrent.TimeUnit.SECONDS) // Small delay to avoid conflict with inline execution
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "AllocationSafetyNet_$transactionId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
