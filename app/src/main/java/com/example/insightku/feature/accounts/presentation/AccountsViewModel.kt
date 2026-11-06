package com.example.insightku.feature.accounts.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.repository.AccountAllocationRepository
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    private val accountAllocationRepository: AccountAllocationRepository
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
                    _uiState.value = state
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


// --- Accounts MVI contract (Event.kt, State.kt) --------------------------------

/**
 * Events for the Accounts screen.
 */
sealed class AccountsEvent {
    object LoadAccounts : AccountsEvent()
    data class DeleteAccount(val accountId: String, val accountName: String) : AccountsEvent()
    data class SetAsDefault(val accountId: String) : AccountsEvent()
}

/**
 * Events for the Add Account screen.
 */
sealed class AddAccountEvent {
    data class UpdateAccountName(val name: String) : AddAccountEvent()
    data class UpdateAccountType(val type: AccountType) : AddAccountEvent()
    data class UpdateBalance(val balance: String) : AddAccountEvent()
    data class UpdateColor(val color: String) : AddAccountEvent()
    data class UpdateNotes(val notes: String) : AddAccountEvent()
    object SaveAccount : AddAccountEvent()
    object ClearError : AddAccountEvent()
    object ResetForm : AddAccountEvent()
}

/**
 * UI State for the Accounts screen.
 */
data class AccountsUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    // Account allocations map - key is accountId
    val accountAllocations: Map<String, AccountAllocation> = emptyMap(),
    // Account detail: transactions filtered by accountId
    val selectedAccountId: String? = null,
    val accountTransactions: List<Transaction> = emptyList(),
    val error: String? = null
) {
    /**
     * Get allocation for a specific account.
     */
    fun getAllocation(accountId: String): AccountAllocation? = accountAllocations[accountId]
}

/**
 * UI State for the Add Account screen.
 */
data class AddAccountUiState(
    val accountName: String = "",
    val selectedType: AccountType = AccountType.BANK_ACCOUNT,
    val balance: String = "",
    val selectedColor: String = "#7C4DFF",
    val notes: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val balanceError: String? = null,
    val isSaved: Boolean = false
)

/**
 * Available colors for account customization.
 */
val accountColors = listOf(
    "#7C4DFF", // Purple (default)
    "#10B981", // Green
    "#3B82F6", // Blue
    "#F59E0B", // Amber
    "#EF4444", // Red
    "#EC4899", // Pink
    "#8B5CF6", // Violet
    "#06B6D4"  // Cyan
)
