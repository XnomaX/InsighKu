package com.example.insightku.ui.components.budgeting

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

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

@Preview(showBackground = true, name = "Budgeting - Normal Usage")
@Composable
fun BudgetingScreenNormalPreview() {
    val categories = listOf(
        BudgetCategory("1", "Food", 2_000_000.0, 1_250_000.0, "#F59E0B", "Food & Drinks"),
        BudgetCategory("2", "Transport", 1_000_000.0, 620_000.0, "#3B82F6", "Transportation"),
        BudgetCategory("3", "Lifestyle", null, 410_000.0, "#8B5CF6", "Entertainment")
    )

    MaterialTheme {
        BudgetingScreenContent(
            uiState = BudgetingUiState(
                isLoading = false,
                totalBudget = categories.filter { it.hasLimit }.sumOf { it.limitAmount },
                totalSpent = categories.sumOf { it.spentAmount },
                budgetCategories = categories
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Budgeting - Over Budget")
@Composable
fun BudgetingScreenOverBudgetPreview() {
    val categories = listOf(
        BudgetCategory("1", "Bills", 1_500_000.0, 1_820_000.0, "#EF4444", "Bills & Utilities"),
        BudgetCategory("2", "Food", 2_000_000.0, 1_820_000.0, "#F59E0B", "Food & Drinks"),
        BudgetCategory("3", "OCR: Misc", null, 240_000.0, "#79747E", "")
    )

    MaterialTheme {
        BudgetingScreenContent(
            uiState = BudgetingUiState(
                isLoading = false,
                totalBudget = categories.filter { it.hasLimit }.sumOf { it.limitAmount },
                totalSpent = categories.sumOf { it.spentAmount },
                budgetCategories = categories
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Budget Summary")
@Composable
fun BudgetSummaryCardPreview() {
    MaterialTheme {
        BudgetSummaryCard(
            totalBudget = 5_000_000.0,
            totalSpent = 3_250_000.0,
            limitedSpent = 2_850_000.0,
            unlimitedSpent = 400_000.0,
            remaining = 2_150_000.0,
            percentage = 57f,
            overBudgetCategories = emptyList()
        )
    }
}
