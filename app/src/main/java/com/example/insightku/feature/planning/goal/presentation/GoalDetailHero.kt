package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
internal fun PremiumDetailHeader(goal: Goal, goalColor: androidx.compose.ui.graphics.Color, onBack: () -> Unit, onEdit: (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Back button — top-left row, clearly above the goal icon
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), tint = AppPalette.textPrimary)
            }
            if (onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Outlined.Edit, stringResource(R.string.add_goal_edit_title), tint = AppPalette.textPrimary)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Goal icon — centered horizontally, clearly below the arrow
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(goalColor.copy(alpha = 0.18f), goalColor.copy(alpha = 0.06f)),
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
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // ── Goal Name ───────────────────────────────────────────────────────
        Spacer(Modifier.height(28.dp))
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

        // ── Category / Subtitle ─────────────────────────────────────────────
        Spacer(Modifier.height(4.dp))
        val subtitleText = when {
            goal.isCompleted -> stringResource(R.string.goal_hero_goal_achieved)
            goal.isPaused -> stringResource(R.string.goal_hero_goal_paused)
            goal.isOverdue -> stringResource(R.string.goal_hero_overdue_goal)
            else -> stringResource(R.string.goal_hero_saving_goal)
        }
        val subtitleColor = when {
            goal.isCompleted -> SuccessColor
            goal.isOverdue -> ExpenseRed
            else -> AppPalette.textMuted
        }
        Text(
            subtitleText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = subtitleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Bottom padding before next LazyColumn item
        Spacer(Modifier.height(8.dp))
    }
}
