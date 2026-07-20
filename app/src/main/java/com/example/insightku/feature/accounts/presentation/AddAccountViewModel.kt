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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState = _uiState.asStateFlow()

    fun resetForm() {
        _uiState.value = AddAccountUiState()
    }

    fun onEvent(event: AddAccountEvent) {
        when (event) {
            is AddAccountEvent.UpdateAccountName -> updateName(event.name)
            is AddAccountEvent.UpdateAccountType -> updateType(event.type)
            is AddAccountEvent.UpdateBalance -> updateBalance(event.balance)
            is AddAccountEvent.UpdateColor -> updateColor(event.color)
            is AddAccountEvent.UpdateNotes -> updateNotes(event.notes)
            is AddAccountEvent.SaveAccount -> saveAccount()
            is AddAccountEvent.ClearError -> clearError()
            is AddAccountEvent.ResetForm -> resetForm()
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
        // Allow empty or valid number format
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

    private fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
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

        // Validate
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
                val account = Account(
                    name = currentState.accountName.trim(),
                    accountType = currentState.selectedType.name,
                    balance = balance,
                    color = currentState.selectedColor,
                    notes = currentState.notes.trim()
                )

                accountRepository.insertAccount(account)

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