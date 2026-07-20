package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.components.bottomsheet.applyFiftyPercentDismissThreshold
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.feature.planning.goal.domain.model.Goal

// ─── Goal Actions Bottom Sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalActionsBottomSheet(
    goal: Goal,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onArchive: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val goalColor = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { AppPalette.accent }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(sheetState) { applyFiftyPercentDismissThreshold(sheetState) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(Dimens.BottomSheetRadius),
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(Dimens.BottomSheetHandleWidth)
                    .height(Dimens.BottomSheetHandleHeight)
                    .clip(RoundedCornerShape(Dimens.BottomSheetHandleRadius))
                    .background(AppPalette.cardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.BottomSheetContentPadding)
                .padding(bottom = Dimens.BottomSheetFooterPadding + 16.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = getGoalIcon(goal.iconName), contentDescription = null, tint = goalColor, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                    Text(
                        text = "${goal.progressPercent.toInt()}% · ${stringResource(R.string.goal_actions_sheet_target, com.example.insightku.core.i18n.NumberFormatter.formatCurrencyCompact(goal.targetAmount))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Status Actions ─────────────────────────────────────────────
            if (goal.isActive) {
                GoalActionRow(
                    icon = Icons.Outlined.PauseCircleOutline,
                    label = stringResource(R.string.goal_pause),
                    subtitle = stringResource(R.string.goal_pause_desc),
                    iconTint = WarningYellow,
                    onClick = onPause
                )
            }
            if (goal.isPaused) {
                GoalActionRow(
                    icon = Icons.Outlined.PlayCircleOutline,
                    label = stringResource(R.string.goal_resume),
                    subtitle = stringResource(R.string.goal_resume_desc),
                    iconTint = SuccessColor,
                    onClick = onResume
                )
            }
            if (!goal.isCompleted) {
                GoalActionRow(
                    icon = Icons.Outlined.CheckCircleOutline,
                    label = stringResource(R.string.goal_mark_complete),
                    subtitle = stringResource(R.string.goal_mark_complete_desc),
                    iconTint = SuccessColor,
                    onClick = onComplete
                )
            }

            HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(vertical = 8.dp))

            // ── Management ─────────────────────────────────────────────────
            GoalActionRow(
                icon = Icons.Outlined.Edit,
                label = stringResource(R.string.goal_edit),
                subtitle = stringResource(R.string.goal_edit_desc),
                iconTint = AppPalette.textMuted,
                onClick = onEdit
            )
            GoalActionRow(
                icon = Icons.Outlined.Archive,
                label = stringResource(R.string.goal_archive),
                subtitle = stringResource(R.string.goal_archive_desc),
                iconTint = AppPalette.textMuted,
                onClick = onArchive
            )
        }
    }
}

@Composable
private fun GoalActionRow(
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconTint.copy(alpha = 0.1f)),
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
