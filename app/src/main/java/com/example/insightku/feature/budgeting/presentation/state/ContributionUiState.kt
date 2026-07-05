package com.example.insightku.feature.budgeting.presentation.state

import com.example.insightku.core.data.model.Account
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.feature.budgeting.domain.model.Goal

/**
 * UI State for the Contribution screen.
 * Tracks all state for the contribution flow.
 */
data class ContributionUiState(
    // Goal context
    val goal: Goal? = null,
    val goalColor: String = "#7C4DFF",

    // Account selection
    val selectedAccountId: String? = null,
    val accounts: List<Account> = emptyList(),
    val accountAllocations: Map<String, AccountAllocation> = emptyMap(),

    // Amount input
    val rawAmount: String = "",
    val parsedAmount: Double = 0.0,

    // Notes
    val notes: String = "",

    // UI states
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
    val showAccountPicker: Boolean = false,

    // Success state
    val isSuccess: Boolean = false,
    val contributionSaved: Boolean = false
) {
    // Selected account
    val selectedAccount: Account?
        get() = accounts.find { it.id == selectedAccountId }

    val selectedAllocation: AccountAllocation?
        get() = selectedAccountId?.let { accountAllocations[it] }

    // Available cash for selected account
    val availableCash: Double
        get() = selectedAllocation?.availableCash ?: selectedAccount?.balance ?: 0.0

    // Validation
    val isValidAmount: Boolean
        get() = parsedAmount > 0 && parsedAmount <= availableCash

    val isInsufficientFunds: Boolean
        get() = parsedAmount > availableCash && parsedAmount > 0

    val shortfall: Double
        get() = if (isInsufficientFunds) parsedAmount - availableCash else 0.0

    val isValid: Boolean
        get() = selectedAccountId != null && isValidAmount

    // Preview calculations
    val newGoalAmount: Double
        get() = (goal?.currentAmount ?: 0.0) + parsedAmount

    val newProgressPercent: Double
        get() = goal?.let { g ->
            if (g.targetAmount > 0) ((newGoalAmount / g.targetAmount * 100).coerceIn(0.0, 100.0)) else 0.0
        } ?: 0.0

    val newRemainingAmount: Double
        get() = ((goal?.targetAmount ?: 0.0) - newGoalAmount).coerceAtLeast(0.0)

    val willCompleteGoal: Boolean
        get() = goal?.let { newGoalAmount >= it.targetAmount } ?: false

    // Account preview after contribution
    val newAvailableCash: Double
        get() = (availableCash - parsedAmount).coerceAtLeast(0.0)

    companion object {
        fun initial() = ContributionUiState(isLoading = true)
    }
}

/**
 * Quick amount presets for easy selection.
 */
enum class QuickAmount(val rawValue: Long) {
    AMOUNT_50K(50_000),
    AMOUNT_100K(100_000),
    AMOUNT_250K(250_000),
    AMOUNT_500K(500_000),
    AMOUNT_1M(1_000_000);

    val displayText: String
        get() = when {
            rawValue >= 1_000_000 -> "+${rawValue / 1_000_000}M"
            rawValue >= 1_000 -> "+${rawValue / 1_000}K"
            else -> "+$rawValue"
        }
}
