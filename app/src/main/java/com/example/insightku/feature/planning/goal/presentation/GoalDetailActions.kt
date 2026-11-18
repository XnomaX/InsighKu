package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
internal fun ActionButtonsSection(
    goal: Goal,
    goalColor: Color,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.goal_actions), subtitle = stringResource(R.string.goal_actions_desc))
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
            Button(
                onClick = onContribute,
                modifier = Modifier.height(Dimens.ButtonHeightPrimary),
                shape = RoundedCornerShape(Dimens.ButtonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = AppPalette.textPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(Dimens.IconSizeMedium))
                Spacer(Modifier.width(Dimens.PaddingMedium))
                Text(stringResource(R.string.goal_actions_save), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            }
            if (goal.currentAmount > 0) {
                TextButton(onClick = onWithdraw) {
                    Icon(Icons.Outlined.ArrowUpward, null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeSmall))
                    Spacer(Modifier.width(Dimens.PaddingSmall))
                    Text(stringResource(R.string.goal_actions_withdraw), style = MaterialTheme.typography.labelLarge, color = AppPalette.textMuted)
                }
            }
        }
    }
}

@Composable
internal fun DangerZoneSection(onArchive: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
                PlainActionRow(
                    icon = Icons.Outlined.Archive,
                    label = stringResource(R.string.goal_actions_archive_goal),
                    subtitle = stringResource(R.string.goal_actions_hide_from_list),
                    onClick = onArchive
                )
                HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(vertical = Dimens.PaddingMedium))
                PlainActionRow(
                    icon = Icons.Outlined.DeleteOutline,
                    label = stringResource(R.string.goal_actions_delete_goal),
                    subtitle = stringResource(R.string.goal_actions_permanently_remove),
                    onClick = onDelete
                )
            }
        }
    }
}

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
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
                if (goal.isActive) {
                    PlainActionRow(Icons.Outlined.PauseCircleOutline, stringResource(R.string.goal_pause), stringResource(R.string.goal_pause_desc), onPause)
                }
                if (goal.isPaused) {
                    PlainActionRow(Icons.Outlined.PlayCircleOutline, stringResource(R.string.goal_resume), stringResource(R.string.goal_resume_desc), onResume)
                }
                if (!goal.isCompleted) {
                    if (goal.isActive || goal.isPaused) {
                        HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(vertical = Dimens.PaddingMedium))
                    }
                    PlainActionRow(Icons.Outlined.Check, stringResource(R.string.goal_mark_complete), stringResource(R.string.goal_mark_complete_desc), onComplete)
                }
            }
        }
    }
}

@Composable
private fun PlainActionRow(icon: ImageVector, label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        Icon(icon, null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeMedium))
    }
}

@Composable
internal fun CompletedGoalBanner(goalName: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.CardInnerPaddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            Icon(Icons.Outlined.Check, null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.goal_celebration_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary
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
