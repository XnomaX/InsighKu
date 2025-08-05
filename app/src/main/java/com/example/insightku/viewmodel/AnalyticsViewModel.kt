package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.ui.components.analytics.*
import com.example.insightku.ui.components.analytics.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsState())
    val uiState: StateFlow<AnalyticsState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun onEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.LoadAnalytics -> loadAnalytics()
            is AnalyticsEvent.RefreshData -> loadAnalytics()
            is AnalyticsEvent.SelectMonth -> selectMonth(event.month)
            is AnalyticsEvent.ChangeTimePeriod -> changeTimePeriod(event.period)
            is AnalyticsEvent.ChangeBudgetPeriod -> changeBudgetPeriod(event.period)
            is AnalyticsEvent.SelectExpenseCategory -> selectExpenseCategory(event.category)
            is AnalyticsEvent.SelectIncomeCategory -> selectIncomeCategory(event.category)
            is AnalyticsEvent.NextMonth -> navigateToNextMonth()
            is AnalyticsEvent.PreviousMonth -> navigateToPreviousMonth()
        }
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Load monthly data
                val monthlyDataMap = getMonthlyDataMap()
                val availableMonths = monthlyDataMap.keys.sorted().reversed()
                val currentMonthData = monthlyDataMap[_uiState.value.selectedMonth]

                // Load budget and income/expense data based on current periods - use AnalyticsDataSource directly
                val budgetData = if (_uiState.value.budgetPeriod == TimePeriod.WEEKLY) {
                    AnalyticsDataSource.weeklyBudgetData
                } else {
                    AnalyticsDataSource.monthlyBudgetData
                }

                val incomeExpenseData = if (_uiState.value.timePeriod == TimePeriod.WEEKLY) {
                    getWeeklyIncomeExpenseData()
                } else {
                    getMonthlyIncomeExpenseData()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    availableMonths = availableMonths,
                    currentMonthData = currentMonthData,
                    budgetData = budgetData,
                    incomeExpenseData = incomeExpenseData
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load analytics: ${e.message}"
                )
            }
        }
    }

    private fun selectMonth(month: String) {
        viewModelScope.launch {
            try {
                val monthlyDataMap = getMonthlyDataMap()
                val currentMonthData = monthlyDataMap[month]

                _uiState.value = _uiState.value.copy(
                    selectedMonth = month,
                    currentMonthData = currentMonthData,
                    selectedExpenseCategory = null, // Reset category selection when changing month
                    selectedIncomeCategory = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load month data: ${e.message}"
                )
            }
        }
    }

    private fun changeTimePeriod(period: TimePeriod) {
        viewModelScope.launch {
            val incomeExpenseData = if (period == TimePeriod.WEEKLY) {
                getWeeklyIncomeExpenseData()
            } else {
                getMonthlyIncomeExpenseData()
            }

            _uiState.value = _uiState.value.copy(
                timePeriod = period,
                incomeExpenseData = incomeExpenseData
            )
        }
    }

    private fun changeBudgetPeriod(period: TimePeriod) {
        viewModelScope.launch {
            // Use data directly from AnalyticsDataSource instead of helper functions
            val budgetData = if (period == TimePeriod.WEEKLY) {
                AnalyticsDataSource.weeklyBudgetData
            } else {
                AnalyticsDataSource.monthlyBudgetData
            }

            _uiState.value = _uiState.value.copy(
                budgetPeriod = period,
                budgetData = budgetData
            )
        }
    }

    private fun selectExpenseCategory(category: String?) {
        _uiState.value = _uiState.value.copy(
            selectedExpenseCategory = category
        )
    }

    private fun selectIncomeCategory(category: String?) {
        _uiState.value = _uiState.value.copy(
            selectedIncomeCategory = category
        )
    }

    private fun navigateToNextMonth() {
        val currentIndex = _uiState.value.availableMonths.indexOf(_uiState.value.selectedMonth)
        if (currentIndex > 0) {
            val nextMonth = _uiState.value.availableMonths[currentIndex - 1]
            selectMonth(nextMonth)
        }
    }

    private fun navigateToPreviousMonth() {
        val currentIndex = _uiState.value.availableMonths.indexOf(_uiState.value.selectedMonth)
        if (currentIndex < _uiState.value.availableMonths.size - 1) {
            val previousMonth = _uiState.value.availableMonths[currentIndex + 1]
            selectMonth(previousMonth)
        }
    }
}
