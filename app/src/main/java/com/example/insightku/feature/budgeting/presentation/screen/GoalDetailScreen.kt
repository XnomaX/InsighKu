package com.example.insightku.feature.budgeting.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.core.ui.theme.PurpleViolet
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.formatCurrencyCompactIDR
import androidx.compose.ui.text.style.TextOverflow
import com.example.insightku.feature.budgeting.data.model.AllocationTriggerType
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule
import com.example.insightku.feature.budgeting.domain.model.Contribution
import com.example.insightku.feature.budgeting.domain.model.DailyTarget
import com.example.insightku.feature.budgeting.domain.model.Goal
import com.example.insightku.feature.budgeting.presentation.components.getGoalIcon
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
 * - Premium hero with gradient icon, name, subtitle, and progress chip
 * - Summary card with visual separators and highlighted saved amount
 * - Progress card with thicker bar, milestone markers, and concise layout
 * - Contribution summary as mini cards in a 2x2 grid
 * - Contribution history, timeline, actions, and danger zone
 * - Edge-to-edge with proper WindowInsets support
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

    LaunchedEffect(goalId) {
        if (goalId.isNotEmpty()) {
            viewModel.onEvent(GoalDetailEvent.LoadGoal(goalId))
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(GoalDetailEvent.ClearSnackbar)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(GoalDetailEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        if (uiState.snackbarMessage == "Goal archived" || uiState.snackbarMessage == "Goal deleted") {
            onGoalArchived()
        }
    }

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

            if (uiState.showContributeDialog || uiState.showWithdrawDialog) {
                ContributionDialog(uiState = uiState, onEvent = viewModel::onEvent)
            }

            if (uiState.showSuccessAnimation) {
                SuccessOverlay(message = uiState.successMessage)
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
        item { PremiumDetailHeader(goal = goal, goalColor = goalColor, onBack = onBack) }
        item { GoalSummaryCard(goal = goal, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ProgressSection(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ContributionSummarySectionLegacy(contributions = contributions, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ContributionHistorySectionLegacy(contributions = contributions, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); TimelineSectionLegacy(goal = goal, contributions = contributions, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); DailyTargetCard(goal = goal, dailyTarget = dailyTarget, goalColor = goalColor, onSetTarget = onSetDailyTarget, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        if (goal.notes.isNotBlank()) {
            item { Spacer(Modifier.height(12.dp)); NotesSection(notes = goal.notes, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }
        if (!goal.isPaused) {
            item { Spacer(Modifier.height(12.dp)); ActionButtonsRow(goal = goal, goalColor = goalColor, onSave = onSave, onWithdraw = onWithdraw, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
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
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
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
        item { PremiumDetailHeader(goal = goal, goalColor = goalColor, onBack = onBack) }
        item { GoalSummaryCard(goal = goal, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ProgressSection(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ContributionSummarySection(uiState = uiState, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); ContributionHistorySection(contributions = uiState.contributions, accountMap = uiState.accountMap, goalColor = goalColor, isLoadingMore = uiState.isLoadingMore, hasMore = uiState.hasMoreContributions, onLoadMore = { onEvent(GoalDetailEvent.LoadMoreContributions) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); TimelineSection(events = uiState.timelineEvents, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        item { Spacer(Modifier.height(12.dp)); AutoAllocationSection(goal = goal, rules = uiState.allocationRules, goalColor = goalColor, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        if (!goal.isPaused) {
            item { Spacer(Modifier.height(12.dp)); ActionButtonsSection(goal = goal, goalColor = goalColor, onContribute = { onEvent(GoalDetailEvent.ShowContributeDialog) }, onWithdraw = { onEvent(GoalDetailEvent.ShowWithdrawDialog) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
        }
        item { Spacer(Modifier.height(24.dp)); DangerZoneSection(onArchive = { onEvent(GoalDetailEvent.ArchiveGoal) }, onDelete = { onEvent(GoalDetailEvent.DeleteGoal) }, modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)) }
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

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Premium Hero Header ─────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumDetailHeader(
    goal: Goal,
    goalColor: Color,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Top bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = AppPalette.textPrimary)
            }
        }

            // Icon container with gradient background
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Outer gradient glow
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    goalColor.copy(alpha = 0.18f),
                                    goalColor.copy(alpha = 0.06f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGoalIcon(goal.iconName),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Goal name
            Text(
                goal.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            Text(
                "Saving Goal",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Progress chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val chipColor = when {
                    goal.isCompleted -> SuccessColor
                    else -> goalColor
                }
                val chipText = when {
                    goal.isCompleted -> "✓ Completed"
                    else -> "${goal.progressPercent.toInt()}% saved"
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = chipColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, chipColor.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Mini inline progress bar
                        if (!goal.isCompleted) {
                            val miniProgress = (goal.progressPercent.toFloat() / 100f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(chipColor.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(miniProgress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(chipColor)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint = chipColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            chipText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = chipColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Goal Summary Card ───────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun GoalSummaryCard(
    goal: Goal,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Row 1: Target | Saved | Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = "Target",
                    value = formatCurrencyCompactIDR(goal.targetAmount),
                    modifier = Modifier.weight(1f)
                )
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(AppPalette.cardBorder)
                )
                SummaryItem(
                    label = "Saved",
                    value = formatCurrencyCompactIDR(goal.currentAmount),
                    valueColor = if (goal.isCompleted) SuccessColor else goalColor,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(AppPalette.cardBorder)
                )
                SummaryItem(
                    label = "Remaining",
                    value = formatCurrencyCompactIDR(goal.remainingAmount),
                    valueColor = AppPalette.textMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            // Separator
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = AppPalette.cardBorder, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // Row 2: Deadline | Days Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                goal.deadline?.let { deadline ->
                    val daysText = when {
                        goal.isCompleted -> "Done"
                        goal.isOverdue -> "Overdue"
                        goal.daysRemaining == 0 -> "Today"
                        else -> "${goal.daysRemaining} days"
                    }
                    val daysColor = when {
                        goal.isOverdue -> ExpenseRed
                        (goal.daysRemaining ?: 0) <= 7 -> WarningYellow
                        else -> AppPalette.textPrimary
                    }
                    SummaryItem(
                        label = "Deadline",
                        value = deadline.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(AppPalette.cardBorder)
                    )
                    SummaryItem(
                        label = "Days Left",
                        value = daysText,
                        valueColor = daysColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                } ?: SummaryItem(
                    label = "Started",
                    value = goal.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM")),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    valueColor: Color = AppPalette.textPrimary,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.textMuted
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = fontWeight,
            color = valueColor
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Progress Section ────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProgressSection(
    goal: Goal,
    goalColor: Color,
    animatedProgress: Float,
    modifier: Modifier = Modifier
) {
    val barColor = if (goal.isCompleted) SuccessColor else goalColor
    val progress = animatedProgress.coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Top row: percentage pill + saved/target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Percentage chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = barColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${goal.progressPercent.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Saved / Target text
                Text(
                    "${formatCurrencyCompactIDR(goal.currentAmount)} / ${formatCurrencyCompactIDR(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted
                )
            }

            Spacer(Modifier.height(16.dp))

            // Thick progress bar with milestone markers
            Box(modifier = Modifier.fillMaxWidth()) {
                // Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(AppPalette.cardBorder)
                )

                // Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    barColor.copy(alpha = 0.7f),
                                    barColor
                                )
                            )
                        )
                )

                // Milestone markers at 25%, 50%, 75%
                listOf(0.25f, 0.5f, 0.75f).forEach { milestone ->
                    val isPassed = progress >= milestone
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(milestone)
                            .height(14.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isPassed) Color.White else AppPalette.cardBorder)
                                
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Remaining amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (goal.isCompleted) "Goal achieved!" else "${formatCurrencyCompactIDR(goal.remainingAmount)} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (goal.isCompleted) SuccessColor else AppPalette.textMuted
                )
                if (goal.isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuccessColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Check, null, tint = SuccessColor, modifier = Modifier.size(12.dp))
                            Text(
                                "Done",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Contribution Summary Section ────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ContributionSummarySection(
    uiState: GoalDetailUiState,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Contribution Summary", subtitle = "Your saving statistics")
        Spacer(Modifier.height(12.dp))

        // 2x2 grid of mini stat cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContributionMiniCard(
                    label = "Total",
                    value = "${uiState.totalContributions}",
                    subtitle = "contributions",
                    icon = Icons.Outlined.Receipt,
                    color = goalColor,
                    modifier = Modifier.weight(1f)
                )
                uiState.latestContribution?.let { latest ->
                    ContributionMiniCard(
                        label = "Latest",
                        value = formatCurrencyCompactIDR(kotlin.math.abs(latest.amount)),
                        subtitle = "last saved",
                        icon = Icons.Outlined.TrendingUp,
                        color = SuccessColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.averageContribution > 0) {
                    ContributionMiniCard(
                        label = "Average",
                        value = formatCurrencyCompactIDR(uiState.averageContribution),
                        subtitle = "per contribution",
                        icon = Icons.Outlined.Analytics,
                        color = PurpleViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
                uiState.lastActivityDate?.let { date ->
                    ContributionMiniCard(
                        label = "Last Active",
                        value = date.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM")),
                        subtitle = "activity",
                        icon = Icons.Outlined.Schedule,
                        color = AppPalette.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributionMiniCard(
    label: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AppPalette.textPrimary
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.textMuted.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Contribution History Section ────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ContributionHistorySection(
    contributions: List<Contribution>,
    accountMap: Map<String, com.example.insightku.core.data.model.Account>,
    goalColor: Color,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Contribution History", subtitle = "Recent transactions")
        Spacer(Modifier.height(12.dp))
        if (contributions.isEmpty()) {
            EmptyContributionsCard(goalColor)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
                Column {
                    contributions.forEachIndexed { index, contribution ->
                        val isWithdrawal = contribution.isWithdrawal
                        val itemColor = if (isWithdrawal) ExpenseRed else SuccessColor
                        val account = accountMap[contribution.accountId]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(itemColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isWithdrawal) Icons.Outlined.ArrowUpward else Icons.Outlined.Add,
                                    null,
                                    tint = itemColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isWithdrawal) "Withdrawal" else "Saved",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = AppPalette.textPrimary
                                )
                                Text(
                                    account?.name ?: "Unknown Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppPalette.textMuted
                                )
                                val date = contribution.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime()
                                Text(
                                    "${date.dayOfMonth} ${date.month.name.take(3)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppPalette.textMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${if (isWithdrawal) "-" else "+"}${formatCurrencyFull(kotlin.math.abs(contribution.amount))}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = itemColor
                                )
                                if (contribution.notes.isNotBlank()) {
                                    Text(
                                        contribution.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppPalette.textMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (index < contributions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = AppPalette.cardBorder
                            )
                        }
                    }
                    if (hasMore) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = goalColor,
                                    strokeWidth = 2.dp
                                )
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
    Card(
        modifier = Modifier.fillMaxWidth(),                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(goalColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.History, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "No contributions yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary
            )
            Text(
                "Start saving to see your progress here",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Timeline Section ────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun TimelineSection(
    events: List<GoalTimelineEvent>,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Journey", subtitle = "Milestones")
        Spacer(Modifier.height(12.dp))
        if (events.isEmpty()) {
            EmptyTimelineCard(goalColor)
        } else {
            Card(        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            events.forEachIndexed { index, event ->
                TimelineEventItem(
                    event = event,
                    goalColor = goalColor,
                    isLast = index == events.lastIndex
                )
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(iconColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(AppPalette.cardBorder)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary
            )
            Text(
                event.description,
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
            Text(
                event.date.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
        }
        event.amount?.let { amount ->
            Text(
                "${if (amount > 0) "+" else ""}${formatCurrencyCompactIDR(kotlin.math.abs(amount))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (amount > 0) SuccessColor else ExpenseRed
            )
        }
    }
    if (!isLast) Spacer(Modifier.height(12.dp))
}

@Composable
private fun EmptyTimelineCard(goalColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(goalColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Timeline, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "No timeline events",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary
            )
            Text(
                "Your journey will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Reserved Auto Allocation Section ────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun AutoAllocationSection(
    goal: Goal,
    rules: List<AutoAllocationRule>,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Auto Allocation",
            subtitle = if (rules.isEmpty()) "No rules set up yet" else "${rules.size} active rule${if (rules.size != 1) "s" else ""}"
        )
        Spacer(Modifier.height(12.dp))

        if (rules.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.CardInnerPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        null,
                        tint = goalColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Automatic savings",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppPalette.textPrimary
                        )
                        Text(
                            "Set up rules to save automatically from your income or accounts",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.textMuted
                        )
                    }
                }
            }
        } else {
            // Show active rules
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    rules.forEachIndexed { index, rule ->
                        AutoAllocationRuleItem(rule = rule, goalColor = goalColor)
                        if (index < rules.lastIndex) {
                            HorizontalDivider(
                                color = AppPalette.cardBorder,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = Dimens.CardInnerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoAllocationRuleItem(
    rule: AutoAllocationRule,
    goalColor: Color
) {
    val triggerIcon = when (rule.triggerType) {
        AllocationTriggerType.INCOME_RECEIVED -> Icons.Outlined.TrendingUp
        AllocationTriggerType.SPENDING_CATEGORY -> Icons.Outlined.Category
        AllocationTriggerType.ROUND_UP -> Icons.Outlined.ChangeHistory
        AllocationTriggerType.DAILY, AllocationTriggerType.WEEKLY,
        AllocationTriggerType.BIWEEKLY, AllocationTriggerType.MONTHLY -> Icons.Outlined.Schedule
        AllocationTriggerType.BALANCE_ABOVE -> Icons.Outlined.AccountBalance
    }
    val statusColor = if (rule.isEnabled) SuccessColor else AppPalette.textMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(goalColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(triggerIcon, null, tint = goalColor, modifier = Modifier.size(20.dp))
        }

        // Description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.triggerLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = rule.description,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Status badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusColor.copy(alpha = 0.10f)
        ) {
            Text(
                text = if (rule.isEnabled) "Active" else "Off",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Action Buttons Section ─────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ActionButtonsSection(
    goal: Goal,
    goalColor: Color,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Actions", subtitle = "Manage your savings")
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (goal.currentAmount > 0) {
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textMuted),
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Withdraw", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = onContribute,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = goalColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Danger Zone Section ────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun DangerZoneSection(
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Danger Zone",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ExpenseRed
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onArchive)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Archive, null, tint = WarningYellow, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                "Archive Goal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppPalette.textPrimary
                            )
                            Text(
                                "Hide this goal from your active list",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.textMuted
                            )
                        }
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted)
                }
                HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                "Delete Goal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppPalette.textPrimary
                            )
                            Text(
                                "Permanently remove this goal and all its data",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.textMuted
                            )
                        }
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Dialogs ────────────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributionDialog(uiState: GoalDetailUiState, onEvent: (GoalDetailEvent) -> Unit) {
    val isWithdraw = uiState.showWithdrawDialog
    val goal = uiState.goal ?: return
    val goalColor = remember(goal.color) {
        try { Color(android.graphics.Color.parseColor(goal.color)) }
        catch (e: Exception) { PurpleViolet }
    }
    // Parse amount for live calculations
    val amount = uiState.contributionAmount.toDoubleOrNull() ?: 0.0
    val currentSaved = goal.currentAmount
    val target = goal.targetAmount
    val newTotal = if (isWithdraw) (currentSaved - amount).coerceAtLeast(0.0) else currentSaved + amount
    val remainingAfter = (target - newTotal).coerceAtLeast(0.0)
    val currentPercent = if (target > 0) (currentSaved / target * 100).coerceIn(0.0, 100.0) else 0.0
    val newPercent = if (target > 0) (newTotal / target * 100).coerceIn(0.0, 100.0) else 0.0
    val isAmountValid = amount > 0 && (!isWithdraw || amount <= currentSaved)
    val exceedsTarget = !isWithdraw && newTotal > target

    // Animated progress for the preview bar
    val animatedNewPercent by animateFloatAsState(
        targetValue = newPercent.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "previewProgress"
    )

    ModalBottomSheet(
        onDismissRequest = { onEvent(GoalDetailEvent.DismissDialog) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppPalette.cardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // ── Header ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getGoalIcon(goal.iconName),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isWithdraw) "Withdraw from Goal" else "Add Contribution",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        goal.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textMuted
                    )
                }
            }

            // Pending sync indicator
            if (uiState.hasUnsyncedChanges) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningYellow.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CloudUpload,
                            contentDescription = null,
                            tint = WarningYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Pending sync",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = WarningYellow
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Amount Input ──
            Column {
                Text(
                    "Amount",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.contributionAmount,
                    onValueChange = {
                        // Only allow digits and one decimal point
                        val filtered = it.filter { c -> c.isDigit() || c == '.' }
                        if (filtered.count { c -> c == '.' } <= 1) {
                            onEvent(GoalDetailEvent.UpdateAmount(filtered))
                        }
                    },
                    placeholder = {
                        Text("0", color = AppPalette.textMuted.copy(alpha = 0.5f))
                    },
                    prefix = {
                        Text(
                            "Rp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = goalColor
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = goalColor,
                        unfocusedBorderColor = AppPalette.cardBorder,
                        focusedContainerColor = goalColor.copy(alpha = 0.04f),
                        unfocusedContainerColor = AppPalette.cardElevated
                    ),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                )
                if (exceedsTarget) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Amount exceeds remaining target (${formatCurrencyCompactIDR(goal.remainingAmount)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningYellow
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Quick Amount Chips ──
            if (!isWithdraw) {
                val quickAmounts = listOf(50_000.0, 100_000.0, 250_000.0, 500_000.0, 1_000_000.0)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickAmounts.size) { index ->
                        val chipAmount = quickAmounts[index]
                        val isSelected = uiState.contributionAmount.toDoubleOrNull() == chipAmount
                        Surface(
                            onClick = {
                                onEvent(GoalDetailEvent.UpdateAmount(chipAmount.toLong().toString()))
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) goalColor.copy(alpha = 0.15f) else AppPalette.cardElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) goalColor.copy(alpha = 0.4f) else AppPalette.cardBorder
                            )
                        ) {
                            Text(
                                "+${formatCurrencyCompactIDR(chipAmount).removePrefix("Rp")}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) goalColor else AppPalette.textMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Account Selector (Dropdown Style) ──
            val displayAccounts = uiState.linkedAccounts.ifEmpty { uiState.accountMap.values.toList() }
            if (displayAccounts.isNotEmpty()) {
                val selectedAccount = displayAccounts.find { it.id == uiState.selectedAccountId }
                Text(
                    "From Account",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted
                )
                Spacer(Modifier.height(8.dp))
                AccountDropdownSelector(
                    account = selectedAccount,
                    isWithdraw = isWithdraw,
                    onClick = { onEvent(GoalDetailEvent.ShowAccountPicker) }
                )
                Spacer(Modifier.height(4.dp))
            }

            // Account Picker Sheet
            if (uiState.showAccountPicker && displayAccounts.isNotEmpty()) {
                AccountPickerSheet(
                    accounts = displayAccounts,
                    selectedAccountId = uiState.selectedAccountId,
                    isWithdraw = isWithdraw,
                    onAccountSelected = {
                        onEvent(GoalDetailEvent.SelectAccount(it.id))
                        onEvent(GoalDetailEvent.HideAccountPicker)
                    },
                    onDismiss = { onEvent(GoalDetailEvent.HideAccountPicker) }
                )
            }

            // ── Progress Preview ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AppPalette.cardElevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Current Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.textMuted
                            )
                            Text(
                                "${currentPercent.toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppPalette.textPrimary
                            )
                        }
                        Icon(
                            Icons.Outlined.ArrowForward,
                            null,
                            tint = AppPalette.textMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (isAmountValid) "After ${if (isWithdraw) "Withdrawal" else "Contribution"}" else "After",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.textMuted
                            )
                            Text(
                                "${newPercent.toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAmountValid) goalColor else AppPalette.textPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Animated progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AppPalette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedNewPercent)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(goalColor.copy(alpha = 0.7f), goalColor)
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Contribution Summary ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AppPalette.cardElevated
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryRow("Current Saved", formatCurrencyFull(currentSaved), AppPalette.textPrimary)
                    HorizontalDivider(color = AppPalette.cardBorder)
                    SummaryRow(
                        if (isWithdraw) "Withdrawal" else "Contribution",
                        "${if (isWithdraw) "-" else "+"}${formatCurrencyFull(amount)}",
                        if (isAmountValid) (if (isWithdraw) ExpenseRed else SuccessColor) else AppPalette.textMuted
                    )
                    HorizontalDivider(color = AppPalette.cardBorder)
                    SummaryRow("New Total Saved", formatCurrencyFull(newTotal), goalColor)
                    SummaryRow("Remaining After", formatCurrencyFull(remainingAfter), AppPalette.textMuted)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Insufficient Funds Warning ──
            if (uiState.isInsufficientFunds) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Insufficient Funds",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                            Text(
                                "You need ${CurrencyUtils.formatAmountCompact(uiState.shortfall)} more in this account",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Notes ──
            OutlinedTextField(
                value = uiState.contributionNotes,
                onValueChange = { onEvent(GoalDetailEvent.UpdateNotes(it)) },
                placeholder = { Text("Add a note (optional)", color = AppPalette.textMuted.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPalette.cardBorder,
                    unfocusedBorderColor = AppPalette.cardBorder,
                    focusedContainerColor = AppPalette.cardElevated,
                    unfocusedContainerColor = AppPalette.cardElevated
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppPalette.textPrimary)
            )

            Spacer(Modifier.height(24.dp))

            // ── Action Buttons ──
            val isSubmitting = uiState.isSubmitting
            Button(
                onClick = {
                    if (isWithdraw) onEvent(GoalDetailEvent.SubmitWithdrawal)
                    else onEvent(GoalDetailEvent.SubmitContribution)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isAmountValid && !exceedsTarget && !isSubmitting,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWithdraw) WarningYellow else goalColor,
                    disabledContainerColor = AppPalette.cardBorder
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 4.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        if (isWithdraw) "Withdraw" else "Save Contribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmountValid && !exceedsTarget) Color.White else AppPalette.textMuted
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { onEvent(GoalDetailEvent.DismissDialog) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Account Dropdown Selector ─────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun AccountDropdownSelector(
    account: Account?,
    isWithdraw: Boolean,
    onClick: () -> Unit
) {
    val accountColor = remember(account?.color) {
        try { Color(android.graphics.Color.parseColor(account?.color ?: "#9C27B0")) }
        catch (e: Exception) { PurpleViolet }
    }
    val accountIcon = when (account?.type) {
        AccountType.CASH -> Icons.Outlined.Payments
        AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance
        AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet
        AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
        else -> Icons.Outlined.AccountBalance
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppPalette.cardElevated,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Account icon in a tinted circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accountColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    accountIcon,
                    contentDescription = null,
                    tint = accountColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Account name and balance
            Column(modifier = Modifier.weight(1f)) {
                if (account != null) {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        CurrencyUtils.formatAmountCompact(account.balance),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (account.balance > 0) SuccessColor else ExpenseRed
                    )
                } else {
                    Text(
                        if (isWithdraw) "Select destination account" else "Select source account",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppPalette.textMuted
                    )
                }
            }

            // Chevron
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppPalette.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Account Picker Sheet ──────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerSheet(
    accounts: List<Account>,
    selectedAccountId: String?,
    isWithdraw: Boolean,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                if (isWithdraw) "To Account" else "From Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
        },
        text = {
            Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isWithdraw) "To Account" else "From Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = AppPalette.textMuted
                    )
                }
            }

            HorizontalDivider(color = AppPalette.cardBorder)

            // Account list
            accounts.forEachIndexed { index, account ->
                val isSelected = account.id == selectedAccountId
                val accountColor = remember(account.color) {
                    try { Color(android.graphics.Color.parseColor(account.color)) }
                    catch (e: Exception) { PurpleViolet }
                }
                val accountIcon = when (account.type) {
                    AccountType.CASH -> Icons.Outlined.Payments
                    AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance
                    AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet
                    AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAccountSelected(account) }
                        .background(
                            if (isSelected) accountColor.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Account icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accountColor.copy(alpha = 0.15f)
                                else AppPalette.cardElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            accountIcon,
                            contentDescription = null,
                            tint = if (isSelected) accountColor else AppPalette.textMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Account info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = AppPalette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                CurrencyUtils.formatAmountCompact(account.balance),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (account.balance > 0) SuccessColor else ExpenseRed
                            )
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.textMuted.copy(alpha = 0.5f)
                            )
                            Text(
                                when (account.type) {
                                    AccountType.CASH -> "Cash"
                                    AccountType.BANK_ACCOUNT -> "Bank"
                                    AccountType.E_WALLET -> "E-Wallet"
                                    AccountType.CREDIT_CARD -> "Credit Card"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.textMuted
                            )
                        }
                    }

                    // Checkmark for selected
                    if (isSelected) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = "Selected",
                            tint = accountColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (index < accounts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = AppPalette.cardBorder
                    )
                }
            }
        }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppPalette.textMuted)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Success Overlay ───────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun SuccessOverlay(message: String) {
    com.example.insightku.core.ui.components.dialogs.PremiumSuccessOverlay(
        message = message
    )
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun DeleteConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    com.example.insightku.core.ui.components.dialogs.PremiumDeleteConfirmDialog(
        itemName = goalName,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        message = "Are you sure you want to permanently delete \"$goalName\"? This action cannot be undone."
    )
}

@Composable
private fun ArchiveConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    com.example.insightku.core.ui.components.dialogs.PremiumArchiveConfirmDialog(
        itemName = goalName,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        message = "Are you sure you want to archive \"$goalName\"? You can view archived goals in settings."
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Section Header ─────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Helper Functions ───────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════



private fun formatCurrencyFull(amount: Double): String {
    return "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())}"
}



// ═══════════════════════════════════════════════════════════════════════════════════
// ─── Legacy Composables (Dialog-based Entry Point) ─────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ContributionSummarySectionLegacy(
    contributions: List<Contribution>,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    val additions = contributions.filter { it.isAddition }
    val totalCount = additions.size
    val latest = contributions.maxByOrNull { it.createdAt }
    val average = if (additions.isNotEmpty()) additions.sumOf { kotlin.math.abs(it.amount) } / additions.size else 0.0
    val lastActivity = latest?.createdAt

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Contribution Summary", subtitle = "Your saving statistics")
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContributionMiniCard(
                    label = "Total",
                    value = "$totalCount",
                    subtitle = "contributions",
                    icon = Icons.Outlined.Receipt,
                    color = goalColor,
                    modifier = Modifier.weight(1f)
                )
                if (latest != null) {
                    ContributionMiniCard(
                        label = "Latest",
                        value = formatCurrencyCompactIDR(kotlin.math.abs(latest.amount)),
                        subtitle = "last saved",
                        icon = Icons.Outlined.TrendingUp,
                        color = SuccessColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (average > 0) {
                    ContributionMiniCard(
                        label = "Average",
                        value = formatCurrencyCompactIDR(average),
                        subtitle = "per contribution",
                        icon = Icons.Outlined.Analytics,
                        color = PurpleViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (lastActivity != null) {
                    ContributionMiniCard(
                        label = "Last Active",
                        value = lastActivity.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM")),
                        subtitle = "activity",
                        icon = Icons.Outlined.Schedule,
                        color = AppPalette.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributionHistorySectionLegacy(
    contributions: List<Contribution>,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Contribution History", subtitle = "Recent transactions")
        Spacer(Modifier.height(12.dp))
        if (contributions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(goalColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.History, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No contributions yet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        "Start saving to see your progress here",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    contributions.take(5).forEachIndexed { index, contribution ->
                        val isWithdrawal = contribution.isWithdrawal
                        val itemColor = if (isWithdrawal) ExpenseRed else SuccessColor
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(itemColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isWithdrawal) Icons.Outlined.ArrowUpward else Icons.Outlined.Add,
                                    null,
                                    tint = itemColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isWithdrawal) "Withdrawal" else "Saved",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = AppPalette.textPrimary
                                )
                                val date = contribution.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime()
                                Text(
                                    "${date.dayOfMonth} ${date.month.name.take(3)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppPalette.textMuted
                                )
                            }
                            Text(
                                "${if (isWithdrawal) "-" else "+"}${formatCurrencyFull(kotlin.math.abs(contribution.amount))}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = itemColor
                            )
                        }
                        if (index < contributions.take(5).lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = AppPalette.cardBorder
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSectionLegacy(
    goal: Goal,
    contributions: List<Contribution>,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(goalColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Timeline, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No timeline events",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        "Your journey will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    events.forEachIndexed { index, event ->
                        TimelineEventItem(event = event, goalColor = goalColor, isLast = index == events.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTargetCard(
    goal: Goal,
    dailyTarget: DailyTarget,
    goalColor: Color,
    onSetTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Flag, null, tint = goalColor, modifier = Modifier.size(22.dp))
                    Text(
                        "Daily Target",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                }
                TextButton(onClick = onSetTarget) {
                    Text(
                        if (dailyTarget.isSet) "Edit" else "Set",
                        style = MaterialTheme.typography.labelMedium,
                        color = goalColor
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (dailyTarget.isSet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            formatCurrencyFull(dailyTarget.targetAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.textPrimary
                        )
                        Text("per day", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (dailyTarget.isCompleted) SuccessColor.copy(alpha = 0.12f) else goalColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (dailyTarget.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.TrendingUp,
                                null,
                                tint = if (dailyTarget.isCompleted) SuccessColor else goalColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                if (dailyTarget.isCompleted) "Done today!" else "${dailyTarget.progressPercent.toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dailyTarget.isCompleted) SuccessColor else goalColor
                            )
                        }
                    }
                }
            } else {
                val suggestedDaily = if ((goal.daysRemaining ?: 0) > 0) goal.remainingAmount / (goal.daysRemaining ?: 1) else goal.remainingAmount
                Column {
                    Text(
                        formatCurrencyFull(suggestedDaily),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    Text("per day suggested", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                    goal.deadline?.let {
                        Text(
                            "Based on ${goal.daysRemaining ?: 0} days remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = goalColor
                        )
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                notes,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    goal: Goal,
    goalColor: Color,
    onSave: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (goal.currentAmount > 0) {
            OutlinedButton(
                onClick = onWithdraw,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textMuted),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Withdraw", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = goalColor),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
        ) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
