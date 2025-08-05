package com.example.insightku.ui.components.analytics.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color

object AnalyticsDataSource {
    
    // Mock data untuk berbagai bulan
    val monthlyData = mapOf(
        "2024-06" to MonthlyData(
            monthKey = "2024-06",
            totalIncome = 3200000.0,
            totalExpenses = 2650000.0,
            expenseCategories = listOf(
                CategoryData("Food & Drinks", 850000.0, Color(0xFFF59E0B), Icons.Default.Restaurant),
                CategoryData("Transportation", 650000.0, Color(0xFF3B82F6), Icons.Default.DirectionsCar),
                CategoryData("Entertainment", 420000.0, Color(0xFF8B5CF6), Icons.Default.MovieFilter),
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
                CategoryData("Entertainment", 380000.0, Color(0xFF8B5CF6), Icons.Default.MovieFilter),
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
                CategoryData("Entertainment", 510000.0, Color(0xFF8B5CF6), Icons.Default.MovieFilter),
                CategoryData("Shopping", 420000.0, Color(0xFFEC4899), Icons.Default.ShoppingBag),
                CategoryData("Coffee", 260000.0, Color(0xFFF59E0B), Icons.Default.LocalCafe),
                CategoryData("Housing", 300000.0, Color(0xFFEF4444), Icons.Default.Home)
            )
        ),
        "2024-03" to MonthlyData(
            monthKey = "2024-03",
            totalIncome = 3200000.0,
            totalExpenses = 2980000.0,
            expenseCategories = listOf(
                CategoryData("Food & Drinks", 880000.0, Color(0xFFF59E0B), Icons.Default.Restaurant),
                CategoryData("Transportation", 590000.0, Color(0xFF3B82F6), Icons.Default.DirectionsCar),
                CategoryData("Entertainment", 420000.0, Color(0xFF8B5CF6), Icons.Default.MovieFilter),
                CategoryData("Shopping", 350000.0, Color(0xFFEC4899), Icons.Default.ShoppingBag),
                CategoryData("Coffee", 290000.0, Color(0xFFF59E0B), Icons.Default.LocalCafe),
                CategoryData("Housing", 450000.0, Color(0xFFEF4444), Icons.Default.Home)
            )
        ),
        "2024-02" to MonthlyData(
            monthKey = "2024-02",
            totalIncome = 3200000.0,
            totalExpenses = 2850000.0,
            expenseCategories = listOf(
                CategoryData("Food & Drinks", 820000.0, Color(0xFFF59E0B), Icons.Default.Restaurant),
                CategoryData("Transportation", 560000.0, Color(0xFF3B82F6), Icons.Default.DirectionsCar),
                CategoryData("Entertainment", 390000.0, Color(0xFF8B5CF6), Icons.Default.MovieFilter),
                CategoryData("Shopping", 330000.0, Color(0xFFEC4899), Icons.Default.ShoppingBag),
                CategoryData("Coffee", 270000.0, Color(0xFFF59E0B), Icons.Default.LocalCafe),
                CategoryData("Housing", 480000.0, Color(0xFFEF4444), Icons.Default.Home)
            )
        ),
        "2024-01" to MonthlyData(
            monthKey = "2024-01",
            totalIncome = 3200000.0,
            totalExpenses = 3200000.0,
            expenseCategories = listOf(
                CategoryData("Food & Drinks", 890000.0, Color(0xFFF59E0B), Icons.Default.Restaurant),
                CategoryData("Transportation", 620000.0, Color(0xFF3B82F6), Icons.Default.DirectionsCar),
                CategoryData("Entertainment", 450000.0, Color(0xFF8B5CF6), Icons.Default.MovieFilter),
                CategoryData("Shopping", 420000.0, Color(0xFFEC4899), Icons.Default.ShoppingBag),
                CategoryData("Coffee", 320000.0, Color(0xFFF59E0B), Icons.Default.LocalCafe),
                CategoryData("Housing", 500000.0, Color(0xFFEF4444), Icons.Default.Home)
            )
        )
    )

    val incomeCategories = listOf(
        IncomeCategory("Salary", 2800000.0, Color(0xFF10B981), Icons.Default.Work),
        IncomeCategory("Freelance", 300000.0, Color(0xFF3B82F6), Icons.Default.Computer),
        IncomeCategory("Investment", 80000.0, Color(0xFF8B5CF6), Icons.Default.TrendingUp),
        IncomeCategory("Other", 20000.0, Color(0xFFF59E0B), Icons.Default.Add)
    )

    val incomeExpenseData = listOf(
        IncomeExpenseData("Jan", 3200000.0, 2100000.0),
        IncomeExpenseData("Feb", 3200000.0, 1890000.0),
        IncomeExpenseData("Mar", 3200000.0, 2340000.0),
        IncomeExpenseData("Apr", 3500000.0, 2890000.0),
        IncomeExpenseData("May", 3500000.0, 2450000.0),
        IncomeExpenseData("Jun", 3200000.0, 2650000.0)
    )

    val weeklyData = listOf(
        IncomeExpenseData("W1", 800000.0, 520000.0),
        IncomeExpenseData("W2", 800000.0, 680000.0),
        IncomeExpenseData("W3", 800000.0, 590000.0),
        IncomeExpenseData("W4", 800000.0, 610000.0)
    )

    val monthlyBudgetData = listOf(
        BudgetData("Jan", 3300000.0, 2980000.0),
        BudgetData("Feb", 3300000.0, 3150000.0),
        BudgetData("Mar", 3300000.0, 3420000.0),
        BudgetData("Apr", 3300000.0, 3100000.0),
        BudgetData("May", 3300000.0, 2850000.0),
        BudgetData("Jun", 3300000.0, 2785000.0)
    )

    val weeklyBudgetData = listOf(
        BudgetData("W1", 825000.0, 696000.0),
        BudgetData("W2", 825000.0, 787000.0),
        BudgetData("W3", 825000.0, 652000.0),
        BudgetData("W4", 825000.0, 650000.0)
    )
}
