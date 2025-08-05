package com.example.insightku.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.ui.components.dashboard.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    var uiState by mutableStateOf(DashboardState())
        private set

    fun onEvent(event: DashboardEvent) {
        when(event) {
            DashboardEvent.LoadDashboardData -> loadDashboardData()
            DashboardEvent.RefreshData -> loadDashboardData()
            DashboardEvent.ClearError -> clearError()
            DashboardEvent.AddTransaction -> { /* Handle add transaction navigation */ }
            is DashboardEvent.ToggleForecastPeriod -> { /* Handle forecast period toggle */ }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, error = null)
                delay(1000) // Simulate network delay

                // Mock data for AI Forecast
                val weeklyForecastData = (0..6).map { Random.nextInt(50, 200).toFloat() }
                val monthlyForecastData = (0..11).map { Random.nextInt(1500, 2500).toFloat() }

                // Mock data for Recent Transactions
                val mockRecentTransactions = listOf(
                    TransactionItem(
                        id = "1",
                        title = "Starbucks Coffee",
                        category = "Food & Drinks",
                        amount = 4.50,
                        time = "2:30 PM",
                        isIncome = false,
                        iconName = "local_cafe",
                        colorHex = "#F59E0B"
                    ),
                    TransactionItem(
                        id = "2",
                        title = "Uber Ride",
                        category = "Transportation",
                        amount = 12.80,
                        time = "1:15 PM",
                        isIncome = false,
                        iconName = "directions_car",
                        colorHex = "#3B82F6"
                    ),
                    TransactionItem(
                        id = "3",
                        title = "Salary Deposit",
                        category = "Income",
                        amount = 3200.00,
                        time = "9:00 AM",
                        isIncome = true,
                        iconName = "trending_up",
                        colorHex = "#10B981"
                    )
                )

                uiState = uiState.copy(
                    isLoading = false,
                    totalBalance = 4256.80,
                    monthlyIncome = 3200.0,
                    monthlyExpenses = 2650.0,
                    weeklyForecastData = weeklyForecastData,
                    monthlyForecastData = monthlyForecastData,
                    aiInsightMessage = "💡 AI Insight: You're likely to spend $625 this week, which is 12% less than last week!",
                    recentTransactions = mockRecentTransactions,
                    currentStreak = 5,
                    hasTrackedToday = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }

    private fun clearError() {
        uiState = uiState.copy(error = null)
    }
}
