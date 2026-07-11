package com.example.insightku.feature.planning.goal.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.feature.planning.goal.data.local.dao.AutoAllocationRuleDao
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import com.example.insightku.feature.planning.goal.domain.model.Goal
import android.content.Context
import com.example.insightku.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private const val CONTRIBUTION_PAGE_SIZE = 20

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
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
        }
    }

    private fun loadGoalData(goalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                launch { goalRepository.hasUnsyncedGoalsFlow().collect { hasUnsynced -> _uiState.update { it.copy(hasUnsyncedChanges = hasUnsynced) } } }
                launch { autoAllocationRuleDao.getRulesByGoal(goalId).collect { entities -> val rules = entities.map { AutoAllocationRule.fromEntity(it) }; _uiState.update { it.copy(allocationRules = rules) } } }
                launch { categoryRepository.getAllCategories().collect { categories -> val expenseInfo = categories.filter { it.type == CategoryType.EXPENSE && !it.isSystemCategory }.map { CategoryInfo(id = it.id, name = it.name) }; _uiState.update { it.copy(expenseCategories = expenseInfo) } } }
                combine(
                    goalRepository.getGoalByIdFlow(goalId),
                    goalRepository.getContributionsByGoal(goalId),
                    goalRepository.getLinkedAccounts(goalId),
                    accountRepository.getAllAccounts()
                ) { goal, contributions, linkedAccountEntities, allAccounts ->
                    val accountMap = allAccounts.associateBy { it.id }
                    val linkedAccountList = linkedAccountEntities.mapNotNull { entity -> accountMap[entity.accountId] }
                    val contributionSummary = calculateContributionSummary(contributions)
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
        _uiState.update { it.copy(showContributeDialog = false, showWithdrawDialog = false, showEditGoalDialog = false, showDeleteConfirmDialog = false, showArchiveConfirmDialog = false, showAutoAllocationDialog = false, editingAutoAllocationRule = null) }
    }

    private fun selectAccount(accountId: String) { _uiState.update { it.copy(selectedAccountId = accountId, isInsufficientFunds = false, shortfall = 0.0) } }

    private fun updateAmount(amount: String) {
        val state = _uiState.value; val account = state.selectedAccountId?.let { state.accountMap[it] }; val enteredAmount = amount.toDoubleOrNull() ?: 0.0; val insufficientFunds = account != null && enteredAmount > 0 && enteredAmount > account.balance; val shortfall = if (insufficientFunds) enteredAmount - (account?.balance ?: 0.0) else 0.0
        _uiState.update { it.copy(contributionAmount = amount, isInsufficientFunds = insufficientFunds, shortfall = shortfall) }
    }

    private fun updateNotes(notes: String) { _uiState.update { it.copy(contributionNotes = notes) } }

    private fun submitContribution() {
        val state = _uiState.value; val goal = state.goal ?: return; val accountId = state.selectedAccountId ?: return; val amount = state.contributionAmount.toDoubleOrNull() ?: return
        if (amount <= 0) { _uiState.update { it.copy(error = context.getString(R.string.goal_amount_zero)) }; return }
        val account = state.accountMap[accountId]
        if (account != null && amount > account.balance) { _uiState.update { it.copy(isInsufficientFunds = true, shortfall = amount - account.balance) }; return }
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

    private fun archiveGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch { goalRepository.archiveGoal(goal.id).onSuccess { _uiState.update { it.copy(showArchiveConfirmDialog = false, snackbarMessage = context.getString(R.string.goal_archived)) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_archive_failed)) } } }
    }

    private fun deleteGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch { goalRepository.updateGoalStatus(goal.id, GoalStatus.ARCHIVED).onSuccess { _uiState.update { it.copy(showDeleteConfirmDialog = false, snackbarMessage = context.getString(R.string.goal_deleted)) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: context.getString(R.string.goal_delete_failed)) } } }
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
        events.add(GoalTimelineEvent(id = "created-${goal.id}", type = TimelineEventType.GOAL_CREATED, title = context.getString(R.string.goal_timeline_created), description = context.getString(R.string.goal_timeline_created_desc, goal.name), date = goal.createdAt))
        val sortedContributions = contributions.sortedBy { it.createdAt }
        val firstContribution = sortedContributions.firstOrNull { it.isAddition }
        if (firstContribution != null) events.add(GoalTimelineEvent(id = "first-${goal.id}", type = TimelineEventType.FIRST_CONTRIBUTION, title = context.getString(R.string.goal_timeline_first_contribution), description = context.getString(R.string.goal_timeline_first_contribution_desc), date = firstContribution.createdAt, amount = firstContribution.amount))
        val progressPercent = goal.progressPercent
        if (progressPercent >= 25) { sortedContributions.find { calculateProgressAtTime(it, goal.targetAmount, contributions) in 25.0..49.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_25, 25)) } }
        if (progressPercent >= 50) { sortedContributions.find { calculateProgressAtTime(it, goal.targetAmount, contributions) in 50.0..74.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_50, 50)) } }
        if (progressPercent >= 75) { sortedContributions.find { calculateProgressAtTime(it, goal.targetAmount, contributions) in 75.0..89.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_75, 75)) } }
        if (progressPercent >= 90 && progressPercent < 100) { sortedContributions.find { calculateProgressAtTime(it, goal.targetAmount, contributions) in 90.0..99.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_90, 90)) } }
        if (goal.isCompleted) { val completedContribution = sortedContributions.lastOrNull { it.isAddition }; events.add(GoalTimelineEvent(id = "completed-${goal.id}", type = TimelineEventType.GOAL_COMPLETED, title = context.getString(R.string.goal_timeline_completed), description = context.getString(R.string.goal_timeline_completed_desc), date = completedContribution?.createdAt ?: goal.updatedAt)) }
        return events.sortedByDescending { it.date }
    }

    private fun calculateProgressAtTime(contribution: Contribution, targetAmount: Double, allContributions: List<Contribution>): Double {
        val contributionsUpTo = allContributions.filter { it.createdAt <= contribution.createdAt && it.type != ContributionType.WITHDRAWAL }
        val amount = contributionsUpTo.sumOf { kotlin.math.abs(it.amount) }
        return if (targetAmount > 0) (amount / targetAmount) * 100 else 0.0
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

private data class ContributionSummary(val totalCount: Int, val latest: Contribution?, val average: Double, val lastActivity: Instant?)
