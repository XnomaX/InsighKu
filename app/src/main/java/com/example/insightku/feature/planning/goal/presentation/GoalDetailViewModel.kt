package com.example.insightku.feature.planning.goal.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.example.insightku.feature.planning.goal.domain.ContributionSummary
import com.example.insightku.feature.planning.goal.domain.GoalContributionStats
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import com.example.insightku.feature.planning.goal.domain.model.Goal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    /**
     * Localized string lookup.
     *
     * `@ApplicationContext` carries the system locale, not the in-app one, so
     * every message resolved through it ignored the user's language choice. This
     * wraps it via [LocaleHelper] — the documented pattern for non-Composable
     * contexts, used by the 14 other call sites in this codebase.
     *
     * Deliberately NOT cached: a ViewModel survives the Activity recreation that
     * `AppCompatDelegate.setApplicationLocales` triggers, so a cached wrapper
     * would keep resolving the previous language after the user switches. These
     * strings are produced once per user action, never inside a loop.
     */
    private fun str(resId: Int, vararg args: Any?): String =
        LocaleHelper.wrapContext(context).getString(resId, *args)

    private val goalId: String = savedStateHandle.get<String>("goalId") ?: ""
    private val _uiState = MutableStateFlow(GoalDetailUiState.initial())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()
    private var currentGoal: Goal? = null

    init {
        if (goalId.isNotEmpty()) {
            viewModelScope.launch {
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
                    if (update.goal == null) { _uiState.update { it.copy(isLoading = false, error = str(R.string.goal_not_found)) }; return@collect }
                    val allContributions = update.contributions
                    _uiState.update { current -> current.copy(goal = update.goal, contributions = if (current.contributionPage == 0) allContributions.take(CONTRIBUTION_PAGE_SIZE) else current.contributions, linkedAccounts = update.linkedAccountList, accountMap = update.accountMap, totalContributions = update.contributionSummary.totalCount, latestContribution = update.contributionSummary.latest, averageContribution = update.contributionSummary.average, lastActivityDate = update.contributionSummary.lastActivity, timelineEvents = update.timelineEvents, isLoading = false, hasMoreContributions = allContributions.size > CONTRIBUTION_PAGE_SIZE, hasUnsyncedChanges = current.hasUnsyncedChanges) }
                }
            } catch (e: CancellationException) { throw e } catch (e: Exception) { _uiState.update { it.copy(isLoading = false, error = e.message ?: str(R.string.error_load_goal)) } }
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
        val shortfall = if (availableCash != null && enteredAmount > 0 && enteredAmount > availableCash) {
            enteredAmount - availableCash
        } else 0.0
        _uiState.update {
            it.copy(contributionAmount = amount, isInsufficientFunds = shortfall > 0, shortfall = shortfall)
        }
    }

    private fun updateNotes(notes: String) { _uiState.update { it.copy(contributionNotes = notes) } }

    private fun submitContribution() {
        val state = _uiState.value; val goal = state.goal ?: return; val accountId = state.selectedAccountId ?: return; val amount = state.contributionAmount.toDoubleOrNull() ?: return
        if (amount <= 0) { _uiState.update { it.copy(error = str(R.string.goal_amount_zero)) }; return }
        // Envelope model: validate against available cash, not raw balance
        val availableCash = state.accountAllocations[accountId]?.availableCash ?: state.accountMap[accountId]?.balance ?: 0.0
        if (amount > availableCash) { _uiState.update { it.copy(isInsufficientFunds = true, shortfall = amount - availableCash) }; return }
        _uiState.update { it.copy(isSubmitting = true, isInsufficientFunds = false, shortfall = 0.0) }
        viewModelScope.launch {
            goalRepository.contribute(goalId = goal.id, accountId = accountId, amount = amount, type = ContributionType.MANUAL, notes = state.contributionNotes).onSuccess {
                _uiState.update { it.copy(showContributeDialog = false, isSubmitting = false, showSuccessAnimation = true, successMessage = str(R.string.goal_contribution_success)) }
                delay(2.seconds); _uiState.update { it.copy(showSuccessAnimation = false, successMessage = "") }
            }.onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: str(R.string.goal_contribution_failed)) } }
        }
    }

    private fun submitWithdrawal() {
        val state = _uiState.value; val goal = state.goal ?: return; val accountId = state.selectedAccountId ?: return; val amount = state.contributionAmount.toDoubleOrNull() ?: return
        if (amount <= 0) { _uiState.update { it.copy(error = str(R.string.goal_amount_zero)) }; return }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            goalRepository.withdraw(goalId = goal.id, accountId = accountId, amount = amount, notes = state.contributionNotes).onSuccess {
                _uiState.update { it.copy(showWithdrawDialog = false, isSubmitting = false, showSuccessAnimation = true, successMessage = str(R.string.goal_withdrawal_success)) }
                delay(2.seconds); _uiState.update { it.copy(showSuccessAnimation = false, successMessage = "") }
            }.onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: str(R.string.goal_withdrawal_failed)) } }
        }
    }

    private fun pauseGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goal.id, GoalStatus.PAUSED).onSuccess {
                _uiState.update { it.copy(snackbarMessage = str(R.string.goal_paused)) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.error_pause_goal)) } }
        }
    }

    private fun resumeGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goal.id, GoalStatus.ACTIVE).onSuccess {
                _uiState.update { it.copy(snackbarMessage = str(R.string.goal_resumed)) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.error_resume_goal)) } }
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
                _uiState.update { it.copy(completeFunds = null, snackbarMessage = str(R.string.goal_completed_congrats)) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.error_complete_goal)) } }
        }
    }

    private fun completeGoalTransferFunds(targetAccountId: String) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.completeGoalWithTransfer(goal.id, targetAccountId).onSuccess {
                _uiState.update { it.copy(completeFunds = null, snackbarMessage = str(R.string.goal_completed_funds_transferred)) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.error_transfer_funds)) } }
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
                _uiState.update { it.copy(showDeleteConfirmDialog = false, deleteFunds = null, snackbarMessage = str(R.string.goal_deleted), navigateBackForGoalId = goal.id) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.goal_delete_failed)) } }
        }
    }

    private fun deleteGoalReturningFunds() = deleteGoalPermanently()

    private fun deleteGoalTransferFunds(targetAccountId: String) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            goalRepository.deleteGoalWithTransfer(goal.id, targetAccountId).onSuccess {
                _uiState.update { it.copy(deleteFunds = null, snackbarMessage = str(R.string.goal_deleted), navigateBackForGoalId = goal.id) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.goal_delete_failed)) } }
        }
    }

    private fun archiveGoal() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch { goalRepository.archiveGoal(goal.id).onSuccess { _uiState.update { it.copy(showArchiveConfirmDialog = false, snackbarMessage = str(R.string.goal_archived), navigateBackForGoalId = goal.id) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.goal_archive_failed)) } } }
    }

    private fun showAutoAllocationDialog() { _uiState.update { it.copy(showAutoAllocationDialog = true, editingAutoAllocationRule = null) } }
    private fun showEditAutoAllocationRule(rule: AutoAllocationRule) { _uiState.update { it.copy(showAutoAllocationDialog = true, editingAutoAllocationRule = rule) } }
    private fun addAutoAllocationRule(rule: AutoAllocationRule) { viewModelScope.launch { goalRepository.addAutoAllocationRule(rule).onSuccess { _uiState.update { it.copy(showAutoAllocationDialog = false, editingAutoAllocationRule = null) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.goal_add_rule_failed)) } } } }
    private fun updateAutoAllocationRule(rule: AutoAllocationRule) { viewModelScope.launch { goalRepository.updateAutoAllocationRule(rule).onSuccess { _uiState.update { it.copy(showAutoAllocationDialog = false, editingAutoAllocationRule = null) } }.onFailure { e -> _uiState.update { it.copy(error = e.message ?: str(R.string.goal_update_rule_failed)) } } } }
    private fun deleteAutoAllocationRule(ruleId: String) {
        _uiState.update { it.copy(showAutoAllocationDialog = false, editingAutoAllocationRule = null) }
        viewModelScope.launch {
            goalRepository.deleteAutoAllocationRule(ruleId).onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: str(R.string.goal_delete_rule_failed)) }
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
                _uiState.update { it.copy(showEditGoalDialog = false, snackbarMessage = str(R.string.goal_updated)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: str(R.string.error_update_goal)) }
            }
        }
    }

    private fun confirmExtendDeadline(newDeadline: LocalDate) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            val updatedGoal = goal.copy(deadline = newDeadline, updatedAt = Instant.now())
            goalRepository.updateGoal(updatedGoal).onSuccess {
                _uiState.update { it.copy(showExtendDeadlineDialog = false, snackbarMessage = str(R.string.goal_deadline_extended)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: str(R.string.goal_deadline_extend_failed)) }
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
        events.add(GoalTimelineEvent(id = "created-${goal.id}", type = TimelineEventType.GOAL_CREATED, title = str(R.string.goal_timeline_created), description = str(R.string.goal_timeline_created_desc, goal.name), date = goal.createdAt))
        val sortedContributions = contributions.sortedBy { it.createdAt }
        val firstContribution = sortedContributions.firstOrNull { it.isAddition }
        if (firstContribution != null) events.add(GoalTimelineEvent(id = "first-${goal.id}", type = TimelineEventType.FIRST_CONTRIBUTION, title = str(R.string.goal_timeline_first_contribution), description = str(R.string.goal_timeline_first_contribution_desc), date = firstContribution.createdAt, amount = firstContribution.amount))
        val progressPercent = goal.progressPercent
        if (progressPercent >= 25) { sortedContributions.find { GoalContributionStats.progressAtTime(it, goal.targetAmount, contributions) in 25.0..49.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_25, 25)) } }
        if (progressPercent >= 50) { sortedContributions.find { GoalContributionStats.progressAtTime(it, goal.targetAmount, contributions) in 50.0..74.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_50, 50)) } }
        if (progressPercent >= 75) { sortedContributions.find { GoalContributionStats.progressAtTime(it, goal.targetAmount, contributions) in 75.0..89.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_75, 75)) } }
        if (progressPercent >= 90 && progressPercent < 100) { sortedContributions.find { GoalContributionStats.progressAtTime(it, goal.targetAmount, contributions) in 90.0..99.99 }?.let { events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_90, 90)) } }
        if (goal.isCompleted) { val completedContribution = sortedContributions.lastOrNull { it.isAddition }; events.add(GoalTimelineEvent(id = "completed-${goal.id}", type = TimelineEventType.GOAL_COMPLETED, title = str(R.string.goal_timeline_completed), description = str(R.string.goal_timeline_completed_desc), date = completedContribution?.createdAt ?: goal.updatedAt)) }
        return events.sortedByDescending { it.date }
    }

    private fun createMilestoneEvent(contribution: Contribution, type: TimelineEventType, percent: Int): GoalTimelineEvent {
        return GoalTimelineEvent(id = "milestone-$percent-${contribution.id}", type = type, title = str(R.string.goal_timeline_milestone, percent), description = str(R.string.goal_timeline_milestone_desc, percent), date = contribution.createdAt, amount = contribution.amount)
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


// --- GoalDetail MVI contract (Event.kt, UiState.kt) ------------------------------

sealed class GoalDetailEvent {
    data class LoadGoal(val goalId: String) : GoalDetailEvent()
    data object RefreshGoal : GoalDetailEvent()
    data object EditGoal : GoalDetailEvent()
    data class SaveGoalEdit(
        val name: String,
        val targetAmount: Double,
        val deadline: java.time.LocalDate,
        val iconName: String,
        val color: String,
        val notes: String,
        val reminderEnabled: Boolean
    ) : GoalDetailEvent()
    data class UpdateAmount(val amount: String) : GoalDetailEvent()
    data class UpdateNotes(val notes: String) : GoalDetailEvent()
    data class SelectAccount(val accountId: String) : GoalDetailEvent()
    data object ShowContributeDialog : GoalDetailEvent()
    data object ShowWithdrawDialog : GoalDetailEvent()
    data object SubmitContribution : GoalDetailEvent()
    data object SubmitWithdrawal : GoalDetailEvent()
    data object DismissDialog : GoalDetailEvent()
    data object ClearSnackbar : GoalDetailEvent()
    data object ClearError : GoalDetailEvent()
    data object ShowAutoAllocationDialog : GoalDetailEvent()
    data class ShowEditAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class AddAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class UpdateAutoAllocationRule(val rule: AutoAllocationRule) : GoalDetailEvent()
    data class DeleteAutoAllocationRule(val ruleId: String) : GoalDetailEvent()
    data class ToggleAutoAllocationRule(val ruleId: String, val enabled: Boolean) : GoalDetailEvent()

    data object LoadMoreContributions : GoalDetailEvent()
    data object ArchiveGoal : GoalDetailEvent()
    data object DeleteGoal : GoalDetailEvent()
    data object ConfirmArchive : GoalDetailEvent()
    data object ConfirmDelete : GoalDetailEvent()

    // ── Status Actions ───────────────────────────────────────────────────
    data object PauseGoal : GoalDetailEvent()
    data object ResumeGoal : GoalDetailEvent()
    data object CompleteGoal : GoalDetailEvent()

    // ── Extend Deadline ────────────────────────────────────────────────────
    data object ShowExtendDeadlineDialog : GoalDetailEvent()
    data class ConfirmExtendDeadline(val newDeadline: java.time.LocalDate) : GoalDetailEvent()

    // ── Funds flows (envelope model) ────────────────────────────────────────
    data object CompleteGoalKeepFunds : GoalDetailEvent()
    data class CompleteGoalTransferFunds(val targetAccountId: String) : GoalDetailEvent()
    data object DeleteGoalReturnFunds : GoalDetailEvent()
    data class DeleteGoalTransferFunds(val targetAccountId: String) : GoalDetailEvent()
}

data class GoalDetailUiState(
    val isLoading: Boolean = true,
    val goal: Goal? = null,
    val contributions: List<Contribution> = emptyList(),
    val timelineEvents: List<GoalTimelineEvent> = emptyList(),
    val allocationRules: List<AutoAllocationRule> = emptyList(),
    val linkedAccounts: List<com.example.insightku.core.data.model.Account> = emptyList(),
    val accountMap: Map<String, com.example.insightku.core.data.model.Account> = emptyMap(),
    val accountAllocations: Map<String, com.example.insightku.core.domain.model.AccountAllocation> = emptyMap(),
    val totalContributions: Int = 0,
    val latestContribution: Contribution? = null,
    val averageContribution: Double = 0.0,
    val lastActivityDate: java.time.Instant? = null,
    val hasGoal: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
    /**
     * Id of the goal that was just archived or permanently deleted, or null.
     *
     * The screen pops back when this matches the goal it is showing. Scoped by id rather than
     * a plain Boolean because this ViewModel is not always scoped to the detail screen:
     * `GoalsScreen` renders `GoalDetailScreen` inline without passing a `viewModel`, so
     * `hiltViewModel()` resolves to the enclosing navigation back-stack entry, which survives
     * across dialog open/close cycles. A sticky Boolean would still be true when the *next*
     * goal is opened and would immediately close it. Matching on the id makes a stale value
     * inert.
     *
     * Replaces the previous check that compared `snackbarMessage` against the English literals
     * "Goal archived"/"Goal deleted" — that broke navigation for every non-English language.
     */
    val navigateBackForGoalId: String? = null,
    val showContributeDialog: Boolean = false,
    val showWithdrawDialog: Boolean = false,
    val showSuccessAnimation: Boolean = false,
    val successMessage: String = "",
    val showEditGoalDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showArchiveConfirmDialog: Boolean = false,
    val showAutoAllocationDialog: Boolean = false,
    val editingAutoAllocationRule: AutoAllocationRule? = null,
    val contributionAmount: String = "",
    val contributionNotes: String = "",
    val selectedAccountId: String? = null,
    val isSubmitting: Boolean = false,
    val isInsufficientFunds: Boolean = false,
    val shortfall: Double = 0.0,
    val hasUnsyncedChanges: Boolean = false,
    val contributionPage: Int = 0,
    val isLoadingMore: Boolean = false,
    val hasMoreContributions: Boolean = false,
    val showExtendDeadlineDialog: Boolean = false,
    val expenseCategories: List<CategoryInfo> = emptyList(),
    // ── Funds dialogs (envelope model): non-null = show dialog ──────────────
    val completeFunds: Map<String, Double>? = null,
    val deleteFunds: Map<String, Double>? = null
) {
    companion object { fun initial() = GoalDetailUiState() }
}

data class GoalTimelineEvent(
    val id: String,
    val type: TimelineEventType,
    val title: String,
    val description: String,
    val date: java.time.Instant,
    val amount: Double? = null
)

enum class TimelineEventType {
    GOAL_CREATED, FIRST_CONTRIBUTION, MILESTONE_25, MILESTONE_50, MILESTONE_75, MILESTONE_90, GOAL_COMPLETED, WITHDRAWAL
}
