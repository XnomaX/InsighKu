package com.example.insightku.feature.accounts.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditAccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository
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
                nameError = if (name.isBlank()) context.getString(R.string.error_account_name_required) else null
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
                balanceError = if (!isValid) context.getString(R.string.error_invalid_balance) else null
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
            _uiState.update { it.copy(nameError = context.getString(R.string.error_account_name_required)) }
            hasError = true
        }

        val balance = currentState.balance.toDoubleOrNull() ?: 0.0
        if (currentState.balance.isNotEmpty() && balance < 0) {
            _uiState.update { it.copy(balanceError = context.getString(R.string.error_balance_negative)) }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Fetch existing account to preserve mutable fields
                val existingAccount = accountRepository.getAccountById(currentState.accountId)
                val updatedAccount = existingAccount?.copy(
                    name = currentState.accountName.trim(),
                    accountType = currentState.selectedType.name,
                    balance = balance,
                    color = currentState.selectedColor,
                    notes = currentState.notes.trim(),
                    updatedAt = System.currentTimeMillis()
                ) ?: throw Exception("Account not found")

                accountRepository.updateAccount(updatedAccount)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.error_save_account)
                    )
                }
            }
        }
    }
}


// --- EditAccount MVI contract (Event.kt) -----------------------------

/**
 * Events for the Edit Account screen.
 */
sealed class EditAccountEvent {
    data class UpdateAccountName(val name: String) : EditAccountEvent()
    data class UpdateAccountType(val type: AccountType) : EditAccountEvent()
    data class UpdateBalance(val balance: String) : EditAccountEvent()
    data class UpdateColor(val color: String) : EditAccountEvent()
    object SaveAccount : EditAccountEvent()
    object ClearError : EditAccountEvent()
}

/**
 * UI State for the Edit Account screen.
 */
data class EditAccountUiState(
    val accountId: String = "",
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
