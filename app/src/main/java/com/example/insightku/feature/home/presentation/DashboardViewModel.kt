package com.example.insightku.feature.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.core.data.repository.RecurringBudgetRepository
import com.example.insightku.core.data.repository.InstallmentRepository
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.model.Goal
import com.example.insightku.feature.home.domain.AddTransactionUseCase
import com.example.insightku.feature.home.domain.ApproveAllocationDraftUseCase
import com.example.insightku.feature.home.domain.BuildInsightMessagesUseCase
import com.example.insightku.feature.home.domain.CalculateStreakUseCase
import com.example.insightku.feature.home.domain.GetTransactionsUseCase
import com.example.insightku.feature.home.domain.MarkPaymentPaidUseCase
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.core.utils.normalizedCategoryName
import com.example.insightku.core.data.local.preferences.SessionManager
import com.example.insightku.core.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

/** Snapshot of non-transaction meta used by the single dashboard pipeline. */
private data class DashboardMeta(
    val userName: String,
    val goals: List<Goal>,
    val accounts: List<com.example.insightku.core.data.model.Account>
)

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

    // Streak cache — skip recompute when transaction list is unchanged
    private var lastStreakTxIds: Set<String>? = null
    private var cachedStreakResult: CalculateStreakUseCase.StreakResult? = null

    init {
        // Single pipeline — one combine, one uiState update per emission.
        loadDashboardData()
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
                // Invalidate streak cache — goal change can affect repair/freeze logic
                lastStreakTxIds = null
                _uiState.update { it.copy(streakGoal = event.days) }
            }
            DashboardEvent.UseStreakRepair -> viewModelScope.launch {
                val dayKey = _uiState.value.repairDayKey ?: return@launch
                prefs.addOverrideDay(dayKey)
                prefs.setRepairAvailable(false)
                lastStreakTxIds = null
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

    private fun loadDashboardData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Outer combine: meta (user/goals/accounts) × core (tx/recurring/installments/categories/drafts)
                val metaFlow = combine(
                    sessionManager.userName,
                    goalRepository.getActiveGoals(),
                    accountRepository.getAllAccounts()
                ) { userName, goals, accounts ->
                    DashboardMeta(
                        userName = userName.ifBlank { "User" },
                        goals = goals,
                        accounts = accounts
                    )
                }
                val coreFlow = combine(
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
                }
                combine(metaFlow, coreFlow) { meta, core -> meta to core }
                    .collect { (meta, core) ->
                        updateDashboardState(
                            transactions = core.t,
                            recurring = core.r,
                            installments = core.i,
                            categories = core.c,
                            pendingDrafts = core.d,
                            meta = meta
                        )
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

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                getTransactionsUseCase.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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
            when (val outcome = approveAllocationDraftUseCase.approve(draftId)) {
                is ApproveAllocationDraftUseCase.Outcome.DraftNotFound ->
                    _uiState.update { it.copy(error = "Draft not found") }
                is ApproveAllocationDraftUseCase.Outcome.InvalidDraft ->
                    _uiState.update { it.copy(error = "Invalid allocation draft data") }
                is ApproveAllocationDraftUseCase.Outcome.AccountMissing ->
                    _uiState.update { it.copy(error = "Source account no longer exists") }
                is ApproveAllocationDraftUseCase.Outcome.InsufficientBalance ->
                    _uiState.update { it.copy(error = "Insufficient balance in ${outcome.accountName}") }
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
                draftRepository.purgeDismissed(draftId)
                _uiState.update { it.copy(snackbarMessage = "Allocation rejected") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_reject_allocation)) }
            }
        }
    }

    private suspend fun updateDashboardState(
        transactions: List<Transaction>,
        recurring: List<com.example.insightku.core.data.model.RecurringBudget>,
        installments: List<com.example.insightku.core.data.model.Installment>,
        categories: List<Category> = emptyList(),
        pendingDrafts: List<com.example.insightku.core.data.model.DraftTransaction> = emptyList(),
        meta: DashboardMeta
    ) {
        // Heavy pure work off main (partition/group/streak)
        val computed = withContext(Dispatchers.Default) {
            computeDashboardSnapshot(transactions, categories, pendingDrafts)
        }

        // Apply streak pref side-effects after compute (DataStore writes on Main/IO)
        for (update in computed.streakResult.prefUpdates) {
            applyStreakPrefUpdate(update)
        }

        val sortedGoals = meta.goals.sortedWith(
            compareBy<Goal> { it.daysRemaining ?: Int.MAX_VALUE }
                .thenByDescending { it.progressPercent }
                .thenByDescending { it.updatedAt.toEpochMilli() }
        )
        val accountBalance = if (meta.accounts.isNotEmpty()) meta.accounts.sumOf { it.balance } else null
        // Single atomic uiState write — one recomposition for all dashboard data
        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                userName = meta.userName,
                totalBalance = accountBalance ?: it.totalBalance,
                totalAccountBalance = accountBalance ?: it.totalAccountBalance,
                accountCount = meta.accounts.size,
                monthlyIncome = computed.monthlyIncome,
                monthlyExpenses = computed.monthlyExpenses,
                monthlySavings = computed.monthlySavings,
                insightMessages = computed.insightMessages,
                recentTransactions = computed.recentTransactions,
                currentStreak = computed.streakResult.currentStreak,
                bestStreak = computed.streakResult.bestStreak,
                hasTrackedToday = computed.streakResult.hasTrackedToday,
                freezeCount = computed.streakResult.freezeCount,
                isPerfectStreak = computed.streakResult.isPerfectStreak,
                streakGoal = computed.streakResult.streakGoal,
                repairAvailable = computed.streakResult.repairAvailable,
                repairExpiryMs = computed.streakResult.repairExpiryMs,
                repairDayKey = computed.streakResult.repairDayKey,
                streakMilestone = computed.streakResult.streakMilestone,
                recurringBudgets = recurring,
                installments = installments,
                budgetCategorySpending = computed.budgetCategorySpending,
                pendingDrafts = pendingDrafts,
                previewGoals = sortedGoals.take(3),
                totalGoalCount = meta.goals.size,
                hasActiveGoals = meta.goals.isNotEmpty(),
                previewBudgets = computed.previewBudgets,
                totalBudgetCount = computed.totalBudgetCount,
                hasActiveBudgets = computed.previewBudgets.isNotEmpty()
            )
        }
    }

    /**
     * Pure computation for dashboard numbers. Runs on Dispatchers.Default.
     * Streak is cached by transaction-id set so non-tx emissions (drafts/categories) skip the 140ms calc.
     */
    private suspend fun computeDashboardSnapshot(
        transactions: List<Transaction>,
        categories: List<Category>,
        pendingDrafts: List<com.example.insightku.core.data.model.DraftTransaction>
    ): DashboardSnapshot {
        val (monthStart, monthEnd) = currentMonthRange()
        val monthlyTx = transactions.filter { it.date in monthStart..monthEnd }
        val (incomeTxs, expenseTxs) = monthlyTx.partition { it.type == TransactionType.INCOME }
        val monthlyIncome = incomeTxs.sumOf { it.amount }
        val monthlyExpenses = expenseTxs.sumOf { it.amount }
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
                    iconName = matchedCat?.icon ?: t.category.ifBlank { t.title },
                    colorHex = matchedCat?.color ?: "",
                    transactionType = t.type
                )
            }
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
        // Streak: cache by tx id set — skip when only drafts/categories/meta changed
        val txIds = transactions.mapTo(HashSet(transactions.size)) { it.id }
        val streakResult = if (txIds == lastStreakTxIds && cachedStreakResult != null) {
            cachedStreakResult!!
        } else {
            val result = calculateStreakUseCase(transactions)
            lastStreakTxIds = txIds
            cachedStreakResult = result
            result
        }
        val monthlySavings = monthlyIncome - monthlyExpenses
        val insightMessages = buildInsightMessagesUseCase(
            monthlyIncome, monthlyExpenses, monthlySavings, streakResult.currentStreak
        )

        val allBudgetItems = categories
            .filter { it.budgetLimit != null && it.budgetLimit > 0 && !it.isSystemCategory }
            .map { cat ->
                val existing = budgetCategorySpending.find {
                    it.categoryName.normalizedCategoryName() == cat.name.normalizedCategoryName()
                }
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

        return DashboardSnapshot(
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            monthlySavings = monthlySavings,
            insightMessages = insightMessages,
            recentTransactions = recentTransactions,
            budgetCategorySpending = budgetCategorySpending,
            previewBudgets = previewBudgets,
            totalBudgetCount = allBudgetItems.size,
            streakResult = streakResult,
            pendingDraftCount = pendingDrafts.size
        )
    }

    private data class DashboardSnapshot(
        val monthlyIncome: Double,
        val monthlyExpenses: Double,
        val monthlySavings: Double,
        val insightMessages: List<String>,
        val recentTransactions: List<TransactionItem>,
        val budgetCategorySpending: List<BudgetSpendingItem>,
        val previewBudgets: List<BudgetSpendingItem>,
        val totalBudgetCount: Int,
        val streakResult: CalculateStreakUseCase.StreakResult,
        val pendingDraftCount: Int
    )

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
