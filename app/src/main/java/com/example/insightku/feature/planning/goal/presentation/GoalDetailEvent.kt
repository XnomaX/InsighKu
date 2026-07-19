package com.example.insightku.feature.planning.goal.presentation

import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule

sealed class GoalDetailEvent {
    data class LoadGoal(val goalId: String) : GoalDetailEvent()
    data object RefreshGoal : GoalDetailEvent()
    data object EditGoal : GoalDetailEvent()
    data class UpdateAmount(val amount: String) : GoalDetailEvent()
    data class UpdateNotes(val notes: String) : GoalDetailEvent()
    data class SelectAccount(val accountId: String) : GoalDetailEvent()
    data object ShowContributeDialog : GoalDetailEvent()
    data object ShowWithdrawDialog : GoalDetailEvent()
    data object SubmitContribution : GoalDetailEvent()
    data object SubmitWithdrawal : GoalDetailEvent()
    data object DismissDialog : GoalDetailEvent()
    data object ClearSnackbar : GoalDetailEvent()
    data object ClearError : GoalDetailEvent()
    data object ShowAutoAllocationDialog : GoalDetailEvent()
    data class ShowEditAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class AddAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class UpdateAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class DeleteAutoAllocationRule(val ruleId: String) : GoalDetailEvent()
    data class ToggleAutoAllocationRule(val ruleId: String, val enabled: Boolean) : GoalDetailEvent()

    data object LoadMoreContributions : GoalDetailEvent()
    data object ArchiveGoal : GoalDetailEvent()
    data object DeleteGoal : GoalDetailEvent()
    data object ConfirmArchive : GoalDetailEvent()
    data object ConfirmDelete : GoalDetailEvent()

    // ── Status Actions ───────────────────────────────────────────────────
    data object PauseGoal : GoalDetailEvent()
    data object ResumeGoal : GoalDetailEvent()
    data object CompleteGoal : GoalDetailEvent()

    // ── Extend Deadline ────────────────────────────────────────────────────
    data object ShowExtendDeadlineDialog : GoalDetailEvent()
    data class ConfirmExtendDeadline(val newDeadline: java.time.LocalDate) : GoalDetailEvent()
}
