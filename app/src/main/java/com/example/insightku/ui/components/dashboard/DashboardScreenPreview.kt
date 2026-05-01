package com.example.insightku.ui.components.dashboard

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.insightku.ui.theme.InsightKuTheme

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
            isBalanceVisible = true,
            aiInsightMessage = "💡 AI Insight: You're on track this month. Your spending is 8% lower than average.",
            recentTransactions = listOf(
                TransactionItem(
                    id = "1",
                    title = "Grocery Shopping",
                    category = "Food & Dining",
                    amount = 85.50,
                    time = "2 hours ago",
                    isIncome = false,
                    iconName = "shopping",
                    colorHex = "#FF6B6B"
                ),
                TransactionItem(
                    id = "2",
                    title = "Salary Payment",
                    category = "Income",
                    amount = 3500.00,
                    time = "1 day ago",
                    isIncome = true,
                    iconName = "salary",
                    colorHex = "#4ECDC4"
                )
            ),
            currentStreak = 5,
            hasTrackedToday = false
        )
        DashboardScreenContent(uiState = dummyState, onEvent = {}, onNavigateToTransactionDetails = {}, onAddTransaction = {})
    }
}

@Preview(name = "Dashboard Header Light", showBackground = true)
@Composable
fun DashboardHeaderPreview() {
    InsightKuTheme {
        DashboardHeader(
            userName = "Andi",
            totalBalance = 4256.80,
            monthlyIncome = 3200.0,
            monthlyExpenses = 2650.0,
            isBalanceVisible = true,
            onToggleVisibility = {}
        )
    }
}

@Preview(name = "Recent Transactions Card Light", showBackground = true)
@Composable
fun RecentTransactionsCardPreview() {
    InsightKuTheme {
        Surface {
            RecentTransactionsCard(
                transactions = listOf(
                    TransactionItem(
                        id = "1",
                        title = "Grocery Shopping",
                        category = "Food & Dining",
                        amount = 85.50,
                        time = "2 hours ago",
                        isIncome = false,
                        iconName = "shopping",
                        colorHex = "#FF6B6B"
                    )
                ),
                onViewAllClick = {}
            )
        }
    }
}

@Preview(name = "Daily Streak Card Active Light", showBackground = true)
@Composable
fun DailyStreakCardActivePreview() {
    InsightKuTheme {
        Surface {
            DailyStreakCard(
                currentStreak = 5,
                hasTrackedToday = false,
                onAddTransaction = {}
            )
        }
    }
}