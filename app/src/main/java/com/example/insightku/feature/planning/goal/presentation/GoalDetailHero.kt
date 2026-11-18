package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.feature.planning.goal.domain.model.Goal


@Composable
internal fun PremiumDetailHeader(goal: Goal, goalColor: Color, onBack: () -> Unit, onEdit: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.PaddingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), tint = AppPalette.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, stringResource(R.string.add_goal_edit_title), tint = AppPalette.textMuted)
                }
            }
        }
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Dimens.PaddingSmall)
                    .clip(RoundedCornerShape(2.dp))
                    .background(goalColor)
            )
            Spacer(Modifier.width(Dimens.PaddingMedium))
            Text(
                text = goal.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Text(
            text = when {
                goal.isCompleted -> stringResource(R.string.goal_hero_goal_achieved)
                goal.isPaused -> stringResource(R.string.goal_hero_goal_paused)
                goal.isOverdue -> stringResource(R.string.goal_hero_overdue_goal)
                else -> stringResource(R.string.goal_hero_saving_goal)
            },
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted
        )
        Spacer(Modifier.height(Dimens.PaddingLarge))
    }
}
