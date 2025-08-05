package com.example.insightku.ui.components.dashboard

// Main state for the Dashboard screen
data class DashboardState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Balance data
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    // AI Forecast data
    val weeklyForecastData: List<Float> = emptyList(),
    val monthlyForecastData: List<Float> = emptyList(),
    val aiInsightMessage: String = "",
    // Recent Transactions data
    val recentTransactions: List<TransactionItem> = emptyList(),
    // Daily Streak data
    val currentStreak: Int = 0,
    val hasTrackedToday: Boolean = false
)

// Data class for Transactions
data class TransactionItem(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val time: String,
    val isIncome: Boolean = false,
    val iconName: String,
    val colorHex: String
)
