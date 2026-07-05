package com.example.insightku.feature.budgeting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.feature.budgeting.data.model.ContributionType
import com.example.insightku.feature.budgeting.data.model.GoalStatus
import com.example.insightku.feature.budgeting.data.repository.GoalRepository
import com.example.insightku.feature.budgeting.domain.model.Contribution
import com.example.insightku.feature.budgeting.domain.model.Goal
import com.example.insightku.feature.budgeting.presentation.event.GoalDetailEvent
import com.example.insightku.feature.budgeting.presentation.state.GoalDetailUiState
import com.example.insightku.feature.budgeting.presentation.state.GoalTimelineEvent
import com.example.insightku.feature.budgeting.presentation.state.TimelineEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val CONTRIBUTION_PAGE_SIZE = 20

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountDao: AccountDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String = savedStateHandle.get<String>("goalId") ?: ""

    private val _uiState = MutableStateFlow(GoalDetailUiState.initial())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    private var currentGoal: Goal? = null

    init {
        if (goalId.isNotEmpty()) {
            loadGoalData(goalId)
        }
    }

    /**
     * Handle user events.
     */
    fun onEvent(event: GoalDetailEvent) {
        when (event) {
            is GoalDetailEvent.LoadGoal -> loadGoalData(event.goalId)
            is GoalDetailEvent.RefreshGoal -> currentGoal?.let { refreshGoalData(it.id) }
            is GoalDetailEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is GoalDetailEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }

            // Goal Actions
            is GoalDetailEvent.EditGoal -> _uiState.update { it.copy(showEditGoalDialog = true) }
            is GoalDetailEvent.ArchiveGoal -> _uiState.update { it.copy(showArchiveConfirmDialog = true) }
            is GoalDetailEvent.DeleteGoal -> _uiState.update { it.copy(showDeleteConfirmDialog = true) }
            is GoalDetailEvent.ConfirmArchive -> archiveGoal()
            is GoalDetailEvent.ConfirmDelete -> deleteGoal()

            // Contribution Actions
            is GoalDetailEvent.ShowContributeDialog -> showContributeDialog()
            is GoalDetailEvent.ShowWithdrawDialog -> showWithdrawDialog()
            is GoalDetailEvent.DismissDialog -> dismissDialog()
            is GoalDetailEvent.SelectAccount -> selectAccount(event.accountId)
            is GoalDetailEvent.UpdateAmount -> updateAmount(event.amount)
            is GoalDetailEvent.UpdateNotes -> updateNotes(event.notes)
            is GoalDetailEvent.SubmitContribution -> submitContribution()
            is GoalDetailEvent.SubmitWithdrawal -> submitWithdrawal()

            // Pagination
            is GoalDetailEvent.LoadMoreContributions -> loadMoreContributions()
        }
    }

    /**
     * Load goal data by ID.
     */
    private fun loadGoalData(goalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load accounts map first
                val accounts = accountDao.getAllAccounts().first()
                val accountMap = accounts.associateBy { it.id }

                // Load goal as flow
                goalRepository.getGoalByIdFlow(goalId)
                    .combine(goalRepository.getContributionsByGoal(goalId)) { goal, contributions ->
                        Pair(goal, contributions)
                    }
                    .combine(goalRepository.getLinkedAccounts(goalId)) { (goal, contributions), linkedAccounts ->
                        Triple(goal, contributions, linkedAccounts)
                    }
                    .collect { (goal, contributions, linkedAccountEntities) ->
                        currentGoal = goal

                        if (goal == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Goal not found"
                                )
                            }
                            return@collect
                        }

                        // Get full account objects for linked accounts
                        val linkedAccountList = linkedAccountEntities.mapNotNull { entity ->
                            accountMap[entity.accountId]
                        }

                        // Calculate contribution summary
                        val contributionSummary = calculateContributionSummary(contributions)

                        // Generate timeline events
                        val timelineEvents = generateTimelineEvents(goal, contributions)

                        _uiState.update {
                            it.copy(
                                goal = goal,
                                contributions = contributions.take(CONTRIBUTION_PAGE_SIZE),
                                linkedAccounts = linkedAccountList,
                                accountMap = accountMap,
                                totalContributions = contributionSummary.totalCount,
                                latestContribution = contributionSummary.latest,
                                averageContribution = contributionSummary.average,
                                lastActivityDate = contributionSummary.lastActivity,
                                timelineEvents = timelineEvents,
                                isLoading = false,
                                hasMoreContributions = contributions.size > CONTRIBUTION_PAGE_SIZE
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load goal"
                    )
                }
            }
        }
    }

    /**
     * Refresh goal data.
     */
    private fun refreshGoalData(goalId: String) {
        loadGoalData(goalId)
    }

    /**
     * Show contribute dialog.
     */
    private fun showContributeDialog() {
        val defaultAccountId = _uiState.value.linkedAccounts.firstOrNull()?.id
            ?: _uiState.value.accountMap.keys.firstOrNull()
        _uiState.update {
            it.copy(
                showContributeDialog = true,
                showWithdrawDialog = false,
                selectedAccountId = defaultAccountId,
                contributionAmount = "",
                contributionNotes = ""
            )
        }
    }

    /**
     * Show withdraw dialog.
     */
    private fun showWithdrawDialog() {
        val defaultAccountId = _uiState.value.linkedAccounts.firstOrNull()?.id
            ?: _uiState.value.accountMap.keys.firstOrNull()
        _uiState.update {
            it.copy(
                showContributeDialog = false,
                showWithdrawDialog = true,
                selectedAccountId = defaultAccountId,
                contributionAmount = "",
                contributionNotes = ""
            )
        }
    }

    /**
     * Dismiss all dialogs.
     */
    private fun dismissDialog() {
        _uiState.update {
            it.copy(
                showContributeDialog = false,
                showWithdrawDialog = false,
                showEditGoalDialog = false,
                showDeleteConfirmDialog = false,
                showArchiveConfirmDialog = false
            )
        }
    }

    /**
     * Select account for contribution.
     */
    private fun selectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    /**
     * Update contribution amount.
     */
    private fun updateAmount(amount: String) {
        _uiState.update { it.copy(contributionAmount = amount) }
    }

    /**
     * Update contribution notes.
     */
    private fun updateNotes(notes: String) {
        _uiState.update { it.copy(contributionNotes = notes) }
    }

    /**
     * Submit contribution.
     */
    private fun submitContribution() {
        val state = _uiState.value
        val goal = state.goal ?: return
        val accountId = state.selectedAccountId ?: return
        val amount = state.contributionAmount.toDoubleOrNull() ?: return

        if (amount <= 0) {
            _uiState.update { it.copy(error = "Amount must be greater than zero") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            goalRepository.contribute(
                goalId = goal.id,
                accountId = accountId,
                amount = amount,
                type = ContributionType.MANUAL,
                notes = state.contributionNotes
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        showContributeDialog = false,
                        isSubmitting = false,
                        showSuccessAnimation = true,
                        successMessage = "Contribution added successfully"
                    )
                }
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(showSuccessAnimation = false, successMessage = "") }
            }.onFailure { e ->
                _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to add contribution") }
            }
        }
    }

    /**
     * Submit withdrawal.
     */
    private fun submitWithdrawal() {
        val state = _uiState.value
        val goal = state.goal ?: return
        val accountId = state.selectedAccountId ?: return
        val amount = state.contributionAmount.toDoubleOrNull() ?: return

        if (amount <= 0) {
            _uiState.update { it.copy(error = "Amount must be greater than zero") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            goalRepository.withdraw(
                goalId = goal.id,
                accountId = accountId,
                amount = amount,
                notes = state.contributionNotes
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        showWithdrawDialog = false,
                        isSubmitting = false,
                        showSuccessAnimation = true,
                        successMessage = "Withdrawal successful"
                    )
                }
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(showSuccessAnimation = false, successMessage = "") }
            }.onFailure { e ->
                _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to withdraw") }
            }
        }
    }

    /**
     * Archive the goal.
     */
    private fun archiveGoal() {
        val goal = _uiState.value.goal ?: return

        viewModelScope.launch {
            goalRepository.archiveGoal(goal.id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showArchiveConfirmDialog = false,
                            snackbarMessage = "Goal archived"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to archive goal") }
                }
        }
    }

    /**
     * Delete the goal permanently.
     */
    private fun deleteGoal() {
        val goal = _uiState.value.goal ?: return

        viewModelScope.launch {
            goalRepository.updateGoalStatus(goal.id, GoalStatus.ARCHIVED)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showDeleteConfirmDialog = false,
                            snackbarMessage = "Goal deleted"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to delete goal") }
                }
        }
    }

    /**
     * Load more contributions for pagination.
     */
    private fun loadMoreContributions() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreContributions) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val currentPage = _uiState.value.contributionPage + 1
            val goalId = _uiState.value.goal?.id ?: return@launch

            val allContributions = goalRepository.getContributionsByGoal(goalId).first()
            val startIndex = currentPage * CONTRIBUTION_PAGE_SIZE
            val endIndex = minOf(startIndex + CONTRIBUTION_PAGE_SIZE, allContributions.size)
            val newContributions = allContributions.subList(startIndex, endIndex)

            _uiState.update {
                it.copy(
                    contributions = it.contributions + newContributions,
                    contributionPage = currentPage,
                    hasMoreContributions = endIndex < allContributions.size,
                    isLoadingMore = false
                )
            }
        }
    }

    /**
     * Calculate contribution summary statistics.
     */
    private fun calculateContributionSummary(contributions: List<Contribution>): ContributionSummary {
        if (contributions.isEmpty()) {
            return ContributionSummary(0, null, 0.0, null)
        }

        // Filter only additions (exclude withdrawals for summary)
        val additions = contributions.filter { it.isAddition }
        val totalCount = additions.size

        // Sort by date descending to get latest
        val sorted = contributions.sortedByDescending { it.createdAt }
        val latest = sorted.firstOrNull()
        val lastActivity = latest?.createdAt

        // Calculate average (absolute values of additions)
        val average = if (additions.isNotEmpty()) {
            additions.sumOf { kotlin.math.abs(it.amount) } / additions.size
        } else {
            0.0
        }

        return ContributionSummary(
            totalCount = totalCount,
            latest = latest,
            average = average,
            lastActivity = lastActivity
        )
    }

    /**
     * Generate timeline events from goal and contributions.
     */
    private fun generateTimelineEvents(goal: Goal, contributions: List<Contribution>): List<GoalTimelineEvent> {
        val events = mutableListOf<GoalTimelineEvent>()

        // Goal Created event
        events.add(
            GoalTimelineEvent(
                id = "created-${goal.id}",
                type = TimelineEventType.GOAL_CREATED,
                title = "Goal Created",
                description = "\"${goal.name}\" was created",
                date = goal.createdAt
            )
        )

        // First contribution event
        val sortedContributions = contributions.sortedBy { it.createdAt }
        val firstContribution = sortedContributions.firstOrNull { it.isAddition }
        if (firstContribution != null) {
            events.add(
                GoalTimelineEvent(
                    id = "first-${goal.id}",
                    type = TimelineEventType.FIRST_CONTRIBUTION,
                    title = "First Contribution",
                    description = "Started saving towards the goal",
                    date = firstContribution.createdAt,
                    amount = firstContribution.amount
                )
            )
        }

        // Milestone events based on current progress
        val progressPercent = goal.progressPercent

        // 25% milestone
        if (progressPercent >= 25) {
            val at25 = sortedContributions.find {
                val progress = calculateProgressAtTime(it, goal.targetAmount, contributions)
                progress >= 25 && progress < 50
            }
            at25?.let {
                events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_25, 25))
            }
        }

        // 50% milestone
        if (progressPercent >= 50) {
            val at50 = sortedContributions.find {
                val progress = calculateProgressAtTime(it, goal.targetAmount, contributions)
                progress >= 50 && progress < 75
            }
            at50?.let {
                events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_50, 50))
            }
        }

        // 75% milestone
        if (progressPercent >= 75) {
            val at75 = sortedContributions.find {
                val progress = calculateProgressAtTime(it, goal.targetAmount, contributions)
                progress >= 75 && progress < 90
            }
            at75?.let {
                events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_75, 75))
            }
        }

        // 90% milestone
        if (progressPercent >= 90 && progressPercent < 100) {
            val at90 = sortedContributions.find {
                val progress = calculateProgressAtTime(it, goal.targetAmount, contributions)
                progress >= 90 && progress < 100
            }
            at90?.let {
                events.add(createMilestoneEvent(it, TimelineEventType.MILESTONE_90, 90))
            }
        }

        // Goal Completed event
        if (goal.isCompleted) {
            val completedContribution = sortedContributions.lastOrNull { it.isAddition }
            events.add(
                GoalTimelineEvent(
                    id = "completed-${goal.id}",
                    type = TimelineEventType.GOAL_COMPLETED,
                    title = "Goal Achieved!",
                    description = "Successfully reached the target amount",
                    date = completedContribution?.createdAt ?: goal.updatedAt
                )
            )
        }

        // Sort events by date
        return events.sortedByDescending { it.date }
    }

    /**
     * Calculate progress at a specific contribution time.
     */
    private fun calculateProgressAtTime(
        contribution: Contribution,
        targetAmount: Double,
        allContributions: List<Contribution>
    ): Double {
        val contributionsUpTo = allContributions.filter {
            it.createdAt <= contribution.createdAt && it.type != ContributionType.WITHDRAWAL
        }
        val amount = contributionsUpTo.sumOf { kotlin.math.abs(it.amount) }
        return if (targetAmount > 0) (amount / targetAmount) * 100 else 0.0
    }

    /**
     * Create a milestone event.
     */
    private fun createMilestoneEvent(
        contribution: Contribution,
        type: TimelineEventType,
        percent: Int
    ): GoalTimelineEvent {
        return GoalTimelineEvent(
            id = "milestone-$percent-${contribution.id}",
            type = type,
            title = "$percent% Milestone",
            description = "Reached $percent% of the target",
            date = contribution.createdAt,
            amount = contribution.amount
        )
    }
}

/**
 * Internal data class for contribution summary.
 */
private data class ContributionSummary(
    val totalCount: Int,
    val latest: Contribution?,
    val average: Double,
    val lastActivity: Instant?
)
