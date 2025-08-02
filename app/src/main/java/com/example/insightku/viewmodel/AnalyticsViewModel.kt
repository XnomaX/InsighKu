/*
package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AnalyticsData(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val savings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val expenseCategories: List<ExpenseCategory> = emptyList(),
    val incomeCategories: List<IncomeCategory> = emptyList(),
    val monthlyTrend: List<MonthlyTrendData> = emptyList(),
    val selectedMonth: String = "2024-06"
)

data class MonthlyTrendData(
    val month: String,
    val income: Double,
    val expenses: Double
)

data class ExpenseCategory(
    val name: String,
    val amount: Double,
    val percentage: Double,
    val color: String,
    val iconName: String
)

data class IncomeCategory(
    val name: String,
    val amount: Double,
    val percentage: Double,
    val color: String,
    val iconName: String
)

class AnalyticsViewModel : ViewModel() {
    
    private val _analyticsData = MutableStateFlow(AnalyticsData())
    val analyticsData: StateFlow<AnalyticsData> = _analyticsData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedExpenseCategory = MutableStateFlow<String?>(null)
    val selectedExpenseCategory: StateFlow<String?> = _selectedExpenseCategory.asStateFlow()
    
    private val _selectedIncomeCategory = MutableStateFlow<String?>(null)
    val selectedIncomeCategory: StateFlow<String?> = _selectedIncomeCategory.asStateFlow()
    
    init {
        loadAnalyticsData()
    }
    
    fun loadAnalyticsData(month: String = "2024-06") {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Simulate API call or database query
                val expenseCategories = listOf(
                    ExpenseCategory(
                        name = "Food & Drinks",
                        amount = 850000.0,
                        percentage = 32.1,
                        color = "#F59E0B",
                        iconName = "restaurant"
                    ),
                    ExpenseCategory(
                        name = "Transportation", 
                        amount = 650000.0,
                        percentage = 24.5,
                        color = "#3B82F6",
                        iconName = "directions_car"
                    ),
                    ExpenseCategory(
                        name = "Entertainment",
                        amount = 420000.0,
                        percentage = 15.8,
                        color = "#8B5CF6", 
                        iconName = "sports_esports"
                    ),
                    ExpenseCategory(
                        name = "Shopping",
                        amount = 380000.0,
                        percentage = 14.3,
                        color = "#EC4899",
                        iconName = "shopping_bag"
                    ),
                    ExpenseCategory(
                        name = "Coffee",
                        amount = 280000.0,
                        percentage = 10.6,
                        color = "#92400E",
                        iconName = "local_cafe"
                    ),
                    ExpenseCategory(
                        name = "Housing",
                        amount = 1070000.0,
                        percentage = 2.7,
                        color = "#EF4444",
                        iconName = "home"
                    )
                )
                
                val incomeCategories = listOf(
                    IncomeCategory(
                        name = "Salary",
                        amount = 2800000.0,
                        percentage = 80.0,
                        color = "#10B981",
                        iconName = "work"
                    ),
                    IncomeCategory(
                        name = "Freelance",
                        amount = 400000.0,
                        percentage = 11.4,
                        color = "#06B6D4",
                        iconName = "computer"
                    ),
                    IncomeCategory(
                        name = "Investment",
                        amount = 200000.0,
                        percentage = 5.7,
                        color = "#8B5CF6",
                        iconName = "trending_up"
                    ),
                    IncomeCategory(
                        name = "Other",
                        amount = 100000.0,
                        percentage = 2.9,
                        color = "#F59E0B",
                        iconName = "account_balance_wallet"
                    )
                )
                
                val totalIncome = incomeCategories.sumOf { it.amount }
                val totalExpenses = expenseCategories.sumOf { it.amount }
                val savings = totalIncome - totalExpenses
                val savingsRate = (savings / totalIncome) * 100
                
                val monthlyTrend = listOf(
                    MonthlyTrendData("Jan", 3200000.0, 2100000.0),
                    MonthlyTrendData("Feb", 3200000.0, 1890000.0),
                    MonthlyTrendData("Mar", 3200000.0, 2340000.0),
                    MonthlyTrendData("Apr", 3500000.0, 2890000.0),
                    MonthlyTrendData("May", 3500000.0, 2450000.0),
                    MonthlyTrendData("Jun", totalIncome, totalExpenses)
                )
                
                _analyticsData.value = AnalyticsData(
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    savings = savings,
                    savingsRate = savingsRate,
                    expenseCategories = expenseCategories,
                    incomeCategories = incomeCategories,
                    monthlyTrend = monthlyTrend,
                    selectedMonth = month
                )
                
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectExpenseCategory(categoryName: String?) {
        _selectedExpenseCategory.value = if (_selectedExpenseCategory.value == categoryName) {
            null
        } else {
            categoryName
        }
    }
    
    fun selectIncomeCategory(categoryName: String?) {
        _selectedIncomeCategory.value = if (_selectedIncomeCategory.value == categoryName) {
            null
        } else {
            categoryName
        }
    }
    
    fun changeMonth(month: String) {
        loadAnalyticsData(month)
    }
    
    fun refreshData() {
        loadAnalyticsData(_analyticsData.value.selectedMonth)
    }
}*/
