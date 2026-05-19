package com.example.insightku.ui.components.dashboard

/**
 * Represents the entire state for the Dashboard screen.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userName: String = "User", // Default name
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val isBalanceVisible: Boolean = true,
    val weeklyForecastData: List<Float> = emptyList(),
    val monthlyForecastData: List<Float> = emptyList(),
    val aiInsightMessage: String = "",
    val recentTransactions: List<TransactionItem> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val hasTrackedToday: Boolean = false,
    val forecastPeriod: ForecastPeriod = ForecastPeriod.WEEKLY
)

enum class ForecastPeriod {
    WEEKLY, MONTHLY
}

// Data class for Transactions - This might be shared or moved to a model package
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