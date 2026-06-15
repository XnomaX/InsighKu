package com.example.insightku.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.feature.home.domain.CategoryMemory
import com.example.insightku.core.utils.ErrorBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val draftRepository: DraftTransactionRepository,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val categoryMemory = CategoryMemory()

    // All categories — for backward compat
    val categories: StateFlow<List<Category>> = transactionRepository
        .getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Expense categories only — shown when transaction type = EXPENSE.
    // Filter isProtected (prefix id "system-") sebagai pertahanan: data Room lama bisa
    // punya kategori sistem dengan isSystemCategory=false akibat bug penamaan Firestore.
    val expenseCategories: StateFlow<List<Category>> = transactionRepository
        .getExpenseCategories()
        .map { list -> list.filterNot { it.isProtected } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Income categories only — shown when transaction type = INCOME
    val incomeCategories: StateFlow<List<Category>> = transactionRepository
        .getIncomeCategories()
        .map { list -> list.filterNot { it.isProtected } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    fun addTransaction(transaction: Transaction, draftId: String? = null) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: run {
                _uiState.update { it.copy(error = "Not logged in") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                transactionRepository.addTransaction(transaction, userId)
                // Transaksi terkonfirmasi & tersimpan → hapus draft sumbernya (jika ada).
                if (draftId != null) {
                    draftRepository.confirmAndRemove(draftId)
                }
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to save transaction"
                _uiState.update { it.copy(isSaving = false, error = msg) }
                errorBus.send(msg)
            }
        }
    }

    fun clearSavedState() {
        _uiState.update { it.copy(savedSuccessfully = false, error = null) }
    }

    /**
     * Suggest a category for a freshly-typed merchant [title] from the user's own logging history —
     * the real, backend-free category learning. Returns null when learning is off, the merchant was
     * forgotten, or there's no confident memory yet (caller should leave the field untouched).
     */
    suspend fun suggestedCategory(title: String): String? {
        if (title.isBlank()) return null
        if (!preferencesDataStore.categoryLearningEnabled.first()) return null
        val forgotten = preferencesDataStore.forgottenMerchants.first()
        if (title.trim().lowercase() in forgotten) return null
        val transactions = transactionRepository.getAllTransactions().first()
        return categoryMemory.suggestCategory(title, transactions)
    }
}



