package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.ZoneId

// The goal's own color survives as a 6dp identity dot. Every other surface,
// action and figure uses the single app accent so contrast stays at 4.5:1.
@Composable
private fun goalDotColor(goal: Goal): Color = try {
    Color(goal.color.toColorInt())
} catch (_: Exception) {
    AppPalette.textPrimary
}

@Composable
fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressPercent.toFloat() / 100f,
        animationSpec = tween(300),
        label = "progress"
    )

    when {
        goal.status == GoalStatus.COMPLETED ->
            CompletedGoalCard(goal, animatedProgress, onClick, onEdit, modifier)
        goal.status == GoalStatus.PAUSED ->
            PausedGoalCard(goal, animatedProgress, onClick, onEdit, modifier)
        else ->
            ActiveGoalCard(goal, animatedProgress, onClick, onContribute, onWithdraw, onEdit, modifier)
    }
}

// ─── Shared shell: one radius, one padding, one hairline ──────────────────────

@Composable
private fun GoalCardShell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
            content()
        }
    }
}

@Composable
private fun GoalCardTitle(
    goal: Goal,
    dimmed: Boolean = false,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.PaddingSmall)
                .clip(RoundedCornerShape(2.dp))
                .background(goalDotColor(goal).copy(alpha = if (dimmed) 0.4f else 1f))
        )
        Text(
            text = goal.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = AppPalette.textPrimary.copy(alpha = if (dimmed) 0.6f else 1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun GoalMoreButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.goal_card_options),
            tint = AppPalette.textMuted,
            modifier = Modifier.size(Dimens.IconSizeMedium)
        )
    }
}

// ─── Shared amount + progress: hierarchy from size, never from weight ─────────

@Composable
private fun GoalAmountRow(goal: Goal, dimmed: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.goal_card_collected),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = AppPalette.textMuted
        )
        Text(
            text = NumberFormatter.formatCurrencyCompact(goal.currentAmount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = AppPalette.textPrimary.copy(alpha = if (dimmed) 0.6f else 1f)
        )
        Text(
            text = stringResource(
                R.string.label_of_amount,
                NumberFormatter.formatCurrencyCompact(goal.targetAmount)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted
        )
    }
}

@Composable
private fun GoalProgressSection(
    goal: Goal,
    progress: Float,
    dimmed: Boolean = false,
    completed: Boolean = false
) {
    val accent = LocalAccent.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.ProgressBarHeight)
                .clip(RoundedCornerShape(Dimens.PaddingSmall))
                .background(AppPalette.cardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Dimens.PaddingSmall))
                    .background(if (dimmed) AppPalette.textMuted.copy(alpha = 0.5f) else accent)
            )
        }
        Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${goal.progressPercent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
            // Signature: remaining amount closes the progress row.
            Text(
                text = if (completed) {
                    stringResource(R.string.goal_card_completed)
                } else {
                    stringResource(
                        R.string.goal_card_left,
                        NumberFormatter.formatCurrencyCompact(goal.remainingAmount)
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary.copy(alpha = if (dimmed) 0.6f else 1f)
            )
        }
    }
}

@Composable
private fun GoalDeadlineLine(goal: Goal) {
    val deadline = goal.deadline ?: return
    val date = DateFormatter.formatShortDate(
        deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val status = when {
        goal.isOverdue -> stringResource(R.string.goal_card_overdue)
        goal.daysRemaining == 0 -> stringResource(R.string.goal_card_due_today)
        else -> stringResource(R.string.goal_card_deadline, date)
    }
    Text(
        text = status,
        style = MaterialTheme.typography.labelSmall,
        color = AppPalette.textMuted
    )
}

// ─── Active ────────────────────────────────────────────────────────────────────

@Composable
private fun ActiveGoalCard(
    goal: Goal,
    progress: Float,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    GoalCardShell(onClick = onClick, modifier = modifier) {
        GoalCardTitle(goal) { GoalMoreButton(onMoreClick) }
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        GoalAmountRow(goal)
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        GoalProgressSection(goal, progress)
        GoalDeadlineLine(goal)
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onContribute,
                modifier = Modifier.height(Dimens.ButtonHeightSecondary),
                shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = AppPalette.textPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(Dimens.IconSizeSmall))
                Spacer(Modifier.width(Dimens.PaddingSmall))
                Text(
                    stringResource(R.string.goal_card_save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            if (goal.currentAmount > 0) {
                TextButton(onClick = onWithdraw) {
                    Icon(
                        Icons.Outlined.ArrowUpward,
                        null,
                        tint = AppPalette.textMuted,
                        modifier = Modifier.size(Dimens.IconSizeSmall)
                    )
                    Spacer(Modifier.width(Dimens.PaddingSmall))
                    Text(
                        stringResource(R.string.goal_card_withdraw),
                        style = MaterialTheme.typography.labelLarge,
                        color = AppPalette.textMuted
                    )
                }
            }
        }
    }
}

// ─── Paused ────────────────────────────────────────────────────────────────────

@Composable
private fun PausedGoalCard(
    goal: Goal,
    progress: Float,
    onClick: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    GoalCardShell(onClick = onClick, modifier = modifier) {
        GoalCardTitle(goal, dimmed = true) {
            Text(
                stringResource(R.string.goal_card_paused),
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
        }
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        GoalAmountRow(goal, dimmed = true)
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        GoalProgressSection(goal, progress, dimmed = true)
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        OutlinedButton(
            onClick = onResume,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.ButtonHeightSecondary),
            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
            border = BorderStroke(1.dp, AppPalette.cardBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textPrimary)
        ) {
            Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(Dimens.IconSizeMedium))
            Spacer(Modifier.width(Dimens.PaddingMedium))
            Text(
                stringResource(R.string.goal_resume),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Completed ─────────────────────────────────────────────────────────────────

@Composable
private fun CompletedGoalCard(
    goal: Goal,
    progress: Float,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    GoalCardShell(onClick = onClick, modifier = modifier) {
        GoalCardTitle(goal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Check,
                    null,
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(Dimens.IconSizeSmall)
                )
                Spacer(Modifier.width(Dimens.PaddingSmall))
                Text(
                    stringResource(R.string.goal_card_completed),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.textMuted
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        GoalAmountRow(goal)
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        GoalProgressSection(goal, progress, completed = true)
        Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.goal_tap_to_view_history),
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
            TextButton(onClick = onArchive) {
                Icon(
                    Icons.Outlined.Archive,
                    null,
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(Dimens.IconSizeSmall)
                )
                Spacer(Modifier.width(Dimens.PaddingSmall))
                Text(
                    stringResource(R.string.goal_archive),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}

// ─── Archived row (ArchivedGoalsSheet) ────────────────────────────────────────

@Composable
fun ArchivedGoalRow(
    goal: Goal,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.CardInnerPaddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.PaddingSmall)
                    .clip(RoundedCornerShape(2.dp))
                    .background(goalDotColor(goal).copy(alpha = 0.5f))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.goal_card_collected_of,
                        NumberFormatter.formatCurrencyCompact(goal.currentAmount),
                        NumberFormatter.formatCurrencyCompact(goal.targetAmount)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.goal_restore),
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.goal_delete_permanently),
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            }
        }
    }
}
