package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.ZoneId

// ─── Active Goal Card ─────────────────────────────────────────────────────────

@Composable
fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goalColor = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { AppPalette.textPrimary }
    val isCompleted = goal.status == GoalStatus.COMPLETED
    val isPaused = goal.status == GoalStatus.PAUSED
    val isOverdue = goal.isOverdue
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressPercent.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "progress"
    )

    when {
        isCompleted -> CompletedGoalCard(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, onClick = onClick, onArchive = onEdit, modifier = modifier)
        isPaused -> PausedGoalCard(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, onClick = onClick, onResume = onEdit, modifier = modifier)
        else -> ActiveGoalCard(goal = goal, goalColor = goalColor, animatedProgress = animatedProgress, isOverdue = isOverdue, onClick = onClick, onContribute = onContribute, onWithdraw = onWithdraw, onMoreClick = onEdit, modifier = modifier)
    }
}

@Composable
private fun ActiveGoalCard(
    goal: Goal,
    goalColor: Color,
    animatedProgress: Float,
    isOverdue: Boolean,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: Icon + Name + Deadline + Overflow ──────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGoalIcon(goal.iconName),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (goal.deadline != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        DeadlineBadge(
                            daysLeft = goal.daysRemaining ?: 0,
                            deadline = DateFormatter.formatShortDate(goal.deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()),
                            isOverdue = isOverdue
                        )
                    }
                }
                IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.goal_card_options),
                        tint = AppPalette.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Amount Row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = NumberFormatter.formatCurrencyCompact(goal.currentAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.label_of_amount, NumberFormatter.formatCurrencyCompact(goal.targetAmount)),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
                Text(
                    text = "${goal.progressPercent.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        goal.progressPercent >= 75 -> goalColor
                        else -> AppPalette.textMuted
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Progress Bar ───────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppPalette.cardBorder)) {
                Box(
                    modifier = Modifier.fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(brush = Brush.horizontalGradient(colors = listOf(goalColor.copy(alpha = 0.8f), goalColor)))
                )
            }

            // ── Action Row ─────────────────────────────────────────────────
            if (goal.remainingAmount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = goalColor.copy(alpha = 0.08f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Flag, contentDescription = null, tint = goalColor, modifier = Modifier.size(11.dp))
                            Text(
                                text = stringResource(R.string.goal_card_left, NumberFormatter.formatCurrencyCompact(goal.remainingAmount)),
                                style = MaterialTheme.typography.labelSmall,
                                color = goalColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (goal.currentAmount > 0) {
                        Surface(
                            onClick = onWithdraw,
                            shape = RoundedCornerShape(50.dp),
                            color = AppPalette.cardElevated,
                            border = BorderStroke(1.dp, AppPalette.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.ArrowUpward, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(13.dp))
                                Text(text = stringResource(R.string.goal_card_withdraw), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                            }
                        }
                    }
                    Button(
                        onClick = onContribute,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = goalColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.goal_card_save), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Paused Goal Card ─────────────────────────────────────────────────────────

@Composable
private fun PausedGoalCard(
    goal: Goal,
    goalColor: Color,
    animatedProgress: Float,
    onClick: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mutedColor = goalColor.copy(alpha = 0.45f)
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(mutedColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = getGoalIcon(goal.iconName), contentDescription = null, tint = mutedColor, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    // Paused pill
                    Surface(shape = RoundedCornerShape(10.dp), color = AppPalette.textMuted.copy(alpha = 0.12f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.PauseCircleOutline, null, tint = AppPalette.textMuted, modifier = Modifier.size(12.dp))
                            Text(stringResource(R.string.goal_card_paused), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Amount (muted) ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = NumberFormatter.formatCurrencyCompact(goal.currentAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary.copy(alpha = 0.5f))
                    Text(text = stringResource(R.string.label_of_amount, NumberFormatter.formatCurrencyCompact(goal.targetAmount)), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted.copy(alpha = 0.6f))
                }
                Text(text = "${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textMuted.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Progress Bar (muted) ───────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppPalette.cardBorder.copy(alpha = 0.5f))) {
                Box(
                    modifier = Modifier.fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(mutedColor)
                )
            }

            // ── Resume button ──────────────────────────────────────────────
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppPalette.cardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textMuted)
            ) {
                Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.goal_resume), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Completed Goal Card ──────────────────────────────────────────────────────

@Composable
private fun CompletedGoalCard(
    goal: Goal,
    goalColor: Color,
    animatedProgress: Float,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(SuccessColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = SuccessColor.copy(alpha = 0.12f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.EmojiEvents, null, tint = SuccessColor, modifier = Modifier.size(12.dp))
                            Text(stringResource(R.string.goal_card_completed), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SuccessColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Amount (success) ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = NumberFormatter.formatCurrencyCompact(goal.currentAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SuccessColor)
                    Text(text = stringResource(R.string.goal_completed_target, NumberFormatter.formatCurrencyCompact(goal.targetAmount)), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                }
                Text(text = "100%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SuccessColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Progress Bar (full, success) ───────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(SuccessColor.copy(alpha = 0.15f))) {
                Box(
                    modifier = Modifier.fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(SuccessColor.copy(alpha = 0.7f), SuccessColor)))
                )
            }

            // ── Archive row ────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.goal_tap_to_view_history), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                TextButton(onClick = onArchive) {
                    Icon(Icons.Outlined.Archive, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.goal_archive), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Shared Components ────────────────────────────────────────────────────────

@Composable
private fun DeadlineBadge(daysLeft: Int, deadline: String, isOverdue: Boolean) {
    val badgeColor = when {
        isOverdue -> ExpenseRed
        daysLeft <= 7 -> WarningYellow
        else -> AppPalette.textMuted
    }
    Surface(shape = RoundedCornerShape(6.dp), color = badgeColor.copy(alpha = 0.1f)) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = badgeColor, modifier = Modifier.size(10.dp))
            Text(
                text = when {
                    isOverdue -> stringResource(R.string.goal_card_overdue)
                    daysLeft <= 0 -> stringResource(R.string.goal_card_due_today)
                    daysLeft <= 7 -> stringResource(R.string.goal_card_days_left, daysLeft)
                    else -> deadline
                },
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CompactGoalCard(goal: Goal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val goalColor = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { AppPalette.textPrimary }
    val isCompleted = goal.status == GoalStatus.COMPLETED
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = getGoalIcon(goal.iconName), contentDescription = null, tint = goalColor, modifier = Modifier.size(20.dp))
                }
                Text(text = "${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isCompleted) SuccessColor else goalColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = NumberFormatter.formatCurrencyCompact(goal.currentAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
            Text(text = stringResource(R.string.label_of_amount, NumberFormatter.formatCurrencyCompact(goal.targetAmount)), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppPalette.cardBorder)) {
                Box(
                    modifier = Modifier.fillMaxWidth((goal.progressPercent / 100).toFloat()).fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isCompleted) SuccessColor else goalColor)
                )
            }
        }
    }
}

// ─── Archived Goal Row (for ArchivedGoalsSheet) ───────────────────────────────

@Composable
fun ArchivedGoalRow(
    goal: Goal,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goalColor = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { AppPalette.textPrimary }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(goalColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = getGoalIcon(goal.iconName), contentDescription = null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = goal.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${NumberFormatter.formatCurrencyCompact(goal.currentAmount)} / ${NumberFormatter.formatCurrencyCompact(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            // Restore
            IconButton(onClick = onRestore, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Restore, contentDescription = stringResource(R.string.goal_restore), tint = SuccessColor, modifier = Modifier.size(20.dp))
            }
            // Delete permanently
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.goal_delete_permanently), tint = ExpenseRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}
