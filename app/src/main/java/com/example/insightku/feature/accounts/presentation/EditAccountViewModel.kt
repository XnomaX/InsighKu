package com.example.insightku.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditAccountViewModel @Inject constructor(
    private val accountDao: AccountDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditAccountUiState())
    val uiState = _uiState.asStateFlow()

    fun loadAccount(account: Account) {
        _uiState.update {
            EditAccountUiState(
                accountId = account.id,
                accountName = account.name,
                selectedType = account.type,
                balance = if (account.balance != 0.0) formatBalance(account.balance) else "",
                selectedColor = account.color,
                notes = account.notes,
                isLoading = false
            )
        }
    }

    private fun formatBalance(balance: Double): String {
        return if (balance == balance.toLong().toDouble()) {
            balance.toLong().toString()
        } else {
            balance.toString()
        }
    }

    fun onEvent(event: EditAccountEvent) {
        when (event) {
            is EditAccountEvent.UpdateAccountName -> updateName(event.name)
            is EditAccountEvent.UpdateAccountType -> updateType(event.type)
            is EditAccountEvent.UpdateBalance -> updateBalance(event.balance)
            is EditAccountEvent.UpdateColor -> updateColor(event.color)
            is EditAccountEvent.SaveAccount -> saveAccount()
            is EditAccountEvent.ClearError -> clearError()
        }
    }

    private fun updateName(name: String) {
        _uiState.update {
            it.copy(
                accountName = name,
                nameError = if (name.isBlank()) "Account name is required" else null
            )
        }
    }

    private fun updateType(type: AccountType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    private fun updateBalance(balance: String) {
        val isValid = balance.isEmpty() || balance.toDoubleOrNull() != null
        _uiState.update {
            it.copy(
                balance = balance,
                balanceError = if (!isValid) "Invalid balance format" else null
            )
        }
    }

    private fun updateColor(color: String) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    private fun clearError() {
        _uiState.update {
            it.copy(
                error = null,
                nameError = null,
                balanceError = null
            )
        }
    }

    private fun saveAccount() {
        val currentState = _uiState.value

        var hasError = false
        if (currentState.accountName.isBlank()) {
            _uiState.update { it.copy(nameError = "Account name is required") }
            hasError = true
        }

        val balance = currentState.balance.toDoubleOrNull() ?: 0.0
        if (currentState.balance.isNotEmpty() && balance < 0) {
            _uiState.update { it.copy(balanceError = "Balance cannot be negative") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Fetch existing account to preserve mutable fields
                val existingAccount = accountDao.getAccountById(currentState.accountId)
                val updatedAccount = existingAccount?.copy(
                    name = currentState.accountName.trim(),
                    accountType = currentState.selectedType.name,
                    balance = balance,
                    color = currentState.selectedColor,
                    notes = currentState.notes.trim(),
                    updatedAt = System.currentTimeMillis()
                ) ?: throw Exception("Account not found")

                accountDao.updateAccount(updatedAccount)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save account"
                    )
                }
            }
        }
    }
}
