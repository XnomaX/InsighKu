package com.example.insightku.feature.home.presentation

import com.example.insightku.core.data.model.DraftTransaction
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.planning.goal.domain.model.Goal

/**
 * Represents the entire state for the Dashboard screen.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userName: String = "User",
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val isBalanceVisible: Boolean = true,
    val weeklyForecastData: List<Float> = emptyList(),
    val monthlyForecastData: List<Float> = emptyList(),
    val aiInsightMessage: String = "",
    val recentTransactions: List<TransactionItem> = emptyList(),
    // ── Streak system ──────────────────────────────────────────────────────────
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val hasTrackedToday: Boolean = false,
    val freezeCount: Int = 0,
    val isPerfectStreak: Boolean = false,
    val streakGoal: Int = 7,
    val repairAvailable: Boolean = false,
    val repairExpiryMs: Long = 0L,
    val repairDayKey: String? = null,
    val streakMilestone: StreakMilestone? = null,
    // ──────────────────────────────────────────────────────────────────────────
    val forecastPeriod: ForecastPeriod = ForecastPeriod.WEEKLY,
    val recurringBudgets: List<RecurringBudget> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val budgetCategorySpending: List<BudgetSpendingItem> = emptyList(),
    val monthlySavings: Double = 0.0,
    val insightMessages: List<String> = emptyList(),
    // Draft Inbox — draft hasil deteksi notifikasi bank yang menunggu ditinjau.
    val pendingDrafts: List<DraftTransaction> = emptyList(),
    // ── Goals Preview ────────────────────────────────────────────────────────
    val previewGoals: List<Goal> = emptyList(),
    val totalGoalCount: Int = 0,
    val hasActiveGoals: Boolean = false,
    // ── Budget Preview ────────────────────────────────────────────────────────
    val previewBudgets: List<BudgetSpendingItem> = emptyList(),
    val totalBudgetCount: Int = 0,
    val hasActiveBudgets: Boolean = false,
    // ── Account Balances ──────────────────────────────────────────────────────
    val totalAccountBalance: Double = 0.0,
    val accountCount: Int = 0,
    // ── Snackbar ──────────────────────────────────────────────────────────────
    val snackbarMessage: String? = null
)

enum class ForecastPeriod {
    WEEKLY, MONTHLY
}

enum class StreakMilestone(val days: Int, val label: String) {
    DAY_3(3, "3-Day Habit"),
    DAY_7(7, "Week Warrior"),
    DAY_14(14, "Two Week Strong"),
    DAY_30(30, "Monthly Master"),
    DAY_100(100, "Century Legend");

    companion object {
        fun forStreak(streak: Int): StreakMilestone? =
            entries.sortedByDescending { it.days }.firstOrNull { streak >= it.days }

        fun next(streak: Int): StreakMilestone? =
            entries.sortedBy { it.days }.firstOrNull { it.days > streak }
    }
}

data class TransactionItem(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val time: String,
    val isIncome: Boolean = false,
    val iconName: String,
    val colorHex: String,
    val transactionType: TransactionType = TransactionType.EXPENSE
)

data class BudgetSpendingItem(
    val id: String = "",
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val spent: Double,
    val limit: Double?
)

