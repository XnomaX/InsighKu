package com.example.insightku.ui.components.budgeting

import com.example.insightku.data.model.Category
import com.example.insightku.data.model.RecurringBudget

data class BudgetingState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val budgetCategories: List<BudgetCategory> = emptyList(),
    val selectedPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val showAddBudgetDialog: Boolean = false,
    val budgetUtilizationPercentage: Double = 0.0,
    val overBudgetCategories: List<BudgetCategory> = emptyList(),
    // --- PROPERTI BARU UNTUK EDIT DIALOG ---
    val editingCategory: Category? = null,
    val showRecurringBudgetsDialog: Boolean = false,
    val recurringBudgets: List<RecurringBudget> = emptyList()
)

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
    val remainingAmount: Double
        get() = budgetedAmount - spentAmount

    val utilizationPercentage: Double
        get() = if (budgetedAmount > 0) (spentAmount / budgetedAmount) * 100 else 0.0

    val isOverBudget: Boolean
        get() = spentAmount > budgetedAmount
}
