package com.example.insightku.feature.planning.goal.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.model.Goal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        val prefillAmount = savedStateHandle.get<String>("prefillAmount")?.toDoubleOrNull()
        if (goalId.isNotEmpty()) {
            loadData(goalId, prefillAmount)
        }
    }

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
            is ContributionEvent.LoadGoal -> loadData(event.goalId)
            is ContributionEvent.SubmitContribution -> submitContribution()
            is ContributionEvent.SubmitWithdrawal -> submitContribution()
            is ContributionEvent.SetMode -> _uiState.update { it.copy(isWithdraw = event.isWithdraw) }
            is ContributionEvent.Dismiss -> { /* Handled by UI layer */ }
            is ContributionEvent.NavigateBack -> { /* Handled by UI layer */ }
            is ContributionEvent.NavigateToSuccess -> { /* Handled by UI layer */ }
            is ContributionEvent.ClearError -> clearError()
            is ContributionEvent.ClearSnackbar -> clearSnackbar()
        }
    }

    private fun loadData(goalId: String, prefillAmount: Double? = null) {
        this.goalId = goalId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val accounts = accountRepository.getAllAccounts().first()
                val defaultAccount = accounts.firstOrNull()
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
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load data") }
            }
        }
    }

    private fun updateAmount(rawAmount: String) {
        val digits = rawAmount.filter { it.isDigit() }.take(12)
        val parsed = digits.toDoubleOrNull() ?: 0.0
        _uiState.update { it.copy(rawAmount = digits, parsedAmount = parsed) }
    }

    private fun selectQuickAmount(amount: Long) {
        _uiState.update { it.copy(rawAmount = amount.toString(), parsedAmount = amount.toDouble()) }
    }

    private fun clearAmount() {
        _uiState.update { it.copy(rawAmount = "", parsedAmount = 0.0) }
    }

    private fun selectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    private fun showAccountPicker() {
        _uiState.update { it.copy(showAccountPicker = true) }
    }

    private fun hideAccountPicker() {
        _uiState.update { it.copy(showAccountPicker = false) }
    }

    private fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

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
                _uiState.update { it.copy(isSubmitting = false, contributionSaved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to save contribution") }
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
