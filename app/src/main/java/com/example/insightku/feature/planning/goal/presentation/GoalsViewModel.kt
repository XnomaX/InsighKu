package com.example.insightku.feature.planning.goal.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.repository.AccountAllocationRepository
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.model.GoalAccountEntity
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.DailyTarget
import com.example.insightku.feature.planning.goal.domain.model.Goal
import com.example.insightku.feature.planning.goal.domain.model.GoalSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

private data class CoreGoalsData(
    val goals: List<Goal>,
    val dailyTarget: DailyTarget,
    val rules: List<AutoAllocationRule>,
    val summary: GoalSummary,
    val accounts: List<Account>
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val accountAllocationRepository: AccountAllocationRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState.initial())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        // ── Core data flow (combined active goals + accounts + rules) ───────
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val coreFlow = combine(
                goalRepository.getActiveGoals(),
                goalRepository.getDailyTarget(),
                goalRepository.getAutoAllocationRules(),
                goalRepository.getGoalsSummary(),
                accountRepository.getAllAccounts()
            ) { goals, dailyTarget, rules, summary, accounts ->
                CoreGoalsData(goals, dailyTarget, rules, summary, accounts)
            }
            coreFlow.combine(accountAllocationRepository.getAllAccountAllocations()) { core, allocations ->
                val allocationMap = allocations.associateBy { it.account.id }
                GoalsUiState(
                    goals = core.goals,
                    activeGoals = core.goals.filter { it.isActive },
                    pausedGoals = core.goals.filter { it.isPaused },
                    completedGoals = core.goals.filter { it.isCompleted },
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
                        linkedAccounts = current.linkedAccounts,
                        dialogState = current.dialogState,
                        selectedTab = current.selectedTab,
                        showArchivedSheet = current.showArchivedSheet,
                        showActionsSheet = current.showActionsSheet,
                        actionsGoalId = current.actionsGoalId,
                        showCompletionCelebration = current.showCompletionCelebration,
                        completionGoalName = current.completionGoalName,
                        archivedGoals = current.archivedGoals
                    )
                }
            }
        }

        // ── Archived goals (separate flow) ──────────────────────────────────
        viewModelScope.launch {
            goalRepository.getArchivedGoals().collect { archived ->
                _uiState.update { it.copy(archivedGoals = archived) }
            }
        }

        // ── Linked accounts for active goals (streamed — re-emits on any link change) ──
        viewModelScope.launch {
            goalRepository.getAllGoalAccountLinks().collect { links ->
                _uiState.update { it.copy(linkedAccounts = links.groupBy { link -> link.goalId }) }
            }
        }

        // ── Expense categories ──────────────────────────────────────────────
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                val expenseInfo = categories.filter { it.type == CategoryType.EXPENSE && !it.isSystemCategory }.map { CategoryInfo(id = it.id, name = it.name) }
                _uiState.update { it.copy(expenseCategories = expenseInfo) }
            }
        }
    }

    fun onEvent(event: GoalsEvent) {
        when (event) {
            is GoalsEvent.CreateGoal -> createGoal(event)
            is GoalsEvent.UpdateGoal -> updateGoal(event)
            is GoalsEvent.UpdateGoalStatus -> updateGoalStatus(event.goalId, event.status)
            is GoalsEvent.ArchiveGoal -> archiveGoal(event.goalId)
            is GoalsEvent.PauseGoal -> updateGoalStatus(event.goalId, GoalStatus.PAUSED)
            is GoalsEvent.ResumeGoal -> updateGoalStatus(event.goalId, GoalStatus.ACTIVE)
            is GoalsEvent.Contribute -> contribute(event)
            is GoalsEvent.Withdraw -> withdraw(event)
            is GoalsEvent.LinkAccount -> linkAccount(event)
            is GoalsEvent.UnlinkAccount -> unlinkAccount(event.goalId, event.accountId)
            is GoalsEvent.SetPrimaryAccount -> setPrimaryAccount(event.goalId, event.accountId)
            is GoalsEvent.SetDailyTarget -> setDailyTarget(event.amount)
            is GoalsEvent.ClearDailyTarget -> clearDailyTarget()
            is GoalsEvent.AddAutoAllocationRule -> addAutoAllocationRule(event.rule)
            is GoalsEvent.UpdateAutoAllocationRule -> updateAutoAllocationRule(event.rule)
            is GoalsEvent.DeleteAutoAllocationRule -> deleteAutoAllocationRule(event.ruleId)
            is GoalsEvent.ToggleAutoAllocationRule -> toggleAutoAllocationRule(event.ruleId, event.enabled)
            is GoalsEvent.SetGoalAutoAllocate -> setGoalAutoAllocate(event.goalId, event.enabled)
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
            is GoalsEvent.ShowGoalDetail -> showGoalDetail(event.goalId)
            is GoalsEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is GoalsEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }
            is GoalsEvent.DeleteGoal -> archiveGoal(event.goalId)
            is GoalsEvent.LoadGoals -> { /* Data auto-loaded in init */ }
            is GoalsEvent.SelectAccount -> { /* Handled by specific dialogs */ }
            is GoalsEvent.ShowAddBudgetDialog -> { /* Not applicable in Goals */ }

            // ── Redesigned events ───────────────────────────────────────────
            is GoalsEvent.SelectTab -> _uiState.update { it.copy(selectedTab = event.tab) }
            is GoalsEvent.ShowArchivedGoals -> _uiState.update { it.copy(showArchivedSheet = true) }
            is GoalsEvent.DismissArchivedSheet -> _uiState.update { it.copy(showArchivedSheet = false) }
            is GoalsEvent.ShowGoalActionsSheet -> _uiState.update { it.copy(showActionsSheet = true, actionsGoalId = event.goalId) }
            is GoalsEvent.DismissActionsSheet -> _uiState.update { it.copy(showActionsSheet = false, actionsGoalId = null) }
            is GoalsEvent.RestoreGoal -> restoreGoal(event.goalId)
            is GoalsEvent.DeleteGoalPermanently -> deleteGoalPermanently(event.goalId)
            is GoalsEvent.ShowDeleteGoalConfirm -> showDeleteGoalConfirm(event.goalId)
            is GoalsEvent.ShowRestoreGoalConfirm -> showRestoreGoalConfirm(event.goalId)
            is GoalsEvent.DismissCompletionCelebration -> _uiState.update { it.copy(showCompletionCelebration = false, completionGoalName = "") }

            // ── Funds flows (envelope model) ────────────────────────────────
            is GoalsEvent.RequestCompleteGoal -> requestCompleteGoal(event.goalId)
            is GoalsEvent.CompleteGoalKeepFunds -> completeGoalKeepFunds(event.goalId)
            is GoalsEvent.CompleteGoalTransferFunds -> completeGoalTransferFunds(event.goalId, event.targetAccountId)
            is GoalsEvent.DeleteGoalReturnFunds -> deleteGoalPermanently(event.goalId)
            is GoalsEvent.DeleteGoalTransferFunds -> deleteGoalTransferFunds(event.goalId, event.targetAccountId)
        }
    }

    private fun createGoal(event: GoalsEvent.CreateGoal) {
        viewModelScope.launch {
            val goal = Goal(
                id = UUID.randomUUID().toString(), name = event.name, targetAmount = event.targetAmount,
                deadline = event.deadline, status = GoalStatus.ACTIVE, autoAllocate = false,
                allocationPriority = 0, iconName = event.iconName, color = event.color, notes = event.notes,
                reminderEnabled = event.reminderEnabled,
                createdAt = Instant.now(), updatedAt = Instant.now()
            )
            goalRepository.createGoal(goal).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "Goal created successfully") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_create_goal)) } }
        }
    }

    private fun updateGoal(event: GoalsEvent.UpdateGoal) {
        viewModelScope.launch {
            val existingGoal = findGoalAcrossAll(event.id)
            if (existingGoal == null) {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, error = "Goal not found") }
                return@launch
            }
            val updatedGoal = existingGoal.copy(name = event.name, targetAmount = event.targetAmount, deadline = event.deadline, iconName = event.iconName, color = event.color, notes = event.notes, reminderEnabled = event.reminderEnabled, updatedAt = Instant.now())
            goalRepository.updateGoal(updatedGoal).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "Goal updated") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_update_goal)) } }
        }
    }

    private fun updateGoalStatus(goalId: String, status: GoalStatus) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goalId, status).onSuccess {
                val message = when (status) { GoalStatus.COMPLETED -> "Congratulations! Goal completed!"; GoalStatus.PAUSED -> "Goal paused"; GoalStatus.ACTIVE -> "Goal resumed"; GoalStatus.ARCHIVED -> "Goal archived" }
                _uiState.update { it.copy(
                    snackbarMessage = message,
                    showActionsSheet = false,
                    actionsGoalId = null
                ) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun archiveGoal(goalId: String) {
        viewModelScope.launch {
            goalRepository.archiveGoal(goalId).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, showActionsSheet = false, actionsGoalId = null, snackbarMessage = "Goal archived") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun restoreGoal(goalId: String) {
        viewModelScope.launch {
            goalRepository.restoreGoal(goalId).onSuccess {
                _uiState.update { it.copy(
                    dialogState = GoalsDialogState.None,
                    showArchivedSheet = false,
                    snackbarMessage = "Goal restored to active"
                ) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_restore_goal)) } }
        }
    }

    private fun deleteGoalPermanently(goalId: String) {
        viewModelScope.launch {
            goalRepository.deleteGoalPermanently(goalId).onSuccess {
                _uiState.update { it.copy(
                    dialogState = GoalsDialogState.None,
                    showArchivedSheet = false,
                    snackbarMessage = "Goal permanently deleted"
                ) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_delete_goal)) } }
        }
    }

    // ── Funds flows (envelope model) ────────────────────────────────────────

    /** Complete a goal: if it still holds allocated funds, ask what to do with them first. */
    private fun requestCompleteGoal(goalId: String) {
        viewModelScope.launch {
            val goal = findGoalAcrossAll(goalId) ?: return@launch
            val funds = goalRepository.getGoalFundsByAccount(goalId)
            if (funds.isEmpty()) {
                completeGoalKeepFunds(goalId)
            } else {
                _uiState.update { it.copy(
                    showActionsSheet = false, actionsGoalId = null,
                    dialogState = GoalsDialogState.CompleteGoalFunds(goalId, goal.name, goal.iconName, goal.color, funds)
                ) }
            }
        }
    }

    private fun completeGoalKeepFunds(goalId: String) {
        viewModelScope.launch {
            val goal = findGoalAcrossAll(goalId)
            goalRepository.updateGoalStatus(goalId, GoalStatus.COMPLETED).onSuccess {
                _uiState.update { it.copy(
                    dialogState = GoalsDialogState.None,
                    showActionsSheet = false, actionsGoalId = null,
                    snackbarMessage = "Congratulations! Goal completed!",
                    showCompletionCelebration = true,
                    completionGoalName = goal?.name ?: ""
                ) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_complete_goal)) } }
        }
    }

    private fun completeGoalTransferFunds(goalId: String, targetAccountId: String) {
        viewModelScope.launch {
            val goal = findGoalAcrossAll(goalId)
            goalRepository.completeGoalWithTransfer(goalId, targetAccountId).onSuccess {
                _uiState.update { it.copy(
                    dialogState = GoalsDialogState.None,
                    showActionsSheet = false, actionsGoalId = null,
                    snackbarMessage = "Goal completed — funds transferred",
                    showCompletionCelebration = true,
                    completionGoalName = goal?.name ?: ""
                ) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_transfer_funds)) } }
        }
    }

    private fun deleteGoalTransferFunds(goalId: String, targetAccountId: String) {
        viewModelScope.launch {
            goalRepository.deleteGoalWithTransfer(goalId, targetAccountId).onSuccess {
                _uiState.update { it.copy(
                    dialogState = GoalsDialogState.None,
                    showArchivedSheet = false,
                    snackbarMessage = "Goal deleted — funds transferred"
                ) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_delete_goal)) } }
        }
    }

    private fun contribute(event: GoalsEvent.Contribute) {
        viewModelScope.launch {
            goalRepository.contribute(goalId = event.goalId, accountId = event.accountId, amount = event.amount, type = ContributionType.MANUAL, notes = event.notes).onSuccess { contribution ->
                val goal = _uiState.value.goals.find { it.id == event.goalId }
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "${NumberFormatter.formatCurrency(event.amount)} added to ${goal?.name ?: "goal"}") }
                // The repository auto-completes a goal when it reaches its target.
                // If that happened and funds are allocated, ask what to do with them.
                val refreshed = goalRepository.getGoalById(event.goalId)
                if (refreshed != null && refreshed.isCompleted && !(goal?.isCompleted ?: false)) {
                    val funds = goalRepository.getGoalFundsByAccount(event.goalId)
                    if (funds.isNotEmpty()) {
                        _uiState.update { it.copy(dialogState = GoalsDialogState.CompleteGoalFunds(event.goalId, refreshed.name, refreshed.iconName, refreshed.color, funds)) }
                    }
                }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_contribute)) } }
        }
    }

    private fun withdraw(event: GoalsEvent.Withdraw) {
        viewModelScope.launch {
            goalRepository.withdraw(goalId = event.goalId, accountId = event.accountId, amount = event.amount, notes = event.notes).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "${NumberFormatter.formatCurrency(event.amount)} withdrawn") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.error_withdraw)) } }
        }
    }

    private fun linkAccount(event: GoalsEvent.LinkAccount) {
        viewModelScope.launch {
            goalRepository.linkAccountToGoal(goalId = event.goalId, accountId = event.accountId, allocationPercent = event.allocationPercent, isPrimary = event.isPrimary).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "Account linked to goal") }
                loadLinkedAccounts(event.goalId)
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun unlinkAccount(goalId: String, accountId: String) {
        viewModelScope.launch {
            goalRepository.unlinkAccountFromGoal(goalId, accountId).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Account unlinked") }
                loadLinkedAccounts(goalId)
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun setPrimaryAccount(goalId: String, accountId: String) {
        viewModelScope.launch { goalRepository.linkAccountToGoal(goalId, accountId, isPrimary = true); loadLinkedAccounts(goalId) }
    }

    private fun loadLinkedAccounts(goalId: String) {
        viewModelScope.launch {
            goalRepository.getLinkedAccounts(goalId).first().let { accounts ->
                _uiState.update { state -> state.copy(linkedAccounts = state.linkedAccounts + (goalId to accounts)) }
            }
        }
    }

    private fun setDailyTarget(amount: Double) {
        viewModelScope.launch {
            goalRepository.setDailyTarget(amount).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = if (amount > 0) "Daily target set to ${NumberFormatter.formatCurrency(amount)}" else "Daily target cleared") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun clearDailyTarget() {
        viewModelScope.launch {
            goalRepository.clearDailyTarget().onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "Daily target cleared") }
            }
        }
    }

    private fun addAutoAllocationRule(rule: AutoAllocationRule) {
        viewModelScope.launch {
            goalRepository.addAutoAllocationRule(rule).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "Auto-allocation rule added") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun updateAutoAllocationRule(rule: AutoAllocationRule) {
        viewModelScope.launch {
            goalRepository.updateAutoAllocationRule(rule).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "Rule updated") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun deleteAutoAllocationRule(ruleId: String) {
        viewModelScope.launch {
            goalRepository.deleteAutoAllocationRule(ruleId).onSuccess {
                _uiState.update { it.copy(dialogState = GoalsDialogState.None, snackbarMessage = "Rule deleted") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun toggleAutoAllocationRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch { goalRepository.setRuleEnabled(ruleId, enabled) }
    }

    private fun setGoalAutoAllocate(goalId: String, enabled: Boolean) {
        viewModelScope.launch { goalRepository.setAutoAllocate(goalId, enabled) }
    }

    private fun dismissDialog() { _uiState.update { it.copy(dialogState = GoalsDialogState.None) } }
    private fun showAddGoalDialog(prefillDeadline: LocalDate?) { _uiState.update { it.copy(dialogState = GoalsDialogState.AddGoal(deadline = prefillDeadline)) } }
    private fun showEditGoalDialog(goalId: String) { val goal = findGoalAcrossAll(goalId) ?: return; _uiState.update { it.copy(dialogState = GoalsDialogState.EditGoal(goal)) } }
    private fun showArchiveGoalDialog(goalId: String) { val goal = _uiState.value.goals.find { it.id == goalId } ?: return; _uiState.update { it.copy(dialogState = GoalsDialogState.ArchiveGoal(goalId, goal.name)) } }
    private fun showContributeDialog(goalId: String) { val defaultAccountId = _uiState.value.accounts.firstOrNull()?.id; _uiState.update { it.copy(dialogState = GoalsDialogState.Contribute(goalId, defaultAccountId)) } }
    private fun showWithdrawDialog(goalId: String) { val defaultAccountId = _uiState.value.accounts.firstOrNull()?.id; _uiState.update { it.copy(dialogState = GoalsDialogState.Withdraw(goalId, defaultAccountId)) } }
    private fun showLinkAccountDialog(goalId: String) { _uiState.update { it.copy(dialogState = GoalsDialogState.LinkAccount(goalId)) } }
    private fun showSelectAccountDialog(goalId: String, action: String) { _uiState.update { it.copy(dialogState = GoalsDialogState.SelectAccount(goalId, when (action) { "contribute" -> AccountAction.CONTRIBUTE; "withdraw" -> AccountAction.WITHDRAW; else -> AccountAction.CONTRIBUTE })) } }
    private fun showSetDailyTargetDialog() { val currentAmount = _uiState.value.dailyTarget.targetAmount; _uiState.update { it.copy(dialogState = GoalsDialogState.SetDailyTarget(if (currentAmount > 0) currentAmount.toString() else "")) } }
    private fun showAddAutoAllocationRuleDialog(goalId: String?) { _uiState.update { it.copy(dialogState = GoalsDialogState.AddAutoAllocationRule(goalId)) } }
    private fun showEditAutoAllocationRuleDialog(rule: AutoAllocationRule) { _uiState.update { it.copy(dialogState = GoalsDialogState.EditAutoAllocationRule(rule)) } }
    private fun showGoalDetail(goalId: String) {
        viewModelScope.launch {
            _uiState.update {
                val goal = it.goals.find { g -> g.id == goalId }
                val contributions = goalRepository.getContributionsByGoal(goalId).first()
                it.copy(selectedGoal = goal, selectedGoalContributions = contributions, dialogState = GoalsDialogState.GoalDetail(goalId))
            }
        }
    }

    private fun showDeleteGoalConfirm(goalId: String) {
        viewModelScope.launch {
            val goal = findGoalAcrossAll(goalId) ?: return@launch
            val funds = goalRepository.getGoalFundsByAccount(goalId)
            _uiState.update {
                it.copy(dialogState = if (funds.isEmpty()) {
                    GoalsDialogState.DeleteGoalConfirm(goalId, goal.name)
                } else {
                    GoalsDialogState.DeleteGoalFunds(goalId, goal.name, goal.iconName, goal.color, funds)
                })
            }
        }
    }

    private fun showRestoreGoalConfirm(goalId: String) {
        val goal = _uiState.value.archivedGoals.find { it.id == goalId } ?: return
        _uiState.update { it.copy(dialogState = GoalsDialogState.RestoreGoalConfirm(goalId, goal.name)) }
    }

    /** Find a goal across all lists (active, paused, completed, archived). */
    private fun findGoalAcrossAll(goalId: String): Goal? {
        val state = _uiState.value
        return state.activeGoals.find { it.id == goalId }
            ?: state.pausedGoals.find { it.id == goalId }
            ?: state.completedGoals.find { it.id == goalId }
            ?: state.archivedGoals.find { it.id == goalId }
            ?: state.goals.find { it.id == goalId }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearSnackbar() { _uiState.update { it.copy(snackbarMessage = null) } }
}
