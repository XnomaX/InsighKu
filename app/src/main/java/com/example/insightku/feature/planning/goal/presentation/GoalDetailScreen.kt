package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.ui.components.PremiumDatePicker
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.PurpleViolet
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import com.example.insightku.feature.planning.goal.domain.model.DailyTarget
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
    onNavigateToEditGoal: (String) -> Unit,
    onNavigateToAccounts: () -> Unit,
    onGoalArchived: () -> Unit,
    onGoalDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(goalId) { if (goalId.isNotEmpty()) viewModel.onEvent(GoalDetailEvent.LoadGoal(goalId)) }
    LaunchedEffect(uiState.snackbarMessage) { uiState.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.onEvent(GoalDetailEvent.ClearSnackbar) } }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.onEvent(GoalDetailEvent.ClearError) } }
    LaunchedEffect(uiState.snackbarMessage) { if (uiState.snackbarMessage == "Goal archived" || uiState.snackbarMessage == "Goal deleted") onGoalArchived() }

    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = AppPalette.background) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> LoadingContent(onBack = onBack)
                uiState.error != null && !uiState.hasGoal -> ErrorContent(message = uiState.error!!, onBack = onBack, onRetry = { viewModel.onEvent(GoalDetailEvent.LoadGoal(goalId)) })
                uiState.goal != null -> GoalDetailContent(uiState = uiState, onBack = onBack, onEvent = viewModel::onEvent, onNavigateToEditGoal = onNavigateToEditGoal)
            }
            if (uiState.showContributeDialog || uiState.showWithdrawDialog) { GoalDetailContributionDialog(uiState = uiState, onEvent = viewModel::onEvent) }
            if (uiState.showSuccessAnimation) { GoalDetailSuccessOverlay(message = uiState.successMessage) }
            if (uiState.showDeleteConfirmDialog) { GoalDeleteConfirmDialog(goalName = uiState.goal?.name ?: "", onConfirm = { viewModel.onEvent(GoalDetailEvent.ConfirmDelete) }, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }) }
            if (uiState.showArchiveConfirmDialog) { GoalArchiveConfirmDialog(goalName = uiState.goal?.name ?: "", onConfirm = { viewModel.onEvent(GoalDetailEvent.ConfirmArchive) }, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }) }
            if (uiState.showAutoAllocationDialog) { AutoAllocationDialog(rule = uiState.editingAutoAllocationRule, goal = uiState.goal, accounts = uiState.accountMap.values.toList(), expenseCategories = uiState.expenseCategories, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }, onSave = { rule -> if (uiState.editingAutoAllocationRule != null) viewModel.onEvent(GoalDetailEvent.UpdateAutoAllocationRule(rule)) else viewModel.onEvent(GoalDetailEvent.AddAutoAllocationRule(rule)) }, onDelete = uiState.editingAutoAllocationRule?.let { rule -> { viewModel.onEvent(GoalDetailEvent.DeleteAutoAllocationRule(rule.id)) } }, onNavigateToAccounts = onNavigateToAccounts) }
            if (uiState.showExtendDeadlineDialog) {
                ExtendDeadlineDialog(goalDeadline = uiState.goal?.deadline, onConfirm = { newDeadline -> viewModel.onEvent(GoalDetailEvent.ConfirmExtendDeadline(newDeadline)) }, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) })
            }
        }
    }
}

// ─── Dialog-based Entry Point (Legacy) ───────────────────────────────────────────

@Composable
fun GoalDetailScreen(goal: Goal, dailyTarget: DailyTarget, contributions: List<Contribution>, onBack: () -> Unit, onEdit: () -> Unit, onSetDailyTarget: () -> Unit, onSave: () -> Unit, onWithdraw: () -> Unit, modifier: Modifier = Modifier) {
    val goalColor = remember(goal.color) { try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { PurpleViolet } }
    val animatedProgress by animateFloatAsState(targetValue = goal.progressPercent.toFloat() / 100f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f), label = "progress")
    LazyColumn(modifier = modifier.fillMaxSize().background(AppPalette.background), contentPadding = PaddingValues(bottom = 120.dp)) {
        item { PremiumDetailHeader(goal = goal, goalColor = goalColor, onBack = onBack) }
        item { GoalSummaryCard(goal = goal, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ProgressSection(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ActionButtonsSection(goal = goal, goalColor = goalColor, onContribute = onSave, onWithdraw = onWithdraw, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
    }
}

// ─── Main Content ────────────────────────────────────────────────────────────────

@Composable
private fun GoalDetailContent(uiState: GoalDetailUiState, onBack: () -> Unit, onEvent: (GoalDetailEvent) -> Unit, onNavigateToEditGoal: (String) -> Unit) {
    val goal = uiState.goal ?: return
    val goalColor = remember(goal.color) { try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { PurpleViolet } }
    val animatedProgress by animateFloatAsState(targetValue = goal.progressPercent.toFloat() / 100f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f), label = "progress")

    LazyColumn(modifier = Modifier.fillMaxSize().background(AppPalette.background), contentPadding = PaddingValues(bottom = 120.dp)) {
        item { PremiumDetailHeader(goal = goal, goalColor = goalColor, onBack = onBack) }
        item { GoalSummaryCard(goal = goal, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ProgressSection(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        // ── Deadline Warning Banner ──────────────────────────────────────
        item { DeadlineWarningBanner(goal = goal, goalColor = goalColor, onExtend = { onEvent(GoalDetailEvent.ShowExtendDeadlineDialog) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ContributionSummarySection(uiState = uiState, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); SavingsActivityCombinedCard(contributions = uiState.contributions, goalColor = goalColor, goalStartDate = goal.createdAt, goalDeadline = goal.deadline, targetAmount = goal.targetAmount, accountMap = uiState.accountMap, isLoadingMore = uiState.isLoadingMore, hasMore = uiState.hasMoreContributions, onLoadMore = { onEvent(GoalDetailEvent.LoadMoreContributions) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); TimelineSection(events = uiState.timelineEvents, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); AutoAllocationSection(goal = goal, rules = uiState.allocationRules, goalColor = goalColor, onAddRule = { onEvent(GoalDetailEvent.ShowAutoAllocationDialog) }, onEditRule = { onEvent(GoalDetailEvent.ShowEditAutoAllocationRule(it)) }, onToggleRule = { ruleId, enabled -> onEvent(GoalDetailEvent.ToggleAutoAllocationRule(ruleId, enabled)) }, onDeleteRule = { onEvent(GoalDetailEvent.DeleteAutoAllocationRule(it)) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        if (!goal.isPaused) {
            item { Spacer(Modifier.height(12.dp)); ActionButtonsSection(goal = goal, goalColor = goalColor, onContribute = { onEvent(GoalDetailEvent.ShowContributeDialog) }, onWithdraw = { onEvent(GoalDetailEvent.ShowWithdrawDialog) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }
        item { Spacer(Modifier.height(24.dp)); DangerZoneSection(onArchive = { onEvent(GoalDetailEvent.ArchiveGoal) }, onDelete = { onEvent(GoalDetailEvent.DeleteGoal) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
    }
}

// ─── Deadline Warning Banner ──────────────────────────────────────────────────────

@Composable
private fun DeadlineWarningBanner(goal: Goal, goalColor: Color, onExtend: () -> Unit, modifier: Modifier = Modifier) {
    val deadline = goal.deadline ?: return
    val today = LocalDate.now()
    val daysRemaining = ChronoUnit.DAYS.between(today, deadline).toInt()

    val isUrgent = daysRemaining in 0..7
    val isOverdue = deadline.isBefore(today)

    if (!isUrgent && !isOverdue) return

    val bannerColor = if (isOverdue) ExpenseRed else goalColor
    val icon = if (isOverdue) Icons.Outlined.ErrorOutline else Icons.Outlined.AccessTime
    val title = if (isOverdue) stringResource(R.string.goal_deadline_overdue) else stringResource(R.string.goal_deadline_urgent, daysRemaining)
    val subtitle = if (isOverdue) stringResource(R.string.goal_deadline_overdue_desc) else stringResource(R.string.goal_deadline_urgent_desc)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = bannerColor.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = bannerColor.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = bannerColor.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onExtend,
                shape = RoundedCornerShape(10.dp),
                color = bannerColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = stringResource(R.string.goal_extend_deadline_btn),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = bannerColor
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
