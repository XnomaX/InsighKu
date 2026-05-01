package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import com.example.insightku.domain.usecase.transaction.GetTransactionsUseCase
import com.example.insightku.ui.components.analytics.AnalyticsEvent
import com.example.insightku.ui.components.analytics.AnalyticsUiState
import com.example.insightku.ui.components.analytics.model.CategoryData
import com.example.insightku.ui.components.analytics.model.MonthlyData
import com.example.insightku.utils.CategoryUtils
import com.example.insightku.utils.ErrorBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * AnalyticsViewModel — refactored.
 *
 * MASALAH SEBELUMNYA:
 * 1. Inject RootViewModel + AuthRepository + TransactionRepository langsung di ViewModel
 *    → 3 dependency yang seharusnya dihandle UseCase
 * 2. Nested launch ganda seperti DashboardViewModel
 * 3. `loadInitialAnalytics()` dipanggil dari onEvent, tapi juga membuat launch sendiri
 *
 * SEKARANG:
 * - Inject GetTransactionsUseCase + ErrorBus saja
 * - Tidak ada nested launch
 * - Logic pemrosesan data (groupBy, map, dll.) tetap di ViewModel karena ini
 *   adalah transformasi data untuk UI, bukan business logic
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var monthlyDataMap: Map<String, MonthlyData> = emptyMap()

    // BUG5 FIX: Simpan referensi Job untuk mencegah multiple collectors
    private var loadJob: Job? = null

    init {
        loadAnalytics()
        // ISSUE 2 FIX: Trigger refresh dari Firestore saat VM pertama dibuat
        // agar data muncul bahkan setelah logout+login (Room sudah dikosongkan)
        refreshFromRemote()
    }

    fun onEvent(event: AnalyticsEvent) {
        // PERBAIKAN: onEvent tidak launch, langsung panggil fungsi
        when (event) {
            AnalyticsEvent.LoadAnalytics -> loadAnalytics()
            AnalyticsEvent.RefreshData -> refreshAnalytics()
            is AnalyticsEvent.SelectMonth -> selectMonth(event.month)
            is AnalyticsEvent.SelectExpenseCategory -> _uiState.update { it.copy(selectedExpenseCategory = event.category) }
            is AnalyticsEvent.SelectIncomeCategory -> _uiState.update { it.copy(selectedIncomeCategory = event.category) }
            AnalyticsEvent.NextMonth -> navigateMonth(1)
            AnalyticsEvent.PreviousMonth -> navigateMonth(-1)
            is AnalyticsEvent.ChangeTimePeriod -> { /* TODO */ }
            is AnalyticsEvent.ChangeBudgetPeriod -> { /* TODO */ }
        }
    }

    private fun loadAnalytics() {
        // Cancel job lama sebelum launch baru
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                getTransactionsUseCase().collect { transactions ->
                    allTransactions = transactions
                    processAndUpdateState()
                }
            } catch (e: CancellationException) {
                // ISSUE 1 FIX: Rethrow — ini terjadi normal saat navigasi ke/dari Analytics.
                // Jangan kirim ke ErrorBus (penyebab error "StandaloneCoroutine was cancelled").
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal memuat data analitik"
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun refreshFromRemote() {
        viewModelScope.launch {
            try {
                getTransactionsUseCase.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Silent fail — Room cache masih bisa dipakai
            }
        }
    }

    private fun refreshAnalytics() {
        viewModelScope.launch {
            try {
                getTransactionsUseCase.refresh()
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal refresh data analitik"
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun processAndUpdateState() {
        processTransactionsIntoMonthlyData()
        val availableMonths = monthlyDataMap.keys.sortedDescending()
        val selectedMonth = availableMonths.firstOrNull() ?: ""
        _uiState.update {
            it.copy(
                isLoading = false,
                availableMonths = availableMonths,
                selectedMonth = selectedMonth,
                currentMonthData = monthlyDataMap[selectedMonth]
            )
        }
    }

    private fun processTransactionsIntoMonthlyData() {
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        monthlyDataMap = allTransactions
            .groupBy { monthFormat.format(Date(it.date)) }
            .mapValues { (month, transactions) -> createMonthlyData(month, transactions) }
    }

    private fun createMonthlyData(month: String, transactions: List<Transaction>): MonthlyData {
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val expenseCategories = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (name, trans) ->
                CategoryData(
                    name,
                    trans.sumOf { it.amount },
                    CategoryUtils.getColorForCategoryName(name),
                    CategoryUtils.getIconForCategoryName(name)
                )
            }

        val incomeCategories = transactions
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .map { (name, trans) ->
                CategoryData(
                    name,
                    trans.sumOf { it.amount },
                    CategoryUtils.getColorForCategoryName(name),
                    CategoryUtils.getIconForCategoryName(name)
                )
            }

        return MonthlyData(month, income, expenses, expenseCategories, incomeCategories)
    }

    private fun selectMonth(month: String) {
        _uiState.update {
            it.copy(
                selectedMonth = month,
                currentMonthData = monthlyDataMap[month],
                selectedExpenseCategory = null,
                selectedIncomeCategory = null
            )
        }
    }

    private fun navigateMonth(offset: Int) {
        val currentState = _uiState.value
        val availableMonths = currentState.availableMonths
        if (availableMonths.isEmpty()) return

        val currentIndex = availableMonths.indexOf(currentState.selectedMonth)
        val newIndex = (currentIndex - offset).coerceIn(availableMonths.indices)
        if (currentIndex != newIndex) selectMonth(availableMonths[newIndex])
    }
}