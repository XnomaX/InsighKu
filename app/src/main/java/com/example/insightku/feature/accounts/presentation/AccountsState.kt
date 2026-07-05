package com.example.insightku.feature.accounts.presentation

import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.domain.model.AccountAllocation

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
