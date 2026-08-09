package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
internal fun ActionButtonsSection(
    goal: Goal,
    goalColor: Color,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.goal_actions), subtitle = stringResource(R.string.goal_actions_desc))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Text(stringResource(R.string.goal_actions_withdraw), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = onContribute,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = goalColor),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.goal_actions_save), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun DangerZoneSection(onArchive: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.goal_actions_danger_zone), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onArchive)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Archive, null, tint = WarningYellow, modifier = Modifier.size(24.dp))
                        Column { Text(stringResource(R.string.goal_actions_archive_goal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary); Text(stringResource(R.string.goal_actions_hide_from_list), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted) }
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
                        Column { Text(stringResource(R.string.goal_actions_delete_goal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary); Text(stringResource(R.string.goal_actions_permanently_remove), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted) }
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted)
                }
            }
        }
    }
}

// ─── Status-Aware Quick Actions ───────────────────────────────────────────────────

@Composable
internal fun StatusActionsSection(
    goal: Goal,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.goal_status_actions_title),
            subtitle = stringResource(R.string.goal_status_actions_subtitle)
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding)) {
                // Active goal → Pause
                if (goal.isActive) {
                    StatusActionRow(
                        icon = Icons.Outlined.PauseCircleOutline,
                        label = stringResource(R.string.goal_pause),
                        subtitle = stringResource(R.string.goal_pause_desc),
                        iconTint = WarningYellow,
                        onClick = onPause
                    )
                }
                // Paused goal → Resume
                if (goal.isPaused) {
                    StatusActionRow(
                        icon = Icons.Outlined.PlayCircleOutline,
                        label = stringResource(R.string.goal_resume),
                        subtitle = stringResource(R.string.goal_resume_desc),
                        iconTint = SuccessColor,
                        onClick = onResume
                    )
                }
                // Non-completed → Mark Complete
                if (!goal.isCompleted) {
                    if (goal.isActive || goal.isPaused) {
                        HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    StatusActionRow(
                        icon = Icons.Outlined.CheckCircleOutline,
                        label = stringResource(R.string.goal_mark_complete),
                        subtitle = stringResource(R.string.goal_mark_complete_desc),
                        iconTint = SuccessColor,
                        onClick = onComplete
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted, modifier = Modifier.size(18.dp))
    }
}

// ─── Completed Goal Banner ──────────────────────────────────────────────────────

@Composable
internal fun CompletedGoalBanner(goalName: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = SuccessColor.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SuccessColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = SuccessColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.goal_celebration_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SuccessColor
                )
                Text(
                    text = stringResource(R.string.goal_completed_banner_desc, goalName),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}
