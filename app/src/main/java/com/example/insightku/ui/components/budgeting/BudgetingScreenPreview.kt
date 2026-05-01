
package com.example.insightku.ui.components.budgeting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, name = "Full Budgeting Screen")
@Composable
fun BudgetingScreenPreview() {
    val fakeState = BudgetingUiState(
        isLoading = false,
        totalBudget = 5000000.0,
        totalSpent = 3750000.0,
        budgetCategories = BudgetingDataSource.getDummyBudgetCategories(),
    )

    MaterialTheme {
        BudgetingScreenContent(uiState = fakeState, onEvent = {})
    }
}

@Preview(showBackground = true, name = "Budget Summary Card")
@Composable
private fun BudgetSummaryCardPreview() {
    MaterialTheme {
        BudgetSummaryCard(
            totalBudget = 15000000.0,
            totalSpent = 7850000.0,
            remaining = 7150000.0,
            percentage = 52.3f,
            overBudgetCategories = emptyList()
        )
    }
}

@Preview(showBackground = true, name = "Budget Summary With Alert")
@Composable
private fun BudgetSummaryCardWithAlertPreview() {
    MaterialTheme {
        BudgetSummaryCard(
            totalBudget = 15000000.0,
            totalSpent = 16000000.0,
            remaining = -1000000.0,
            percentage = 106.7f,
            overBudgetCategories = listOf(
                BudgetCategory("1", "Shopping", 0.0, 0.0, "", "")
            )
        )
    }
}

@Preview(showBackground = true, name = "Category Budgets Card")
@Composable
private fun CategoryBudgetsCardPreview() {
    MaterialTheme {
        CategoryBudgetsCard(
            categories = BudgetingDataSource.getDummyBudgetCategories(),
            onAddCategory = {},
            onEditCategory = {}
        )
    }
}

@Preview(showBackground = true, name = "Scheduled Payments Card")
@Composable
private fun ScheduledPaymentsCardPreview() {
    val payments = listOf(
        ScheduledPayment("Netflix Subscription", 250000.0, "Jun 28", Icons.Default.Repeat, Color(0xFFEF4444)),
        ScheduledPayment("Rent Payment", 2500000.0, "Jul 1", Icons.Default.Home, Color(0xFF3B82F6)),
        ScheduledPayment("Gym Membership", 350000.0, "Jul 5", Icons.Default.FitnessCenter, Color(0xFF10B981))
    )
    MaterialTheme {
        ScheduledPaymentsCard(payments = payments, onManageRecurring = {})
    }
}
