package com.example.insightku.feature.planning.budget.presentation

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.data.local.preferences.SessionManager
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.core.data.repository.InstallmentRepository
import com.example.insightku.core.data.repository.RecurringBudgetRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.core.utils.normalizedCategoryName
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.feature.planning.budget.domain.DefaultBudgetCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringBudgetRepository: RecurringBudgetRepository,
    private val installmentRepository: InstallmentRepository,
    private val authRepository: AuthRepository,
    private val errorBus: ErrorBus,
    private val sessionManager: SessionManager,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private data class BudgetSnapshot(
        val budgetCategories: List<BudgetCategory>,
        val incomeCategories: List<BudgetCategory>,
        val recurringBudgets: List<RecurringBudget>,
        val installments: List<Installment>,
        val rawCategories: List<Category>,
        val accounts: List<Account>
    )

    private val _uiState = MutableStateFlow(BudgetingUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var isRefreshComplete = false

    private var cachedRecurringBudgets: List<RecurringBudget> = emptyList()

    private val pendingDeleteIds = mutableSetOf<String>()
    private val pendingDeleteNames = mutableSetOf<String>()

    init {
        loadBudgetData()
    }

    fun onEvent(event: BudgetingEvent) {
        when (event) {
            is BudgetingEvent.LoadBudgetData -> loadBudgetData()
            is BudgetingEvent.RefreshData -> refreshData()
            is BudgetingEvent.ClearError -> _uiState.update { it.copy(error = null) }

            is BudgetingEvent.ShowAddBudgetDialog -> _uiState.update { it.copy(dialogState = DialogState.AddBudget(event.categoryType)) }
            is BudgetingEvent.HideAddBudgetDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.ShowEditBudgetDialog -> _uiState.update { it.copy(dialogState = DialogState.EditBudget(event.category)) }
            is BudgetingEvent.HideEditBudgetDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.ShowDeleteConfirmDialog -> _uiState.update { it.copy(dialogState = DialogState.DeleteConfirm(event.category)) }
            is BudgetingEvent.HideDeleteConfirmDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.DeleteCategory -> {
                val category = _uiState.value.budgetCategories.find { it.id == event.categoryId }
                if (category != null) _uiState.update { it.copy(dialogState = DialogState.DeleteConfirm(category)) }
            }
            is BudgetingEvent.ConfirmDeleteCategory -> deleteCategory(event.categoryId, event.categoryName)
            is BudgetingEvent.AddCategory -> addCategory(event.category)
            is BudgetingEvent.UpdateCategory -> updateCategory(event.category)

            is BudgetingEvent.ShowAddRecurringDialog -> _uiState.update { it.copy(dialogState = DialogState.AddRecurringPayment) }
            is BudgetingEvent.ShowEditRecurringDialog -> _uiState.update { it.copy(dialogState = DialogState.EditRecurringPayment(event.budget)) }
            is BudgetingEvent.HideRecurringDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.AddRecurringBudget -> addRecurringBudget(event.budget)
            is BudgetingEvent.UpdateRecurringBudget -> updateRecurringBudget(event.budget)
            is BudgetingEvent.DeleteRecurringBudget -> deleteRecurringBudget(event.budget)

            is BudgetingEvent.ShowAddInstallmentDialog -> _uiState.update { it.copy(dialogState = DialogState.AddInstallment) }
            is BudgetingEvent.ShowEditInstallmentDialog -> _uiState.update { it.copy(dialogState = DialogState.EditInstallment(event.installment)) }
            is BudgetingEvent.HideInstallmentDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.AddInstallment -> addInstallment(event.installment)
            is BudgetingEvent.UpdateInstallment -> updateInstallment(event.installment)
            is BudgetingEvent.DeleteInstallment -> deleteInstallment(event.installmentId)
        }
    }

    private fun loadBudgetData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val monthRange = currentMonthRange()
                combine(
                    categoryRepository.getAllCategories(),
                    transactionRepository.getTransactionsByDateRange(monthRange.first, monthRange.last),
                    recurringBudgetRepository.getAllRecurringBudgets(),
                    installmentRepository.getAllInstallments(),
                    accountRepository.getAllAccounts()
                ) { categories, transactions, recurringBudgets, installments, accounts ->
                    buildBudgetSnapshot(categories, transactions, recurringBudgets, installments, accounts)
                }.collect { snapshot ->
                    cachedRecurringBudgets = snapshot.recurringBudgets

                    // ── Count only repository-backed categories (exclude transaction-only rows)
                    //    so that Empty State is shown when the user has no actual budget categories.
                    val hasExpenseCategories = snapshot.budgetCategories.any { !it.id.startsWith("transaction-only-") }
                    val hasIncomeCategories  = snapshot.incomeCategories.isNotEmpty()

                    val totalBudget = snapshot.budgetCategories.filter { it.hasLimit }.sumOf { it.limitAmount }
                    val totalSpent  = snapshot.budgetCategories.sumOf { it.spentAmount }
                    _uiState.update {
                        it.copy(
                            isLoading           = false,
                            totalBudget         = totalBudget,
                            totalSpent          = totalSpent,
                            budgetCategories    = snapshot.budgetCategories,
                            incomeCategories    = snapshot.incomeCategories,
                            hasExpenseCategories = hasExpenseCategories,
                            hasIncomeCategories  = hasIncomeCategories,
                            recurringBudgets    = snapshot.recurringBudgets,
                            installments        = snapshot.installments,
                            rawCategories       = snapshot.rawCategories,
                            accounts            = snapshot.accounts
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_load_budget)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    /**
     * Aggregate the raw category/transaction streams into the budget + income rows shown on screen.
     * Extracted from [loadBudgetData] for readability; behavior is unchanged.
     */
    private fun buildBudgetSnapshot(
        categories: List<Category>,
        transactions: List<Transaction>,
        recurringBudgets: List<RecurringBudget>,
        installments: List<Installment>,
        accounts: List<Account>
    ): BudgetSnapshot {
        val activeCategories = categories.filter { it.isActive && it.id !in pendingDeleteIds && !it.isSystemCategory }
        val monthlyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val monthlyIncome = transactions.filter { it.type == TransactionType.INCOME }

        val expenseCategories = activeCategories.filter { it.type == CategoryType.EXPENSE }
        val incomeCategories = activeCategories.filter { it.type == CategoryType.INCOME }
        val knownExpenseNames = expenseCategories.map { it.name.normalizedCategoryName() }.toSet()

        val expenseCategoryRows = expenseCategories.map { category ->
            val spentAmount = monthlyExpenses
                .filter { it.category.normalizedCategoryName() == category.name.normalizedCategoryName() }
                .sumOf { it.amount }
            BudgetCategory(
                id = category.id,
                name = category.name,
                budgetedAmount = category.budgetLimit,
                spentAmount = spentAmount,
                color = category.color,
                icon = category.icon ?: "",
                recurringPeriod = category.recurringPeriod,
                isSystemCategory = category.isSystemCategory,
                categoryType = CategoryType.EXPENSE
            )
        }

        val transactionOnlyRows = monthlyExpenses
            .filter {
                it.category.normalizedCategoryName() !in knownExpenseNames
                    && it.category.isNotBlank()
                    && it.category.normalizedCategoryName() != "uncategorized"
                    && it.category.normalizedCategoryName() !in pendingDeleteNames
            }
            .groupBy { it.category }
            .map { (categoryName, txs) ->
                BudgetCategory(
                    id = "transaction-only-${categoryName.normalizedCategoryName()}",
                    name = categoryName,
                    budgetedAmount = null,
                    spentAmount = txs.sumOf { it.amount },
                    color = DefaultBudgetCategories.colorForCategory(categoryName),
                    icon = categoryName,
                    categoryType = CategoryType.EXPENSE
                )
            }

        val budgetCategories = (expenseCategoryRows + transactionOnlyRows)
            .sortedWith(
                compareByDescending<BudgetCategory> { it.isOverBudget }
                    .thenByDescending { it.hasLimit }
                    .thenBy { it.name.lowercase() }
            )

        val incomeCategoryRows = incomeCategories.map { category ->
            val earnedAmount = monthlyIncome
                .filter { it.category.normalizedCategoryName() == category.name.normalizedCategoryName() }
                .sumOf { it.amount }
            BudgetCategory(
                id = category.id,
                name = category.name,
                budgetedAmount = null,
                spentAmount = earnedAmount,
                color = category.color,
                icon = category.icon ?: "",
                recurringPeriod = null,
                isSystemCategory = category.isSystemCategory,
                categoryType = CategoryType.INCOME
            )
        }

        return BudgetSnapshot(budgetCategories, incomeCategoryRows, recurringBudgets, installments, categories, accounts)
    }

    private fun refreshData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                val alreadySeeded = sessionManager.getHasSeededCategories()
                val hasLocalCategories = categoryRepository.hasAnyCategories()

                if (!hasLocalCategories && !alreadySeeded) {
                    // Fresh install: fetch remote categories, then seed defaults if still empty
                    categoryRepository.refreshCategories(userId)
                    val stillEmpty = !categoryRepository.hasAnyCategories()
                    if (stillEmpty) {
                        sessionManager.setHasSeededCategories(true)
                        seedDefaultCategories(userId)
                    } else {
                        // Remote categories exist — mark as seeded so we never re-seed
                        sessionManager.setHasSeededCategories(true)
                    }
                }
                transactionRepository.refreshTransactions(userId)
                recurringBudgetRepository.refreshRecurringBudgets(userId)
                installmentRepository.refreshInstallments(userId)
                transactionRepository.cleanupUncategorizedTransactions(userId)
                categoryRepository.cleanupVirtualCategoryDocuments(userId)
                recurringBudgetRepository.cleanupInvalidRecurringBudgets()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Log error
            } finally {
                isRefreshComplete = true
            }
        }
    }

    private fun addCategory(category: Category) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                categoryRepository.insertCategory(category, userId)
                _uiState.update { it.copy(dialogState = DialogState.None) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_add_category)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun updateCategory(category: Category) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                if (category.id.startsWith("transaction-only-")) {
                    val realCategory = category.copy(id = java.util.UUID.randomUUID().toString())
                    categoryRepository.insertCategory(realCategory, userId)
                    transactionRepository.moveTransactionsByCategory(category.name, realCategory.name)
                } else {
                    categoryRepository.updateCategory(category, userId)
                }
                _uiState.update { it.copy(dialogState = DialogState.None) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_update_category)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun deleteCategory(categoryId: String, categoryName: String) {
        val category = _uiState.value.rawCategories.find { it.id == categoryId }
        if (category?.isSystemCategory == true) {
            _uiState.update { it.copy(dialogState = DialogState.None) }
            return
        }
        pendingDeleteIds.add(categoryId)
        pendingDeleteNames.add(categoryName.trim().lowercase())

        _uiState.update {
            it.copy(
                dialogState = DialogState.None,
                budgetCategories = it.budgetCategories.filter { c -> c.id != categoryId },
                incomeCategories = it.incomeCategories.filter { c -> c.id != categoryId }
            )
        }
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: run {
                pendingDeleteIds.remove(categoryId)
                pendingDeleteNames.remove(categoryName.trim().lowercase())
                return@launch
            }
            try {
                if (categoryId.startsWith("transaction-only-")) {
                    transactionRepository.moveTransactionsToBlank(categoryName, userId)
                } else {
                    categoryRepository.deleteCategoryAndMigrateTransactions(categoryId, categoryName, userId)
                }
                pendingDeleteIds.remove(categoryId)
                pendingDeleteNames.remove(categoryName.trim().lowercase())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pendingDeleteIds.remove(categoryId)
                pendingDeleteNames.remove(categoryName.trim().lowercase())
                val errorMessage = e.message ?: context.getString(R.string.error_delete_category)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun addRecurringBudget(budget: RecurringBudget) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                recurringBudgetRepository.insertRecurringBudget(budget, userId)
                _uiState.update { it.copy(dialogState = DialogState.None) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_add_recurring_budget)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun updateRecurringBudget(budget: RecurringBudget) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                recurringBudgetRepository.updateRecurringBudget(budget, userId)
                _uiState.update { it.copy(dialogState = DialogState.None) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_update_recurring_budget)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun deleteRecurringBudget(budget: RecurringBudget) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                recurringBudgetRepository.deleteRecurringBudget(budget.id.toString(), userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_delete_recurring_budget)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun addInstallment(installment: Installment) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                installmentRepository.insertInstallment(installment, userId)
                _uiState.update { it.copy(dialogState = DialogState.None) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_add_installment)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun updateInstallment(installment: Installment) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                installmentRepository.updateInstallment(installment, userId)
                _uiState.update { it.copy(dialogState = DialogState.None) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_update_installment)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun deleteInstallment(installmentId: String) {
        _uiState.update { state ->
            state.copy(
                dialogState = DialogState.None,
                installments = state.installments.filter { it.id != installmentId }
            )
        }
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                installmentRepository.deleteInstallment(installmentId, userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: context.getString(R.string.error_delete_installment)
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private suspend fun seedDefaultCategories(userId: String) {
        DefaultBudgetCategories.all.forEach { category ->
            try {
                categoryRepository.insertCategory(category, userId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { /* best-effort */
            }
        }
    }

    private fun currentMonthRange(): LongRange {
        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }
        return start.timeInMillis..end.timeInMillis
    }
}


// --- Budgeting MVI contract (Action.kt, Event.kt, State.kt) -------------------------------

/**
 * One-shot actions dispatched from Home/Dashboard to the Budgeting screen.
 * Consumed by the screen's LaunchedEffect, then cleared.
 */
sealed class BudgetingAction {
    data object OpenCreateGoal : BudgetingAction()
    data object NavigateToGoals : BudgetingAction()
    data class OpenCreateBudget(
        val categoryType: CategoryType = CategoryType.EXPENSE
    ) : BudgetingAction()
}

sealed class BudgetingEvent {
    // General
    object LoadBudgetData : BudgetingEvent()
    object RefreshData : BudgetingEvent()
    object ClearError : BudgetingEvent()

    // Category dialogs
    data class ShowAddBudgetDialog(val categoryType: CategoryType) : BudgetingEvent()
    object HideAddBudgetDialog : BudgetingEvent()
    data class AddCategory(val category: Category) : BudgetingEvent()
    data class ShowEditBudgetDialog(val category: BudgetCategory) : BudgetingEvent()
    object HideEditBudgetDialog : BudgetingEvent()
    data class UpdateCategory(val category: Category) : BudgetingEvent()
    data class ShowDeleteConfirmDialog(val category: BudgetCategory) : BudgetingEvent()
    object HideDeleteConfirmDialog : BudgetingEvent()
    data class ConfirmDeleteCategory(val categoryId: String, val categoryName: String) : BudgetingEvent()
    data class DeleteCategory(val categoryId: String) : BudgetingEvent()

    // Recurring payment dialogs
    object ShowAddRecurringDialog : BudgetingEvent()
    data class ShowEditRecurringDialog(val budget: RecurringBudget) : BudgetingEvent()
    object HideRecurringDialog : BudgetingEvent()
    data class AddRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()
    data class UpdateRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()
    data class DeleteRecurringBudget(val budget: RecurringBudget) : BudgetingEvent()

    // Installment dialogs
    object ShowAddInstallmentDialog : BudgetingEvent()
    data class ShowEditInstallmentDialog(val installment: Installment) : BudgetingEvent()
    object HideInstallmentDialog : BudgetingEvent()
    data class AddInstallment(val installment: Installment) : BudgetingEvent()
    data class UpdateInstallment(val installment: Installment) : BudgetingEvent()
    data class DeleteInstallment(val installmentId: String) : BudgetingEvent()
}

@Immutable
data class BudgetingUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val budgetCategories: List<BudgetCategory> = emptyList(),
    val incomeCategories: List<BudgetCategory> = emptyList(),
    val recurringBudgets: List<RecurringBudget> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val dialogState: DialogState = DialogState.None,
    val hasExpenseCategories: Boolean = false,
    val hasIncomeCategories: Boolean = false,
    val rawCategories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList()
) {
    val remainingBudget: Double get() = totalBudget - limitedSpent
    val limitedSpent: Double get() = budgetCategories.filter { it.hasLimit }.sumOf { it.spentAmount }
    val unlimitedSpent: Double get() = budgetCategories.filterNot { it.hasLimit }.sumOf { it.spentAmount }
    val budgetUtilizationPercentage: Double get() = if (totalBudget > 0) (limitedSpent / totalBudget) * 100 else 0.0
    val overBudgetCategories: List<BudgetCategory> get() = budgetCategories.filter { it.isOverBudget }
    val allCategoriesForPicker: List<Category> get() = rawCategories.filter { !it.isSystemCategory }
}

sealed class DialogState {
    data object None : DialogState()
    data class AddBudget(val categoryType: CategoryType) : DialogState()
    data class EditBudget(val category: BudgetCategory) : DialogState()
    data class DeleteConfirm(val category: BudgetCategory) : DialogState()
    data class ManageRecurring(val budgets: List<RecurringBudget>) : DialogState()
    data object AddRecurringPayment : DialogState()
    data class EditRecurringPayment(val budget: RecurringBudget) : DialogState()
    data object AddInstallment : DialogState()
    data class EditInstallment(val installment: Installment) : DialogState()
}

enum class BudgetHealth {
    Unlimited,
    Good,
    Warning,
    Over
}

@Immutable
data class BudgetCategory(
    val id: String,
    val name: String,
    val budgetedAmount: Double?,
    val spentAmount: Double,
    val color: String,
    val icon: String,
    val recurringPeriod: String? = null,
    val isSystemCategory: Boolean = false,
    val categoryType: CategoryType = CategoryType.EXPENSE
) {
    val hasLimit: Boolean get() = (budgetedAmount ?: 0.0) > 0.0
    val limitAmount: Double get() = budgetedAmount ?: 0.0
    val remainingAmount: Double get() = if (hasLimit) limitAmount - spentAmount else 0.0
    val utilizationPercentage: Double get() = if (hasLimit) (spentAmount / limitAmount) * 100 else 0.0
    val progressFraction: Float get() = (utilizationPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
    val isOverBudget: Boolean get() = hasLimit && spentAmount > limitAmount
    val health: BudgetHealth
        get() = when {
            !hasLimit -> BudgetHealth.Unlimited
            utilizationPercentage < 70.0 -> BudgetHealth.Good
            utilizationPercentage <= 100.0 -> BudgetHealth.Warning
            else -> BudgetHealth.Over
        }
}
