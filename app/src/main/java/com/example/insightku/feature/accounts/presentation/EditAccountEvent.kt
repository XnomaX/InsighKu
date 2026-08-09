package com.example.insightku.feature.accounts.presentation

import com.example.insightku.core.data.model.AccountType

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
