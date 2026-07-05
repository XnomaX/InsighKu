package com.example.insightku.feature.budgeting.presentation.state

import com.example.insightku.core.data.model.Account
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule
import com.example.insightku.feature.budgeting.domain.model.Contribution
import com.example.insightku.feature.budgeting.domain.model.Goal
import java.time.Instant
import java.time.LocalDate

/**
 * UI State for the Goal Detail screen.
 * Contains all information needed to render a comprehensive goal detail page.
 */
data class GoalDetailUiState(
    // Core data
    val goal: Goal? = null,
    val contributions: List<Contribution> = emptyList(),
    val linkedAccounts: List<Account> = emptyList(),
    val accountMap: Map<String, Account> = emptyMap(),

    // Contribution summary
    val totalContributions: Int = 0,
    val latestContribution: Contribution? = null,
    val averageContribution: Double = 0.0,
    val lastActivityDate: Instant? = null,

    // Timeline events
    val timelineEvents: List<GoalTimelineEvent> = emptyList(),

    // UI states
    val isLoading: Boolean = true,
    val isLoadingContributions: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,

    // Dialog states
    val showContributeDialog: Boolean = false,
    val showWithdrawDialog: Boolean = false,
    val showEditGoalDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showArchiveConfirmDialog: Boolean = false,

    // Contribution form state
    val selectedAccountId: String? = null,
    val contributionAmount: String = "",
    val contributionNotes: String = "",

    // Submission state
    val isSubmitting: Boolean = false,
    val showSuccessAnimation: Boolean = false,
    val successMessage: String = "",

    // Balance validation
    val isInsufficientFunds: Boolean = false,
    val shortfall: Double = 0.0,

    // Account picker
    val showAccountPicker: Boolean = false,

    // Pending sync indicator
    val hasUnsyncedChanges: Boolean = false,

    // Pagination for contribution history
    val contributionPage: Int = 0,
    val hasMoreContributions: Boolean = false,
    val isLoadingMore: Boolean = false,

    // Auto-allocation rules
    val allocationRules: List<AutoAllocationRule> = emptyList()
) {
    // Computed properties
    val hasContributions: Boolean get() = contributions.isNotEmpty()
    val hasGoal: Boolean get() = goal != null
    val isError: Boolean get() = error != null
    val isEmpty: Boolean get() = hasGoal && !isLoading && contributions.isEmpty()

    // Get account name for a contribution
    fun getAccountName(accountId: String): String {
        return accountMap[accountId]?.name ?: "Unknown Account"
    }

    // Get account for a contribution
    fun getAccount(accountId: String): Account? {
        return accountMap[accountId]
    }

    companion object {
        fun initial() = GoalDetailUiState(isLoading = true)
    }
}

/**
 * Represents a timeline event for a goal.
 */
data class GoalTimelineEvent(
    val id: String,
    val type: TimelineEventType,
    val title: String,
    val description: String,
    val date: Instant,
    val amount: Double? = null
)

/**
 * Types of timeline events.
 */
enum class TimelineEventType {
    GOAL_CREATED,
    FIRST_CONTRIBUTION,
    MILESTONE_25,
    MILESTONE_50,
    MILESTONE_75,
    MILESTONE_90,
    GOAL_COMPLETED,
    WITHDRAWAL,
    GOAL_PAUSED,
    GOAL_RESUMED,
    DEADLINE_UPDATED
}
