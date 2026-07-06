package com.example.insightku.feature.budgeting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.feature.budgeting.data.model.ContributionType
import com.example.insightku.feature.budgeting.data.repository.GoalRepository
import com.example.insightku.feature.budgeting.domain.model.Goal
import com.example.insightku.feature.budgeting.presentation.event.ContributionEvent
import com.example.insightku.feature.budgeting.presentation.state.ContributionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Contribution screen.
 */
@HiltViewModel
class ContributionViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContributionUiState.initial())
    val uiState: StateFlow<ContributionUiState> = _uiState.asStateFlow()

    private var goalId: String = savedStateHandle.get<String>("goalId") ?: ""

    init {
        // Initialize from saved state if available
        val prefillAmount = savedStateHandle.get<String>("prefillAmount")?.toDoubleOrNull()
        if (goalId.isNotEmpty()) {
            loadData(goalId, prefillAmount)
        }
    }

    /**
     * Handle user events.
     */
    fun onEvent(event: ContributionEvent) {
        when (event) {
            is ContributionEvent.Initialize -> loadData(event.goalId, event.prefillAmount)
            is ContributionEvent.UpdateAmount -> updateAmount(event.rawAmount)
            is ContributionEvent.SelectQuickAmount -> selectQuickAmount(event.amount)
            is ContributionEvent.ClearAmount -> clearAmount()
            is ContributionEvent.SelectAccount -> selectAccount(event.accountId)
            is ContributionEvent.ShowAccountPicker -> showAccountPicker()
            is ContributionEvent.HideAccountPicker -> hideAccountPicker()
            is ContributionEvent.UpdateNotes -> updateNotes(event.notes)
            is ContributionEvent.SubmitContribution -> submitContribution()
            is ContributionEvent.NavigateBack -> { /* Handled by UI layer */ }
            is ContributionEvent.NavigateToSuccess -> { /* Handled by UI layer */ }
            is ContributionEvent.ClearError -> clearError()
            is ContributionEvent.ClearSnackbar -> clearSnackbar()
        }
    }

    /**
     * Load goal and account data.
     */
    private fun loadData(goalId: String, prefillAmount: Double? = null) {
        this.goalId = goalId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load accounts
                val accounts = accountRepository.getAllAccounts().first()
                val defaultAccount = accounts.firstOrNull()

                // Load goal
                val goal = goalRepository.getGoalById(goalId)
                if (goal == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Goal not found") }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        goal = goal,
                        goalColor = goal.color,
                        accounts = accounts,
                        selectedAccountId = defaultAccount?.id,
                        rawAmount = prefillAmount?.let { it.toLong().toString() } ?: "",
                        parsedAmount = prefillAmount ?: 0.0,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load data"
                    )
                }
            }
        }
    }

    /**
     * Update amount from text input.
     */
    private fun updateAmount(rawAmount: String) {
        val digits = rawAmount.filter { it.isDigit() }.take(12)
        val parsed = digits.toDoubleOrNull() ?: 0.0

        _uiState.update {
            it.copy(
                rawAmount = digits,
                parsedAmount = parsed
            )
        }
    }

    /**
     * Select a quick amount preset.
     */
    private fun selectQuickAmount(amount: Long) {
        _uiState.update {
            it.copy(
                rawAmount = amount.toString(),
                parsedAmount = amount.toDouble()
            )
        }
    }

    /**
     * Clear the amount input.
     */
    private fun clearAmount() {
        _uiState.update {
            it.copy(rawAmount = "", parsedAmount = 0.0)
        }
    }

    /**
     * Select an account for the contribution.
     */
    private fun selectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    /**
     * Switch to the next available account.
     */
    private fun switchAccount() {
        val state = _uiState.value
        val accounts = state.accounts
        if (accounts.size < 2) return

        val currentIndex = accounts.indexOfFirst { it.id == state.selectedAccountId }
        val nextIndex = (currentIndex + 1) % accounts.size
        _uiState.update { it.copy(selectedAccountId = accounts[nextIndex].id) }
    }

    /**
     * Show the account picker dialog.
     */
    private fun showAccountPicker() {
        _uiState.update { it.copy(showAccountPicker = true) }
    }

    /**
     * Hide the account picker dialog.
     */
    private fun hideAccountPicker() {
        _uiState.update { it.copy(showAccountPicker = false) }
    }

    /**
     * Update notes.
     */
    private fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    /**
     * Submit the contribution.
     */
    private fun submitContribution() {
        val state = _uiState.value
        val goal = state.goal ?: return
        val accountId = state.selectedAccountId ?: return

        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            goalRepository.contribute(
                goalId = goal.id,
                accountId = accountId,
                amount = state.parsedAmount,
                type = ContributionType.MANUAL,
                notes = state.notes
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        contributionSaved = true
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "Failed to save contribution"
                    )
                }
            }
        }
    }

    /**
     * Clear error message.
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear snackbar message.
     */
    private fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
