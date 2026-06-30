package com.example.insightku.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.feature.home.domain.AddTransactionUseCase
import com.example.insightku.feature.home.domain.GetTransactionsUseCase
import com.example.insightku.feature.home.presentation.BudgetSpendingItem
import com.example.insightku.feature.home.presentation.DashboardEvent
import com.example.insightku.feature.home.presentation.DashboardUiState
import com.example.insightku.feature.home.presentation.ForecastPeriod
import com.example.insightku.feature.home.presentation.TransactionItem
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.feature.home.presentation.StreakMilestone
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.core.datastore.SessionManager
import com.example.insightku.core.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val draftRepository: DraftTransactionRepository,
    private val authRepository: AuthRepository,
    private val errorBus: ErrorBus,
    private val sessionManager: SessionManager,
    private val prefs: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    // Cache categories so mark-paid functions can resolve real category names
    private var cachedCategories: List<Category> = emptyList()



    private var loadJob: Job? = null
    private var userNameJob: Job? = null

    init {
        loadUserName()
        loadDashboardData()
        refreshData()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.LoadDashboardData -> loadDashboardData()
            DashboardEvent.RefreshData       -> refreshData()
            DashboardEvent.ClearError        -> _uiState.update { it.copy(error = null) }
            DashboardEvent.ToggleBalanceVisibility -> viewModelScope.launch {
                // Privacy is global + persisted: flip the app-wide hideAmounts setting. The hero reads
                // LocalHideAmounts, so Home and Budgeting stay in sync and the choice survives restart.
                prefs.setHideAmounts(!prefs.hideAmounts.first())
            }
            is DashboardEvent.ToggleForecastPeriod -> _uiState.update {
                it.copy(forecastPeriod = if (event.period == "week") ForecastPeriod.WEEKLY else ForecastPeriod.MONTHLY)
            }
            is DashboardEvent.AddTransaction      -> addTransaction(event.transaction)
            is DashboardEvent.MarkRecurringPaid   -> markRecurringPaid(event.budget)
            is DashboardEvent.MarkInstallmentPaid -> markInstallmentPaid(event.installment)
            is DashboardEvent.SetStreakGoal -> viewModelScope.launch {
                prefs.setStreakGoal(event.days)
                _uiState.update { it.copy(streakGoal = event.days) }
            }
            DashboardEvent.UseStreakRepair -> viewModelScope.launch {
                prefs.setRepairAvailable(false)
                _uiState.update { it.copy(repairAvailable = false, repairExpiryMs = 0L) }
                // Trigger a transaction log prompt � handled by UI
            }
            is DashboardEvent.DismissDraft -> viewModelScope.launch {
                draftRepository.dismiss(event.draftId)
            }
            is DashboardEvent.UndoDismissDraft -> viewModelScope.launch {
                draftRepository.restore(event.draftId)
            }
            is DashboardEvent.CommitDismissDraft -> viewModelScope.launch {
                draftRepository.purgeDismissed(event.draftId)
            }
        }
    }

    private fun loadUserName() {
        // BUG4 FIX: Cancel job lama sebelum launch baru
        userNameJob?.cancel()
        userNameJob = viewModelScope.launch {
            sessionManager.userName.collect { name ->
                _uiState.update { it.copy(userName = name.ifBlank { "User" }) }
            }
        }
    }

    private fun loadDashboardData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                combine(
                    getTransactionsUseCase(),
                    transactionRepository.getRecurringBudgets(),
                    transactionRepository.getAllInstallments(),
                    transactionRepository.getAllCategories(),
                    draftRepository.observePendingDrafts()
                ) { transactions, recurring, installments, categories, drafts ->
                    object {
                        val t = transactions
                        val r = recurring
                        val i = installments
                        val c = categories
                        val d = drafts
                    }
                }.collect { data ->
                    updateStateFromTransactions(data.t, data.r, data.i, data.c, data.d)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: "Gagal memuat data dashboard"
                _uiState.update { it.copy(isLoading = false, error = msg) }
                errorBus.send(msg)
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                getTransactionsUseCase.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Jika Room kosong (LoginUseCase pre-fetch juga gagal karena offline),
                // tampilkan error agar user tahu dan bisa retry.
                // Jika Room sudah ada data, ini adalah silent fail � data lama masih tampil.
                val hasData = _uiState.value.recentTransactions.isNotEmpty()
                if (!hasData) {
                    val msg = "Tidak dapat memuat data. Periksa koneksi internet."
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    // Tidak kirim ke errorBus agar tidak double-show (sudah ada di state)
                }
            }
        }
    }

    private fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            addTransactionUseCase(transaction)
                .onFailure { exception ->
                    val msg = exception.message ?: "Gagal menambah transaksi"
                    _uiState.update { it.copy(error = msg) }
                    errorBus.send(msg)
                }
        }
    }

    private fun markRecurringPaid(budget: com.example.insightku.core.data.model.RecurringBudget) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                // Resolve real category name from cache � fall back to budget.name only if no match
                val resolvedCategory = budget.categoryId?.let { id ->
                    cachedCategories.firstOrNull { it.id == id }?.name
                        ?: cachedCategories.firstOrNull { it.name.trim().lowercase() == id.trim().lowercase() }?.name
                } ?: budget.name

                val tx = Transaction(
                    title         = budget.name,
                    amount        = budget.amount,
                    category      = resolvedCategory,
                    type          = TransactionType.EXPENSE,
                    date          = System.currentTimeMillis(),
                    description   = "Recurring payment: ${budget.name}",
                    accountId     = budget.accountId ?: ""
                )
                transactionRepository.addTransaction(tx, userId)

                val cal = Calendar.getInstance().apply { timeInMillis = budget.nextDue }
                when (budget.frequency) {
                    com.example.insightku.core.data.model.BudgetFrequency.WEEKLY    -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    com.example.insightku.core.data.model.BudgetFrequency.BIWEEKLY  -> cal.add(Calendar.WEEK_OF_YEAR, 2)
                    com.example.insightku.core.data.model.BudgetFrequency.MONTHLY   -> cal.add(Calendar.MONTH, 1)
                    com.example.insightku.core.data.model.BudgetFrequency.QUARTERLY -> cal.add(Calendar.MONTH, 3)
                    com.example.insightku.core.data.model.BudgetFrequency.YEARLY    -> cal.add(Calendar.YEAR, 1)
                }
                val updated = budget.copy(
                    nextDue       = cal.timeInMillis,
                    lastProcessed = System.currentTimeMillis()
                )
                transactionRepository.updateRecurringBudget(updated, userId)
            } catch (e: Exception) {
                val msg = e.message ?: "Gagal menandai pembayaran"
                _uiState.update { it.copy(error = msg) }
                errorBus.send(msg)
            }
        }
    }

    private fun markInstallmentPaid(installment: com.example.insightku.core.data.model.Installment) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                // Resolve real category name from cache
                val resolvedCategory = installment.categoryId?.let { id ->
                    cachedCategories.firstOrNull { it.id == id }?.name
                        ?: cachedCategories.firstOrNull { it.name.trim().lowercase() == id.trim().lowercase() }?.name
                } ?: installment.name

                val tx = Transaction(
                    title         = installment.name,
                    amount        = installment.monthlyPayment,
                    category      = resolvedCategory,
                    type          = TransactionType.EXPENSE,
                    date          = System.currentTimeMillis(),
                    description   = "Installment payment: ${installment.name} (${installment.paidMonths + 1}/${installment.totalMonths})",
                    accountId     = installment.accountId ?: ""
                )
                transactionRepository.addTransaction(tx, userId)

                val cal = Calendar.getInstance().apply { timeInMillis = installment.nextDueDate }
                cal.add(Calendar.MONTH, 1)
                val newPaid = (installment.paidMonths + 1).coerceAtMost(installment.totalMonths)
                val updated = installment.copy(
                    paidMonths  = newPaid,
                    nextDueDate = cal.timeInMillis,
                    isActive    = newPaid < installment.totalMonths
                )
                transactionRepository.updateInstallment(updated, userId)
            } catch (e: Exception) {
                val msg = e.message ?: "Gagal menandai cicilan"
                _uiState.update { it.copy(error = msg) }
                errorBus.send(msg)
            }
        }
    }

    private suspend fun updateStateFromTransactions(
        transactions: List<Transaction>,
        recurring: List<com.example.insightku.core.data.model.RecurringBudget>,
        installments: List<com.example.insightku.core.data.model.Installment>,
        categories: List<Category> = emptyList(),
        pendingDrafts: List<com.example.insightku.core.data.model.DraftTransaction> = emptyList()
    ) {
        // All-time balance
        val totalBalance = transactions.sumOf {
            if (it.type == TransactionType.INCOME) it.amount else -it.amount
        }

        // Current-month range
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        val monthlyTx = transactions.filter { it.date in monthStart..monthEnd }

        val monthlyIncome   = monthlyTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val monthlyExpenses = monthlyTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // Build lookup map first � used by both recentTransactions and budgetCategorySpending
        val categoryMap = categories.associateBy { it.name.trim().lowercase() }
        cachedCategories = categories

        val recentTransactions = transactions
            .sortedByDescending { it.date }
            .take(5)
            .map { t ->
                val matchedCat = categoryMap[t.category.trim().lowercase()]
                TransactionItem(
                    id       = t.id,
                    title    = t.title,
                    category = t.category,
                    amount   = t.amount,
                    time     = TimeUtils.toShortRelativeTime(t.date),
                    isIncome = t.type == TransactionType.INCOME,
                    iconName = matchedCat?.icon ?: t.category,
                    colorHex = matchedCat?.color ?: ""
                )
            }

        // Budget spending per category (current month expenses)
        val budgetCategorySpending = monthlyTx
            .filter { it.type == TransactionType.EXPENSE && it.category.isNotBlank() }
            .groupBy { it.category }
            .map { (cat, txs) ->
                val matchedCategory = categoryMap[cat.trim().lowercase()]
                BudgetSpendingItem(
                    categoryName = cat,
                    // Use category.icon field for icon resolution � more accurate than name
                    iconName     = matchedCategory?.icon ?: cat,
                    colorHex     = matchedCategory?.color ?: "",
                    spent        = txs.sumOf { it.amount },
                    limit        = matchedCategory?.budgetLimit
                )
            }
            .sortedByDescending { it.spent }

        // -- Streak calculation -------------------------------------------------
        val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val trackedDayKeys: Set<String> = transactions.map { dayFmt.format(Date(it.date)) }.toSet()

        fun dayKey(cal: Calendar): String = dayFmt.format(cal.time)

        val today = Calendar.getInstance()
        val todayKey = dayKey(today)
        val hasTrackedToday = todayKey in trackedDayKeys

        // Current streak � walk backwards from today
        var streak = 0
        val check = today.clone() as Calendar
        if (!hasTrackedToday) check.add(Calendar.DAY_OF_YEAR, -1)
        while (dayKey(check) in trackedDayKeys) {
            streak++
            check.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Best streak � scan all tracked days
        val sortedDays = trackedDayKeys.sorted()
        var bestStreak = 0
        var runStreak = 0
        var prevCal: Calendar? = null
        for (key in sortedDays) {
            val cal = Calendar.getInstance().apply {
                time = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(key) ?: return@apply
            }
            if (prevCal == null) {
                runStreak = 1
            } else {
                val prev = prevCal.clone() as Calendar
                prev.add(Calendar.DAY_OF_YEAR, 1)
                runStreak = if (dayKey(prev) == key) runStreak + 1 else 1
            }
            if (runStreak > bestStreak) bestStreak = runStreak
            prevCal = cal
        }

        // Freeze / repair logic � read current prefs synchronously via first()
        val freezeCount    = prefs.freezeCount.first()
        val lastFreezeDate = prefs.lastFreezeDate.first()
        val isPerfect      = prefs.isPerfectStreak.first()
        val streakGoal     = prefs.streakGoal.first()
        val repairAvail    = prefs.repairAvailable.first()
        val repairExpiry   = prefs.repairExpiry.first()

        // Yesterday key � used to detect missed day
        val yesterday = today.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = dayKey(yesterday)

        // Auto-consume freeze if yesterday was missed and freeze available
        var updatedFreezeCount = freezeCount
        var updatedPerfect     = isPerfect
        var repairAvailable    = repairAvail
        var repairExpiryMs     = repairExpiry

        val missedYesterday = yesterdayKey !in trackedDayKeys && streak == 0 && !hasTrackedToday
        if (missedYesterday && updatedFreezeCount > 0 && lastFreezeDate != yesterdayKey) {
            // Consume one freeze to protect streak
            updatedFreezeCount--
            updatedPerfect = false
            viewModelScope.launch {
                prefs.updateFreezeCount(updatedFreezeCount)
                prefs.setPerfectStreak(false)
                prefs.setLastFreezeDate(yesterdayKey)
            }
        } else if (streak == 0 && !hasTrackedToday && !missedYesterday) {
            // Streak just broke � offer repair for 24h
            if (!repairAvailable) {
                repairExpiryMs  = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                repairAvailable = true
                viewModelScope.launch {
                    prefs.setRepairAvailable(true, repairExpiryMs)
                }
            }
        }

        // Expire repair if window passed
        if (repairAvailable && System.currentTimeMillis() > repairExpiryMs) {
            repairAvailable = false
            viewModelScope.launch { prefs.setRepairAvailable(false) }
        }

        // Perfect streak � true only if streak > 0 and no freeze ever used
        if (hasTrackedToday && streak > 0 && lastFreezeDate.isEmpty()) {
            updatedPerfect = true
            viewModelScope.launch { prefs.setPerfectStreak(true) }
        }

        // Award freeze at milestones (7, 30 days) � max 2
        if (hasTrackedToday && (streak == 7 || streak == 30) && updatedFreezeCount < 2) {
            updatedFreezeCount = (updatedFreezeCount + 1).coerceAtMost(2)
            viewModelScope.launch { prefs.updateFreezeCount(updatedFreezeCount) }
        }

        val milestone = StreakMilestone.forStreak(streak)
        val monthlySavings = monthlyIncome - monthlyExpenses
        val insightMessages = buildInsightMessages(monthlyIncome, monthlyExpenses, monthlySavings, streak)
        // -----------------------------------------------------------------------

        _uiState.update {
            it.copy(
                isLoading              = false,
                totalBalance           = totalBalance,
                monthlyIncome          = monthlyIncome,
                monthlyExpenses        = monthlyExpenses,
                monthlySavings         = monthlySavings,
                insightMessages        = insightMessages,
                recentTransactions     = recentTransactions,
                currentStreak          = streak,
                bestStreak             = bestStreak,
                hasTrackedToday        = hasTrackedToday,
                freezeCount            = updatedFreezeCount,
                isPerfectStreak        = updatedPerfect,
                streakGoal             = streakGoal,
                repairAvailable        = repairAvailable,
                repairExpiryMs         = repairExpiryMs,
                streakMilestone        = milestone,
                recurringBudgets       = recurring,
                installments           = installments,
                budgetCategorySpending = budgetCategorySpending,
                pendingDrafts          = pendingDrafts
            )
        }
    }

    private fun buildInsightMessages(
        income: Double,
        expenses: Double,
        savings: Double,
        streak: Int
    ): List<String> {
        val messages = mutableListOf<String>()
        val savingsRate = if (income > 0) savings / income else 0.0

        when {
            income == 0.0 -> messages.add("Start logging income to see your financial picture.")
            savingsRate >= 0.3 -> messages.add("You're saving ${(savingsRate * 100).toInt()}% of your income this month. Excellent discipline.")
            savingsRate >= 0.1 -> messages.add("Savings are on track at ${(savingsRate * 100).toInt()}% of income. Keep the momentum.")
            savings < 0 -> messages.add("Expenses exceeded income this month. Review your spending to get back on track.")
            else -> messages.add("You're building healthy spending habits this month.")
        }

        when {
            expenses > 0 && income > 0 && expenses / income < 0.7 ->
                messages.add("Recurring payments are under control � spending ratio looks healthy.")
            expenses > income ->
                messages.add("Consider reviewing recurring payments to reduce monthly outflow.")
        }

        when {
            streak >= 30 -> messages.add("$streak days of consistent tracking. Financial mastery in motion.")
            streak >= 7  -> messages.add("$streak-day tracking streak. Your habit is becoming automatic.")
            streak >= 3  -> messages.add("$streak days in � the habit is forming. Don't break the chain.")
            streak == 0  -> messages.add("Log your first transaction today to start building your streak.")
        }

        return messages.take(3)
    }
}


