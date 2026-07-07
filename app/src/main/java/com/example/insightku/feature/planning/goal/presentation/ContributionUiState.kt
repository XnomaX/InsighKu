package com.example.insightku.feature.planning.goal.presentation

import com.example.insightku.core.data.model.Account
import com.example.insightku.feature.planning.goal.domain.model.Goal

data class ContributionUiState(
    val goal: Goal? = null,
    val goalColor: String = "#7C4DFF",
    val rawAmount: String = "",
    val parsedAmount: Double = 0.0,
    val notes: String = "",
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String? = null,
    val showAccountPicker: Boolean = false,
    val availableCash: Double = 0.0,
    val isInsufficientFunds: Boolean = false,
    val shortfall: Double = 0.0,
    val newGoalAmount: Double = 0.0,
    val newProgressPercent: Double = 0.0,
    val newAvailableCash: Double = 0.0,
    val isWithdraw: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
    val isSuccess: Boolean = false,
    val contributionSaved: Boolean = false
) {
    val isValid: Boolean get() = parsedAmount > 0 && selectedAccountId != null && !isInsufficientFunds
    val selectedAccount: Account? get() = accounts.find { it.id == selectedAccountId }

    companion object {
        fun initial() = ContributionUiState()
    }
}
