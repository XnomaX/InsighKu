package com.example.insightku.ui.components.analytics.model

// This file contains additional analytics-related utilities and extensions
// The main data models are now properly organized in AnalyticsState.kt and AnalyticsDataSource.kt

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.NumberFormat
import java.util.*

// Enum for time periods
enum class TimePeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}

object AnalyticsUtils {

    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    fun formatCurrencyShort(amount: Double): String {
        return when {
            amount >= 1_000_000 -> "Rp ${(amount / 1_000_000).format(1)}M"
            amount >= 1_000 -> "Rp ${(amount / 1_000).format(0)}K"
            else -> "Rp ${amount.format(0)}"
        }
    }

    private fun Double.format(digits: Int): String {
        return "%.${digits}f".format(this)
    }

    fun calculateSavingsRate(income: Double, expenses: Double): Double {
        return if (income > 0) ((income - expenses) / income) * 100 else 0.0
    }

    fun getMonthDisplayName(monthKey: String): String {
        return try {
            val parts = monthKey.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            "${monthNames[month - 1]} $year"
        } catch (e: Exception) {
            monthKey
        }
    }
}

data class CategoryData(
    val name: String,
    val amount: Double,
    val color: Color,
    val icon: ImageVector,
    val percentage: Double = 0.0
) {
    val value: Double get() = amount // Alias for compatibility
}

data class MonthlyData(
    val monthKey: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val expenseCategories: List<CategoryData>,
    val incomeCategories: List<CategoryData> = getDefaultIncomeCategories()
) {
    val savings: Double get() = totalIncome - totalExpenses
    val savingsRate: Double get() = if (totalIncome > 0) (savings / totalIncome) * 100 else 0.0
}

data class BudgetData(
    val period: String, // "W1", "W2", "Jan", "Feb", etc.
    val budget: Double,
    val actual: Double
) {
    val variance: Double get() = actual - budget
    val isOverBudget: Boolean get() = actual > budget
}

data class IncomeExpenseData(
    val period: String,
    val income: Double,
    val expenses: Double
)

// Data class for income categories
data class IncomeCategory(
    val name: String,
    val amount: Double,
    val color: Color,
    val icon: ImageVector
)

// Target data for budget performance
data class Target(
    val amount: Double,
    val period: String
)

// Default income categories
fun getDefaultIncomeCategories(): List<CategoryData> = listOf(
    CategoryData(
        name = "Salary",
        amount = 2800.0,
        color = Color(0xFF10B981),
        icon = Icons.Default.Work
    ),
    CategoryData(
        name = "Freelance",
        amount = 400.0,
        color = Color(0xFF06B6D4),
        icon = Icons.Default.Computer
    ),
    CategoryData(
        name = "Investment",
        amount = 200.0,
        color = Color(0xFF8B5CF6),
        icon = Icons.Default.TrendingUp
    ),
    CategoryData(
        name = "Other",
        amount = 100.0,
        color = Color(0xFFF59E0B),
        icon = Icons.Default.AccountBalance
    )
)

// Mock data for different months
fun getMonthlyDataMap(): Map<String, MonthlyData> = mapOf(
    "2024-06" to MonthlyData(
        monthKey = "2024-06",
        totalIncome = 3200000.0,
        totalExpenses = 2650000.0,
        expenseCategories = listOf(
            CategoryData("Food & Drinks", 850000.0, Color(0xFFF59E0B), Icons.Default.Restaurant),
            CategoryData("Transportation", 650000.0, Color(0xFF3B82F6), Icons.Default.DirectionsCar),
            CategoryData("Entertainment", 420000.0, Color(0xFF8B5CF6), Icons.Default.SportsEsports),
            CategoryData("Shopping", 380000.0, Color(0xFFEC4899), Icons.Default.ShoppingBag),
            CategoryData("Coffee", 280000.0, Color(0xFFF59E0B), Icons.Default.LocalCafe),
            CategoryData("Housing", 1070000.0, Color(0xFFEF4444), Icons.Default.Home)
        )
    ),
    "2024-05" to MonthlyData(
        monthKey = "2024-05",
        totalIncome = 3200000.0,
        totalExpenses = 2850000.0,
        expenseCategories = listOf(
            CategoryData("Food & Drinks", 920000.0, Color(0xFFF59E0B), Icons.Default.Restaurant),
            CategoryData("Transportation", 580000.0, Color(0xFF3B82F6), Icons.Default.DirectionsCar),
            CategoryData("Entertainment", 380000.0, Color(0xFF8B5CF6), Icons.Default.SportsEsports),
            CategoryData("Shopping", 450000.0, Color(0xFFEC4899), Icons.Default.ShoppingBag),
            CategoryData("Coffee", 320000.0, Color(0xFFF59E0B), Icons.Default.LocalCafe),
            CategoryData("Housing", 200000.0, Color(0xFFEF4444), Icons.Default.Home)
        )
    ),
    "2024-04" to MonthlyData(
        monthKey = "2024-04",
        totalIncome = 3500000.0,
        totalExpenses = 3100000.0,
        expenseCategories = listOf(
            CategoryData("Food & Drinks", 890000.0, Color(0xFFF59E0B), Icons.Default.Restaurant),
            CategoryData("Transportation", 720000.0, Color(0xFF3B82F6), Icons.Default.DirectionsCar),
            CategoryData("Entertainment", 510000.0, Color(0xFF8B5CF6), Icons.Default.SportsEsports),
            CategoryData("Shopping", 420000.0, Color(0xFFEC4899), Icons.Default.ShoppingBag),
            CategoryData("Coffee", 260000.0, Color(0xFFF59E0B), Icons.Default.LocalCafe),
            CategoryData("Housing", 300000.0, Color(0xFFEF4444), Icons.Default.Home)
        )
    )
)

// Budget performance data
fun getMonthlyBudgetData(): List<BudgetData> = listOf(
    BudgetData("Jan", 3300000.0, 2980000.0),
    BudgetData("Feb", 3300000.0, 3150000.0),
    BudgetData("Mar", 3300000.0, 3420000.0),
    BudgetData("Apr", 3300000.0, 3100000.0),
    BudgetData("May", 3300000.0, 2850000.0),
    BudgetData("Jun", 3300000.0, 2785000.0)
)

fun getWeeklyBudgetData(): List<BudgetData> = listOf(
    BudgetData("W1", 825000.0, 696000.0),
    BudgetData("W2", 825000.0, 787000.0),
    BudgetData("W3", 825000.0, 652000.0),
    BudgetData("W4", 825000.0, 650000.0)
)

// Income vs Expenses data
fun getMonthlyIncomeExpenseData(): List<IncomeExpenseData> = listOf(
    IncomeExpenseData("Jan", 3200000.0, 2100000.0),
    IncomeExpenseData("Feb", 3200000.0, 1890000.0),
    IncomeExpenseData("Mar", 3200000.0, 2340000.0),
    IncomeExpenseData("Apr", 3500000.0, 2890000.0),
    IncomeExpenseData("May", 3500000.0, 2450000.0),
    IncomeExpenseData("Jun", 3200000.0, 2650000.0)
)

fun getWeeklyIncomeExpenseData(): List<IncomeExpenseData> = listOf(
    IncomeExpenseData("W1", 800000.0, 520000.0),
    IncomeExpenseData("W2", 800000.0, 680000.0),
    IncomeExpenseData("W3", 800000.0, 590000.0),
    IncomeExpenseData("W4", 800000.0, 610000.0)
)
