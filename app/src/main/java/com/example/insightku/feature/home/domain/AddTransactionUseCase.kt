package com.example.insightku.feature.home.domain

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.feature.budgeting.data.model.ContributionType
import com.example.insightku.feature.budgeting.data.repository.GoalRepository
import com.example.insightku.feature.budgeting.domain.engine.AutoAllocationEngine
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationResult
import javax.inject.Inject

/**
 * AddTransactionUseCase — menambah transaksi baru ke Room dan Firestore.
 *
 * Enhanced with Smart Auto Allocation:
 * After a transaction is saved, the AutoAllocationEngine evaluates all enabled
 * auto-allocation rules against the new transaction. Matching rules create
 * allocation suggestions or auto-execute allocations depending on their
 * confirmation mode.
 *
 * Flow:
 * 1. Save transaction to Room + Firestore
 * 2. Run AutoAllocationEngine.processTransaction()
 * 3. For AUTO-mode rules: execute allocations immediately via GoalRepository.contribute()
 * 4. For CONFIRMATION_REQUIRED rules: return suggestions for ViewModel to display
 * 5. Return AutoAllocationResult for ViewModel to handle
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository,
    private val autoAllocationEngine: AutoAllocationEngine,
    private val goalRepository: GoalRepository
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
            val executedSuggestions = mutableListOf<com.example.insightku.feature.budgeting.domain.model.AllocationSuggestion>()
            for (suggestion in allocationResult.autoExecuted) {
                try {
                    goalRepository.contribute(
                        goalId = suggestion.goalId,
                        accountId = suggestion.sourceAccountId,
                        amount = suggestion.amount,
                        type = ContributionType.AUTO_ALLOCATION
                    ).onSuccess {
                        executedSuggestions.add(suggestion)
                    }.onFailure { _ ->
                        // Allocation failed — skip this suggestion
                    }
                } catch (_: Exception) {
                    // Allocation execution error — skip this suggestion
                }
            }

            // Return result with executed suggestions moved to autoExecuted
            Result.success(allocationResult.copy(autoExecuted = executedSuggestions))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
