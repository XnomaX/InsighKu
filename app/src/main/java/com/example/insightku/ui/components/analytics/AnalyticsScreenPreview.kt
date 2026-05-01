package com.example.insightku.ui.components.analytics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.insightku.ui.components.analytics.model.AnalyticsDataSource
import com.example.insightku.ui.components.analytics.model.CategoryData
import com.example.insightku.ui.components.analytics.model.TimePeriod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Preview(showBackground = true, name = "Analytics Screen Full Preview")
@Composable
fun AnalyticsScreenPreview() {
    MaterialTheme {
        val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().time)
        val dummyState = AnalyticsUiState(
            isLoading = false,
            selectedMonth = currentMonthKey,
            availableMonths = AnalyticsDataSource.monthlyData.keys.toList(),
            currentMonthData = AnalyticsDataSource.monthlyData[currentMonthKey],
            selectedExpenseCategory = null,
            selectedIncomeCategory = null,
            chartTimePeriod = TimePeriod.MONTHLY,
            budgetTimePeriod = TimePeriod.MONTHLY,
            budgetData = AnalyticsDataSource.monthlyBudgetData,
            incomeExpenseData = AnalyticsDataSource.incomeExpenseData
        )
        AnalyticsScreenContent(uiState = dummyState, onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsSectionPreview() {
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
fun SavingsRateSectionPreview() {
    MaterialTheme {
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        SavingsRateSection(
            savingsRate = 17.2,
            selectedMonth = currentMonth
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MonthSelectorPreview() {
    MaterialTheme {
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        MonthSelector(
            selectedMonth = currentMonth,
            onPreviousMonth = {},
            onNextMonth = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetPerformanceCardPreview() {
    MaterialTheme {
        BudgetPerformanceCard(
            budgetPeriod =  TimePeriod.MONTHLY,
            onPeriodChange = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IncomeExpensesChartPreview() {
    MaterialTheme {
        IncomeExpensesChart(
            timePeriod = TimePeriod.MONTHLY,
            onPeriodChange = { },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseDonutChartPreview() {
    MaterialTheme {
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        InteractiveDonutChart(
            categories = listOf(
                CategoryData("Food", 1200000.0, Color(0xFFEF4444), Icons.Default.Fastfood),
                CategoryData("Transport", 500000.0, Color(0xFF10B981), Icons.Default.DirectionsCar),
                CategoryData("Entertainment", 300000.0, Color(0xFF3B82F6), Icons.Default.Movie)
            ),
            selectedCategory = null,
            onCategoryClick = {},
            centerColor = Color(0xFFEF4444),
            monthName = currentMonth,
            title = "Expense Breakdown",
            titleIcon = "",
            subtitle = "Your top spending categories this month."
        )
    }
}
