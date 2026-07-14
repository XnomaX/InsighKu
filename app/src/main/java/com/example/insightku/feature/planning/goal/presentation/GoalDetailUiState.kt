package com.example.insightku.feature.planning.goal.presentation

import com.example.insightku.feature.planning.goal.domain.model.*

data class GoalDetailUiState(
    val isLoading: Boolean = true,
    val goal: Goal? = null,
    val contributions: List<Contribution> = emptyList(),
    val timelineEvents: List<GoalTimelineEvent> = emptyList(),
    val allocationRules: List<AutoAllocationRule> = emptyList(),
    val linkedAccounts: List<com.example.insightku.core.data.model.Account> = emptyList(),
    val accountMap: Map<String, com.example.insightku.core.data.model.Account> = emptyMap(),
    val totalContributions: Int = 0,
    val latestContribution: Contribution? = null,
    val averageContribution: Double = 0.0,
    val lastActivityDate: java.time.Instant? = null,
    val hasGoal: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
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
    val expenseCategories: List<CategoryInfo> = emptyList()
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
    GOAL_CREATED, FIRST_CONTRIBUTION, MILESTONE_25, MILESTONE_50, MILESTONE_75, MILESTONE_90, GOAL_COMPLETED, WITHDRAWAL, OTHER
}
