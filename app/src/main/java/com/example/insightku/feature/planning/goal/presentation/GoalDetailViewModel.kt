package com.example.insightku.feature.planning.goal.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.feature.planning.goal.data.local.dao.AutoAllocationRuleDao
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import com.example.insightku.feature.planning.goal.domain.model.Goal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private const val CONTRIBUTION_PAGE_SIZE = 20

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val autoAllocationRuleDao: AutoAllocationRuleDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String = savedStateHandle.get<String>("goalId") ?: ""
    private val _uiState = MutableStateFlow(GoalDetailUiState.initial())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()
    private var currentGoal: Goal? = null

    init { if (goalId.isNotEmpty()) loadGoalData(goalId) }

    fun onEvent(event: GoalDetailEvent) {
        when (event) {
            is GoalDetailEvent.LoadGoal -> loadGoalData(event.goalId)
            is GoalDetailEvent.RefreshGoal -> currentGoal?.let { refreshGoalData(it.id) }
            is GoalDetailEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is GoalDetailEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }
            is GoalDetailEvent.EditGoal -> _uiState.update { it.copy(showEditGoalDialog = true) }
            is GoalDetailEvent.ArchiveGoal -> _uiState.update { it.copy(showArchiveConfirmDialog = true) }
            is GoalDetailEvent.DeleteGoal -> _uiState.update { it.copy(showDeleteConfirmDialog = true) }
            is GoalDetailEvent.ConfirmArchive -> archiveGoal()
            is GoalDetailEvent.ConfirmDelete -> deleteGoal()
            is GoalDetailEvent.ShowContributeDialog -> showContributeDialog()
            is GoalDetailEvent.ShowWithdrawDialog -> showWithdrawDialog()
            is GoalDetailEvent.DismissDialog -> dismissDialog()
            is GoalDetailEvent.ShowAccountPicker -> _uiState.update { it.copy(showAccountPicker = true) }
            is GoalDetailEvent.HideAccountPicker -> _uiState.update { it.copy(showAccountPicker = false) }
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
            is GoalDetailEvent.DeleteAutoAllocationRule -> showDeleteAutoAllocationConfirm(event.ruleId)
            is GoalDetailEvent.ShowDeleteAutoAllocationConfirm -> showDeleteAutoAllocationConfirm(event.ruleId)
            is GoalDetailEvent.ConfirmDeleteAutoAllocationRule -> confirmDeleteAutoAllocationRule()
            is GoalDetailEvent.CancelDeleteAutoAllocationRule -> cancelDeleteAutoAllocationRule()
            is GoalDetailEvent.ToggleAutoAllocationRule -> toggleAutoAllocationRule(event.ruleId, event.enabled)
        }
    }

    private fun loadGoalData(goalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val accounts = accountRepository.getAllAccounts().first()
                val accountMap = accounts.associateBy { it.id }
                launch { goalRepository.hasUnsyncedGoalsFlow().collect { hasUnsynced -> _uiState.update { it.copy(hasUnsyncedChanges = hasUnsynced) } } }
                launch { autoAllocationRuleDao.getRulesByGoal(goalId).collect { entities -> val rules = entities.map { AutoAllocationRule.fromEntity(it) }; _uiState.update { it.copy(allocationRules = rules) } } }
                goalRepository.getGoalByIdFlow(goalId).combine(goalRepository.getContributionsByGoal(goalId)) { goal, contributions -> Pair(goal, contributions) }.combine(goalRepository.getLinkedAccounts(goalId)) { (goal, contributions), linkedAccounts -> Triple(goal, contributions, linkedAccounts) }.collect { (goal, contributions, linkedAccountEntities) ->
                    currentGoal = goal
                    if (goal == null) { _uiState.update { it.copy(isLoading = false, error = "Goal not found") }; return@collect }
                    val linkedAccountList = linkedAccountEntities.mapNotNull { entity -> accountMap[entity.accountId] }
                    val contributionSummary = calculateContributionSummary(contributions)
                    val timelineEvents = generateTimelineEvents(goal, contributions)
                    _uiState.update { current -> current.copy(goal = goal, contributions = contributions.take(CONTRIBUTION_PAGE_SIZE), linkedAccounts = linkedAccountList, accountMap = accountMap, totalContributions = contributionSummary.totalCount, latestContribution = contributionSummary.latest, averageContribution = contributionSummary.average, lastActivityDate = contributionSummary.lastActivity, timelineEvents = timelineEvents, isLoading = false, hasMoreContributions = contributions.size > CONTRIBUTION_PAGE_SIZE, hasUnsyncedChanges = current.hasUnsyncedChanges) }
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
        _uiState.update { it.copy(showContributeDialog = false, showWithdrawDialog = false, showEditGoalDialog = false, showDeleteConfirmDialog = false, showArchiveConfirmDialog = false, showAccountPicker = false, showAutoAllocationDialog = false, editingAutoAllocationRule = null) }
    }

    private fun selectAccount(accountId: String) { _uiState.update { it.copy(selectedAccountId = accountId, isInsufficientFunds = false, shortfall = 0.0) } }

    private fun updateAmount(amount: String) {
        val state = _uiState.value; val account = state.selectedAccountId?.let { state.accountMap[it] }; val enteredAmount = amount.toDoubleOrNull() ?: 0.0; val insufficientFunds = account != null && enteredAmount > 0 && enteredAmount > account.balance; val shortfall = if (insufficientFunds) enteredAmount - (account?.balance ?: 0.0) else 0.0
        _uiState.update { it.copy(contributionAmount = amount, isInsufficientFunds = insufficientFunds, shortfall = shortfall) }
    }

    private fun updateNotes(notes: String) { _uiState.update { it.copy(contributionNotes = notes) } }

    private fun submitContribution() {
        val state = _uiState.value; val goal = state.goal ?: return; val accountId = state.selectedAccountId ?: return; val amount = state.contributionAmount.toDoubleOrNull() ?: return
        if (amount <= 0) { _uiState.update { it.copy(error = "Amount must be greater than zero") }; return }
        val account = state.accountMap[accountId]
        if (account != null && amount > account.balance) { _uiState.update { it.copy(isInsufficientFunds = true, shortfall = amount - account.balance) }; return }
        _uiState.update { it.copy(isSubmitting = true, isInsufficientFunds = false, shortfall = 0.0) }
        viewModelScope.launch {
            goalRepository.contribute(goalId = goal.id, accountId = accountId, amount = amount, type = ContributionType.MANUAL, notes = state.contributionNotes).onSuccess {
                _uiState.update { it.copy(showContributeDialog = false, isSubmitting = false, showSuccessAnimation = true, successMessage = "Contribution added successfully") }
                kotlinx.coroutines.delay(2000); _uiState.update { it.copy(showSuccessAnimation = false, successMessage = "") }
            }.onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to add contribution") } }
        }
    }

    private fun submitWithdrawal() {
        val state = _uiState.value; val goal = state.goal ?: return; val accountId = state.selectedAccountId ?: return; val amount = state.contributionAmount.toDoubleOrNull() ?: return
        if (amount <= 0) { _uiState.update { it.copy(error = "Amount must be greater than zero") }; return }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            goalRepository.withdraw(goalId = goal.id, accountId = accountId, amount = amount, notes = state.contributionNotes).onSuccess {
                _uiState.update { it.copy(showWithdrawDialog = false, isSubmitting = false, showSuccessAnimation = true, successMessage = "Withdrawal successful") }
                kotlinx.coroutines.delay(2000); _uiState.update { it.copy(showSuccessAnimation = false, successMessage = "") }
            }.onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to withdraw") } }
        }
    }

    private fun archiveGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch { goalRepository.archiveGoal(goal.id).onSuccess { _uiState.update { it.copy(showArchiveConfirmDialog = false, snackbarMessage = "Goal archived") } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to archive goal") } } }
    }

    private fun deleteGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch { goalRepository.updateGoalStatus(goal.id, GoalStatus.ARCHIVED).onSuccess { _uiState.update { it.copy(showDeleteConfirmDialog = false, snackbarMessage = "Goal deleted") } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to delete goal") } } }
    }

    private fun showAutoAllocationDialog() { _uiState.update { it.copy(showAutoAllocationDialog = true, editingAutoAllocationRule = null) } }
    private fun showEditAutoAllocationRule(rule: AutoAllocationRule) { _uiState.update { it.copy(showAutoAllocationDialog = true, editingAutoAllocationRule = rule) } }
    private fun addAutoAllocationRule(rule: AutoAllocationRule) { viewModelScope.launch { goalRepository.addAutoAllocationRule(rule).onSuccess { _uiState.update { it.copy(showAutoAllocationDialog = false, editingAutoAllocationRule = null) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to add rule") } } } }
    private fun updateAutoAllocationRule(rule: AutoAllocationRule) { viewModelScope.launch { goalRepository.updateAutoAllocationRule(rule).onSuccess { _uiState.update { it.copy(showAutoAllocationDialog = false, editingAutoAllocationRule = null) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to update rule") } } } }
    private fun showDeleteAutoAllocationConfirm(ruleId: String) { _uiState.update { it.copy(showDeleteAutoAllocationRuleConfirm = true, pendingDeleteAutoAllocationRuleId = ruleId) } }
    private fun confirmDeleteAutoAllocationRule() { val ruleId = _uiState.value.pendingDeleteAutoAllocationRuleId ?: return; _uiState.update { it.copy(showDeleteAutoAllocationRuleConfirm = false, pendingDeleteAutoAllocationRuleId = null, showAutoAllocationDialog = false, editingAutoAllocationRule = null) }; viewModelScope.launch { goalRepository.deleteAutoAllocationRule(ruleId).onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to delete rule") } } } }
    private fun cancelDeleteAutoAllocationRule() { _uiState.update { it.copy(showDeleteAutoAllocationRuleConfirm = false, pendingDeleteAutoAllocationRuleId = null) } }
    private fun toggleAutoAllocationRule(ruleId: String, enabled: Boolean) { viewModelScope.launch { goalRepository.setRuleEnabled(ruleId, enabled) } }

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

    private fun calculateContributionSummary(contributions: List<Contribution>): ContributionSummary {
        if (contributions.isEmpty()) return ContributionSummary(0, null, 0.0, null)
        val additions = contributions.filter { it.isAddition }; val totalCount = additions.size
        val sorted = contributions.sortedByDescending { it.createdAt }; val latest = sorted.firstOrNull(); val lastActivity = latest?.createdAt
        val average = if (additions.isNotEmpty()) additions.sumOf { kotlin.math.abs(it.amount) } / additions.size else 0.0
        return ContributionSummary(totalCount = totalCount, latest = latest, average = average, lastActivity = lastActivity)
    }

    private fun generateTimelineEvents(goal: Goal, contributions: List<Contribution>): List<GoalTimelineEvent> {
        val events = mutableListOf<GoalTimelineEvent>()
        events.add(GoalTimelineEvent(id = "created-${goal.id}", type = TimelineEventType.GOAL_CREATED, title = "Goal Created", description = "\"${goal.name}\" was created", date = goal.createdAt))
        val sortedContributions = contributions.sortedBy { it.createdAt }
        val firstContribution = sortedContributions.firstOrNull { it.isAddition }
        if (firstContribution != null) events.add(GoalTimelineEvent(id = "first-${goal.id}", type = TimelineEventType.FIRST_CONTRIBUTION, title = "First Contribution", description = "Started saving towards the goal", date = firstContribution.createdAt, amount = firstContribution.amount))
        val progressPercent = goal.progressPercent
        if (progressPercent >= 25) { sortedContributions.find { calculateProgressAtTime(it, goal.targetAmount, contributions) in 25.0..49.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_25, 25)) } }
        if (progressPercent >= 50) { sortedContributions.find { calculateProgressAtTime(it, goal.targetAmount, contributions) in 50.0..74.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_50, 50)) } }
        if (progressPercent >= 75) { sortedContributions.find { calculateProgressAtTime(it, goal.targetAmount, contributions) in 75.0..89.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_75, 75)) } }
        if (progressPercent >= 90 && progressPercent < 100) { sortedContributions.find { calculateProgressAtTime(it, goal.targetAmount, contributions) in 90.0..99.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_90, 90)) } }
        if (goal.isCompleted) { val completedContribution = sortedContributions.lastOrNull { it.isAddition }; events.add(GoalTimelineEvent(id = "completed-${goal.id}", type = TimelineEventType.GOAL_COMPLETED, title = "Goal Achieved!", description = "Successfully reached the target amount", date = completedContribution?.createdAt ?: goal.updatedAt)) }
        return events.sortedByDescending { it.date }
    }

    private fun calculateProgressAtTime(contribution: Contribution, targetAmount: Double, allContributions: List<Contribution>): Double {
        val contributionsUpTo = allContributions.filter { it.createdAt <= contribution.createdAt && it.type != ContributionType.WITHDRAWAL }
        val amount = contributionsUpTo.sumOf { kotlin.math.abs(it.amount) }
        return if (targetAmount > 0) (amount / targetAmount) * 100 else 0.0
    }

    private fun createMilestoneEvent(contribution: Contribution, type: TimelineEventType, percent: Int): GoalTimelineEvent {
        return GoalTimelineEvent(id = "milestone-$percent-${contribution.id}", type = type, title = "$percent% Milestone", description = "Reached $percent% of the target", date = contribution.createdAt, amount = contribution.amount)
    }
}

private data class ContributionSummary(val totalCount: Int, val latest: Contribution?, val average: Double, val lastActivity: Instant?)
