package com.example.insightku.feature.home.domain

import android.content.Context
import com.example.insightku.R
import com.example.insightku.core.data.model.DraftType
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * ApproveAllocationDraftUseCase — executes a pending auto-allocation draft.
 *
 * Extracted from DashboardViewModel. Validates the draft and the source account,
 * then contributes to the goal and removes the draft. All business rules live here;
 * the ViewModel only maps the [Outcome] to UI state.
 */
class ApproveAllocationDraftUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val draftRepository: DraftTransactionRepository,
    private val accountRepository: AccountRepository,
    private val goalRepository: GoalRepository
) {

    sealed interface Outcome {
        /** Draft missing or not an auto-allocation draft. */
        data object DraftNotFound : Outcome
        /** Draft data was invalid; the draft was removed. */
        data object InvalidDraft : Outcome
        /** Source account no longer exists; the draft was removed. */
        data object AccountMissing : Outcome
        /** Source account has insufficient balance; the draft was removed. */
        data class InsufficientBalance(val accountName: String) : Outcome
        /** Allocation executed and draft removed. */
        data class Allocated(val amount: Double, val goalName: String?) : Outcome
        /** Allocation failed with an error message. */
        data class Failed(val message: String) : Outcome
    }

    suspend fun approve(draftId: String): Outcome = try {
        val draft = draftRepository.getById(draftId)
        if (draft == null || draft.draftType != DraftType.AUTO_ALLOCATION) {
            return Outcome.DraftNotFound
        }

        val accountId = draft.sourceAccountId
        val goalId = draft.goalId
        val amount = draft.allocationAmount

        if (accountId == null || goalId == null || amount == null || amount <= 0) {
            draftRepository.confirmAndRemove(draftId)
            return Outcome.InvalidDraft
        }

        val account = accountRepository.getAccountById(accountId)
        if (account == null) {
            draftRepository.confirmAndRemove(draftId)
            return Outcome.AccountMissing
        }
        if (goalRepository.getAvailableCash(accountId) < amount) {
            draftRepository.confirmAndRemove(draftId)
            return Outcome.InsufficientBalance(account.name)
        }

        val result = goalRepository.contribute(
            goalId = goalId,
            accountId = accountId,
            amount = amount,
            type = ContributionType.AUTO_ALLOCATION
        )

        if (result.isSuccess) {
            draftRepository.confirmAndRemove(draftId)
            Outcome.Allocated(amount, draft.goalName)
        } else {
            Outcome.Failed(result.exceptionOrNull()?.message ?: context.getString(R.string.error_allocate))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Outcome.Failed(e.message ?: context.getString(R.string.error_approve_allocation))
    }
}
