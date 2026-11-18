package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.ui.components.PremiumDatePicker
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// ─── Navigation-based Entry Point ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    onBack: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onGoalArchived: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(goalId) { if (goalId.isNotEmpty()) viewModel.onEvent(GoalDetailEvent.LoadGoal(goalId)) }
    LaunchedEffect(uiState.snackbarMessage) { uiState.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.onEvent(GoalDetailEvent.ClearSnackbar) } }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.onEvent(GoalDetailEvent.ClearError) } }
    LaunchedEffect(uiState.navigateBackForGoalId) {
        // Compare against this screen's goalId: when GoalDetailScreen is hosted inline by
        // GoalsScreen, its ViewModel is scoped to the surrounding back-stack entry and survives
        // the dialog closing, so a flag left over from a previously archived goal must not
        // close the newly opened one.
        if (uiState.navigateBackForGoalId != null && uiState.navigateBackForGoalId == goalId) onGoalArchived()
    }

    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = AppPalette.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingContent(onBack = onBack)
                uiState.error != null && !uiState.hasGoal -> ErrorContent(message = uiState.error!!, onBack = onBack, onRetry = { viewModel.onEvent(GoalDetailEvent.LoadGoal(goalId)) })
                uiState.goal != null -> GoalDetailContent(uiState = uiState, onBack = onBack, onEvent = viewModel::onEvent)
            }
            if (uiState.showContributeDialog || uiState.showWithdrawDialog) { GoalDetailContributionDialog(uiState = uiState, onEvent = viewModel::onEvent) }
            if (uiState.showSuccessAnimation) { GoalDetailSuccessOverlay(message = uiState.successMessage) }
            if (uiState.showDeleteConfirmDialog) { GoalDeleteConfirmDialog(goalName = uiState.goal?.name ?: "", onConfirm = { viewModel.onEvent(GoalDetailEvent.ConfirmDelete) }, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }) }
            if (uiState.showArchiveConfirmDialog) { GoalArchiveConfirmDialog(goalName = uiState.goal?.name ?: "", onConfirm = { viewModel.onEvent(GoalDetailEvent.ConfirmArchive) }, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }) }
            if (uiState.showAutoAllocationDialog) { AutoAllocationDialog(rule = uiState.editingAutoAllocationRule, goal = uiState.goal, accounts = uiState.accountMap.values.toList(), expenseCategories = uiState.expenseCategories, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }, onSave = { rule -> if (uiState.editingAutoAllocationRule != null) viewModel.onEvent(GoalDetailEvent.UpdateAutoAllocationRule(rule)) else viewModel.onEvent(GoalDetailEvent.AddAutoAllocationRule(rule)) }, onDelete = uiState.editingAutoAllocationRule?.let { rule -> { viewModel.onEvent(GoalDetailEvent.DeleteAutoAllocationRule(rule.id)) } }, onNavigateToAccounts = onNavigateToAccounts) }
            if (uiState.showExtendDeadlineDialog) {
                ExtendDeadlineDialog(goalDeadline = uiState.goal?.deadline, onConfirm = { newDeadline -> viewModel.onEvent(GoalDetailEvent.ConfirmExtendDeadline(newDeadline)) }, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) })
            }
            if (uiState.showEditGoalDialog) {
                uiState.goal?.let { goal ->
                    AddGoalDialog(
                        initialDeadline = goal.deadline,
                        initialName = goal.name,
                        initialAmount = goal.targetAmount,
                        initialIcon = goal.iconName,
                        initialColor = goal.color,
                        initialNotes = goal.notes,
                        initialReminderEnabled = goal.reminderEnabled,
                        currentAmount = goal.currentAmount,
                        isEditing = true,
                        onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) },
                        onCreateGoal = { name, amount, deadline, icon, color, notes, reminder ->
                            viewModel.onEvent(GoalDetailEvent.SaveGoalEdit(name, amount, deadline, icon, color, notes, reminder))
                        }
                    )
                }
            }
            uiState.completeFunds?.let { funds ->
                uiState.goal?.let { goal ->
                    GoalFundsBottomSheet(goalName = goal.name, goalIcon = goal.iconName, goalColor = goal.color,
                        totalFunds = funds.values.sum(), fundsByAccount = funds,
                        accounts = uiState.accountMap.values.toList(), isDelete = false,
                        onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) },
                        onKeepOrReturn = { viewModel.onEvent(GoalDetailEvent.CompleteGoalKeepFunds) },
                        onTransfer = { targetId -> viewModel.onEvent(GoalDetailEvent.CompleteGoalTransferFunds(targetId)) })
                }
            }
            uiState.deleteFunds?.let { funds ->
                uiState.goal?.let { goal ->
                    GoalFundsBottomSheet(goalName = goal.name, goalIcon = goal.iconName, goalColor = goal.color,
                        totalFunds = funds.values.sum(), fundsByAccount = funds,
                        accounts = uiState.accountMap.values.toList(), isDelete = true,
                        onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) },
                        onKeepOrReturn = { viewModel.onEvent(GoalDetailEvent.DeleteGoalReturnFunds) },
                        onTransfer = { targetId -> viewModel.onEvent(GoalDetailEvent.DeleteGoalTransferFunds(targetId)) })
                }
            }
        }
    }
}

// ─── Main Content ────────────────────────────────────────────────────────────────

@Composable
private fun GoalDetailContent(uiState: GoalDetailUiState, onBack: () -> Unit, onEvent: (GoalDetailEvent) -> Unit) {
    val goal = uiState.goal ?: return
    // A single accent flows through every section, including the chart, so no
    // section competes with the progress figure.
    val accent = LocalAccent.current
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressPercent.toFloat() / 100f,
        animationSpec = tween(300),
        label = "progress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.background),
        contentPadding = PaddingValues(bottom = Dimens.ContentBottomPadding),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.CardSpacing)
    ) {
        item { PremiumDetailHeader(goal = goal, goalColor = accent, onBack = onBack, onEdit = { onEvent(GoalDetailEvent.EditGoal) }) }
        item { GoalSummaryCard(goal = goal, goalColor = accent, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }

        if (goal.isCompleted) {
            item { CompletedGoalBanner(goalName = goal.name, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }

        item { ProgressSection(goal = goal, goalColor = accent, animatedProgress = animatedProgress, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }

        if (!goal.isCompleted && !goal.isArchived) {
            item { DeadlineWarningBanner(goal = goal, onExtend = { onEvent(GoalDetailEvent.ShowExtendDeadlineDialog) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }

        item { ContributionSummarySection(uiState = uiState, goalColor = accent, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { SavingsActivityCombinedCard(contributions = uiState.contributions, goalColor = accent, goalStartDate = goal.createdAt, goalDeadline = goal.deadline, targetAmount = goal.targetAmount, accountMap = uiState.accountMap, isLoadingMore = uiState.isLoadingMore, hasMore = uiState.hasMoreContributions, onLoadMore = { onEvent(GoalDetailEvent.LoadMoreContributions) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { TimelineSection(events = uiState.timelineEvents, goalColor = accent, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }

        if (!goal.isCompleted && !goal.isArchived) {
            item {
                AutoAllocationSection(
                    rules = uiState.allocationRules,
                    goalColor = accent,
                    onAddRule = { onEvent(GoalDetailEvent.ShowAutoAllocationDialog) },
                    onEditRule = { onEvent(GoalDetailEvent.ShowEditAutoAllocationRule(it)) },
                    onToggleRule = { ruleId, enabled -> onEvent(GoalDetailEvent.ToggleAutoAllocationRule(ruleId, enabled)) },
                    modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
                )
            }
        }

        if (!goal.isPaused && !goal.isCompleted && !goal.isArchived) {
            item { ActionButtonsSection(goal = goal, goalColor = accent, onContribute = { onEvent(GoalDetailEvent.ShowContributeDialog) }, onWithdraw = { onEvent(GoalDetailEvent.ShowWithdrawDialog) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }

        if (goal.isActive || goal.isPaused || !goal.isCompleted) {
            item {
                StatusActionsSection(
                    goal = goal,
                    onPause = { onEvent(GoalDetailEvent.PauseGoal) },
                    onResume = { onEvent(GoalDetailEvent.ResumeGoal) },
                    onComplete = { onEvent(GoalDetailEvent.CompleteGoal) },
                    modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
                )
            }
        }

        item { DangerZoneSection(onArchive = { onEvent(GoalDetailEvent.ArchiveGoal) }, onDelete = { onEvent(GoalDetailEvent.DeleteGoal) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
    }
}

// ─── Deadline Warning Banner ──────────────────────────────────────────────────────

@Composable
private fun DeadlineWarningBanner(goal: Goal, onExtend: () -> Unit, modifier: Modifier = Modifier) {
    val deadline = goal.deadline ?: return
    val today = LocalDate.now()
    val daysRemaining = ChronoUnit.DAYS.between(today, deadline).toInt()

    val isUrgent = daysRemaining in 0..7
    val isOverdue = deadline.isBefore(today)

    if (!isUrgent && !isOverdue) return

    val title = if (isOverdue) stringResource(R.string.goal_deadline_overdue) else pluralStringResource(
        R.plurals.goal_deadline_urgent, daysRemaining, daysRemaining
    )
    val subtitle = if (isOverdue) stringResource(R.string.goal_deadline_overdue_desc) else stringResource(R.string.goal_deadline_urgent_desc)
    val icon = if (isOverdue) Icons.Outlined.ErrorOutline else Icons.Outlined.AccessTime

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = AppPalette.card,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.CardInnerPaddingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeMedium))
            Spacer(Modifier.width(Dimens.PaddingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary
                )
                Spacer(Modifier.height(Dimens.PaddingSmall))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            TextButton(onClick = onExtend) {
                Text(
                    text = stringResource(R.string.goal_extend_deadline_btn),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppPalette.textPrimary
                )
            }
        }
    }
}

// ─── Extend Deadline Dialog ───────────────────────────────────────────────────────

@Composable
private fun ExtendDeadlineDialog(
    goalDeadline: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = goalDeadline
        ?.atStartOfDay(ZoneId.systemDefault())
        ?.toInstant()
        ?.toEpochMilli()
        ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) // default +30 days

    PremiumDatePicker(
        initialMillis = initialMillis,
        onDateSelected = { millis ->
            val newDeadline = java.time.Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            onConfirm(newDeadline)
        },
        onDismiss = onDismiss
    )
}
