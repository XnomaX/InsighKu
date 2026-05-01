
package com.example.insightku.ui.components.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.insightku.ui.components.analytics.model.CategoryData
import com.example.insightku.ui.components.analytics.model.TimePeriod

@Preview(showBackground = true, name = "Analytics Screen Full Preview")
@Composable
fun AnalyticsScreenPreview() {
    MaterialTheme {
        val dummyState = AnalyticsUiState(
            isLoading = false,
            selectedMonth = "2024-06",
            totalIncome = 3200000.0,
            totalExpenses = 2650000.0,
            savings = 550000.0,
            savingsRate = 17.2,
            currentMonthData = com.example.insightku.ui.components.analytics.model.AnalyticsDataSource.getMonthlyDataMap()["2024-06"],
            budgetData = com.example.insightku.ui.components.analytics.model.AnalyticsDataSource.monthlyBudgetData,
            incomeExpenseData = com.example.insightku.ui.components.analytics.model.AnalyticsDataSource.getMonthlyIncomeExpenseData()
        )
        AnalyticsScreenContent(uiState = dummyState, onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StatisticsSectionPreview() {
    MaterialTheme {
        StatisticsSection(
            totalIncome = 3200000.0,
            totalExpenses = 2650000.0,
            savings = 550000.0
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsRateSectionPreview() {
    MaterialTheme {
        SavingsRateSection(
            savingsRate = 17.2,
            selectedMonth = "June 2024"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthSelectorPreview() {
    MaterialTheme {
        MonthSelector(
            selectedMonth = "June 2024",
            onPreviousMonth = {},
            onNextMonth = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BudgetPerformanceCardPreview() {
    MaterialTheme {
        BudgetPerformanceCard(
            budgetPeriod = TimePeriod.MONTHLY,
            onPeriodChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IncomeExpensesChartPreview() {
    MaterialTheme {
        IncomeExpensesChart(
            timePeriod = TimePeriod.MONTHLY,
            onPeriodChange = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DonutChartsPreview() {
    MaterialTheme {
        InteractiveDonutChart(
            title = "Expense Categories",
            titleIcon = "💸",
            subtitle = "Monthly expenses by category",
            categories = listOf(
                CategoryData("Food", 1500000.0, Color(0xFFEF4444), Icons.Default.Fastfood),
                CategoryData("Transport", 500000.0, Color(0xFF10B981), Icons.Default.DirectionsCar),
                CategoryData("Entertainment", 300000.0, Color(0xFF3B82F6), Icons.Default.Movie)
            ),
            selectedCategory = null,
            onCategoryClick = {},
            centerColor = Color(0xFFEF4444),
            monthName = "June 2024"
        )
    }
}
