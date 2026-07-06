package com.example.insightku.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.feature.home.domain.GetTransactionsUseCase
import com.example.insightku.core.utils.ErrorBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadTransactions()
        loadCategories()
    }

    private fun loadTransactions() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                getTransactionsUseCase().collect { list ->
                    _transactions.value = list
                    _isLoading.value = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                errorBus.send(e.message ?: "Gagal memuat transaksi")
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().collect { cats ->
                    _categories.value = cats
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // silent — categories are optional for icon resolution
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                transactionRepository.updateTransaction(transaction, userId)
            } catch (e: Exception) {
                errorBus.send(e.message ?: "Gagal menyimpan perubahan transaksi")
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                transactionRepository.deleteTransaction(transactionId, userId)
            } catch (e: Exception) {
                errorBus.send(e.message ?: "Gagal menghapus transaksi")
            }
        }
    }
}



