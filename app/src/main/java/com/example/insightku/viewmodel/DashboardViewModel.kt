package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.data.model.RecurringBudget
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.repository.TransactionRepository
import com.example.insightku.data.local.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val balance: Double = 0.0,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val balanceVisible: Boolean = true,
    val recentTransactions: List<Transaction> = emptyList(),
    val recurringBudgets: List<RecurringBudget> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalDays: Int = 0,
    val streakData: List<Boolean> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Combine all data flows
                combine(
                    transactionRepository.getAllTransactions(),
                    transactionRepository.getRecurringBudgets(),
                    userPreferencesDataStore.currentStreak,
                    userPreferencesDataStore.bestStreak,
                    userPreferencesDataStore.totalDays
                ) { transactions, recurringBudgets, currentStreak, bestStreak, totalDays ->
                    
                    val balance = transactions.sumOf { 
                        if (it.type == com.example.insightku.data.model.TransactionType.INCOME) it.amount else -it.amount
                    }
                    val income = transactions.filter { 
                        it.type == com.example.insightku.data.model.TransactionType.INCOME
                    }.sumOf { it.amount }
                    val expenses = transactions.filter { 
                        it.type == com.example.insightku.data.model.TransactionType.EXPENSE
                    }.sumOf { it.amount }
                    
                    val streakData = generateStreakData(currentStreak)
                    
                    DashboardUiState(
                        balance = balance,
                        income = income,
                        expenses = expenses,
                        recentTransactions = transactions.take(10),
                        recurringBudgets = recurringBudgets,
                        currentStreak = currentStreak,
                        bestStreak = bestStreak,
                        totalDays = totalDays,
                        streakData = streakData,
                        isLoading = false
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun toggleBalanceVisibility() {
        _uiState.value = _uiState.value.copy(
            balanceVisible = !_uiState.value.balanceVisible
        )
    }

    fun refreshData() {
        loadDashboardData()
    }

    private fun generateStreakData(currentStreak: Int): List<Boolean> {
        // Generate 7 days of streak data for calendar
        val today = java.util.Calendar.getInstance()
        val streakData = mutableListOf<Boolean>()
        
        for (i in 6 downTo 0) {
            val date = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -i)
            }
            // Mark as completed if within current streak
            streakData.add(i < currentStreak)
        }
        
        return streakData
    }

    fun onTransactionAdded() {
        viewModelScope.launch {
            // Update streak when transaction is added
            val today = System.currentTimeMillis()
            userPreferencesDataStore.updateLastTransactionDate(today)
            
            // Increment streak logic would go here
            val newStreak = _uiState.value.currentStreak + 1
            userPreferencesDataStore.updateCurrentStreak(newStreak)
            
            if (newStreak > _uiState.value.bestStreak) {
                userPreferencesDataStore.updateBestStreak(newStreak)
            }
            
            refreshData()
        }
    }
}