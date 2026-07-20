package com.example.insightku.feature.planning.goal.presentation

import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.LocalDate

sealed class GoalsEvent {
    data object LoadGoals : GoalsEvent()
    data class ShowAddGoalDialog(val prefillDeadline: LocalDate? = null) : GoalsEvent()
    data class ShowEditGoalDialog(val goalId: String) : GoalsEvent()
    data class ShowArchiveGoalDialog(val goalId: String) : GoalsEvent()
    data class ShowContributeDialog(val goalId: String) : GoalsEvent()
    data class ShowWithdrawDialog(val goalId: String) : GoalsEvent()
    data class ShowLinkAccountDialog(val goalId: String) : GoalsEvent()
    data class ShowSelectAccountDialog(val goalId: String, val action: String) : GoalsEvent()
    data object ShowSetDailyTargetDialog : GoalsEvent()
    data class ShowAddAutoAllocationRuleDialog(val goalId: String?) : GoalsEvent()
    data class ShowEditAutoAllocationRuleDialog(val rule: AutoAllocationRule) : GoalsEvent()
    data class ShowGoalDetail(val goalId: String) : GoalsEvent()
    data object DismissDialog : GoalsEvent()
    data object ClearError : GoalsEvent()
    data object ClearSnackbar : GoalsEvent()
    data class CreateGoal(
        val name: String,
        val targetAmount: Double,
        val deadline: LocalDate,  // required — goals must have a deadline
        val iconName: String,
        val color: String,
        val notes: String = "",
        val reminderEnabled: Boolean = false,
        val linkedAccountIds: List<String> = emptyList()
    ) : GoalsEvent()
    data class UpdateGoal(
        val id: String,
        val name: String,
        val targetAmount: Double,
        val deadline: LocalDate,  // required — goals must have a deadline
        val iconName: String,
        val color: String,
        val notes: String,
        val reminderEnabled: Boolean
    ) : GoalsEvent()
    data class DeleteGoal(val goalId: String) : GoalsEvent()
    data class UpdateGoalStatus(val goalId: String, val status: GoalStatus) : GoalsEvent()
    data class ArchiveGoal(val goalId: String) : GoalsEvent()
    data class PauseGoal(val goalId: String) : GoalsEvent()
    data class ResumeGoal(val goalId: String) : GoalsEvent()
    data class Contribute(val goalId: String, val accountId: String, val amount: Double, val notes: String = "") : GoalsEvent()
    data class Withdraw(val goalId: String, val accountId: String, val amount: Double, val notes: String = "") : GoalsEvent()
    data class LinkAccount(val goalId: String, val accountId: String, val allocationPercent: Double = 100.0, val isPrimary: Boolean = false) : GoalsEvent()
    data class UnlinkAccount(val goalId: String, val accountId: String) : GoalsEvent()
    data class SetPrimaryAccount(val goalId: String, val accountId: String) : GoalsEvent()
    data class SetDailyTarget(val amount: Double) : GoalsEvent()
    data object ClearDailyTarget : GoalsEvent()
    data class AddAutoAllocationRule(val rule: AutoAllocationRule) : GoalsEvent()
    data class UpdateAutoAllocationRule(val rule: AutoAllocationRule) : GoalsEvent()
    data class DeleteAutoAllocationRule(val ruleId: String) : GoalsEvent()
    data class ToggleAutoAllocationRule(val ruleId: String, val enabled: Boolean) : GoalsEvent()
    data class SetGoalAutoAllocate(val goalId: String, val enabled: Boolean) : GoalsEvent()
    data class ShowAddBudgetDialog(val categoryType: CategoryType = CategoryType.EXPENSE) : GoalsEvent()
    data class SelectAccount(val accountId: String) : GoalsEvent()

    // ── Redesigned events ───────────────────────────────────────────────────
    data class SelectTab(val tab: GoalFilterTab) : GoalsEvent()
    data object ShowArchivedGoals : GoalsEvent()
    data object DismissArchivedSheet : GoalsEvent()
    data class ShowGoalActionsSheet(val goalId: String) : GoalsEvent()
    data object DismissActionsSheet : GoalsEvent()
    data class RestoreGoal(val goalId: String) : GoalsEvent()
    data class DeleteGoalPermanently(val goalId: String) : GoalsEvent()
    data class ShowDeleteGoalConfirm(val goalId: String) : GoalsEvent()
    data class ShowRestoreGoalConfirm(val goalId: String) : GoalsEvent()
    data object DismissCompletionCelebration : GoalsEvent()

    // ── Funds flows (envelope model) ────────────────────────────────────────
    /** Entry point for completing a goal — checks for allocated funds first. */
    data class RequestCompleteGoal(val goalId: String) : GoalsEvent()
    /** Complete keeping funds in their current accounts (envelope release). */
    data class CompleteGoalKeepFunds(val goalId: String) : GoalsEvent()
    /** Complete and transfer allocated funds to another account. */
    data class CompleteGoalTransferFunds(val goalId: String, val targetAccountId: String) : GoalsEvent()
    /** Delete returning funds to their original accounts (envelope release). */
    data class DeleteGoalReturnFunds(val goalId: String) : GoalsEvent()
    /** Delete after transferring allocated funds to another account. */
    data class DeleteGoalTransferFunds(val goalId: String, val targetAccountId: String) : GoalsEvent()
}
