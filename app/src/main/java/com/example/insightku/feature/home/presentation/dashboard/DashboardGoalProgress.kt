package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.IncomeGreen
import com.example.insightku.feature.home.presentation.formatCurrencyShort
import com.example.insightku.feature.planning.goal.domain.model.Goal
import com.example.insightku.feature.planning.goal.presentation.getGoalIcon

@Composable
fun GoalsPreviewSection(
    goals: List<Goal>,
    totalCount: Int,
    isBalanceVisible: Boolean,
    onClickGoal: (String) -> Unit,
    onClickViewAll: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateGoal: () -> Unit = {}
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.dashboard_goals),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                val subtitle = when {
                    totalCount == 0 -> stringResource(R.string.dashboard_no_active_goals)
                    totalCount <= 3 -> pluralStringResource(
                        R.plurals.dashboard_active_goals_count,
                        totalCount
                    )

                    else -> pluralStringResource(
                        R.plurals.dashboard_goals_of_count,
                        goals.size,
                        totalCount
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            if (totalCount > 3) {
                Surface(
                    modifier = Modifier.clickable(onClick = onClickViewAll),
                    shape    = RoundedCornerShape(50.dp),
                    color    = AppPalette.card,
                    border   = BorderStroke(1.dp, NavPurple)
                ) {
                    Text(
                        stringResource(R.string.dashboard_view_all),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NavPurple
                    )
                }
            }
        }

        if (goals.isEmpty()) {
            // Empty state
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(Dimens.CardRadiusLarge),
                color    = AppPalette.card,
                border   = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(NavPurple.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            tint = NavPurple.copy(alpha = 0.45f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        stringResource(R.string.dashboard_no_active_goals),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                    Surface(
                        modifier = Modifier.clickable(onClick = onCreateGoal),
                        shape = RoundedCornerShape(Dimens.ButtonRadius),
                        color = NavPurple
                    ) {
                        Text(
                            stringResource(R.string.dashboard_create_goal),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Goal items
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
                color           = AppPalette.card,
                tonalElevation  = 0.dp,
                shadowElevation = Dimens.ElevationSmall,
                border          = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    goals.forEachIndexed { index, goal ->
                        GoalPreviewItem(
                            goal = goal,
                            isBalanceVisible = isBalanceVisible,
                            onClick = { onClickGoal(goal.id) }
                        )
                        if (index < goals.lastIndex) {
                            HorizontalDivider(
                                color = AppPalette.cardBorder,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = Dimens.CardInnerPadding)
                            )
                        }
                    }
                }
            }

            if (totalCount > 3) {
                Text(
                    pluralStringResource(R.plurals.dashboard_more_goals, totalCount - 3),
                    style = MaterialTheme.typography.labelMedium,
                    color = NavPurple,
                    modifier = Modifier
                        .clickable(onClick = onClickViewAll)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalPreviewItem(
    goal: Goal,
    isBalanceVisible: Boolean,
    onClick: () -> Unit
) {
    val icon = getGoalIcon(goal.iconName)
    val iconColor = try {
        Color(goal.color.toColorInt())
    } catch (_: Exception) {
        NavPurple
    }
    val progressColor = when {
        goal.progressPercent >= 100.0 -> IncomeGreen
        else -> NavPurple
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }

        // Name + progress bar
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((goal.progressPercent / 100.0).coerceIn(0.0, 1.0).toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressColor)
                )
            }
            // Amount
            Text(
                text = if (isBalanceVisible) "${formatCurrencyShort(goal.currentAmount)} / ${formatCurrencyShort(goal.targetAmount)}" else "·····",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }

        // Percentage badge
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = progressColor.copy(alpha = 0.10f)
        ) {
            Text(
                text = "${goal.progressPercent.toInt()}%",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }
    }
}
