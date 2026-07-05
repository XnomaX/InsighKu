package com.example.insightku.feature.home.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.insightku.core.ui.theme.InsightKuTheme

@Preview(name = "Dashboard Screen Light", showBackground = true)
@Preview(name = "Dashboard Screen Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DashboardScreenPreview() {
    InsightKuTheme {
        val dummyState = DashboardUiState(
            isLoading = false,
            userName = "Andi",
            totalBalance = 4256.80,
            monthlyIncome = 3200.0,
            monthlyExpenses = 2650.0,
            monthlySavings = 550.0,
            isBalanceVisible = true,
            aiInsightMessage = "💡 You're on track this month. Spending is 8% lower than average.",
            insightMessages = listOf(
                "You're saving 17% of your income this month. Keep the momentum.",
                "Recurring payments are under control — spending ratio looks healthy.",
                "5-day tracking streak. Your habit is becoming automatic."
            ),
            recentTransactions = listOf(
                TransactionItem(
                    id = "1", title = "Grocery Shopping", category = "Food & Dining",
                    amount = 85.50, time = "2h", isIncome = false,
                    iconName = "shopping", colorHex = "#FF6B6B"
                ),
                TransactionItem(
                    id = "2", title = "Salary Payment", category = "Income",
                    amount = 3500.00, time = "1d", isIncome = true,
                    iconName = "salary", colorHex = "#4ECDC4"
                )
            ),
            currentStreak = 5,
            hasTrackedToday = false
        )
        DashboardScreenContent(
            uiState = dummyState,
            onEvent = {},
            onNavigateToTransactionDetails = {},
            onAddTransaction = {}
        )
    }
}
