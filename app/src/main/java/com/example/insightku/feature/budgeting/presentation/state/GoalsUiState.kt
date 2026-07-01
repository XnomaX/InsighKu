package com.example.insightku.feature.budgeting.presentation.state

import com.example.insightku.feature.budgeting.data.model.AllocationTriggerType
import com.example.insightku.feature.budgeting.data.model.AllocationValueType
import com.example.insightku.feature.budgeting.data.model.GoalAccountEntity
import com.example.insightku.feature.budgeting.data.model.GoalStatus
import com.example.insightku.feature.budgeting.domain.model.*
import java.time.LocalDate

/**
 * UI state for the Goals feature.
 */
data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val selectedGoal: Goal? = null,
    val goalProgress: Map<String, GoalProgress> = emptyMap(),
    val dailyTarget: DailyTarget = DailyTarget.empty(),
    val autoAllocationRules: List<AutoAllocationRule> = emptyList(),
    val pendingSuggestions: List<AllocationSuggestion> = emptyList(),
    val linkedAccounts: Map<String, List<GoalAccountEntity>> = emptyMap(),
    val accounts: List<com.example.insightku.core.data.model.Account> = emptyList(),
    val goalSummary: GoalSummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val dialogState: GoalsDialogState = GoalsDialogState.None,
    val snackbarMessage: String? = null
) {
    val hasPendingSuggestions: Boolean get() = pendingSuggestions.isNotEmpty()
    val pendingSuggestionsCount: Int get() = pendingSuggestions.size
    val hasGoals: Boolean get() = goals.isNotEmpty()
    val hasError: Boolean get() = error != null

    companion object {
        fun initial() = GoalsUiState(isLoading = true)
    }
}

/**
 * Dialog states for the Goals feature.
 */
sealed class GoalsDialogState {
    data object None : GoalsDialogState()

    // Goal Dialogs
    data class AddGoal(val name: String = "", val targetAmount: String = "", val deadline: LocalDate? = null) : GoalsDialogState()
    data class EditGoal(val goal: Goal) : GoalsDialogState()
    data class ArchiveGoal(val goalId: String, val goalName: String) : GoalsDialogState()

    // Contribution Dialogs
    data class Contribute(val goalId: String, val selectedAccountId: String? = null, val amount: String = "") : GoalsDialogState()
    data class Withdraw(val goalId: String, val selectedAccountId: String? = null, val amount: String = "") : GoalsDialogState()

    // Account Linking Dialogs
    data class LinkAccount(val goalId: String) : GoalsDialogState()
    data class SelectAccount(val goalId: String, val action: AccountAction) : GoalsDialogState()

    // Daily Target Dialogs
    data class SetDailyTarget(val amount: String = "") : GoalsDialogState()

    // Auto-Allocation Dialogs
    data class AddAutoAllocationRule(val goalId: String? = null) : GoalsDialogState()
    data class EditAutoAllocationRule(val rule: AutoAllocationRule) : GoalsDialogState()

    // Suggestions Dialog
    data object PendingSuggestions : GoalsDialogState()

    // Goal Detail
    data class GoalDetail(val goalId: String) : GoalsDialogState()
}

enum class AccountAction {
    CONTRIBUTE,
    WITHDRAW,
    LINK
}

/**
 * State for auto-allocation rule creation/editing form.
 */
data class AutoAllocationRuleForm(
    val goalId: String = "",
    val goalName: String = "",
    val triggerType: AllocationTriggerType = AllocationTriggerType.INCOME_RECEIVED,
    val categoryId: String? = null,
    val accountId: String? = null,
    val threshold: String = "",
    val allocationType: AllocationValueType = AllocationValueType.PERCENT,
    val allocationValue: String = "10",
    val isEnabled: Boolean = true
) {
    val isValid: Boolean
        get() = goalId.isNotBlank() && allocationValue.toDoubleOrNull()?.let { it > 0 } == true

    fun toRule(): AutoAllocationRule? {
        val value = allocationValue.toDoubleOrNull() ?: return null
        return AutoAllocationRule(
            id = "", // Will be generated
            goalId = goalId,
            goalName = goalName,
            triggerType = triggerType,
            triggerParams = AllocationTriggerParams(
                categoryId = categoryId,
                accountId = accountId,
                threshold = threshold.toDoubleOrNull()
            ),
            allocationType = allocationType,
            allocationValue = value,
            isEnabled = isEnabled,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
    }
}
