package com.example.insightku.feature.budgeting.presentation.event

import com.example.insightku.feature.budgeting.data.model.GoalStatus
import com.example.insightku.feature.budgeting.domain.model.AllocationSuggestion
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule
import com.example.insightku.feature.budgeting.presentation.state.AutoAllocationRuleForm
import java.time.LocalDate

/**
 * User events for the Goals feature.
 */
sealed class GoalsEvent {

    // ── Goal CRUD ────────────────────────────────────────────────────────────────

    data class CreateGoal(
        val name: String,
        val targetAmount: Double,
        val deadline: LocalDate?,
        val iconName: String = "savings",
        val color: String = "#7C4DFF"
    ) : GoalsEvent()

    data class UpdateGoal(
        val id: String,
        val name: String,
        val targetAmount: Double,
        val deadline: LocalDate?,
        val iconName: String,
        val color: String,
        val notes: String
    ) : GoalsEvent()

    data class UpdateGoalStatus(val goalId: String, val status: GoalStatus) : GoalsEvent()

    data class ArchiveGoal(val goalId: String) : GoalsEvent()

    data class PauseGoal(val goalId: String) : GoalsEvent()

    data class ResumeGoal(val goalId: String) : GoalsEvent()

    // ── Contributions ─────────────────────────────────────────────────────────────

    data class Contribute(
        val goalId: String,
        val accountId: String,
        val amount: Double,
        val notes: String = ""
    ) : GoalsEvent()

    data class Withdraw(
        val goalId: String,
        val accountId: String,
        val amount: Double,
        val notes: String = ""
    ) : GoalsEvent()

    // ── Account Linking ────────────────────────────────────────────────────────────

    data class LinkAccount(
        val goalId: String,
        val accountId: String,
        val allocationPercent: Double = 100.0,
        val isPrimary: Boolean = true
    ) : GoalsEvent()

    data class UnlinkAccount(val goalId: String, val accountId: String) : GoalsEvent()

    data class SetPrimaryAccount(val goalId: String, val accountId: String) : GoalsEvent()

    // ── Daily Target ─────────────────────────────────────────────────────────────

    data class SetDailyTarget(val amount: Double) : GoalsEvent()

    data object ClearDailyTarget : GoalsEvent()

    // ── Auto-Allocation ───────────────────────────────────────────────────────────

    data class AddAutoAllocationRule(val rule: AutoAllocationRule) : GoalsEvent()

    data class UpdateAutoAllocationRule(val rule: AutoAllocationRule) : GoalsEvent()

    data class DeleteAutoAllocationRule(val ruleId: String) : GoalsEvent()

    data class ToggleAutoAllocationRule(val ruleId: String, val enabled: Boolean) : GoalsEvent()

    data class SetGoalAutoAllocate(val goalId: String, val enabled: Boolean) : GoalsEvent()

    // ── Suggestions ─────────────────────────────────────────────────────────────

    data class ConfirmSuggestion(val suggestion: AllocationSuggestion) : GoalsEvent()

    data class DismissSuggestion(val suggestion: AllocationSuggestion) : GoalsEvent()

    data object DismissAllSuggestions : GoalsEvent()

    // ── Dialog Management ────────────────────────────────────────────────────────

    data object DismissDialog : GoalsEvent()

    data class ShowAddGoalDialog(val prefillDeadline: LocalDate? = null) : GoalsEvent()

    data class ShowEditGoalDialog(val goalId: String) : GoalsEvent()

    data class ShowArchiveGoalDialog(val goalId: String) : GoalsEvent()

    data class ShowContributeDialog(val goalId: String) : GoalsEvent()

    data class ShowWithdrawDialog(val goalId: String) : GoalsEvent()

    data class ShowLinkAccountDialog(val goalId: String) : GoalsEvent()

    data class ShowSelectAccountDialog(val goalId: String, val action: String) : GoalsEvent()

    data object ShowSetDailyTargetDialog : GoalsEvent()

    data class ShowAddAutoAllocationRuleDialog(val goalId: String? = null) : GoalsEvent()

    data class ShowEditAutoAllocationRuleDialog(val rule: AutoAllocationRule) : GoalsEvent()

    data object ShowPendingSuggestions : GoalsEvent()

    data class ShowGoalDetail(val goalId: String) : GoalsEvent()

    // ── Error Handling ───────────────────────────────────────────────────────────

    data object ClearError : GoalsEvent()

    data object ClearSnackbar : GoalsEvent()
}
