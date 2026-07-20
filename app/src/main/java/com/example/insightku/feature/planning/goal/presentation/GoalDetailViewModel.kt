package com.example.insightku.feature.planning.goal.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import com.example.insightku.feature.planning.goal.domain.model.Goal
import com.example.insightku.feature.planning.goal.domain.ContributionSummary
import com.example.insightku.feature.planning.goal.domain.GoalContributionStats
import android.content.Context
import com.example.insightku.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

private const val CONTRIBUTION_PAGE_SIZE = 20

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val accountAllocationRepository: com.example.insightku.core.data.repository.AccountAllocationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String = savedStateHandle.get<String>("goalId") ?: ""
    private val _uiState = MutableStateFlow(GoalDetailUiState.initial())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()
    private var currentGoal: Goal? = null

    init {
        if (goalId.isNotEmpty()) {
            viewModelScope.launch {
                // Fix legacy epoch timestamps BEFORE loading data.
                // This prevents contributions with createdAt=0L from appearing
                // at wrong positions on the savings activity chart.
                goalRepository.fixLegacyContributionTimestamps()
                loadGoalData(goalId)
            }
        }
    }

    fun onEvent(event: GoalDetailEvent) {
        when (event) {
            is GoalDetailEvent.LoadGoal -> loadGoalData(event.goalId)
            is GoalDetailEvent.RefreshGoal -> currentGoal?.let { refreshGoalData(it.id) }
            is GoalDetailEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is GoalDetailEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }
            is GoalDetailEvent.EditGoal -> _uiState.update { it.copy(showEditGoalDialog = true) }
            is GoalDetailEvent.SaveGoalEdit -> saveGoalEdit(event)
            is GoalDetailEvent.ArchiveGoal -> _uiState.update { it.copy(showArchiveConfirmDialog = true) }
            is GoalDetailEvent.DeleteGoal -> requestDeleteGoal()
            is GoalDetailEvent.ConfirmArchive -> archiveGoal()
            is GoalDetailEvent.ConfirmDelete -> deleteGoalPermanently()
            is GoalDetailEvent.ShowContributeDialog -> showContributeDialog()
            is GoalDetailEvent.ShowWithdrawDialog -> showWithdrawDialog()
            is GoalDetailEvent.DismissDialog -> dismissDialog()
            is GoalDetailEvent.SelectAccount -> selectAccount(event.accountId)
            is GoalDetailEvent.UpdateAmount -> updateAmount(event.amount)
            is GoalDetailEvent.UpdateNotes -> updateNotes(event.notes)
            is GoalDetailEvent.SubmitContribution -> submitContribution()
            is GoalDetailEvent.SubmitWithdrawal -> submitWithdrawal()
            is GoalDetailEvent.LoadMoreContributions -> loadMoreContributions()
            is GoalDetailEvent.ShowAutoAllocationDialog -> showAutoAllocationDialog()
            is GoalDetailEvent.ShowEditAutoAllocationRule -> showEditAutoAllocationRule(event.rule)
            is GoalDetailEvent.AddAutoAllocationRule -> addAutoAllocationRule(event.rule)
            is GoalDetailEvent.UpdateAutoAllocationRule -> updateAutoAllocationRule(event.rule)
            is GoalDetailEvent.DeleteAutoAllocationRule -> deleteAutoAllocationRule(event.ruleId)
            is GoalDetailEvent.ToggleAutoAllocationRule -> toggleAutoAllocationRule(event.ruleId, event.enabled)
            is GoalDetailEvent.PauseGoal -> pauseGoal()
            is GoalDetailEvent.ResumeGoal -> resumeGoal()
            is GoalDetailEvent.CompleteGoal -> completeGoal()
            is GoalDetailEvent.ShowExtendDeadlineDialog -> _uiState.update { it.copy(showExtendDeadlineDialog = true) }
            is GoalDetailEvent.ConfirmExtendDeadline -> confirmExtendDeadline(event.newDeadline)
            is GoalDetailEvent.CompleteGoalKeepFunds -> completeGoalKeepFunds()
            is GoalDetailEvent.CompleteGoalTransferFunds -> completeGoalTransferFunds(event.targetAccountId)
            is GoalDetailEvent.DeleteGoalReturnFunds -> deleteGoalReturningFunds()
            is GoalDetailEvent.DeleteGoalTransferFunds -> deleteGoalTransferFunds(event.targetAccountId)
        }
    }

    private fun loadGoalData(goalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                launch { goalRepository.hasUnsyncedGoalsFlow().collect { hasUnsynced -> _uiState.update { it.copy(hasUnsyncedChanges = hasUnsynced) } } }
                launch { goalRepository.getRulesByGoalFlow(goalId).collect { rules -> _uiState.update { it.copy(allocationRules = rules) } } }
                launch { accountAllocationRepository.getAllAccountAllocations().collect { allocations -> _uiState.update { it.copy(accountAllocations = allocations.associateBy { allocation -> allocation.account.id }) } } }
                launch { categoryRepository.getAllCategories().collect { categories -> val expenseInfo = categories.filter { it.type == CategoryType.EXPENSE && !it.isSystemCategory }.map { CategoryInfo(id = it.id, name = it.name) }; _uiState.update { it.copy(expenseCategories = expenseInfo) } } }
                combine(
                    goalRepository.getGoalByIdFlow(goalId),
                    goalRepository.getContributionsByGoal(goalId),
                    goalRepository.getLinkedAccounts(goalId),
                    accountRepository.getAllAccounts()
                ) { goal, contributions, linkedAccountEntities, allAccounts ->
                    val accountMap = allAccounts.associateBy { it.id }
                    val linkedAccountList = linkedAccountEntities.mapNotNull { entity -> accountMap[entity.accountId] }
                    val contributionSummary = GoalContributionStats.summarize(contributions)
                    val timelineEvents = if (goal != null) generateTimelineEvents(goal, contributions) else emptyList()
                    GoalDetailUpdate(goal, accountMap, linkedAccountList, contributions, contributionSummary, timelineEvents)
                }.collect { update ->
                    currentGoal = update.goal
                    if (update.goal == null) { _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.goal_not_found)) }; return@collect }
                    val allContributions = update.contributions
                    _uiState.update { current -> current.copy(goal = update.goal, contributions = if (current.contributionPage == 0) allContributions.take(CONTRIBUTION_PAGE_SIZE) else current.contributions, linkedAccounts = update.linkedAccountList, accountMap = update.accountMap, totalContributions = update.contributionSummary.totalCount, latestContribution = update.contributionSummary.latest, averageContribution = update.contributionSummary.average, lastActivityDate = update.contributionSummary.lastActivity, timelineEvents = update.timelineEvents, isLoading = false, hasMoreContributions = allContributions.size > CONTRIBUTION_PAGE_SIZE, hasUnsyncedChanges = current.hasUnsyncedChanges) }
                }
            } catch (e: Exception) { _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load goal") } }
        }
    }

    private fun refreshGoalData(goalId: String) { loadGoalData(goalId) }

    private fun showContributeDialog() {
        val defaultAccountId = _uiState.value.linkedAccounts.firstOrNull()?.id ?: _uiState.value.accountMap.keys.firstOrNull()
        _uiState.update { it.copy(showContributeDialog = true, showWithdrawDialog = false, selectedAccountId = defaultAccountId, contributionAmount = "", contributionNotes = "") }
    }

    private fun showWithdrawDialog() {
        val defaultAccountId = _uiState.value.linkedAccounts.firstOrNull()?.id ?: _uiState.value.accountMap.keys.firstOrNull()
        _uiState.update { it.copy(showContributeDialog = false, showWithdrawDialog = true, selectedAccountId = defaultAccountId, contributionAmount = "", contributionNotes = "") }
    }

    private fun dismissDialog() {
        _uiState.update { it.copy(showContributeDialog = false, showWithdrawDialog = false, showEditGoalDialog = false, showDeleteConfirmDialog = false, showArchiveConfirmDialog = false, showAutoAllocationDialog = false, showExtendDeadlineDialog = false, editingAutoAllocationRule = null, completeFunds = null, deleteFunds = null) }
    }

    private fun selectAccount(accountId: String) { _uiState.update { it.copy(selectedAccountId = accountId, isInsufficientFunds = false, shortfall = 0.0) } }

    private fun updateAmount(amount: String) {
        val state = _uiState.value
        val enteredAmount = amount.toDoubleOrNull() ?: 0.0
        // Envelope model: validate against available cash, not raw balance
        val availableCash = state.selectedAccountId?.let { id -> state.accountAllocations[id]?.availableCash }
        val insufficientFunds = availableCash != null && enteredAmount > 0 && enteredAmount > availableCash
        val shortfall = if (insufficientFunds && availableCash != null) enteredAmount - availableCash else 0.0
        _uiState.update { it.copy(contributionAmount = amount, isInsufficientFunds = insufficientFunds, shortfall = shortfall) }
    }

    private fun updateNotes(notes: String) { _uiState.update { it.copy(contributionNotes = notes) } }

    private fun submitContribution() {
        val state = _uiState.value; val goal = state.goal ?: return; val accountId = state.selectedAccountId ?: return; val amount = state.contributionAmount.toDoubleOrNull() ?: return
        if (amount <= 0) { _uiState.update { it.copy(error = context.getString(R.string.goal_amount_zero)) }; return }
        // Envelope model: validate against available cash, not raw balance
        val availableCash = state.accountAllocations[accountId]?.availableCash ?: state.accountMap[accountId]?.balance ?: 0.0
        if (amount > availableCash) { _uiState.update { it.copy(isInsufficientFunds = true, shortfall = amount - availableCash) }; return }
        _uiState.update { it.copy(isSubmitting = true, isInsufficientFunds = false, shortfall = 0.0) }
        viewModelScope.launch {
            goalRepository.contribute(goalId = goal.id, accountId = accountId, amount = amount, type = ContributionType.MANUAL, notes = state.contributionNotes).onSuccess {
                _uiState.update { it.copy(showContributeDialog = false, isSubmitting = false, showSuccessAnimation = true, successMessage = context.getString(R.string.goal_contribution_success)) }
                kotlinx.coroutines.delay(2000); _uiState.update { it.copy(showSuccessAnimation = false, successMessage = "") }
            }.onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: context.getString(R.string.goal_contribution_failed)) } }
        }
    }

    private fun submitWithdrawal() {
        val state = _uiState.value; val goal = state.goal ?: return; val accountId = state.selectedAccountId ?: return; val amount = state.contributionAmount.toDoubleOrNull() ?: return
        if (amount <= 0) { _uiState.update { it.copy(error = context.getString(R.string.goal_amount_zero)) }; return }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            goalRepository.withdraw(goalId = goal.id, accountId = accountId, amount = amount, notes = state.contributionNotes).onSuccess {
                _uiState.update { it.copy(showWithdrawDialog = false, isSubmitting = false, showSuccessAnimation = true, successMessage = context.getString(R.string.goal_withdrawal_success)) }
                kotlinx.coroutines.delay(2000); _uiState.update { it.copy(showSuccessAnimation = false, successMessage = "") }
            }.onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: context.getString(R.string.goal_withdrawal_failed)) } }
        }
    }

    private fun pauseGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goal.id, GoalStatus.PAUSED).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Goal paused") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to pause goal") } }
        }
    }

    private fun resumeGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goal.id, GoalStatus.ACTIVE).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Goal resumed") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to resume goal") } }
        }
    }

    private fun completeGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            val funds = goalRepository.getGoalFundsByAccount(goal.id)
            if (funds.isEmpty()) completeGoalKeepFunds()
            else _uiState.update { it.copy(completeFunds = funds) }
        }
    }

    private fun completeGoalKeepFunds() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goal.id, GoalStatus.COMPLETED).onSuccess {
                _uiState.update { it.copy(completeFunds = null, snackbarMessage = "Congratulations! Goal completed!") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to complete goal") } }
        }
    }

    private fun completeGoalTransferFunds(targetAccountId: String) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.completeGoalWithTransfer(goal.id, targetAccountId).onSuccess {
                _uiState.update { it.copy(completeFunds = null, snackbarMessage = "Goal completed — funds transferred") }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to transfer funds") } }
        }
    }

    /** Delete entry point: if the goal still holds funds, ask what to do with them first. */
    private fun requestDeleteGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            val funds = goalRepository.getGoalFundsByAccount(goal.id)
            if (funds.isEmpty()) _uiState.update { it.copy(showDeleteConfirmDialog = true) }
            else _uiState.update { it.copy(deleteFunds = funds) }
        }
    }

    /** Permanently delete the goal — funds return to their original accounts (envelope release). */
    private fun deleteGoalPermanently() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.deleteGoalPermanently(goal.id).onSuccess {
                _uiState.update { it.copy(showDeleteConfirmDialog = false, deleteFunds = null, snackbarMessage = context.getString(R.string.goal_deleted)) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_delete_failed)) } }
        }
    }

    private fun deleteGoalReturningFunds() = deleteGoalPermanently()

    private fun deleteGoalTransferFunds(targetAccountId: String) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.deleteGoalWithTransfer(goal.id, targetAccountId).onSuccess {
                _uiState.update { it.copy(deleteFunds = null, snackbarMessage = context.getString(R.string.goal_deleted)) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_delete_failed)) } }
        }
    }

    private fun archiveGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch { goalRepository.archiveGoal(goal.id).onSuccess { _uiState.update { it.copy(showArchiveConfirmDialog = false, snackbarMessage = context.getString(R.string.goal_archived)) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_archive_failed)) } } }
    }

    private fun showAutoAllocationDialog() { _uiState.update { it.copy(showAutoAllocationDialog = true, editingAutoAllocationRule = null) } }
    private fun showEditAutoAllocationRule(rule: AutoAllocationRule) { _uiState.update { it.copy(showAutoAllocationDialog = true, editingAutoAllocationRule = rule) } }
    private fun addAutoAllocationRule(rule: AutoAllocationRule) { viewModelScope.launch { goalRepository.addAutoAllocationRule(rule).onSuccess { _uiState.update { it.copy(showAutoAllocationDialog = false, editingAutoAllocationRule = null) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_add_rule_failed)) } } } }
    private fun updateAutoAllocationRule(rule: AutoAllocationRule) { viewModelScope.launch { goalRepository.updateAutoAllocationRule(rule).onSuccess { _uiState.update { it.copy(showAutoAllocationDialog = false, editingAutoAllocationRule = null) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_update_rule_failed)) } } } }
    private fun deleteAutoAllocationRule(ruleId: String) {
        _uiState.update { it.copy(showAutoAllocationDialog = false, editingAutoAllocationRule = null) }
        viewModelScope.launch {
            goalRepository.deleteAutoAllocationRule(ruleId).onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_delete_rule_failed)) }
            }
        }
    }
    private fun toggleAutoAllocationRule(ruleId: String, enabled: Boolean) { viewModelScope.launch { goalRepository.setRuleEnabled(ruleId, enabled) } }

    private fun saveGoalEdit(event: GoalDetailEvent.SaveGoalEdit) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            val updatedGoal = goal.copy(
                name = event.name, targetAmount = event.targetAmount, deadline = event.deadline,
                iconName = event.iconName, color = event.color, notes = event.notes,
                reminderEnabled = event.reminderEnabled, updatedAt = Instant.now()
            )
            goalRepository.updateGoal(updatedGoal).onSuccess {
                _uiState.update { it.copy(showEditGoalDialog = false, snackbarMessage = "Goal updated") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to update goal") }
            }
        }
    }

    private fun confirmExtendDeadline(newDeadline: LocalDate) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            val updatedGoal = goal.copy(deadline = newDeadline, updatedAt = Instant.now())
            goalRepository.updateGoal(updatedGoal).onSuccess {
                _uiState.update { it.copy(showExtendDeadlineDialog = false, snackbarMessage = context.getString(R.string.goal_deadline_extended)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_deadline_extend_failed)) }
            }
        }
    }

    private fun loadMoreContributions() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreContributions) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val currentPage = _uiState.value.contributionPage + 1; val goalId = _uiState.value.goal?.id ?: return@launch
            val allContributions = goalRepository.getContributionsByGoal(goalId).first()
            val startIndex = currentPage * CONTRIBUTION_PAGE_SIZE; val endIndex = minOf(startIndex + CONTRIBUTION_PAGE_SIZE, allContributions.size); val newContributions = allContributions.subList(startIndex, endIndex)
            _uiState.update { it.copy(contributions = it.contributions + newContributions, contributionPage = currentPage, hasMoreContributions = endIndex < allContributions.size, isLoadingMore = false) }
        }
    }

    private fun generateTimelineEvents(goal: Goal, contributions: List<Contribution>): List<GoalTimelineEvent> {
        val events = mutableListOf<GoalTimelineEvent>()
        events.add(GoalTimelineEvent(id = "created-${goal.id}", type = TimelineEventType.GOAL_CREATED, title = context.getString(R.string.goal_timeline_created), description = context.getString(R.string.goal_timeline_created_desc, goal.name), date = goal.createdAt))
        val sortedContributions = contributions.sortedBy { it.createdAt }
        val firstContribution = sortedContributions.firstOrNull { it.isAddition }
        if (firstContribution != null) events.add(GoalTimelineEvent(id = "first-${goal.id}", type = TimelineEventType.FIRST_CONTRIBUTION, title = context.getString(R.string.goal_timeline_first_contribution), description = context.getString(R.string.goal_timeline_first_contribution_desc), date = firstContribution.createdAt, amount = firstContribution.amount))
        val progressPercent = goal.progressPercent
        if (progressPercent >= 25) { sortedContributions.find { GoalContributionStats.progressAtTime(it, goal.targetAmount, contributions) in 25.0..49.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_25, 25)) } }
        if (progressPercent >= 50) { sortedContributions.find { GoalContributionStats.progressAtTime(it, goal.targetAmount, contributions) in 50.0..74.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_50, 50)) } }
        if (progressPercent >= 75) { sortedContributions.find { GoalContributionStats.progressAtTime(it, goal.targetAmount, contributions) in 75.0..89.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_75, 75)) } }
        if (progressPercent >= 90 && progressPercent < 100) { sortedContributions.find { GoalContributionStats.progressAtTime(it, goal.targetAmount, contributions) in 90.0..99.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_90, 90)) } }
        if (goal.isCompleted) { val completedContribution = sortedContributions.lastOrNull { it.isAddition }; events.add(GoalTimelineEvent(id = "completed-${goal.id}", type = TimelineEventType.GOAL_COMPLETED, title = context.getString(R.string.goal_timeline_completed), description = context.getString(R.string.goal_timeline_completed_desc), date = completedContribution?.createdAt ?: goal.updatedAt)) }
        return events.sortedByDescending { it.date }
    }

    private fun createMilestoneEvent(contribution: Contribution, type: TimelineEventType, percent: Int): GoalTimelineEvent {
        return GoalTimelineEvent(id = "milestone-$percent-${contribution.id}", type = type, title = context.getString(R.string.goal_timeline_milestone, percent), description = context.getString(R.string.goal_timeline_milestone_desc, percent), date = contribution.createdAt, amount = contribution.amount)
    }
}

private data class GoalDetailUpdate(
    val goal: Goal?,
    val accountMap: Map<String, Account>,
    val linkedAccountList: List<Account>,
    val contributions: List<Contribution>,
    val contributionSummary: ContributionSummary,
    val timelineEvents: List<GoalTimelineEvent>
)
