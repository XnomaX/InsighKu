package com.example.insightku.feature.budgeting.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, name = "Budgeting - No Data")
@Composable
fun BudgetingScreenNoDataPreview() {
    MaterialTheme {
        BudgetingScreenContent(
            uiState = BudgetingUiState(isLoading = false),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Budgeting - Safe Month")
@Composable
fun BudgetingScreenSafePreview() {
    val categories = listOf(
        BudgetCategory("1", "Food", 2_000_000.0, 860_000.0, "#F59E0B", "Food & Drinks"),
        BudgetCategory("2", "Transport", 1_000_000.0, 420_000.0, "#3B82F6", "Transportation"),
        BudgetCategory("3", "Lifestyle", null, 210_000.0, "#8B5CF6", "Entertainment")
    )

    MaterialTheme {
        BudgetingScreenContent(
            uiState = budgetingPreviewState(categories),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Budgeting - Warning")
@Composable
fun BudgetingScreenWarningPreview() {
    val categories = listOf(
        BudgetCategory("1", "Food", 2_000_000.0, 1_620_000.0, "#F59E0B", "Food & Drinks"),
        BudgetCategory("2", "Bills", 1_500_000.0, 1_020_000.0, "#EF4444", "Bills & Utilities"),
        BudgetCategory("3", "Health", 800_000.0, 190_000.0, "#10B981", "Healthcare")
    )

    MaterialTheme {
        BudgetingScreenContent(
            uiState = budgetingPreviewState(categories),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Budgeting - Over Budget")
@Composable
fun BudgetingScreenOverBudgetPreview() {
    val categories = listOf(
        BudgetCategory("1", "Bills", 1_500_000.0, 1_820_000.0, "#EF4444", "Bills & Utilities"),
        BudgetCategory("2", "Food", 2_000_000.0, 1_780_000.0, "#F59E0B", "Food & Drinks"),
        BudgetCategory("3", "OCR: Misc", null, 240_000.0, "#79747E", "")
    )

    MaterialTheme {
        BudgetingScreenContent(
            uiState = budgetingPreviewState(categories),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Budget Category Card")
@Composable
fun BudgetCategoryCardPreview() {
    MaterialTheme {
        BudgetCategoryCard(
            category = BudgetCategory(
                id = "1",
                name = "Food",
                budgetedAmount = 2_000_000.0,
                spentAmount = 1_620_000.0,
                color = "#F59E0B",
                icon = "Food & Drinks"
            ),
            onEdit = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun budgetingPreviewState(categories: List<BudgetCategory>): BudgetingUiState {
    return BudgetingUiState(
        isLoading = false,
        totalBudget = categories.filter { it.hasLimit }.sumOf { it.limitAmount },
        totalSpent = categories.sumOf { it.spentAmount },
        budgetCategories = categories
    )
}

