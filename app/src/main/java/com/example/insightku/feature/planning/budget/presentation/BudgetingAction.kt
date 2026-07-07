package com.example.insightku.feature.planning.budget.presentation

import com.example.insightku.core.data.model.CategoryType

/**
 * One-shot actions dispatched from Home/Dashboard to the Budgeting screen.
 * Consumed by the screen's LaunchedEffect, then cleared.
 */
sealed class BudgetingAction {
    data object OpenCreateGoal : BudgetingAction()
    data object NavigateToGoals : BudgetingAction()
    data class NavigateToBudgetDetail(val budgetId: String) : BudgetingAction()
    data class OpenCreateBudget(
        val categoryType: CategoryType = CategoryType.EXPENSE
    ) : BudgetingAction()
}
