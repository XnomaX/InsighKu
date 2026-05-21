package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.data.model.Category
import com.example.insightku.data.model.RecurringBudget
import com.example.insightku.data.model.TransactionType
import com.example.insightku.data.repository.AuthRepository
import com.example.insightku.data.repository.TransactionRepository
import com.example.insightku.ui.components.budgeting.BudgetCategory
import com.example.insightku.ui.components.budgeting.BudgetingEvent
import com.example.insightku.ui.components.budgeting.BudgetingUiState
import com.example.insightku.ui.components.budgeting.DialogState
import com.example.insightku.utils.ErrorBus
import com.example.insightku.utils.SessionManager
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
    private val authRepository: AuthRepository,
    private val errorBus: ErrorBus,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetingUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var isRefreshComplete = false

    private var cachedRecurringBudgets: List<RecurringBudget> = emptyList()

    init {
        loadBudgetData()
        // ISSUE 2 FIX: Trigger refresh dari Firestore saat VM pertama dibuat
        refreshData()
    }

    fun onEvent(event: BudgetingEvent) {
        // PERBAIKAN: onEvent tidak launch sendiri
        when (event) {
            is BudgetingEvent.LoadBudgetData -> loadBudgetData()
            is BudgetingEvent.RefreshData -> refreshData()
            is BudgetingEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is BudgetingEvent.ChangePeriod -> { /* Not implemented yet */ }

            // Dialogs
            is BudgetingEvent.ShowAddBudgetDialog -> _uiState.update { it.copy(dialogState = DialogState.AddBudget) }
            is BudgetingEvent.HideAddBudgetDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.ShowEditBudgetDialog -> _uiState.update { it.copy(dialogState = DialogState.EditBudget(event.category)) }
            is BudgetingEvent.HideEditBudgetDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.ShowDeleteConfirmDialog -> _uiState.update { it.copy(dialogState = DialogState.DeleteConfirm(event.category)) }
            is BudgetingEvent.HideDeleteConfirmDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }
            is BudgetingEvent.ShowRecurringBudgetsDialog -> _uiState.update { it.copy(dialogState = DialogState.ManageRecurring(cachedRecurringBudgets)) }
            is BudgetingEvent.HideRecurringBudgetsDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }

            // Data Operations
            is BudgetingEvent.AddCategory -> addCategory(event.category)
            is BudgetingEvent.UpdateCategory -> updateCategory(event.category)
            is BudgetingEvent.DeleteCategory -> {
                // Route through confirmation dialog
                val category = _uiState.value.budgetCategories.find { it.id == event.categoryId }
                if (category != null) {
                    _uiState.update { it.copy(dialogState = DialogState.DeleteConfirm(category)) }
                }
            }
            is BudgetingEvent.ConfirmDeleteCategory -> deleteCategory(event.categoryId, event.categoryName)
            is BudgetingEvent.AddRecurringBudget -> addRecurringBudget(event.budget)
            is BudgetingEvent.UpdateRecurringBudget -> updateRecurringBudget(event.budget)
            is BudgetingEvent.DeleteRecurringBudget -> deleteRecurringBudget(event.budget)
        }
    }

    private fun loadBudgetData() {
        // BUG7 FIX: Cancel job lama sebelum launch baru
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                combine(
                    transactionRepository.getAllCategories(),
                    transactionRepository.getAllTransactions(),
                    transactionRepository.getRecurringBudgets()
                ) { categories, transactions, recurringBudgets ->
                    val activeCategories = categories.filter { it.isActive }
                    val monthlyExpenses = transactions
                        .filter { it.type == TransactionType.EXPENSE && it.date in currentMonthRange() }

                    val knownNames = activeCategories.map { it.name.normalizedCategoryName() }.toSet()
                    val categoryRows = activeCategories.map { category ->
                        val spentAmount = monthlyExpenses
                            .filter { it.category.normalizedCategoryName() == category.name.normalizedCategoryName() }
                            .sumOf { it.amount }
                        BudgetCategory(
                            id = category.id.toString(),
                            name = category.name,
                            budgetedAmount = category.budgetLimit,
                            spentAmount = spentAmount,
                            color = category.color ?: "",
                            icon = category.icon ?: "",
                            recurringPeriod = category.recurringPeriod
                        )
                    }

                    // Only show transaction-only rows for non-blank, non-uncategorized names.
                    // "Uncategorized" string was written by old migration code and must be
                    // treated the same as blank — silently ignored, not surfaced as a card.
                    val transactionOnlyRows = monthlyExpenses
                        .filter {
                            it.category.normalizedCategoryName() !in knownNames
                                && it.category.isNotBlank()
                                && it.category.normalizedCategoryName() != "uncategorized"
                        }
                        .groupBy { it.category }
                        .map { (categoryName, categoryTransactions) ->
                            BudgetCategory(
                                id = "transaction-only-${categoryName.normalizedCategoryName()}",
                                name = categoryName,
                                budgetedAmount = null,
                                spentAmount = categoryTransactions.sumOf { it.amount },
                                color = defaultColorForCategory(categoryName),
                                icon = categoryName
                            )
                        }

                    val budgetCategories = (categoryRows + transactionOnlyRows)
                        .sortedWith(
                            compareByDescending<BudgetCategory> { it.isOverBudget }
                                .thenByDescending { it.hasLimit }
                                .thenBy { it.name.lowercase() }
                        )
                    Pair(budgetCategories, recurringBudgets)
                }.collect { (budgetCategories, recurringBudgets) ->
                    cachedRecurringBudgets = recurringBudgets

                    if (budgetCategories.isEmpty() && isRefreshComplete) {
                        val alreadySeeded = sessionManager.getHasSeededCategories()
                        if (!alreadySeeded) {
                            sessionManager.setHasSeededCategories(true)
                            seedDefaultCategories(userId)
                        }
                    }

                    val totalBudget = budgetCategories.filter { it.hasLimit }.sumOf { it.limitAmount }
                    val totalSpent = budgetCategories.sumOf { it.spentAmount }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalBudget = totalBudget,
                            totalSpent = totalSpent,
                            budgetCategories = budgetCategories,
                            recurringBudgets = recurringBudgets
                        )
                    }
                }
            } catch (e: CancellationException) {
                // ISSUE 1 FIX: Rethrow — terjadi normal saat navigasi back
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
            try {
                transactionRepository.refreshCategories(userId)
                transactionRepository.refreshTransactions(userId)
                transactionRepository.refreshRecurringBudgets(userId)
                // Clean up stale "Uncategorized" transaction strings from old migration code
                transactionRepository.cleanupUncategorizedTransactions(userId)
                // Delete any "transaction-only-" documents that were incorrectly written to Firestore
                transactionRepository.cleanupVirtualCategoryDocuments(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Silent fail — Room cache still usable
            } finally {
                isRefreshComplete = true
            }
        }
    }

    private fun addCategory(category: Category) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                transactionRepository.insertCategory(category, userId)
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
                    transactionRepository.insertCategory(realCategory, userId)
                    // Also rename the transactions so they match the new real category name
                    transactionRepository.moveTransactionsByCategory(
                        category.name, realCategory.name
                    )
                } else {
                    transactionRepository.updateCategory(category, userId)
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
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                if (categoryId.startsWith("transaction-only-")) {
                    // Virtual row — just blank out the category on all its transactions
                    transactionRepository.moveTransactionsToBlank(categoryName, userId)
                } else {
                    transactionRepository.deleteCategoryAndMigrateTransactions(categoryId, categoryName, userId)
                }
                _uiState.update { it.copy(dialogState = DialogState.None) }
            } catch (e: Exception) {
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
                transactionRepository.insertRecurringBudget(budget, userId)
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
                transactionRepository.updateRecurringBudget(budget, userId)
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
                transactionRepository.deleteRecurringBudget(budget.id.toString(), userId)
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal hapus budget berulang"
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private suspend fun seedDefaultCategories(userId: String) {
        defaultBudgetCategories().forEach { category ->
            try {
                transactionRepository.insertCategory(category, userId)
            } catch (e: Exception) {
                // Keep the screen usable from Room cache even if one remote write fails.
            }
        }
    }

    private fun defaultBudgetCategories(): List<Category> {
        return listOf(
            Category(name = "Food", color = "#F59E0B", icon = "Food & Drinks", budgetLimit = null),
            Category(name = "Transport", color = "#3B82F6", icon = "Transportation", budgetLimit = null),
            Category(name = "Bills", color = "#EF4444", icon = "Bills & Utilities", budgetLimit = null),
            Category(name = "Lifestyle", color = "#8B5CF6", icon = "Entertainment", budgetLimit = null),
            Category(name = "Health", color = "#10B981", icon = "Healthcare", budgetLimit = null),
            Category(name = "Shopping", color = "#EC4899", icon = "Shopping", budgetLimit = null)
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
