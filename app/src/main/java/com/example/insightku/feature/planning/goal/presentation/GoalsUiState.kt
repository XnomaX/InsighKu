package com.example.insightku.feature.planning.goal.presentation

import com.example.insightku.core.data.model.Account
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.feature.planning.goal.data.model.GoalAccountEntity
import com.example.insightku.feature.planning.goal.domain.model.*
import java.time.LocalDate

/**
 * Filter tabs for the Goals screen.
 * - [ACTIVE]: Active goals in progress
 * - [PAUSED]: Paused goals (on hold)
 */
enum class GoalFilterTab { ACTIVE, PAUSED, COMPLETED }

data class GoalsUiState(
    // ── Status-separated goal lists ─────────────────────────────────────────
    val activeGoals: List<Goal> = emptyList(),
    val pausedGoals: List<Goal> = emptyList(),
    val completedGoals: List<Goal> = emptyList(),
    val archivedGoals: List<Goal> = emptyList(),

    // ── Legacy combined list (kept for dialog lookups) ──────────────────────
    val goals: List<Goal> = emptyList(),

    val dailyTarget: DailyTarget = DailyTarget.empty(),
    val autoAllocationRules: List<AutoAllocationRule> = emptyList(),
    val goalSummary: GoalSummary? = null,
    val accounts: List<Account> = emptyList(),
    val accountAllocations: Map<String, AccountAllocation> = emptyMap(),
    val expenseCategories: List<CategoryInfo> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val snackbarMessage: String? = null,
    val selectedGoal: Goal? = null,
    val selectedGoalContributions: List<Contribution> = emptyList(),
    val linkedAccounts: Map<String, List<GoalAccountEntity>> = emptyMap(),
    val dialogState: GoalsDialogState = GoalsDialogState.None,

    // ── Redesigned state ────────────────────────────────────────────────────
    val selectedTab: GoalFilterTab = GoalFilterTab.ACTIVE,
    val showArchivedSheet: Boolean = false,
    val showActionsSheet: Boolean = false,
    val actionsGoalId: String? = null,
    val showCompletionCelebration: Boolean = false,
    val completionGoalName: String = ""
) {
    val hasGoals: Boolean get() = goals.isNotEmpty()

    /** Currently displayed goals based on the selected tab. */
    val displayedGoals: List<Goal>
        get() = when (selectedTab) {
            GoalFilterTab.ACTIVE -> activeGoals
            GoalFilterTab.PAUSED -> pausedGoals
            GoalFilterTab.COMPLETED -> completedGoals
        }

    val activeGoalCount: Int get() = activeGoals.size
    val pausedGoalCount: Int get() = pausedGoals.size
    val completedGoalCount: Int get() = completedGoals.size
    val archivedGoalCount: Int get() = archivedGoals.size

    companion object {
        fun initial() = GoalsUiState()
    }
}

sealed class GoalsDialogState {
    data object None : GoalsDialogState()
    data class AddGoal(val deadline: LocalDate? = null) : GoalsDialogState()
    data class EditGoal(val goal: Goal) : GoalsDialogState()
    data class ArchiveGoal(val goalId: String, val goalName: String) : GoalsDialogState()
    data class Contribute(val goalId: String, val selectedAccountId: String?) : GoalsDialogState()
    data class Withdraw(val goalId: String, val selectedAccountId: String?) : GoalsDialogState()
    data class SetDailyTarget(val amount: String) : GoalsDialogState()
    data class AddAutoAllocationRule(val goalId: String?) : GoalsDialogState()
    data class EditAutoAllocationRule(val rule: AutoAllocationRule) : GoalsDialogState()
    data class GoalDetail(val goalId: String) : GoalsDialogState()
    data class LinkAccount(val goalId: String) : GoalsDialogState()
    data class SelectAccount(val goalId: String, val action: AccountAction) : GoalsDialogState()
    // ── New dialog states for redesign ──────────────────────────────────────
    data class DeleteGoalConfirm(val goalId: String, val goalName: String) : GoalsDialogState()
    data class RestoreGoalConfirm(val goalId: String, val goalName: String) : GoalsDialogState()
}

enum class AccountAction { CONTRIBUTE, WITHDRAW }
