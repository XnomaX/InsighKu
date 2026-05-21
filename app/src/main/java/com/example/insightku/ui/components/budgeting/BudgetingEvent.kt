package com.example.insightku.ui.components.budgeting

import com.example.insightku.data.model.Category
import com.example.insightku.data.model.RecurringBudget

sealed class BudgetingEvent {
    // General Events
    object LoadBudgetData : BudgetingEvent()
    object RefreshData : BudgetingEvent()
    object ClearError : BudgetingEvent()
    data class ChangePeriod(val period: BudgetPeriod) : BudgetingEvent()

    // Add Category Dialog Events
    object ShowAddBudgetDialog : BudgetingEvent()
    object HideAddBudgetDialog : BudgetingEvent()
    data class AddCategory(val category: Category) : BudgetingEvent()

    // Edit Category Dialog Events
    data class ShowEditBudgetDialog(val category: BudgetCategory) : BudgetingEvent()
    object HideEditBudgetDialog : BudgetingEvent()
    data class UpdateCategory(val category: Category) : BudgetingEvent()

    // Delete Category Events
    data class ShowDeleteConfirmDialog(val category: BudgetCategory) : BudgetingEvent()
    object HideDeleteConfirmDialog : BudgetingEvent()
    data class ConfirmDeleteCategory(val categoryId: String, val categoryName: String) : BudgetingEvent()

    // Keep for backward compat — routes through ShowDeleteConfirmDialog now
    data class DeleteCategory(val categoryId: String) : BudgetingEvent()

    // Recurring Budgets Dialog Events
    object ShowRecurringBudgetsDialog : BudgetingEvent()
    object HideRecurringBudgetsDialog : BudgetingEvent()
    data class AddRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()
    data class UpdateRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()
    data class DeleteRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()
}
