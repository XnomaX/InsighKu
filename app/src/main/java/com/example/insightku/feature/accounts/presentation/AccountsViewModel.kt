package com.example.insightku.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.auth.data.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun onEvent(event: AccountsEvent) {
        when (event) {
            is AccountsEvent.LoadAccounts -> loadAccounts()
            is AccountsEvent.DeleteAccount -> deleteAccount(event.accountId)
            is AccountsEvent.SetAsDefault -> setAsDefault(event.accountId)
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                accountDao.getAllAccounts(),
                accountDao.getTotalNetWorth(),
                accountDao.getTotalAssets(),
                accountDao.getTotalLiabilities()
            ) { accounts, netWorth, assets, liabilities ->
                AccountsUiState(
                    isLoading = false,
                    accounts = accounts,
                    totalNetWorth = netWorth ?: 0.0,
                    totalAssets = assets ?: 0.0,
                    totalLiabilities = liabilities ?: 0.0,
                    error = null
                )
            }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load accounts"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()

                // Step 1: Restore account balances for all transactions in this account
                // before deleting them. Each transaction's balance effect must be reversed.
                val transactions = transactionDao.getTransactionsByAccountId(accountId)
                for (tx in transactions) {
                    if (tx.accountId.isNotBlank()) {
                        val delta = if (tx.type == TransactionType.INCOME) {
                            -tx.amount  // Reverse income: subtract
                        } else {
                            tx.amount   // Reverse expense: add back
                        }
                        accountDao.updateBalance(tx.accountId, delta)
                    }
                }

                // Step 2: Delete all transactions in this account from Room
                transactionDao.deleteTransactionsByAccountId(accountId)

                // Step 3: Also delete from Firestore (best effort — if offline, orphaned docs remain
                // until next refresh which will remove them since Room is source of truth)
                if (userId != null) {
                    try {
                        for (tx in transactions) {
                            firestore.collection("users").document(userId)
                                .collection("transactions").document(tx.id).delete().await()
                        }
                    } catch (e: Exception) {
                        // Firestore offline — Room already deleted, orphaned Firestore docs
                        // will be cleaned up on next refreshTransactions()
                    }
                }

                // Step 4: Deactivate the account
                accountDao.deactivateAccount(accountId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete account"
                )
            }
        }
    }

    private fun setAsDefault(accountId: String) {
        viewModelScope.launch {
            try {
                // Clear all defaults first, then set the new one
                accountDao.clearAllDefaults()
                val account = accountDao.getAccountById(accountId)
                if (account != null) {
                    accountDao.updateAccount(account.copy(isDefault = true))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to set default account"
                )
            }
        }
    }
}