package com.example.insightku.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
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
                // Step 1: Delete all transactions in this account (cascade delete)
                transactionDao.deleteTransactionsByAccountId(accountId)
                // Step 2: Deactivate the account
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