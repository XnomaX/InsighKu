package com.example.insightku.feature.planning.budget.domain

import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.utils.normalizedCategoryName

/**
 * Default category seed data and color heuristics for the budget feature.
 *
 * Extracted from BudgetingViewModel so the presentation layer does not own
 * domain seed data. Values are unchanged from the original implementation.
 */
object DefaultBudgetCategories {

    /** System categories created on first seed (cannot be deleted by the user). */
    val systemCategories: List<Category> = listOf(
        Category(id = "system-uncategorized-expense", name = "Uncategorized", color = "#79747E", icon = "Others", categoryType = CategoryType.EXPENSE.name, isSystemCategory = true),
        Category(id = "system-uncategorized-income", name = "Uncategorized Income", color = "#79747E", icon = "Others", categoryType = CategoryType.INCOME.name, isSystemCategory = true)
    )

    /** Default user-facing categories seeded on first run. */
    val userCategories: List<Category> = listOf(
        Category(name = "Food", color = "#F59E0B", icon = "Food & Drinks", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
        Category(name = "Transport", color = "#3B82F6", icon = "Transportation", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
        Category(name = "Bills", color = "#EF4444", icon = "Utilities", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
        Category(name = "Shopping", color = "#EC4899", icon = "Shopping", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
        Category(name = "Health", color = "#10B981", icon = "Healthcare", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
        Category(name = "Entertainment", color = "#8B5CF6", icon = "Entertainment", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
        Category(name = "Salary", color = "#10B981", icon = "Investment", budgetLimit = null, categoryType = CategoryType.INCOME.name),
        Category(name = "Freelance", color = "#06B6D4", icon = "Investment", budgetLimit = null, categoryType = CategoryType.INCOME.name),
        Category(name = "Business", color = "#F59E0B", icon = "Investment", budgetLimit = null, categoryType = CategoryType.INCOME.name),
        Category(name = "Investment", color = "#3B82F6", icon = "Investment", budgetLimit = null, categoryType = CategoryType.INCOME.name),
    )

    /** All categories to seed (system + user). */
    val all: List<Category> get() = systemCategories + userCategories

    /** Heuristic color for a category name, based on keyword matching. */
    fun colorForCategory(categoryName: String): String = when {
        categoryName.normalizedCategoryName().contains("food") -> "#F59E0B"
        categoryName.normalizedCategoryName().contains("transport") -> "#3B82F6"
        categoryName.normalizedCategoryName().contains("bill") -> "#EF4444"
        categoryName.normalizedCategoryName().contains("shop") -> "#EC4899"
        categoryName.normalizedCategoryName().contains("health") -> "#10B981"
        else -> "#79747E"
    }
}
