package com.example.insightku.ui.components.analytics

import com.example.insightku.ui.components.analytics.model.*

data class AnalyticsState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedMonth: String = "2024-06",
    val availableMonths: List<String> = listOf("2024-06", "2024-05", "2024-04", "2024-03", "2024-02", "2024-01"),
    val currentMonthData: MonthlyData? = null,
    val selectedExpenseCategory: String? = null,
    val selectedIncomeCategory: String? = null,
    val timePeriod: TimePeriod = TimePeriod.MONTHLY,
    val budgetPeriod: TimePeriod = TimePeriod.MONTHLY,
    
    // DATA BARU UNTUK BUDGET
    val weeklyBudgetData: List<BudgetData> = emptyList(),
    val monthlyBudgetData: List<BudgetData> = emptyList(),
    val budgetData: List<BudgetData> = emptyList(),

    val incomeExpenseData: List<IncomeExpenseData> = emptyList()
) {
    val savings: Double
        get() = currentMonthData?.savings ?: 0.0

    val savingsRate: Double
        get() = currentMonthData?.savingsRate ?: 0.0

    val totalIncome: Double
        get() = currentMonthData?.totalIncome ?: 0.0

    val totalExpenses: Double
        get() = currentMonthData?.totalExpenses ?: 0.0
}
