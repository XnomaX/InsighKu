package com.example.insightku.feature.budgeting.presentation.event

import com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule

/**
 * User events for the Goal Detail screen.
 */
sealed class GoalDetailEvent {

    // ── Navigation & Lifecycle ───────────────────────────────────────────────────

    data class LoadGoal(val goalId: String) : GoalDetailEvent()

    data object RefreshGoal : GoalDetailEvent()

    data object ClearError : GoalDetailEvent()

    data object ClearSnackbar : GoalDetailEvent()

    // ── Goal Actions ───────────────────────────────────────────────────────────

    data object EditGoal : GoalDetailEvent()

    data object ArchiveGoal : GoalDetailEvent()

    data object DeleteGoal : GoalDetailEvent()

    data object ConfirmArchive : GoalDetailEvent()

    data object ConfirmDelete : GoalDetailEvent()

    // ── Contribution Actions ────────────────────────────────────────────────────

    data object ShowContributeDialog : GoalDetailEvent()

    data object ShowWithdrawDialog : GoalDetailEvent()

    data object DismissDialog : GoalDetailEvent()

    data class SelectAccount(val accountId: String) : GoalDetailEvent()

    data object ShowAccountPicker : GoalDetailEvent()

    data object HideAccountPicker : GoalDetailEvent()

    data class UpdateAmount(val amount: String) : GoalDetailEvent()

    data class UpdateNotes(val notes: String) : GoalDetailEvent()

    data object SubmitContribution : GoalDetailEvent()

    data object SubmitWithdrawal : GoalDetailEvent()

    // ── Pagination ──────────────────────────────────────────────────────────────

    data object LoadMoreContributions : GoalDetailEvent()

    // ── Auto Allocation ──────────────────────────────────────────────────────────

    data object ShowAutoAllocationDialog : GoalDetailEvent()
    data class ShowEditAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class AddAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class UpdateAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class DeleteAutoAllocationRule(val ruleId: String) : GoalDetailEvent()
    data class ShowDeleteAutoAllocationConfirm(val ruleId: String) : GoalDetailEvent()
    data object ConfirmDeleteAutoAllocationRule : GoalDetailEvent()
    data object CancelDeleteAutoAllocationRule : GoalDetailEvent()
    data class ToggleAutoAllocationRule(val ruleId: String, val enabled: Boolean) : GoalDetailEvent()
}
