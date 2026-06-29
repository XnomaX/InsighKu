package com.example.insightku.feature.accounts.presentation

import com.example.insightku.core.data.model.AccountType

/**
 * Events for the Accounts screen.
 */
sealed class AccountsEvent {
    object LoadAccounts : AccountsEvent()
    data class DeleteAccount(val accountId: String) : AccountsEvent()
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