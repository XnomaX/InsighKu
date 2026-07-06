package com.example.insightku.feature.budgeting.presentation

import com.example.insightku.core.data.model.CategoryType

/**
 * Actions that can be triggered from outside the Budgeting screen
 * (e.g., from Home screen CTAs) to open specific creation dialogs.
 */
sealed class BudgetingAction {
    data object OpenCreateGoal : BudgetingAction()
    data class OpenCreateBudget(
        val categoryType: CategoryType = CategoryType.EXPENSE
    ) : BudgetingAction()
}
