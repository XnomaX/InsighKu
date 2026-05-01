
package com.example.insightku.ui.components.dashboard

import kotlin.random.Random

/**
 * A singleton object to provide dummy data for the Dashboard screen.
 * In a real application, this logic would be in a Repository fetching from a database or network.
 */
object DashboardDataSource {

    fun getDummyDashboardState(): DashboardUiState {
        return DashboardUiState(
            isLoading = false,
            userName = "Andi",
            totalBalance = 4256.80,
            monthlyIncome = 3200.0,
            monthlyExpenses = 2650.0,
            isBalanceVisible = true,
            weeklyForecastData = (0..6).map { Random.nextInt(50, 200).toFloat() },
            monthlyForecastData = (0..11).map { Random.nextInt(1500, 2500).toFloat() },
            aiInsightMessage = "💡 AI Insight: You're on track this month. Your spending is 8% lower than average.",
            recentTransactions = getDummyTransactions(),
            currentStreak = 5,
            hasTrackedToday = false,
            forecastPeriod = ForecastPeriod.WEEKLY
        )
    }

    private fun getDummyTransactions(): List<TransactionItem> {
        return listOf(
            TransactionItem(
                id = "1",
                title = "Starbucks Coffee",
                category = "Food & Drinks",
                amount = 4.50,
                time = "2:30 PM",
                isIncome = false,
                iconName = "local_cafe",
                colorHex = "#F59E0B"
            ),
            TransactionItem(
                id = "2",
                title = "Uber Ride",
                category = "Transportation",
                amount = 12.80,
                time = "1:15 PM",
                isIncome = false,
                iconName = "directions_car",
                colorHex = "#3B82F6"
            ),
            TransactionItem(
                id = "3",
                title = "Salary Deposit",
                category = "Income",
                amount = 3200.00,
                time = "9:00 AM",
                isIncome = true,
                iconName = "trending_up",
                colorHex = "#10B981"
            )
        )
    }
}
