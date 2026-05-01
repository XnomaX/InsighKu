package com.example.insightku.ui.components.analytics

import com.example.insightku.ui.components.analytics.model.BudgetData
import com.example.insightku.ui.components.analytics.model.IncomeExpenseData
import com.example.insightku.ui.components.analytics.model.MonthlyData
import com.example.insightku.ui.components.analytics.model.TimePeriod

/**
 * Represents the entire state for the Analytics screen.
 * This is the single source of truth for the UI.
 */
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedMonth: String = "",
    val availableMonths: List<String> = emptyList(),
    val currentMonthData: MonthlyData? = null,
    val selectedExpenseCategory: String? = null,
    val selectedIncomeCategory: String? = null,
    val chartTimePeriod: TimePeriod = TimePeriod.MONTHLY,
    val budgetTimePeriod: TimePeriod = TimePeriod.MONTHLY,
    val budgetData: List<BudgetData> = emptyList(),
    val incomeExpenseData: List<IncomeExpenseData> = emptyList()
) {
    // Derived state - calculated from the core state properties
    val savings: Double
        get() = currentMonthData?.savings ?: 0.0

    val savingsRate: Double
        get() = currentMonthData?.savingsRate ?: 0.0

    val totalIncome: Double
        get() = currentMonthData?.totalIncome ?: 0.0

    val totalExpenses: Double
        get() = currentMonthData?.totalExpenses ?: 0.0
}