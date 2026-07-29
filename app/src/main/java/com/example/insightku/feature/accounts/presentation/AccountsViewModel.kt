package com.example.insightku.feature.accounts.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.AccountAllocationRepository
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    private val accountAllocationRepository: AccountAllocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState = _uiState.asStateFlow()

    // PERF INSTRUMENTATION: Track state emissions
    private var emissionCount = 0

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
                accountRepository.getAllAccounts(),
                accountRepository.getTotalNetWorth(),
                accountRepository.getTotalAssets(),
                accountRepository.getTotalLiabilities(),
                accountAllocationRepository.getAllAccountAllocations()
            ) { accounts, netWorth, assets, liabilities, allocations ->
                // Create allocation map by account ID
                val allocationMap = allocations.associateBy { it.account.id }

                AccountsUiState(
                    isLoading = false,
                    accounts = accounts,
                    totalNetWorth = netWorth ?: 0.0,
                    totalAssets = assets ?: 0.0,
                    totalLiabilities = liabilities ?: 0.0,
                    accountAllocations = allocationMap,
                    error = null
                )
            }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.error_load_accounts)
                    )
                }
                .collect { state ->
                    // PERF INSTRUMENTATION: Log state emission
                    emissionCount++
                    android.util.Log.i("PERF", "⚡ AccountsViewModel emission #$emissionCount | accounts=${state.accounts.size}")
                    _uiState.value = state
                }
        }
    }

    /**
     * Load transactions for a specific account (All Transactions filtered by accountId).
     * This powers the Account Detail screen — no separate history table needed.
     */
    fun loadAccountTransactions(accountId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
            try {
                accountRepository.getTransactionsByAccountIdFlow(accountId).collect { transactions ->
                    _uiState.value = _uiState.value.copy(accountTransactions = transactions)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: context.getString(R.string.error_load_account_transactions)
                )
            }
        }
    }

    /**
     * Clear the selected account (close Account Detail).
     */
    fun clearSelectedAccount() {
        _uiState.value = _uiState.value.copy(
            selectedAccountId = null,
            accountTransactions = emptyList()
        )
    }

    private fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                accountRepository.deleteAccount(accountId, userId)

                // Clear selection if this was the selected account
                if (_uiState.value.selectedAccountId == accountId) {
                    clearSelectedAccount()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: context.getString(R.string.error_delete_account)
                )
            }
        }
    }

    private fun setAsDefault(accountId: String) {
        viewModelScope.launch {
            try {
                accountRepository.setAsDefault(accountId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: context.getString(R.string.error_set_default_account)
                )
            }
        }
    }
}
