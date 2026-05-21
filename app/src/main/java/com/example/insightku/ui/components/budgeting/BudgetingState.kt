package com.example.insightku.ui.components.budgeting

import androidx.compose.runtime.Immutable
import com.example.insightku.data.model.RecurringBudget

@Immutable
data class BudgetingUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val budgetCategories: List<BudgetCategory> = emptyList(),
    val recurringBudgets: List<RecurringBudget> = emptyList(),
    val selectedPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val dialogState: DialogState = DialogState.None
) {
    val remainingBudget: Double get() = totalBudget - limitedSpent
    val limitedSpent: Double get() = budgetCategories.filter { it.hasLimit }.sumOf { it.spentAmount }
    val unlimitedSpent: Double get() = budgetCategories.filterNot { it.hasLimit }.sumOf { it.spentAmount }
    val budgetUtilizationPercentage: Double get() = if (totalBudget > 0) (limitedSpent / totalBudget) * 100 else 0.0
    val overBudgetCategories: List<BudgetCategory> get() = budgetCategories.filter { it.isOverBudget }
}

sealed class DialogState {
    data object None : DialogState()
    data object AddBudget : DialogState()
    data class EditBudget(val category: BudgetCategory) : DialogState()
    data class DeleteConfirm(val category: BudgetCategory) : DialogState()
    data class ManageRecurring(val budgets: List<RecurringBudget>) : DialogState()
}

enum class BudgetPeriod(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class BudgetHealth {
    Unlimited,
    Good,
    Warning,
    Over
}

@Immutable
data class BudgetCategory(
    val id: String,
    val name: String,
    val budgetedAmount: Double?,
    val spentAmount: Double,
    val color: String,
    val icon: String,
    val recurringPeriod: String? = null
) {
    val hasLimit: Boolean get() = (budgetedAmount ?: 0.0) > 0.0
    val limitAmount: Double get() = budgetedAmount ?: 0.0
    val remainingAmount: Double get() = if (hasLimit) limitAmount - spentAmount else 0.0
    val utilizationPercentage: Double get() = if (hasLimit) (spentAmount / limitAmount) * 100 else 0.0
    val progressFraction: Float get() = (utilizationPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
    val isOverBudget: Boolean get() = hasLimit && spentAmount > limitAmount
    val health: BudgetHealth
        get() = when {
            !hasLimit -> BudgetHealth.Unlimited
            utilizationPercentage < 70.0 -> BudgetHealth.Good
            utilizationPercentage <= 100.0 -> BudgetHealth.Warning
            else -> BudgetHealth.Over
        }
}
