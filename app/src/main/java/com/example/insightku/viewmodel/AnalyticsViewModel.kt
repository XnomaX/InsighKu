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
import com.example.insightku.ui.components.analytics.model.TimePeriod
import com.example.insightku.ui.components.analytics.model.BudgetData
import com.example.insightku.ui.components.analytics.model.IncomeExpenseData
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
            is AnalyticsEvent.ChangeTimePeriod -> changeTimePeriod(event.period)
            is AnalyticsEvent.ChangeBudgetPeriod -> changeBudgetPeriod(event.period)
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
        val currentSelected = _uiState.value.selectedMonth
        val selectedMonth = if (currentSelected in availableMonths) currentSelected
                            else availableMonths.firstOrNull() ?: ""
        val currentPeriod = _uiState.value.chartTimePeriod
        val currentBudgetPeriod = _uiState.value.budgetTimePeriod
        _uiState.update {
            it.copy(
                isLoading = false,
                availableMonths = availableMonths,
                selectedMonth = selectedMonth,
                currentMonthData = monthlyDataMap[selectedMonth]
            )
        }
        // Recompute chart data with current period settings
        recomputeIncomeExpenseData(currentPeriod)
        recomputeBudgetData(currentBudgetPeriod)
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

    private fun changeTimePeriod(period: TimePeriod) {
        _uiState.update { it.copy(chartTimePeriod = period) }
        recomputeIncomeExpenseData(period)
    }

    private fun changeBudgetPeriod(period: TimePeriod) {
        _uiState.update { it.copy(budgetTimePeriod = period) }
        recomputeBudgetData(period)
    }

    /**
     * Aggregates allTransactions into IncomeExpenseData buckets based on the selected period.
     * - WEEKLY  → last 4 weeks (W1..W4)
     * - MONTHLY → last 6 months (Jan..Jun style)
     * - YEARLY  → last 3 years
     */
    private fun recomputeIncomeExpenseData(period: TimePeriod) {
        val cal = Calendar.getInstance()
        val data: List<IncomeExpenseData> = when (period) {
            TimePeriod.WEEKLY -> {
                (3 downTo 0).map { weeksAgo ->
                    val weekStart = (cal.clone() as Calendar).apply {
                        add(Calendar.WEEK_OF_YEAR, -weeksAgo)
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val weekEnd = (weekStart.clone() as Calendar).apply {
                        add(Calendar.WEEK_OF_YEAR, 1)
                        add(Calendar.MILLISECOND, -1)
                    }
                    val label = "W${4 - weeksAgo}"
                    val txInRange = allTransactions.filter { it.date in weekStart.timeInMillis..weekEnd.timeInMillis }
                    IncomeExpenseData(
                        period = label,
                        income = txInRange.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        expenses = txInRange.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
            }
            TimePeriod.MONTHLY -> {
                val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
                (5 downTo 0).map { monthsAgo ->
                    val monthCal = (cal.clone() as Calendar).apply {
                        add(Calendar.MONTH, -monthsAgo)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val monthEnd = (monthCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1)
                    }
                    val label = monthFmt.format(monthCal.time)
                    val txInRange = allTransactions.filter { it.date in monthCal.timeInMillis..monthEnd.timeInMillis }
                    IncomeExpenseData(
                        period = label,
                        income = txInRange.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        expenses = txInRange.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
            }
            TimePeriod.YEARLY -> {
                val currentYear = cal.get(Calendar.YEAR)
                (2 downTo 0).map { yearsAgo ->
                    val year = currentYear - yearsAgo
                    val yearStart = Calendar.getInstance().apply {
                        set(year, Calendar.JANUARY, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val yearEnd = Calendar.getInstance().apply {
                        set(year, Calendar.DECEMBER, 31, 23, 59, 59); set(Calendar.MILLISECOND, 999)
                    }
                    val txInRange = allTransactions.filter { it.date in yearStart.timeInMillis..yearEnd.timeInMillis }
                    IncomeExpenseData(
                        period = year.toString(),
                        income = txInRange.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        expenses = txInRange.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
            }
        }
        _uiState.update { it.copy(incomeExpenseData = data) }
    }

    /**
     * Aggregates allTransactions into BudgetData buckets based on the selected period.
     * Budget target is derived from the average monthly spend across all data.
     */
    private fun recomputeBudgetData(period: TimePeriod) {
        val cal = Calendar.getInstance()
        // Use average monthly expense as a simple budget target baseline
        val avgMonthlyExpense = if (monthlyDataMap.isNotEmpty())
            monthlyDataMap.values.map { it.totalExpenses }.average()
        else 0.0

        val data: List<BudgetData> = when (period) {
            TimePeriod.WEEKLY -> {
                val weeklyTarget = avgMonthlyExpense / 4.0
                (3 downTo 0).map { weeksAgo ->
                    val weekStart = (cal.clone() as Calendar).apply {
                        add(Calendar.WEEK_OF_YEAR, -weeksAgo)
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val weekEnd = (weekStart.clone() as Calendar).apply {
                        add(Calendar.WEEK_OF_YEAR, 1); add(Calendar.MILLISECOND, -1)
                    }
                    val actual = allTransactions
                        .filter { it.type == TransactionType.EXPENSE && it.date in weekStart.timeInMillis..weekEnd.timeInMillis }
                        .sumOf { it.amount }
                    BudgetData(period = "W${4 - weeksAgo}", budget = weeklyTarget, actual = actual)
                }
            }
            TimePeriod.MONTHLY -> {
                val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
                (5 downTo 0).map { monthsAgo ->
                    val monthCal = (cal.clone() as Calendar).apply {
                        add(Calendar.MONTH, -monthsAgo)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val monthEnd = (monthCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1)
                    }
                    val label = monthFmt.format(monthCal.time)
                    val actual = allTransactions
                        .filter { it.type == TransactionType.EXPENSE && it.date in monthCal.timeInMillis..monthEnd.timeInMillis }
                        .sumOf { it.amount }
                    BudgetData(period = label, budget = avgMonthlyExpense, actual = actual)
                }
            }
            TimePeriod.YEARLY -> {
                val yearlyTarget = avgMonthlyExpense * 12
                val currentYear = cal.get(Calendar.YEAR)
                (2 downTo 0).map { yearsAgo ->
                    val year = currentYear - yearsAgo
                    val yearStart = Calendar.getInstance().apply {
                        set(year, Calendar.JANUARY, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val yearEnd = Calendar.getInstance().apply {
                        set(year, Calendar.DECEMBER, 31, 23, 59, 59); set(Calendar.MILLISECOND, 999)
                    }
                    val actual = allTransactions
                        .filter { it.type == TransactionType.EXPENSE && it.date in yearStart.timeInMillis..yearEnd.timeInMillis }
                        .sumOf { it.amount }
                    BudgetData(period = year.toString(), budget = yearlyTarget, actual = actual)
                }
            }
        }
        _uiState.update { it.copy(budgetData = data) }
    }
}