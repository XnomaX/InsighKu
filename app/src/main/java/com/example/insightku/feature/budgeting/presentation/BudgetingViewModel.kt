package com.example.insightku.feature.budgeting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.core.data.repository.RecurringBudgetRepository
import com.example.insightku.core.data.repository.InstallmentRepository

import com.example.insightku.feature.budgeting.presentation.BudgetCategory
import com.example.insightku.feature.budgeting.presentation.BudgetingEvent
import com.example.insightku.feature.budgeting.presentation.BudgetingUiState
import com.example.insightku.feature.budgeting.presentation.DialogState
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.core.datastore.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * BudgetingViewModel — refactored.
 *
 * PERUBAHAN:
 * 1. Hapus inject RootViewModel → inject ErrorBus
 * 2. Perbaiki nested launch:
 *    SEBELUMNYA: onEvent launch → panggil loadBudgetData() → loadBudgetData launch lagi
 *    SEKARANG: onEvent tidak launch, langsung panggil fungsi yang launch sendiri
 * 3. ShowRecurringBudgetsDialog masih menggunakan emptyList() karena data sebenarnya
 *    sudah di-observe via combine() di loadBudgetData(). Ini akan diperbaiki di iterasi berikutnya.
 *
 * Note: BudgetingViewModel masih inject TransactionRepository dan AuthRepository secara langsung
 * karena operasi budgeting (kategori + recurring budget) belum memiliki UseCase sendiri.
 * Ini adalah hutang teknis yang bisa di-extract menjadi ManageBudgetUseCase di iterasi berikutnya.
 */
@HiltViewModel
class BudgetingViewModel @Inject constructor(
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

    // IDs that have been deleted optimistically — filtered out of every combine() emission
    // until Room confirms the delete by no longer including them in getAllCategories().
    private val pendingDeleteIds = mutableSetOf<String>()

    // Names of categories being deleted — used to suppress transactionOnlyRows
    // that would otherwise reappear while transactions are still being blanked.
    private val pendingDeleteNames = mutableSetOf<String>()

    init {
        loadBudgetData()
        // ISSUE 2 FIX: Trigger refresh dari Firestore saat VM pertama dibuat
        refreshData()
    }

    fun onEvent(event: BudgetingEvent) {
        when (event) {
            is BudgetingEvent.LoadBudgetData -> loadBudgetData()
            is BudgetingEvent.RefreshData -> refreshData()
            is BudgetingEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is BudgetingEvent.ChangePeriod -> { /* Not implemented yet */ }

            // Category dialogs
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

            // Recurring dialogs
            is BudgetingEvent.ShowAddRecurringDialog -> _uiState.update { it.copy(dialogState = DialogState.AddRecurringPayment) }
            is BudgetingEvent.ShowEditRecurringDialog -> _uiState.update { it.copy(dialogState = DialogState.EditRecurringPayment(event.budget)) }
            is BudgetingEvent.HideRecurringDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.ShowRecurringBudgetsDialog -> _uiState.update { it.copy(dialogState = DialogState.AddRecurringPayment) }
            is BudgetingEvent.HideRecurringBudgetsDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.AddRecurringBudget -> addRecurringBudget(event.budget)
            is BudgetingEvent.UpdateRecurringBudget -> updateRecurringBudget(event.budget)
            is BudgetingEvent.DeleteRecurringBudget -> deleteRecurringBudget(event.budget)

            // Installment dialogs
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
                    val activeCategories = categories.filter { it.isActive && it.id !in pendingDeleteIds && !it.isSystemCategory }
                    val monthlyExpenses = transactions
                        .filter { it.type == TransactionType.EXPENSE }
                    val monthlyIncome = transactions
                        .filter { it.type == TransactionType.INCOME }

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
                                color = defaultColorForCategory(categoryName),
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

                    BudgetSnapshot(budgetCategories, incomeCategoryRows, recurringBudgets, installments, categories, accounts)
                }.collect { snapshot ->
                    cachedRecurringBudgets = snapshot.recurringBudgets

                    val allEmpty = snapshot.budgetCategories.isEmpty() && snapshot.incomeCategories.isEmpty()
                    if (allEmpty && isRefreshComplete) {
                        val alreadySeeded = sessionManager.getHasSeededCategories()
                        if (!alreadySeeded) {
                            sessionManager.setHasSeededCategories(true)
                            seedDefaultCategories(userId)
                        }
                    }

                    val totalBudget = snapshot.budgetCategories.filter { it.hasLimit }.sumOf { it.limitAmount }
                    val totalSpent  = snapshot.budgetCategories.sumOf { it.spentAmount }
                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            totalBudget      = totalBudget,
                            totalSpent       = totalSpent,
                            budgetCategories = snapshot.budgetCategories,
                            incomeCategories = snapshot.incomeCategories,
                            recurringBudgets = snapshot.recurringBudgets,
                            installments     = snapshot.installments,
                            rawCategories    = snapshot.rawCategories,
                            accounts        = snapshot.accounts
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal memuat data budget"
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            android.util.Log.d("InsightKu_Recurring", "=== BudgetingViewModel.refreshData() userId=$userId ===")
            try {
                val hasLocalCategories = categoryRepository.hasAnyCategories()
                android.util.Log.d("InsightKu_Recurring", "hasLocalCategories=$hasLocalCategories")
                if (!hasLocalCategories) {
                    categoryRepository.refreshCategories(userId)
                }
                transactionRepository.refreshTransactions(userId)

                recurringBudgetRepository.refreshRecurringBudgets(userId)
                installmentRepository.refreshInstallments(userId)

                transactionRepository.cleanupUncategorizedTransactions(userId)
                categoryRepository.cleanupVirtualCategoryDocuments(userId)
                recurringBudgetRepository.cleanupInvalidRecurringBudgets()
                android.util.Log.d("InsightKu_Recurring", "=== refreshData() complete ===")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("InsightKu_Recurring", "refreshData error: ${e.message}", e)
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
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal menambah kategori"
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
                    // Virtual row — create a real Category with a proper UUID.
                    // Never write the "transaction-only-" id to Room or Firestore.
                    val realCategory = category.copy(id = java.util.UUID.randomUUID().toString())
                    categoryRepository.insertCategory(realCategory, userId)
                    // Also rename the transactions so they match the new real category name
                    transactionRepository.moveTransactionsByCategory(
                        category.name, realCategory.name
                    )
                } else {
                    categoryRepository.updateCategory(category, userId)
                }
                _uiState.update { it.copy(dialogState = DialogState.None) }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal update kategori"
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun deleteCategory(categoryId: String, categoryName: String) {
        val category = _uiState.value.rawCategories.find { it.id == categoryId }
        if (category?.isSystemCategory == true) {
            android.util.Log.w("InsightKu_Delete", "Blocked attempt to delete protected category: id=$categoryId")
            _uiState.update { it.copy(dialogState = DialogState.None) }
            return
        }
        // Mark as pending-delete so combine() emissions don't restore it
        pendingDeleteIds.add(categoryId)
        pendingDeleteNames.add(categoryName.trim().lowercase())

        // Optimistic update: remove from UI immediately before async ops complete
        _uiState.update {
            it.copy(
                dialogState = DialogState.None,
                budgetCategories = it.budgetCategories.filter { c -> c.id != categoryId },
                incomeCategories = it.incomeCategories.filter { c -> c.id != categoryId }
            )
        }
        viewModelScope.launch {
            android.util.Log.d("InsightKu_Delete", "deleteCategory called: id=$categoryId name=$categoryName")
            val userId = authRepository.getCurrentUserId() ?: run {
                android.util.Log.d("InsightKu_Delete", "No userId — aborting")
                pendingDeleteIds.remove(categoryId)
                pendingDeleteNames.remove(categoryName.trim().lowercase())
                return@launch
            }
            try {
                android.util.Log.d("InsightKu_Delete", "Proceeding with delete: id=$categoryId")
                if (categoryId.startsWith("transaction-only-")) {
                    transactionRepository.moveTransactionsToBlank(categoryName, userId)
                } else {
                    categoryRepository.deleteCategoryAndMigrateTransactions(categoryId, categoryName, userId)
                }
                android.util.Log.d("InsightKu_Delete", "Delete SUCCESS: id=$categoryId")
                // Room no longer emits this id — safe to remove from pending set
                pendingDeleteIds.remove(categoryId)
                pendingDeleteNames.remove(categoryName.trim().lowercase())
            } catch (e: Exception) {
                android.util.Log.e("InsightKu_Delete", "Delete FAILED: ${e.message}", e)
                pendingDeleteIds.remove(categoryId)
                pendingDeleteNames.remove(categoryName.trim().lowercase())
                val errorMessage = e.message ?: "Gagal hapus kategori"
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
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal menambah budget berulang"
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
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal update budget berulang"
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
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal hapus budget berulang"
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
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal menambah cicilan"
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
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal update cicilan"
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
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal hapus cicilan"
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private suspend fun seedDefaultCategories(userId: String) {
        val systemCategories = listOf(
            Category(
                id = "system-uncategorized-expense",
                name = "Uncategorized",
                color = "#79747E",
                icon = "Others",
                categoryType = CategoryType.EXPENSE.name,
                isSystemCategory = true
            ),
            Category(
                id = "system-uncategorized-income",
                name = "Uncategorized Income",
                color = "#79747E",
                icon = "Others",
                categoryType = CategoryType.INCOME.name,
                isSystemCategory = true
            )
        )
        val userCategories = defaultBudgetCategories()
        (systemCategories + userCategories).forEach { category ->
            try {
                categoryRepository.insertCategory(category, userId)
            } catch (e: Exception) {
                // Keep screen usable even if one remote write fails
            }
        }
    }

    private fun defaultBudgetCategories(): List<Category> {
        return listOf(
            // Expense categories
            Category(name = "Food", color = "#F59E0B", icon = "Food & Drinks", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
            Category(name = "Transport", color = "#3B82F6", icon = "Transportation", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
            Category(name = "Bills", color = "#EF4444", icon = "Utilities", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
            Category(name = "Shopping", color = "#EC4899", icon = "Shopping", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
            Category(name = "Health", color = "#10B981", icon = "Healthcare", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
            Category(name = "Entertainment", color = "#8B5CF6", icon = "Entertainment", budgetLimit = null, categoryType = CategoryType.EXPENSE.name),
            // Income categories
            Category(name = "Salary", color = "#10B981", icon = "Investment", budgetLimit = null, categoryType = CategoryType.INCOME.name),
            Category(name = "Freelance", color = "#06B6D4", icon = "Investment", budgetLimit = null, categoryType = CategoryType.INCOME.name),
            Category(name = "Business", color = "#F59E0B", icon = "Investment", budgetLimit = null, categoryType = CategoryType.INCOME.name),
            Category(name = "Investment", color = "#3B82F6", icon = "Investment", budgetLimit = null, categoryType = CategoryType.INCOME.name),
        )
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

    private fun String.normalizedCategoryName(): String = trim().lowercase()

    private fun defaultColorForCategory(categoryName: String): String {
        return when {
            categoryName.normalizedCategoryName().contains("food") -> "#F59E0B"
            categoryName.normalizedCategoryName().contains("transport") -> "#3B82F6"
            categoryName.normalizedCategoryName().contains("bill") -> "#EF4444"
            categoryName.normalizedCategoryName().contains("shop") -> "#EC4899"
            categoryName.normalizedCategoryName().contains("health") -> "#10B981"
            else -> "#79747E"
        }
    }
}




