package com.example.insightku.feature.budgeting.presentation.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.PurpleViolet
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.feature.budgeting.domain.model.Contribution
import com.example.insightku.feature.budgeting.domain.model.DailyTarget
import com.example.insightku.feature.budgeting.domain.model.Goal
import com.example.insightku.feature.budgeting.presentation.state.GoalTimelineEvent
import com.example.insightku.feature.budgeting.presentation.state.TimelineEventType
import com.example.insightku.feature.budgeting.presentation.event.GoalDetailEvent
import com.example.insightku.feature.budgeting.presentation.state.GoalDetailUiState
import com.example.insightku.feature.budgeting.presentation.viewmodel.GoalDetailViewModel
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


/**
 * Goal Detail Screen - The single source of truth for all Goal information.
 *
 * Features:
 * - Goal Summary (icon, name, target, current, progress, deadline, days remaining)
 * - Progress Section (visual progress bar, percentage, amounts)
 * - Contribution Summary (total, latest, average, last activity)
 * - Contribution History (chronological list with account info)
 * - Goal Timeline (milestones and key events)
 * - Reserved section for future Auto Allocation settings
 * - Goal Actions (contribute, withdraw, edit, archive, delete)
 * - Empty states for no contributions/timeline/notes
 * - Loading and error states
 * - Edge-to-edge with proper WindowInsets support
 *
 * This composable supports two usage modes:
 * 1. Navigation-based (with goalId) - uses ViewModel to load data
 * 2. Dialog-based (with direct parameters) - displays data passed directly
 */

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

    // Load goal data on first composition
    LaunchedEffect(goalId) {
        if (goalId.isNotEmpty()) {
            viewModel.onEvent(GoalDetailEvent.LoadGoal(goalId))
        }
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(GoalDetailEvent.ClearSnackbar)
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(GoalDetailEvent.ClearError)
        }
    }

    // Handle archive/delete completion
    LaunchedEffect(uiState.snackbarMessage) {
        if (uiState.snackbarMessage == "Goal archived" || uiState.snackbarMessage == "Goal deleted") {
            onGoalArchived()
        }
    }

    // Main scaffold with proper edge-to-edge support
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppPalette.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingContent(onBack = onBack)
                uiState.error != null && !uiState.hasGoal -> ErrorContent(
                    message = uiState.error!!,
                    onBack = onBack,
                    onRetry = { viewModel.onEvent(GoalDetailEvent.LoadGoal(goalId)) }
                )
                uiState.goal != null -> GoalDetailContent(
                    uiState = uiState,
                    onBack = onBack,
                    onEvent = viewModel::onEvent,
                    onNavigateToEditGoal = onNavigateToEditGoal
                )
            }

            // Dialogs
            if (uiState.showContributeDialog || uiState.showWithdrawDialog) {
                ContributionDialog(uiState = uiState, onEvent = viewModel::onEvent)
            }

            if (uiState.showDeleteConfirmDialog) {
                DeleteConfirmDialog(
                    goalName = uiState.goal?.name ?: "",
                    onConfirm = { viewModel.onEvent(GoalDetailEvent.ConfirmDelete) },
                    onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }
                )
            }

            if (uiState.showArchiveConfirmDialog) {
                ArchiveConfirmDialog(
                    goalName = uiState.goal?.name ?: "",
                    onConfirm = { viewModel.onEvent(GoalDetailEvent.ConfirmArchive) },
                    onDismiss = { viewModel.onEvent(GoalDetailEvent.DismissDialog) }
                )
            }
        }
    }
}

// ─── Dialog-based Entry Point (Legacy) ───────────────────────────────────────────

@Composable
fun GoalDetailScreen(
    goal: Goal,
    dailyTarget: DailyTarget,
    contributions: List<Contribution>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSetDailyTarget: () -> Unit,
    onSave: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goalColor = remember(goal.color) {
        try { Color(android.graphics.Color.parseColor(goal.color)) }
        catch (e: Exception) { PurpleViolet }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressPercent.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "progress"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().background(AppPalette.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { PremiumDetailHeader(goal = goal, goalColor = goalColor, onBack = onBack, onEdit = onEdit) }
        item { GoalSummaryCard(goal = goal, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); ProgressSection(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); ContributionSummarySectionLegacy(contributions = contributions, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); ContributionHistorySectionLegacy(contributions = contributions, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); TimelineSectionLegacy(goal = goal, contributions = contributions, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); DailyTargetCard(goal = goal, dailyTarget = dailyTarget, goalColor = goalColor, onSetTarget = onSetDailyTarget, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        if (goal.notes.isNotBlank()) {
            item { Spacer(Modifier.height(Dimens.CardSpacing)); NotesSection(notes = goal.notes, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }
        if (!goal.isPaused) {
            item { Spacer(Modifier.height(Dimens.CardSpacing)); ActionButtonsRow(goal = goal, goalColor = goalColor, onSave = onSave, onWithdraw = onWithdraw, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }
    }
}

@Composable
private fun LoadingContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppPalette.background)
    ) {
        DetailTopBar(onBack = onBack, title = "")
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = PurpleViolet,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppPalette.background)) {
        DetailTopBar(onBack = onBack, title = "")
        Box(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppPalette.card)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Error, null, tint = ExpenseRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PurpleViolet)) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalDetailContent(
    uiState: GoalDetailUiState,
    onBack: () -> Unit,
    onEvent: (GoalDetailEvent) -> Unit,
    onNavigateToEditGoal: (String) -> Unit
) {
    val goal = uiState.goal ?: return
    val goalColor = remember(goal.color) {
        try { Color(android.graphics.Color.parseColor(goal.color)) }
        catch (e: Exception) { PurpleViolet }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressPercent.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "progress"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppPalette.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { PremiumDetailHeader(goal = goal, goalColor = goalColor, onBack = onBack, onEdit = { onNavigateToEditGoal(goal.id) }) }
        item { GoalSummaryCard(goal = goal, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); ProgressSection(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); ContributionSummarySection(uiState = uiState, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); ContributionHistorySection(contributions = uiState.contributions, accountMap = uiState.accountMap, goalColor = goalColor, isLoadingMore = uiState.isLoadingMore, hasMore = uiState.hasMoreContributions, onLoadMore = { onEvent(GoalDetailEvent.LoadMoreContributions) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); TimelineSection(events = uiState.timelineEvents, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(Dimens.CardSpacing)); ReservedAutoAllocationSection(goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        if (!goal.isPaused) {
            item { Spacer(Modifier.height(Dimens.CardSpacing)); ActionButtonsSection(goal = goal, goalColor = goalColor, onContribute = { onEvent(GoalDetailEvent.ShowContributeDialog) }, onWithdraw = { onEvent(GoalDetailEvent.ShowWithdrawDialog) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }
        item { Spacer(Modifier.height(Dimens.SectionSpacing)); DangerZoneSection(onArchive = { onEvent(GoalDetailEvent.ArchiveGoal) }, onDelete = { onEvent(GoalDetailEvent.DeleteGoal) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────────

@Composable
private fun DetailTopBar(onBack: () -> Unit, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, "Back", tint = AppPalette.textPrimary)
        }
        if (title.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
        }
    }
}

// ─── Premium Header ──────────────────────────────────────────────────────────────

@Composable
private fun PremiumDetailHeader(goal: com.example.insightku.feature.budgeting.domain.model.Goal, goalColor: Color, onBack: () -> Unit, onEdit: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = AppPalette.textPrimary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, "Edit goal", tint = AppPalette.textMuted)
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(72.dp).shadow(8.dp, RoundedCornerShape(20.dp), spotColor = goalColor.copy(alpha = 0.4f)).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(goalColor.copy(alpha = 0.2f), goalColor.copy(alpha = 0.08f)))), contentAlignment = Alignment.Center) {
                Icon(getGoalIcon(goal.iconName), null, tint = goalColor, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(goal.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, textAlign = TextAlign.Center)
            if (goal.isCompleted) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = SuccessColor.copy(alpha = 0.12f)) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = SuccessColor, modifier = Modifier.size(16.dp))
                        Text("Completed!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = SuccessColor)
                    }
                }
            }
        }
    }
}

// ─── Goal Summary Card ───────────────────────────────────────────────────────────

@Composable
private fun GoalSummaryCard(goal: com.example.insightku.feature.budgeting.domain.model.Goal, goalColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem(label = "Target", value = formatCurrencyCompact(goal.targetAmount))
                SummaryItem(label = "Saved", value = formatCurrencyCompact(goal.currentAmount), valueColor = if (goal.isCompleted) SuccessColor else goalColor)
                SummaryItem(label = "Remaining", value = formatCurrencyCompact(goal.remainingAmount))
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                goal.deadline?.let { deadline ->
                    val daysText = when {
                        goal.isCompleted -> "Done"
                        goal.isOverdue -> "Overdue"
                        goal.daysRemaining == 0 -> "Today"
                        else -> "${goal.daysRemaining} days"
                    }
                    SummaryItem(label = "Deadline", value = deadline.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
                    SummaryItem(label = "Days Left", value = daysText, valueColor = when {
                        goal.isOverdue -> ExpenseRed
                        (goal.daysRemaining ?: 0) <= 7 -> WarningYellow
                        else -> AppPalette.textPrimary
                    })
                } ?: SummaryItem(label = "Started", value = goal.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM")))
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, valueColor: Color = AppPalette.textPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ─── Progress Section ─────────────────────────────────────────────────────────────

@Composable
private fun ProgressSection(goal: com.example.insightku.feature.budgeting.domain.model.Goal, goalColor: Color, animatedProgress: Float, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = if (goal.isCompleted) SuccessColor else goalColor)
            Text(if (goal.isCompleted) "Goal Achieved!" else "Progress", style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
            Spacer(Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(AppPalette.cardBorder)) {
                Box(modifier = Modifier.fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(7.dp)).background(Brush.horizontalGradient(listOf(if (goal.isCompleted) SuccessColor.copy(alpha = 0.8f) else goalColor.copy(alpha = 0.8f), if (goal.isCompleted) SuccessColor else goalColor))))
            }
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Saved", style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted)
                    Text(formatCurrencyFull(goal.currentAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (goal.isCompleted) SuccessColor else AppPalette.textPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target", style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted)
                    Text(formatCurrencyFull(goal.targetAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                }
            }
        }
    }
}

// ─── Contribution Summary Section ─────────────────────────────────────────────────

@Composable
private fun ContributionSummarySection(uiState: GoalDetailUiState, goalColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Contribution Summary", subtitle = "Statistics")
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(Dimens.CardInnerPadding), horizontalArrangement = Arrangement.SpaceEvenly) {
                ContributionStatItem(label = "Total", value = "${uiState.totalContributions}", icon = Icons.Outlined.Receipt, color = goalColor)
                uiState.latestContribution?.let {
                    ContributionStatItem(label = "Latest", value = formatCurrencyCompact(kotlin.math.abs(it.amount)), icon = Icons.Outlined.TrendingUp, color = SuccessColor)
                }
                if (uiState.averageContribution > 0) {
                    ContributionStatItem(label = "Average", value = formatCurrencyCompact(uiState.averageContribution), icon = Icons.Outlined.Analytics, color = PurpleViolet)
                }
                uiState.lastActivityDate?.let {
                    ContributionStatItem(label = "Last", value = it.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM")), icon = Icons.Outlined.Schedule, color = AppPalette.textMuted)
                }
            }
        }
    }
}

@Composable
private fun ContributionStatItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
    }
}

// ─── Contribution History Section ────────────────────────────────────────────────

@Composable
private fun ContributionHistorySection(contributions: List<Contribution>, accountMap: Map<String, com.example.insightku.core.data.model.Account>, goalColor: Color, isLoadingMore: Boolean, hasMore: Boolean, onLoadMore: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Contribution History", subtitle = "Recent transactions")
        Spacer(Modifier.height(12.dp))
        if (contributions.isEmpty()) {
            EmptyContributionsCard(goalColor)
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column {
                    contributions.forEachIndexed { index, contribution ->
                        val isWithdrawal = contribution.isWithdrawal
                        val itemColor = if (isWithdrawal) ExpenseRed else SuccessColor
                        val account = accountMap[contribution.accountId]

                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(itemColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(if (isWithdrawal) Icons.Outlined.ArrowUpward else Icons.Outlined.Add, null, tint = itemColor, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isWithdrawal) "Withdrawal" else "Saved", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                                Text(account?.name ?: "Unknown Account", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                val date = contribution.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime()
                                Text("${date.dayOfMonth} ${date.month.name.take(3)}", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${if (isWithdrawal) "-" else "+"}${formatCurrencyFull(kotlin.math.abs(contribution.amount))}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = itemColor)
                                if (contribution.notes.isNotBlank()) {
                                    Text(contribution.notes, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, maxLines = 1)
                                }
                            }
                        }
                        if (index < contributions.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder)
                        }
                    }
                    if (hasMore) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = goalColor, strokeWidth = 2.dp)
                            } else {
                                TextButton(onClick = onLoadMore) {
                                    Text("Load More", color = goalColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContributionsCard(goalColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.History, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("No contributions yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text("Start saving to see your progress here", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
        }
    }
}

// ─── Timeline Section ─────────────────────────────────────────────────────────────

@Composable
private fun TimelineSection(events: List<GoalTimelineEvent>, goalColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Journey", subtitle = "Milestones")
        Spacer(Modifier.height(12.dp))
        if (events.isEmpty()) {
            EmptyTimelineCard(goalColor)
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
                    events.forEachIndexed { index, event ->
                        TimelineEventItem(event = event, goalColor = goalColor, isLast = index == events.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEventItem(event: GoalTimelineEvent, goalColor: Color, isLast: Boolean) {
    val iconColor = when (event.type) {
        TimelineEventType.GOAL_COMPLETED -> SuccessColor
        TimelineEventType.WITHDRAWAL -> ExpenseRed
        TimelineEventType.MILESTONE_25, TimelineEventType.MILESTONE_50, TimelineEventType.MILESTONE_75, TimelineEventType.MILESTONE_90 -> goalColor
        else -> PurpleViolet
    }
    val icon = when (event.type) {
        TimelineEventType.GOAL_CREATED -> Icons.Outlined.Flag
        TimelineEventType.FIRST_CONTRIBUTION -> Icons.Outlined.Star
        TimelineEventType.MILESTONE_25 -> Icons.Outlined.TrendingUp
        TimelineEventType.MILESTONE_50 -> Icons.Outlined.TrendingUp
        TimelineEventType.MILESTONE_75 -> Icons.Outlined.TrendingUp
        TimelineEventType.MILESTONE_90 -> Icons.Outlined.TrendingUp
        TimelineEventType.GOAL_COMPLETED -> Icons.Outlined.CheckCircle
        TimelineEventType.WITHDRAWAL -> Icons.Outlined.ArrowDownward
        else -> Icons.Outlined.Event
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(iconColor))
            if (!isLast) Box(modifier = Modifier.width(2.dp).height(40.dp).background(AppPalette.cardBorder))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text(event.description, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
            Text(event.date.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM yyyy")), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
        }
        event.amount?.let { amount ->
            Text("${if (amount > 0) "+" else ""}${formatCurrencyCompact(kotlin.math.abs(amount))}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (amount > 0) SuccessColor else ExpenseRed)
        }
    }
    if (!isLast) Spacer(Modifier.height(12.dp))
}

@Composable
private fun EmptyTimelineCard(goalColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Timeline, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("No timeline events", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text("Your journey will appear here", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
        }
    }
}

// ─── Reserved Auto Allocation Section ─────────────────────────────────────────────

@Composable
private fun ReservedAutoAllocationSection(goalColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Auto Allocation", subtitle = "Coming soon")
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.cardElevated), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(Dimens.CardInnerPadding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = AppPalette.textMuted.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Automatic savings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textMuted)
                    Text("Set up automatic contributions from your accounts", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted.copy(alpha = 0.7f))
                }
                Surface(shape = RoundedCornerShape(8.dp), color = AppPalette.cardBorder) {
                    Text("Coming", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

// ─── Action Buttons Section ─────────────────────────────────────────────────────

@Composable
private fun ActionButtonsSection(goal: com.example.insightku.feature.budgeting.domain.model.Goal, goalColor: Color, onContribute: () -> Unit, onWithdraw: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Actions", subtitle = "Manage your savings")
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (goal.currentAmount > 0) {
                OutlinedButton(onClick = onWithdraw, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textMuted), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                    Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Withdraw", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(onClick = onContribute, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = goalColor), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Danger Zone Section ─────────────────────────────────────────────────────────

@Composable
private fun DangerZoneSection(onArchive: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding)) {
                Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onArchive).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Archive, null, tint = WarningYellow, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Archive Goal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                            Text("Hide this goal from your active list", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                        }
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted)
                }
                HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Delete Goal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                            Text("Permanently remove this goal and all its data", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                        }
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted)
                }
            }
        }
    }
}

// ─── Dialogs ─────────────────────────────────────────────────────────────────────

@Composable
private fun ContributionDialog(uiState: GoalDetailUiState, onEvent: (GoalDetailEvent) -> Unit) {
    val isWithdraw = uiState.showWithdrawDialog
    AlertDialog(onDismissRequest = { onEvent(GoalDetailEvent.DismissDialog) }, title = { Text(if (isWithdraw) "Withdraw from Goal" else "Add Contribution") }, text = {
        Column {
            OutlinedTextField(value = uiState.contributionAmount, onValueChange = { onEvent(GoalDetailEvent.UpdateAmount(it)) }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = uiState.contributionNotes, onValueChange = { onEvent(GoalDetailEvent.UpdateNotes(it)) }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
        }
    }, confirmButton = { Button(onClick = { if (isWithdraw) onEvent(GoalDetailEvent.SubmitWithdrawal) else onEvent(GoalDetailEvent.SubmitContribution) }, colors = ButtonDefaults.buttonColors(containerColor = if (isWithdraw) WarningYellow else PurpleViolet)) { Text(if (isWithdraw) "Withdraw" else "Contribute") } }, dismissButton = { TextButton(onClick = { onEvent(GoalDetailEvent.DismissDialog) }) { Text("Cancel") } })
}

@Composable
private fun DeleteConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Outlined.Delete, null, tint = ExpenseRed) }, title = { Text("Delete Goal?") }, text = { Text("Are you sure you want to permanently delete \"$goalName\"? This action cannot be undone.") }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)) { Text("Delete") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ArchiveConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Outlined.Archive, null, tint = WarningYellow) }, title = { Text("Archive Goal?") }, text = { Text("Are you sure you want to archive \"$goalName\"? You can view archived goals in settings.") }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = WarningYellow)) { Text("Archive") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─── Section Header ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
    }
}

// ─── Helper Functions ───────────────────────────────────────────────────────────

private fun getGoalIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "savings", "piggy bank" -> Icons.Outlined.Savings
        "wallet", "account balance wallet" -> Icons.Outlined.AccountBalanceWallet
        "cash", "money", "paid" -> Icons.Outlined.Paid
        "flight", "airplane" -> Icons.Outlined.Flight
        "car", "directions car" -> Icons.Outlined.DirectionsCar
        "home", "house" -> Icons.Outlined.Home
        "school", "education", "graduation" -> Icons.Outlined.School
        "health", "health and safety" -> Icons.Outlined.HealthAndSafety
        "warning", "emergency" -> Icons.Outlined.Warning
        "trending up", "investment", "stocks" -> Icons.Outlined.TrendingUp
        "card giftcard", "gift" -> Icons.Outlined.CardGiftcard
        "celebration" -> Icons.Outlined.Celebration
        "star" -> Icons.Outlined.Star
        "flag", "target", "gps fixed" -> Icons.Outlined.Flag
        "beach", "travel" -> Icons.Outlined.BeachAccess
        "hotel", "suitcase" -> Icons.Outlined.Luggage
        "laptop", "technology" -> Icons.Outlined.Laptop
        "phone", "smartphone" -> Icons.Outlined.Smartphone
        "diamond", "gold", "investment" -> Icons.Outlined.Diamond
        else -> Icons.Outlined.Savings
    }
}

private fun formatCurrencyFull(amount: Double): String {
    return "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())}"
}

private fun formatCurrencyCompact(amount: Double): String {
    return when {
        amount >= 1_000_000 -> {
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount / 1_000_000)
            "Rp$formatted M"
        }
        amount >= 1_000 -> {
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount / 1_000)
            "Rp$formatted K"
        }
        else -> "Rp${NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())}"
    }
}

// ─── Legacy Helper Functions for Dialog-based Usage ─────────────────────────────────

@Composable
private fun ContributionSummarySectionLegacy(contributions: List<Contribution>, goalColor: Color, modifier: Modifier = Modifier) {
    val additions = contributions.filter { it.isAddition }
    val totalCount = additions.size
    val latest = contributions.maxByOrNull { it.createdAt }
    val average = if (additions.isNotEmpty()) additions.sumOf { kotlin.math.abs(it.amount) } / additions.size else 0.0
    val lastActivity = latest?.createdAt

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Contribution Summary", subtitle = "Statistics")
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(Dimens.CardInnerPadding), horizontalArrangement = Arrangement.SpaceEvenly) {
                ContributionStatItem(label = "Total", value = "$totalCount", icon = Icons.Outlined.Receipt, color = goalColor)
                if (latest != null) {
                    ContributionStatItem(label = "Latest", value = formatCurrencyCompact(kotlin.math.abs(latest.amount)), icon = Icons.Outlined.TrendingUp, color = SuccessColor)
                }
                if (average > 0) {
                    ContributionStatItem(label = "Average", value = formatCurrencyCompact(average), icon = Icons.Outlined.Analytics, color = PurpleViolet)
                }
                lastActivity?.let {
                    ContributionStatItem(label = "Last", value = it.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM")), icon = Icons.Outlined.Schedule, color = AppPalette.textMuted)
                }
            }
        }
    }
}

@Composable
private fun ContributionHistorySectionLegacy(contributions: List<Contribution>, goalColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Contribution History", subtitle = "Recent transactions")
        Spacer(Modifier.height(12.dp))
        if (contributions.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.History, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("No contributions yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
                    Text("Start saving to see your progress here", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column {
                    contributions.take(5).forEachIndexed { index, contribution ->
                        val isWithdrawal = contribution.isWithdrawal
                        val itemColor = if (isWithdrawal) ExpenseRed else SuccessColor
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(itemColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(if (isWithdrawal) Icons.Outlined.ArrowUpward else Icons.Outlined.Add, null, tint = itemColor, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isWithdrawal) "Withdrawal" else "Saved", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                                val date = contribution.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime()
                                Text("${date.dayOfMonth} ${date.month.name.take(3)}", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                            }
                            Text("${if (isWithdrawal) "-" else "+"}${formatCurrencyFull(kotlin.math.abs(contribution.amount))}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = itemColor)
                        }
                        if (index < contributions.take(5).lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSectionLegacy(goal: Goal, contributions: List<Contribution>, goalColor: Color, modifier: Modifier = Modifier) {
    val events = buildList {
        add(GoalTimelineEvent("created", TimelineEventType.GOAL_CREATED, "Goal Created", "\"${goal.name}\" was created", goal.createdAt))
        val firstContrib = contributions.filter { it.isAddition }.minByOrNull { it.createdAt }
        firstContrib?.let {
            add(GoalTimelineEvent("first", TimelineEventType.FIRST_CONTRIBUTION, "First Contribution", "Started saving towards the goal", it.createdAt, it.amount))
        }
        if (goal.isCompleted) {
            add(GoalTimelineEvent("completed", TimelineEventType.GOAL_COMPLETED, "Goal Achieved!", "Successfully reached the target amount", goal.updatedAt))
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Journey", subtitle = "Milestones")
        Spacer(Modifier.height(12.dp))
        if (events.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Timeline, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("No timeline events", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
                    Text("Your journey will appear here", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
                    events.forEachIndexed { index, event ->
                        TimelineEventItem(event = event, goalColor = goalColor, isLast = index == events.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTargetCard(goal: Goal, dailyTarget: DailyTarget, goalColor: Color, onSetTarget: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.CardInnerPaddingLarge)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Flag, null, tint = goalColor, modifier = Modifier.size(22.dp))
                    Text("Daily Target", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
                }
                TextButton(onClick = onSetTarget) {
                    Text(if (dailyTarget.isSet) "Edit" else "Set", style = MaterialTheme.typography.labelMedium, color = goalColor)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (dailyTarget.isSet) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(formatCurrencyFull(dailyTarget.targetAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                        Text("per day", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = if (dailyTarget.isCompleted) SuccessColor.copy(alpha = 0.12f) else goalColor.copy(alpha = 0.12f)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(if (dailyTarget.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.TrendingUp, null, tint = if (dailyTarget.isCompleted) SuccessColor else goalColor, modifier = Modifier.size(16.dp))
                            Text(if (dailyTarget.isCompleted) "Done today!" else "${dailyTarget.progressPercent.toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = if (dailyTarget.isCompleted) SuccessColor else goalColor)
                        }
                    }
                }
            } else {
                val suggestedDaily = if ((goal.daysRemaining ?: 0) > 0) goal.remainingAmount / (goal.daysRemaining ?: 1) else goal.remainingAmount
                Column {
                    Text(formatCurrencyFull(suggestedDaily), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                    Text("per day suggested", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                    goal.deadline?.let {
                        Text("Based on ${goal.daysRemaining ?: 0} days remaining", style = MaterialTheme.typography.labelSmall, color = goalColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesSection(notes: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Notes", subtitle = "Details")
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Text(notes, style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun ActionButtonsRow(goal: Goal, goalColor: Color, onSave: () -> Unit, onWithdraw: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (goal.currentAmount > 0) {
            OutlinedButton(onClick = onWithdraw, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textMuted), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Withdraw", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(onClick = onSave, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = goalColor), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
