package com.example.insightku.feature.budgeting.presentation.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.budgeting.data.model.GoalStatus
import com.example.insightku.feature.budgeting.domain.model.Contribution
import com.example.insightku.feature.budgeting.domain.model.DailyTarget
import com.example.insightku.feature.budgeting.domain.model.Goal
import com.example.insightku.feature.budgeting.presentation.components.GoalProgressBar
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Premium Goal Detail Screen with comprehensive goal information.
 *
 * Features:
 * - Beautiful hero header with goal details
 * - Large progress visualization
 * - Timeline/milestones for contribution history
 * - Recent activity with beautiful cards
 * - Action buttons
 * - Notes section
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val goalColor = try {
        Color(android.graphics.Color.parseColor(goal.color))
    } catch (e: Exception) {
        LocalAccent.current
    }

    val isCompleted = goal.status == GoalStatus.COMPLETED
    val isPaused = goal.status == GoalStatus.PAUSED
    val isOverdue = goal.isOverdue

    // Animation for progress
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressPercent.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "progress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppPalette.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // ── Premium Header ──────────────────────────────────────────────────────
        item {
            PremiumDetailHeader(
                goal = goal,
                goalColor = goalColor,
                isCompleted = isCompleted,
                onBack = onBack,
                onEdit = onEdit
            )
        }

        // ── Hero Progress Card ─────────────────────────────────────────────────
        item {
            HeroProgressCard(
                goal = goal,
                goalColor = goalColor,
                isCompleted = isCompleted,
                animatedProgress = animatedProgress,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // ── Stats Row ───────────────────────────────────────────────────────────
        item {
            StatsRow(
                goal = goal,
                goalColor = goalColor,
                isCompleted = isCompleted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        // ── Timeline Section ────────────────────────────────────────────────────
        if (contributions.isNotEmpty()) {
            item {
                TimelineSection(
                    contributions = contributions,
                    goalColor = goalColor,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // ── Recent Activity Section ────────────────────────────────────────────
        if (contributions.isNotEmpty()) {
            item {
                RecentActivitySection(
                    contributions = contributions,
                    goalColor = goalColor,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        } else {
            item {
                EmptyActivitySection(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }

        // ── Daily Target Card ──────────────────────────────────────────────────
        item {
            DailyTargetCard(
                goal = goal,
                dailyTarget = dailyTarget,
                goalColor = goalColor,
                onSetTarget = onSetDailyTarget,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // ── Notes Section ──────────────────────────────────────────────────────
        if (goal.notes.isNotBlank()) {
            item {
                NotesSection(
                    notes = goal.notes,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        // ── Action Buttons ─────────────────────────────────────────────────────
        if (!isPaused) {
            item {
                ActionButtonsRow(
                    goal = goal,
                    goalColor = goalColor,
                    onSave = onSave,
                    onWithdraw = onWithdraw,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    }
}

// ─── Premium Header ─────────────────────────────────────────────────────────────

@Composable
private fun PremiumDetailHeader(
    goal: Goal,
    goalColor: Color,
    isCompleted: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = AppPalette.textPrimary
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit goal",
                    tint = AppPalette.textMuted
                )
            }
        }

        // Goal icon and title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = goalColor.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                goalColor.copy(alpha = 0.2f),
                                goalColor.copy(alpha = 0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getGoalIcon(goal.iconName),
                    contentDescription = null,
                    tint = goalColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = goal.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )

            if (isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SuccessColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = SuccessColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Completed!",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessColor
                        )
                    }
                }
            }
        }
    }
}

// ─── Hero Progress Card ────────────────────────────────────────────────────────

@Composable
private fun HeroProgressCard(
    goal: Goal,
    goalColor: Color,
    isCompleted: Boolean,
    animatedProgress: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large percentage
            Text(
                text = "${goal.progressPercent.toInt()}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) SuccessColor else goalColor
            )

            Text(
                text = if (isCompleted) "Goal Achieved!" else "Progress",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    if (isCompleted) SuccessColor.copy(alpha = 0.8f) else goalColor.copy(alpha = 0.8f),
                                    if (isCompleted) SuccessColor else goalColor
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = formatCurrencyFull(goal.currentAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) SuccessColor else AppPalette.textPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = formatCurrencyFull(goal.targetAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                }
            }
        }
    }
}

// ─── Stats Row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(
    goal: Goal,
    goalColor: Color,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Remaining
        StatCard(
            title = "Remaining",
            value = if (isCompleted) "Rp 0" else formatCurrencyCompact(goal.remainingAmount),
            icon = Icons.Outlined.Flag,
            color = if (isCompleted) SuccessColor else goalColor,
            modifier = Modifier.weight(1f)
        )

        // Days left
        if (goal.deadline != null) {
            StatCard(
                title = "Days Left",
                value = when {
                    isCompleted -> "Done"
                    goal.isOverdue -> "Overdue"
                    goal.daysRemaining == 0 -> "Today"
                    else -> "${goal.daysRemaining}"
                },
                icon = Icons.Outlined.CalendarMonth,
                color = when {
                    goal.isOverdue -> ExpenseRed
                    (goal.daysRemaining ?: 0) <= 7 -> WarningYellow
                    else -> AppPalette.textMuted
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            StatCard(
                title = "Started",
                value = goal.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("d MMM")),
                icon = Icons.Outlined.Event,
                color = AppPalette.textMuted,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Stat Card ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
        }
    }
}

// ─── Timeline Section ──────────────────────────────────────────────────────────

@Composable
private fun TimelineSection(
    contributions: List<Contribution>,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    val recentContributions = contributions.take(5)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Journey",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                recentContributions.forEachIndexed { index, contribution ->
                    val isWithdrawal = contribution.isWithdrawal
                    val itemColor = if (isWithdrawal) ExpenseRed else SuccessColor

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Timeline indicator
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == 0) itemColor
                                        else itemColor.copy(alpha = 0.5f)
                                    )
                            )
                            if (index < recentContributions.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(40.dp)
                                        .background(AppPalette.cardBorder)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val date = contribution.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            val formattedDate = date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))

                            Text(
                                text = if (isWithdrawal) "Withdrawal" else "Saved",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPalette.textPrimary
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.textMuted
                            )
                        }

                        Text(
                            text = "${if (isWithdrawal) "-" else "+"}${formatCurrencyCompact(kotlin.math.abs(contribution.amount))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = itemColor
                        )
                    }

                    if (index < recentContributions.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// ─── Recent Activity Section ───────────────────────────────────────────────────

@Composable
private fun RecentActivitySection(
    contributions: List<Contribution>,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
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
                                imageVector = if (isWithdrawal) Icons.Outlined.ArrowUpward else Icons.Outlined.Add,
                                contentDescription = null,
                                tint = itemColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            val date = contribution.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                            val formattedDate = "${date.dayOfMonth} ${date.month.name.take(3)}"

                            Text(
                                text = if (isWithdrawal) "Withdrawal" else "Saved",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppPalette.textPrimary
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.textMuted
                            )
                        }

                        Text(
                            text = "${if (isWithdrawal) "-" else "+"}${formatCurrencyFull(kotlin.math.abs(contribution.amount))}",
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

// ─── Empty Activity Section ───────────────────────────────────────────────────

@Composable
private fun EmptyActivitySection(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LocalAccent.current.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = LocalAccent.current.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "No activity yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary
                )
                Text(
                    text = "Start saving to see your progress here",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}

// ─── Daily Target Card ─────────────────────────────────────────────────────────

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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Daily Target",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                }

                TextButton(onClick = onSetTarget) {
                    Text(
                        text = if (dailyTarget.isSet) "Edit" else "Set",
                        style = MaterialTheme.typography.labelMedium,
                        color = goalColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (dailyTarget.isSet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formatCurrencyFull(dailyTarget.targetAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.textPrimary
                        )
                        Text(
                            text = "per day",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (dailyTarget.isCompleted) SuccessColor.copy(alpha = 0.12f)
                        else goalColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (dailyTarget.isCompleted) Icons.Outlined.CheckCircle
                                else Icons.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = if (dailyTarget.isCompleted) SuccessColor else goalColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (dailyTarget.isCompleted) "Done today!"
                                else "${dailyTarget.progressPercent.toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dailyTarget.isCompleted) SuccessColor else goalColor
                            )
                        }
                    }
                }
            } else {
                // Show suggested daily target
                val suggestedDaily = calculateSuggestedDaily(goal)
                Column {
                    Text(
                        text = formatCurrencyFull(suggestedDaily),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        text = "per day suggested",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                    if (goal.deadline != null) {
                        Text(
                            text = "Based on ${goal.daysRemaining ?: 0} days remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = goalColor
                        )
                    }
                }
            }
        }
    }
}

// ─── Notes Section ─────────────────────────────────────────────────────────────

@Composable
private fun NotesSection(
    notes: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Notes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// ─── Action Buttons ────────────────────────────────────────────────────────────

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
        // Withdraw button (secondary)
        if (goal.currentAmount > 0) {
            OutlinedButton(
                onClick = onWithdraw,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppPalette.textMuted
                ),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Withdraw",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Save button (primary)
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = goalColor
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Save",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Helper Functions ──────────────────────────────────────────────────────────

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

private fun calculateSuggestedDaily(goal: Goal): Double {
    val daysLeft = goal.daysRemaining ?: return goal.remainingAmount
    if (daysLeft <= 0) return goal.remainingAmount
    return goal.remainingAmount / daysLeft
}

// Color references
private val ExpenseRed = Color(0xFFEF4444)
private val WarningYellow = Color(0xFFF59E0B)
