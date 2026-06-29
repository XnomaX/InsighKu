package com.example.insightku.feature.accounts.presentation

import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType

/**
 * UI State for the Accounts screen.
 */
data class AccountsUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val error: String? = null
)

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