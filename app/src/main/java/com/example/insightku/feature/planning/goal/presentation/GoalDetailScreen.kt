package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.PurpleViolet
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import com.example.insightku.feature.planning.goal.domain.model.DailyTarget
import com.example.insightku.feature.planning.goal.domain.model.Goal

// ─── Navigation-based Entry Point ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    onBack: () -> Unit,
    onNavigateToEditGoal: (String) -> Unit,
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
            if (uiState.showDeleteAutoAllocationRuleConfirm) { GoalDeleteAutoAllocationRuleConfirmDialog(onConfirm = { viewModel.onEvent(GoalDetailEvent.ConfirmDeleteAutoAllocationRule) }, onDismiss = { viewModel.onEvent(GoalDetailEvent.CancelDeleteAutoAllocationRule) }) }
            if (uiState.showAutoAllocationDialog) { AutoAllocationDialog(rule = uiState.editingAutoAllocationRule, goals = listOfNotNull(uiState.goal), accounts = uiState.linkedAccounts, onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }, onSave = { rule -> if (uiState.editingAutoAllocationRule != null) viewModel.onEvent(GoalDetailEvent.UpdateAutoAllocationRule(rule)) else viewModel.onEvent(GoalDetailEvent.AddAutoAllocationRule(rule)) }, onDelete = uiState.editingAutoAllocationRule?.let { rule -> { viewModel.onEvent(GoalDetailEvent.ShowDeleteAutoAllocationConfirm(rule.id)) } }) }
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
        item { Spacer(Modifier.height(12.dp)); ContributionSummarySection(uiState = uiState, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ContributionHistorySection(contributions = uiState.contributions, accountMap = uiState.accountMap, goalColor = goalColor, isLoadingMore = uiState.isLoadingMore, hasMore = uiState.hasMoreContributions, onLoadMore = { onEvent(GoalDetailEvent.LoadMoreContributions) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); TimelineSection(events = uiState.timelineEvents, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); AutoAllocationSection(goal = goal, rules = uiState.allocationRules, goalColor = goalColor, onAddRule = { onEvent(GoalDetailEvent.ShowAutoAllocationDialog) }, onEditRule = { onEvent(GoalDetailEvent.ShowEditAutoAllocationRule(it)) }, onToggleRule = { ruleId, enabled -> onEvent(GoalDetailEvent.ToggleAutoAllocationRule(ruleId, enabled)) }, onDeleteRule = { onEvent(GoalDetailEvent.DeleteAutoAllocationRule(it)) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        if (!goal.isPaused) {
            item { Spacer(Modifier.height(12.dp)); ActionButtonsSection(goal = goal, goalColor = goalColor, onContribute = { onEvent(GoalDetailEvent.ShowContributeDialog) }, onWithdraw = { onEvent(GoalDetailEvent.ShowWithdrawDialog) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }
        item { Spacer(Modifier.height(24.dp)); DangerZoneSection(onArchive = { onEvent(GoalDetailEvent.ArchiveGoal) }, onDelete = { onEvent(GoalDetailEvent.DeleteGoal) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
    }
}
