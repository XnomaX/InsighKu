package com.example.insightku.ui.components.budgeting

import com.example.insightku.data.model.Category
import com.example.insightku.data.model.RecurringBudget

data class BudgetingUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val budgetCategories: List<BudgetCategory> = emptyList(),
    val selectedPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val dialogState: DialogState = DialogState.None
) {
    val remainingBudget: Double get() = totalBudget - totalSpent
    val budgetUtilizationPercentage: Double get() = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0
    val overBudgetCategories: List<BudgetCategory> get() = budgetCategories.filter { it.isOverBudget }
}

sealed class DialogState {
    object None : DialogState()
    object AddBudget : DialogState()
    data class EditBudget(val category: BudgetCategory) : DialogState()
    data class ManageRecurring(val budgets: List<RecurringBudget>) : DialogState()
}

enum class BudgetPeriod(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class BudgetCategory(
    val id: String,
    val name: String,
    val budgetedAmount: Double,
    val spentAmount: Double,
    val color: String,
    val icon: String
) {
    val remainingAmount: Double get() = budgetedAmount - spentAmount
    val utilizationPercentage: Double get() = if (budgetedAmount > 0) (spentAmount / budgetedAmount) * 100 else 0.0
    val isOverBudget: Boolean get() = spentAmount > budgetedAmount
}