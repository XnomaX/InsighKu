package com.example.insightku.feature.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.data.local.preferences.SessionManager
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.core.data.repository.InstallmentRepository
import com.example.insightku.core.data.repository.RecurringBudgetRepository
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.core.utils.TimeUtils
import com.example.insightku.core.utils.normalizedCategoryName
import com.example.insightku.feature.home.domain.AddTransactionUseCase
import com.example.insightku.feature.home.domain.ApproveAllocationDraftUseCase
import com.example.insightku.feature.home.domain.BuildInsightMessagesUseCase
import com.example.insightku.feature.home.domain.CalculateStreakUseCase
import com.example.insightku.feature.home.domain.GetTransactionsUseCase
import com.example.insightku.feature.home.domain.MarkPaymentPaidUseCase
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.model.Goal
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private const val TAG = "DashboardViewModel"

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val buildInsightMessagesUseCase: BuildInsightMessagesUseCase,
    private val markPaymentPaidUseCase: MarkPaymentPaidUseCase,
    private val approveAllocationDraftUseCase: ApproveAllocationDraftUseCase,
    private val categoryRepository: CategoryRepository,
    private val recurringBudgetRepository: RecurringBudgetRepository,
    private val installmentRepository: InstallmentRepository,
    private val draftRepository: DraftTransactionRepository,
    private val errorBus: ErrorBus,
    private val sessionManager: SessionManager,
    private val prefs: UserPreferencesDataStore,
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var userNameJob: Job? = null

    init {
        loadUserName()
        loadDashboardData()
        loadGoalProgress()
        loadBudgetProgress()
        loadAccountBalances()
        refreshData()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.LoadDashboardData -> loadDashboardData()
            DashboardEvent.RefreshData -> refreshData()
            DashboardEvent.ClearError -> _uiState.update { it.copy(error = null) }
            DashboardEvent.ToggleBalanceVisibility -> viewModelScope.launch {
                prefs.setHideAmounts(!prefs.hideAmounts.first())
            }
            is DashboardEvent.ToggleForecastPeriod -> _uiState.update {
                it.copy(forecastPeriod = if (event.period == "week") ForecastPeriod.WEEKLY else ForecastPeriod.MONTHLY)
            }
            is DashboardEvent.AddTransaction -> addTransaction(event.transaction)
            is DashboardEvent.MarkRecurringPaid -> markRecurringPaid(event.budget)
            is DashboardEvent.MarkInstallmentPaid -> markInstallmentPaid(event.installment)
            is DashboardEvent.SetStreakGoal -> viewModelScope.launch {
                prefs.setStreakGoal(event.days)
                _uiState.update { it.copy(streakGoal = event.days) }
            }
            DashboardEvent.UseStreakRepair -> viewModelScope.launch {
                val dayKey = _uiState.value.repairDayKey ?: return@launch
                prefs.addOverrideDay(dayKey)
                prefs.setRepairAvailable(false)
                _uiState.update { it.copy(repairAvailable = false, repairExpiryMs = 0L, repairDayKey = null) }
                loadDashboardData()
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
            is DashboardEvent.ApproveAllocationDraft -> approveAllocationDraft(event.draftId)
            is DashboardEvent.RejectAllocationDraft -> rejectAllocationDraft(event.draftId)
            DashboardEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun loadUserName() {
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
                    recurringBudgetRepository.getAllRecurringBudgets(),
                    installmentRepository.getAllInstallments(),
                    categoryRepository.getAllCategories(),
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
                val msg = e.message ?: context.getString(R.string.error_load_dashboard)
                _uiState.update { it.copy(isLoading = false, error = msg) }
                errorBus.send(msg)
            }
        }
    }

    private fun loadGoalProgress() {
        viewModelScope.launch {
            goalRepository.getActiveGoals().collect { goals ->
                val sorted = goals.sortedWith(
                    compareBy<Goal> { it.daysRemaining ?: Int.MAX_VALUE }
                        .thenByDescending { it.progressPercent }
                        .thenByDescending { it.updatedAt.toEpochMilli() }
                )
                _uiState.update {
                    it.copy(
                        previewGoals = sorted.take(3),
                        totalGoalCount = goals.size,
                        hasActiveGoals = goals.isNotEmpty()
                    )
                }
            }
        }
    }

    private fun loadAccountBalances() {
        viewModelScope.launch {
            try {
                accountRepository.getAllAccounts().collect { accounts ->
                    if (accounts.isNotEmpty()) {
                        val accountBalance = accounts.sumOf { acc -> acc.balance }
                        _uiState.update {
                            it.copy(
                                totalBalance = accountBalance,
                                totalAccountBalance = accountBalance,
                                accountCount = accounts.size
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Silently keep the existing totalBalance on error
            }
        }
    }

    private fun loadBudgetProgress() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                val budgetCategories = categories.filter { it.budgetLimit != null && it.budgetLimit > 0 && !it.isSystemCategory }
                _uiState.update {
                    it.copy(
                        totalBudgetCount = budgetCategories.size,
                        hasActiveBudgets = budgetCategories.isNotEmpty()
                    )
                }
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
            } catch (_: Exception) {
                val hasData = _uiState.value.recentTransactions.isNotEmpty()
                if (!hasData) {
                    val msg = "Tidak dapat memuat data. Periksa koneksi internet."
                    _uiState.update { it.copy(isLoading = false, error = msg) }
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
            markPaymentPaidUseCase.markRecurringPaid(budget).onFailure { e ->
                val msg = e.message ?: context.getString(R.string.error_mark_payment)
                _uiState.update { it.copy(error = msg) }
                errorBus.send(msg)
            }
        }
    }

    private fun markInstallmentPaid(installment: com.example.insightku.core.data.model.Installment) {
        viewModelScope.launch {
            markPaymentPaidUseCase.markInstallmentPaid(installment).onFailure { e ->
                val msg = e.message ?: context.getString(R.string.error_mark_installment)
                _uiState.update { it.copy(error = msg) }
                errorBus.send(msg)
            }
        }
    }

    /** Called from MainScreen to fetch a draft by ID for deep-link / inbox tap. */
    suspend fun getDraftById(draftId: String): com.example.insightku.core.data.model.DraftTransaction? {
        return draftRepository.getById(draftId)
    }

    // ─── Auto-allocation draft handling ────────────────────────────────────

    private fun approveAllocationDraft(draftId: String) {
        viewModelScope.launch {
            android.util.Log.i(TAG, "[DraftApproved] Processing draft=$draftId")
            when (val outcome = approveAllocationDraftUseCase.approve(draftId)) {
                is ApproveAllocationDraftUseCase.Outcome.DraftNotFound ->
                    _uiState.update { it.copy(error = context.getString(R.string.error_draft_not_found)) }
                is ApproveAllocationDraftUseCase.Outcome.InvalidDraft ->
                    _uiState.update { it.copy(error = context.getString(R.string.error_invalid_allocation_draft)) }
                is ApproveAllocationDraftUseCase.Outcome.AccountMissing ->
                    _uiState.update { it.copy(error = context.getString(R.string.error_account_missing)) }
                is ApproveAllocationDraftUseCase.Outcome.InsufficientBalance ->
                    _uiState.update {
                        it.copy(
                            error = context.getString(
                                R.string.error_insufficient_alloc_balance,
                                outcome.accountName
                            )
                        )
                    }
                is ApproveAllocationDraftUseCase.Outcome.Allocated ->
                    _uiState.update { it.copy(snackbarMessage = "${NumberFormatter.formatCurrency(outcome.amount)} allocated to ${outcome.goalName ?: "goal"}") }
                is ApproveAllocationDraftUseCase.Outcome.Failed ->
                    _uiState.update { it.copy(error = outcome.message) }
            }
        }
    }

    private fun rejectAllocationDraft(draftId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.i(TAG, "[DraftRejected] Rejecting draft=$draftId")
                draftRepository.purgeDismissed(draftId)
                android.util.Log.d(TAG, "[DraftRejected] Draft deleted — no allocation, rule remains active")
                _uiState.update { it.copy(snackbarMessage = context.getString(R.string.snackbar_allocation_rejected)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(TAG, "[DraftRejected] EXCEPTION — ${e.message}", e)
                _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_reject_allocation)) }
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
        // Current-month range
        val (monthStart, monthEnd) = currentMonthRange()
        val monthlyTx = transactions.filter { it.date in monthStart..monthEnd }

        val monthlyIncome = monthlyTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val monthlyExpenses = monthlyTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // Build lookup map
        val categoryMap = categories.associateBy { it.name.normalizedCategoryName() }
        val recentTransactions = transactions
            .sortedByDescending { it.date }
            .take(5)
            .map { t ->
                val matchedCat = categoryMap[t.category.normalizedCategoryName()]
                TransactionItem(
                    id = t.id,
                    title = t.title,
                    category = t.category,
                    amount = t.amount,
                    time = TimeUtils.toShortRelativeTime(t.date),
                    isIncome = t.type == TransactionType.INCOME,
                    iconName = matchedCat?.icon ?: t.category,
                    colorHex = matchedCat?.color ?: "",
                    transactionType = t.type
                )
            }

        // Budget spending per category
        val budgetCategorySpending = monthlyTx
            .filter { it.type == TransactionType.EXPENSE && it.category.isNotBlank() }
            .groupBy { it.category }
            .map { (cat, txs) ->
                val matchedCategory = categoryMap[cat.normalizedCategoryName()]
                BudgetSpendingItem(
                    id = matchedCategory?.id ?: cat,
                    categoryName = cat,
                    iconName = matchedCategory?.icon ?: cat,
                    colorHex = matchedCategory?.color ?: "",
                    spent = txs.sumOf { it.amount },
                    limit = matchedCategory?.budgetLimit
                )
            }
            .sortedByDescending { it.spent }

        // -- Streak calculation (delegated to use case) --
        val streakResult = calculateStreakUseCase(transactions)

        // Apply preference updates from the use case
        for (update in streakResult.prefUpdates) {
            applyStreakPrefUpdate(update)
        }

        val monthlySavings = monthlyIncome - monthlyExpenses
        val insightMessages = buildInsightMessagesUseCase(monthlyIncome, monthlyExpenses, monthlySavings, streakResult.currentStreak)
        // -----------------------------------------------------------------------

        // Preserve the account-sourced totalBalance
        val currentTotalBalance = _uiState.value.totalBalance

        // Top 3 budgets by spent percentage
        val allBudgetItems = categories
            .filter { it.budgetLimit != null && it.budgetLimit > 0 && !it.isSystemCategory }
            .map { cat ->
                val existing = budgetCategorySpending.find { it.categoryName.normalizedCategoryName() == cat.name.normalizedCategoryName() }
                BudgetSpendingItem(
                    id = cat.id,
                    categoryName = cat.name,
                    iconName = cat.icon ?: cat.name,
                    colorHex = cat.color,
                    spent = existing?.spent ?: 0.0,
                    limit = cat.budgetLimit
                )
            }
        val previewBudgets = allBudgetItems
            .sortedByDescending { it.spent / (it.limit ?: 1.0) }
            .take(3)

        _uiState.update {
            it.copy(
                isLoading = false,
                totalBalance = currentTotalBalance,
                monthlyIncome = monthlyIncome,
                monthlyExpenses = monthlyExpenses,
                monthlySavings = monthlySavings,
                insightMessages = insightMessages,
                recentTransactions = recentTransactions,
                currentStreak = streakResult.currentStreak,
                bestStreak = streakResult.bestStreak,
                hasTrackedToday = streakResult.hasTrackedToday,
                freezeCount = streakResult.freezeCount,
                isPerfectStreak = streakResult.isPerfectStreak,
                streakGoal = streakResult.streakGoal,
                repairAvailable = streakResult.repairAvailable,
                repairExpiryMs = streakResult.repairExpiryMs,
                repairDayKey = streakResult.repairDayKey,
                streakMilestone = streakResult.streakMilestone,
                recurringBudgets = recurring,
                installments = installments,
                budgetCategorySpending = budgetCategorySpending,
                pendingDrafts = pendingDrafts,
                previewBudgets = previewBudgets,
                totalBudgetCount = allBudgetItems.size,
                hasActiveBudgets = previewBudgets.isNotEmpty()
            )
        }
    }

    /** Inclusive [start, end] epoch-millis range covering the current calendar month. */
    private fun currentMonthRange(): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        return start to end
    }

    /** Persist a single streak preference update computed by [CalculateStreakUseCase]. */
    private fun applyStreakPrefUpdate(update: CalculateStreakUseCase.PrefUpdate) {
        viewModelScope.launch {
            when (update) {
                is CalculateStreakUseCase.PrefUpdate.FreezeConsumed -> {
                    prefs.updateFreezeCount(update.newCount)
                    prefs.setPerfectStreak(false)
                    prefs.setLastFreezeDate(update.lastFreezeDate)
                }
                is CalculateStreakUseCase.PrefUpdate.OverrideDayAdded -> {
                    prefs.addOverrideDay(update.dayKey)
                }
                is CalculateStreakUseCase.PrefUpdate.RepairEnabled -> {
                    prefs.setRepairAvailable(true, update.expiryMs)
                }
                is CalculateStreakUseCase.PrefUpdate.RepairDisabled -> {
                    prefs.setRepairAvailable(false)
                }
                is CalculateStreakUseCase.PrefUpdate.PerfectStreakEnabled -> {
                    prefs.setPerfectStreak(true)
                }
                is CalculateStreakUseCase.PrefUpdate.FreezeAwarded -> {
                    prefs.updateFreezeCount(update.newCount)
                }
                is CalculateStreakUseCase.PrefUpdate.MilestoneAwarded -> {
                    prefs.addAwardedMilestone(update.milestone)
                }
            }
        }
    }
}
