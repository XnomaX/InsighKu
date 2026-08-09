package com.example.insightku.feature.planning.budget.presentation

import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget

sealed class BudgetingEvent {
    // General
    object LoadBudgetData : BudgetingEvent()
    object RefreshData : BudgetingEvent()
    object ClearError : BudgetingEvent()

    // Category dialogs
    data class ShowAddBudgetDialog(val categoryType: CategoryType) : BudgetingEvent()
    object HideAddBudgetDialog : BudgetingEvent()
    data class AddCategory(val category: Category) : BudgetingEvent()
    data class ShowEditBudgetDialog(val category: BudgetCategory) : BudgetingEvent()
    object HideEditBudgetDialog : BudgetingEvent()
    data class UpdateCategory(val category: Category) : BudgetingEvent()
    data class ShowDeleteConfirmDialog(val category: BudgetCategory) : BudgetingEvent()
    object HideDeleteConfirmDialog : BudgetingEvent()
    data class ConfirmDeleteCategory(val categoryId: String, val categoryName: String) : BudgetingEvent()
    data class DeleteCategory(val categoryId: String) : BudgetingEvent()

    // Recurring payment dialogs
    object ShowAddRecurringDialog : BudgetingEvent()
    data class ShowEditRecurringDialog(val budget: RecurringBudget) : BudgetingEvent()
    object HideRecurringDialog : BudgetingEvent()
    data class AddRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()
    data class UpdateRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()
    data class DeleteRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()

    // Installment dialogs
    object ShowAddInstallmentDialog : BudgetingEvent()
    data class ShowEditInstallmentDialog(val installment: Installment) : BudgetingEvent()
    object HideInstallmentDialog : BudgetingEvent()
    data class AddInstallment(val installment: Installment) : BudgetingEvent()
    data class UpdateInstallment(val installment: Installment) : BudgetingEvent()
    data class DeleteInstallment(val installmentId: String) : BudgetingEvent()
}
