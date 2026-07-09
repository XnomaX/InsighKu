package com.example.insightku.feature.planning.goal.presentation

import com.example.insightku.core.data.model.Account
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.feature.planning.goal.data.model.GoalAccountEntity
import com.example.insightku.feature.planning.goal.domain.model.*
import java.time.LocalDate

data class GoalsUiState(
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
    val dialogState: GoalsDialogState = GoalsDialogState.None
) {
    val hasGoals: Boolean get() = goals.isNotEmpty()

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
}

enum class AccountAction { CONTRIBUTE, WITHDRAW }
