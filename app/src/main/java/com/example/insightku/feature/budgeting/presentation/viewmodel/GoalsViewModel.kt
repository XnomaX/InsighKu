package com.example.insightku.feature.budgeting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.repository.AccountAllocationRepository
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.feature.budgeting.data.model.ContributionType
import com.example.insightku.feature.budgeting.data.model.GoalAccountEntity
import com.example.insightku.feature.budgeting.data.model.GoalStatus
import com.example.insightku.feature.budgeting.data.repository.GoalRepository
import com.example.insightku.feature.budgeting.domain.model.AllocationSuggestion
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule
import com.example.insightku.feature.budgeting.domain.model.Goal
import com.example.insightku.feature.budgeting.domain.model.GoalSummary
import com.example.insightku.feature.budgeting.presentation.event.GoalsEvent
import com.example.insightku.feature.budgeting.presentation.state.GoalsDialogState
import com.example.insightku.feature.budgeting.presentation.state.GoalsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * Intermediate data class for combining flows with different types.
 */
private data class CoreGoalsData(
    val goals: List<Goal>,
    val dailyTarget: com.example.insightku.feature.budgeting.domain.model.DailyTarget,
    val rules: List<AutoAllocationRule>,
    val summary: GoalSummary,
    val accounts: List<Account>
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val accountAllocationRepository: AccountAllocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState.initial())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Load all initial data.
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Combine first 5 flows
            val coreFlow = combine(
                goalRepository.getActiveGoals(),
                goalRepository.getDailyTarget(),
                goalRepository.getAutoAllocationRules(),
                goalRepository.getGoalsSummary(),
                accountRepository.getAllAccounts()
            ) { goals, dailyTarget, rules, summary, accounts ->
                CoreGoalsData(goals, dailyTarget, rules, summary, accounts)
            }

            // Combine with allocations flow
            coreFlow.combine(accountAllocationRepository.getAllAccountAllocations()) { core, allocations ->
                // Create allocation map by account ID
                val allocationMap = allocations.associateBy { it.account.id }

                GoalsUiState(
                    goals = core.goals,
                    dailyTarget = core.dailyTarget,
                    autoAllocationRules = core.rules,
                    goalSummary = core.summary,
                    accounts = core.accounts,
                    accountAllocations = allocationMap,
                    isLoading = false
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(
                        selectedGoal = current.selectedGoal,
                        pendingSuggestions = current.pendingSuggestions,
                        linkedAccounts = current.linkedAccounts,
                        dialogState = current.dialogState
                    )
                }
            }
        }

        // Load linked accounts for each goal
        viewModelScope.launch {
            goalRepository.getActiveGoals().collect { goals ->
                val linkedAccountsMap = mutableMapOf<String, List<GoalAccountEntity>>()
                goals.forEach { goal ->
                    goalRepository.getLinkedAccounts(goal.id).first().let { accounts ->
                        linkedAccountsMap[goal.id] = accounts
                    }
                }
                _uiState.update { it.copy(linkedAccounts = linkedAccountsMap) }
            }
        }
    }

    /**
     * Handle user events.
     */
    fun onEvent(event: GoalsEvent) {
        when (event) {
            // Goal CRUD
            is GoalsEvent.CreateGoal -> createGoal(event)
            is GoalsEvent.UpdateGoal -> updateGoal(event)
            is GoalsEvent.UpdateGoalStatus -> updateGoalStatus(event.goalId, event.status)
            is GoalsEvent.ArchiveGoal -> archiveGoal(event.goalId)
            is GoalsEvent.PauseGoal -> updateGoalStatus(event.goalId, GoalStatus.PAUSED)
            is GoalsEvent.ResumeGoal -> updateGoalStatus(event.goalId, GoalStatus.ACTIVE)

            // Contributions
            is GoalsEvent.Contribute -> contribute(event)
            is GoalsEvent.Withdraw -> withdraw(event)

            // Account Linking
            is GoalsEvent.LinkAccount -> linkAccount(event)
            is GoalsEvent.UnlinkAccount -> unlinkAccount(event.goalId, event.accountId)
            is GoalsEvent.SetPrimaryAccount -> setPrimaryAccount(event.goalId, event.accountId)

            // Daily Target
            is GoalsEvent.SetDailyTarget -> setDailyTarget(event.amount)
            is GoalsEvent.ClearDailyTarget -> clearDailyTarget()

            // Auto-Allocation
            is GoalsEvent.AddAutoAllocationRule -> addAutoAllocationRule(event.rule)
            is GoalsEvent.UpdateAutoAllocationRule -> updateAutoAllocationRule(event.rule)
            is GoalsEvent.DeleteAutoAllocationRule -> deleteAutoAllocationRule(event.ruleId)
            is GoalsEvent.ToggleAutoAllocationRule -> toggleAutoAllocationRule(event.ruleId, event.enabled)
            is GoalsEvent.SetGoalAutoAllocate -> setGoalAutoAllocate(event.goalId, event.enabled)

            // Suggestions
            is GoalsEvent.ConfirmSuggestion -> confirmSuggestion(event.suggestion)
            is GoalsEvent.DismissSuggestion -> dismissSuggestion(event.suggestion)
            is GoalsEvent.DismissAllSuggestions -> dismissAllSuggestions()

            // Dialog Management
            is GoalsEvent.DismissDialog -> dismissDialog()
            is GoalsEvent.ShowAddGoalDialog -> showAddGoalDialog(event.prefillDeadline)
            is GoalsEvent.ShowEditGoalDialog -> showEditGoalDialog(event.goalId)
            is GoalsEvent.ShowArchiveGoalDialog -> showArchiveGoalDialog(event.goalId)
            is GoalsEvent.ShowContributeDialog -> showContributeDialog(event.goalId)
            is GoalsEvent.ShowWithdrawDialog -> showWithdrawDialog(event.goalId)
            is GoalsEvent.ShowLinkAccountDialog -> showLinkAccountDialog(event.goalId)
            is GoalsEvent.ShowSelectAccountDialog -> showSelectAccountDialog(event.goalId, event.action)
            is GoalsEvent.ShowSetDailyTargetDialog -> showSetDailyTargetDialog()
            is GoalsEvent.ShowAddAutoAllocationRuleDialog -> showAddAutoAllocationRuleDialog(event.goalId)
            is GoalsEvent.ShowEditAutoAllocationRuleDialog -> showEditAutoAllocationRuleDialog(event.rule)
            is GoalsEvent.ShowPendingSuggestions -> showPendingSuggestions()
            is GoalsEvent.ShowGoalDetail -> showGoalDetail(event.goalId)

            // Error Handling
            is GoalsEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is GoalsEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    // ── Goal Operations ─────────────────────────────────────────────────────────

    private fun createGoal(event: GoalsEvent.CreateGoal) {
        viewModelScope.launch {
            val goal = Goal(
                id = UUID.randomUUID().toString(),
                name = event.name,
                targetAmount = event.targetAmount,
                deadline = event.deadline,
                status = GoalStatus.ACTIVE,
                autoAllocate = false,
                allocationPriority = 0,
                iconName = event.iconName,
                color = event.color,
                notes = "",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            goalRepository.createGoal(goal)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = GoalsDialogState.None,
                            snackbarMessage = "Goal created successfully"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to create goal") }
                }
        }
    }

    private fun updateGoal(event: GoalsEvent.UpdateGoal) {
        viewModelScope.launch {
            val existingGoal = _uiState.value.goals.find { it.id == event.id } ?: return@launch

            val updatedGoal = existingGoal.copy(
                name = event.name,
                targetAmount = event.targetAmount,
                deadline = event.deadline,
                iconName = event.iconName,
                color = event.color,
                notes = event.notes,
                updatedAt = Instant.now()
            )

            goalRepository.updateGoal(updatedGoal)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = GoalsDialogState.None,
                            snackbarMessage = "Goal updated"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to update goal") }
                }
        }
    }

    private fun updateGoalStatus(goalId: String, status: GoalStatus) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goalId, status)
                .onSuccess {
                    val message = when (status) {
                        GoalStatus.COMPLETED -> "Congratulations! Goal completed!"
                        GoalStatus.PAUSED -> "Goal paused"
                        GoalStatus.ACTIVE -> "Goal resumed"
                        GoalStatus.ARCHIVED -> "Goal archived"
                    }
                    _uiState.update { it.copy(snackbarMessage = message) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    private fun archiveGoal(goalId: String) {
        viewModelScope.launch {
            goalRepository.archiveGoal(goalId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = GoalsDialogState.None,
                            snackbarMessage = "Goal archived"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ── Contribution Operations ─────────────────────────────────────────────────

    private fun contribute(event: GoalsEvent.Contribute) {
        viewModelScope.launch {
            goalRepository.contribute(
                goalId = event.goalId,
                accountId = event.accountId,
                amount = event.amount,
                type = ContributionType.MANUAL,
                notes = event.notes
            ).onSuccess { contribution ->
                val goal = _uiState.value.goals.find { it.id == event.goalId }
                val goalName = goal?.name ?: "goal"
                _uiState.update {
                    it.copy(
                        dialogState = GoalsDialogState.None,
                        snackbarMessage = "$${"%.2f".format(event.amount)} added to $goalName"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to contribute") }
            }
        }
    }

    private fun withdraw(event: GoalsEvent.Withdraw) {
        viewModelScope.launch {
            goalRepository.withdraw(
                goalId = event.goalId,
                accountId = event.accountId,
                amount = event.amount,
                notes = event.notes
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        dialogState = GoalsDialogState.None,
                        snackbarMessage = "$${"%.2f".format(event.amount)} withdrawn"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to withdraw") }
            }
        }
    }

    // ── Account Linking ────────────────────────────────────────────────────────

    private fun linkAccount(event: GoalsEvent.LinkAccount) {
        viewModelScope.launch {
            goalRepository.linkAccountToGoal(
                goalId = event.goalId,
                accountId = event.accountId,
                allocationPercent = event.allocationPercent,
                isPrimary = event.isPrimary
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        dialogState = GoalsDialogState.None,
                        snackbarMessage = "Account linked to goal"
                    )
                }
                // Refresh linked accounts
                loadLinkedAccounts(event.goalId)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun unlinkAccount(goalId: String, accountId: String) {
        viewModelScope.launch {
            goalRepository.unlinkAccountFromGoal(goalId, accountId)
                .onSuccess {
                    _uiState.update { it.copy(snackbarMessage = "Account unlinked") }
                    loadLinkedAccounts(goalId)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    private fun setPrimaryAccount(goalId: String, accountId: String) {
        viewModelScope.launch {
            goalRepository.linkAccountToGoal(goalId, accountId, isPrimary = true)
            loadLinkedAccounts(goalId)
        }
    }

    private fun loadLinkedAccounts(goalId: String) {
        viewModelScope.launch {
            goalRepository.getLinkedAccounts(goalId).first().let { accounts ->
                _uiState.update { state ->
                    state.copy(linkedAccounts = state.linkedAccounts + (goalId to accounts))
                }
            }
        }
    }

    // ── Daily Target Operations ─────────────────────────────────────────────────

    private fun setDailyTarget(amount: Double) {
        viewModelScope.launch {
            goalRepository.setDailyTarget(amount)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = GoalsDialogState.None,
                            snackbarMessage = if (amount > 0) "Daily target set to $${"%.2f".format(amount)}"
                            else "Daily target cleared"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    private fun clearDailyTarget() {
        viewModelScope.launch {
            goalRepository.clearDailyTarget()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = GoalsDialogState.None,
                            snackbarMessage = "Daily target cleared"
                        )
                    }
                }
        }
    }

    // ── Auto-Allocation Operations ─────────────────────────────────────────────

    private fun addAutoAllocationRule(rule: AutoAllocationRule) {
        viewModelScope.launch {
            goalRepository.addAutoAllocationRule(rule)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = GoalsDialogState.None,
                            snackbarMessage = "Auto-allocation rule added"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    private fun updateAutoAllocationRule(rule: AutoAllocationRule) {
        viewModelScope.launch {
            goalRepository.updateAutoAllocationRule(rule)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = GoalsDialogState.None,
                            snackbarMessage = "Rule updated"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    private fun deleteAutoAllocationRule(ruleId: String) {
        viewModelScope.launch {
            goalRepository.deleteAutoAllocationRule(ruleId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = GoalsDialogState.None,
                            snackbarMessage = "Rule deleted"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    private fun toggleAutoAllocationRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            goalRepository.setRuleEnabled(ruleId, enabled)
        }
    }

    private fun setGoalAutoAllocate(goalId: String, enabled: Boolean) {
        viewModelScope.launch {
            goalRepository.setAutoAllocate(goalId, enabled)
        }
    }

    // ── Suggestions Operations ──────────────────────────────────────────────────

    private fun confirmSuggestion(suggestion: AllocationSuggestion) {
        viewModelScope.launch {
            goalRepository.contribute(
                goalId = suggestion.goalId,
                accountId = suggestion.sourceAccountId,
                amount = suggestion.amount,
                type = ContributionType.AUTO_ALLOCATION
            ).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        pendingSuggestions = state.pendingSuggestions.filter { it.id != suggestion.id },
                        snackbarMessage = "$${"%.2f".format(suggestion.amount)} saved to ${suggestion.goalName}"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun dismissSuggestion(suggestion: AllocationSuggestion) {
        _uiState.update { state ->
            state.copy(
                pendingSuggestions = state.pendingSuggestions.filter { it.id != suggestion.id }
            )
        }
    }

    private fun dismissAllSuggestions() {
        _uiState.update { it.copy(pendingSuggestions = emptyList()) }
    }

    // ── Dialog Operations ──────────────────────────────────────────────────────

    private fun dismissDialog() {
        _uiState.update { it.copy(dialogState = GoalsDialogState.None) }
    }

    private fun showAddGoalDialog(prefillDeadline: LocalDate?) {
        _uiState.update {
            it.copy(dialogState = GoalsDialogState.AddGoal(deadline = prefillDeadline))
        }
    }

    private fun showEditGoalDialog(goalId: String) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        _uiState.update { it.copy(dialogState = GoalsDialogState.EditGoal(goal)) }
    }

    private fun showArchiveGoalDialog(goalId: String) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        _uiState.update { it.copy(dialogState = GoalsDialogState.ArchiveGoal(goalId, goal.name)) }
    }

    private fun showContributeDialog(goalId: String) {
        val defaultAccountId = _uiState.value.accounts.firstOrNull()?.id
        _uiState.update {
            it.copy(dialogState = GoalsDialogState.Contribute(goalId, defaultAccountId))
        }
    }

    private fun showWithdrawDialog(goalId: String) {
        val defaultAccountId = _uiState.value.accounts.firstOrNull()?.id
        _uiState.update {
            it.copy(dialogState = GoalsDialogState.Withdraw(goalId, defaultAccountId))
        }
    }

    private fun showLinkAccountDialog(goalId: String) {
        _uiState.update { it.copy(dialogState = GoalsDialogState.LinkAccount(goalId)) }
    }

    private fun showSelectAccountDialog(goalId: String, action: String) {
        _uiState.update {
            it.copy(dialogState = GoalsDialogState.SelectAccount(
                goalId,
                when (action) {
                    "contribute" -> com.example.insightku.feature.budgeting.presentation.state.AccountAction.CONTRIBUTE
                    "withdraw" -> com.example.insightku.feature.budgeting.presentation.state.AccountAction.WITHDRAW
                    else -> com.example.insightku.feature.budgeting.presentation.state.AccountAction.CONTRIBUTE
                }
            ))
        }
    }

    private fun showSetDailyTargetDialog() {
        val currentAmount = _uiState.value.dailyTarget.targetAmount
        _uiState.update {
            it.copy(dialogState = GoalsDialogState.SetDailyTarget(
                if (currentAmount > 0) currentAmount.toString() else ""
            ))
        }
    }

    private fun showAddAutoAllocationRuleDialog(goalId: String?) {
        _uiState.update {
            it.copy(dialogState = GoalsDialogState.AddAutoAllocationRule(goalId))
        }
    }

    private fun showEditAutoAllocationRuleDialog(rule: AutoAllocationRule) {
        _uiState.update { it.copy(dialogState = GoalsDialogState.EditAutoAllocationRule(rule)) }
    }

    private fun showPendingSuggestions() {
        _uiState.update { it.copy(dialogState = GoalsDialogState.PendingSuggestions) }
    }

    private fun showGoalDetail(goalId: String) {
        viewModelScope.launch {
            _uiState.update {
                val goal = it.goals.find { g -> g.id == goalId }
                // Load contributions for this goal
                val contributions = goalRepository.getContributionsByGoal(goalId).first()
                it.copy(
                    selectedGoal = goal,
                    selectedGoalContributions = contributions,
                    dialogState = GoalsDialogState.GoalDetail(goalId)
                )
            }
        }
    }

    // ── Error Handling ─────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
