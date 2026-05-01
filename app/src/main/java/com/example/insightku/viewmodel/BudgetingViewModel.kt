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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetingUiState())
    val uiState = _uiState.asStateFlow()

    // BUG7 FIX: Track Job agar tidak ada multiple collectors bersamaan
    private var loadJob: Job? = null

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
            is BudgetingEvent.ShowRecurringBudgetsDialog -> _uiState.update { it.copy(dialogState = DialogState.ManageRecurring(emptyList())) }
            is BudgetingEvent.HideRecurringBudgetsDialog -> _uiState.update { it.copy(dialogState = DialogState.None) }

            // Data Operations
            is BudgetingEvent.AddCategory -> addCategory(event.category)
            is BudgetingEvent.UpdateCategory -> updateCategory(event.category)
            is BudgetingEvent.DeleteCategory -> deleteCategory(event.categoryId)
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
                    val budgetCategories = categories.filter { (it.budgetLimit ?: 0.0) > 0.0 }.map { category ->
                        val spentAmount = transactions
                            .filter { it.category == category.name && it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }
                        BudgetCategory(
                            id = category.id.toString(),
                            name = category.name,
                            budgetedAmount = category.budgetLimit ?: 0.0,
                            spentAmount = spentAmount,
                            color = category.color ?: "",
                            icon = category.icon ?: ""
                        )
                    }
                    Pair(budgetCategories, recurringBudgets)
                }.collect { (budgetCategories, recurringBudgets) ->
                    val totalBudget = budgetCategories.sumOf { it.budgetedAmount }
                    val totalSpent = budgetCategories.sumOf { it.spentAmount }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalBudget = totalBudget,
                            totalSpent = totalSpent,
                            budgetCategories = budgetCategories,
                            dialogState = DialogState.None
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
            } catch (e: CancellationException) {
                // ISSUE 1 FIX: Rethrow — jangan tampilkan ke user
                throw e
            } catch (e: Exception) {
                // Silent fail — Room cache masih bisa dipakai
            }
        }
    }

    private fun addCategory(category: Category) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                transactionRepository.insertCategory(category, userId)
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
                transactionRepository.updateCategory(category, userId)
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Gagal update kategori"
                _uiState.update { it.copy(error = errorMessage) }
                errorBus.send(errorMessage)
            }
        }
    }

    private fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                transactionRepository.deleteCategory(categoryId, userId)
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
}