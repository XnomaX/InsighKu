package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
internal fun PremiumDetailHeader(goal: Goal, goalColor: androidx.compose.ui.graphics.Color, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {            Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back), tint = AppPalette.textPrimary) }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(goalColor.copy(alpha = 0.18f), goalColor.copy(alpha = 0.06f)), start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))), contentAlignment = Alignment.Center) {
                Icon(imageVector = getGoalIcon(goal.iconName), contentDescription = null, tint = goalColor, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(goal.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp))
        Spacer(Modifier.height(6.dp))
        val subtitleText = when { goal.isCompleted -> stringResource(R.string.goal_hero_goal_achieved); goal.isPaused -> stringResource(R.string.goal_hero_goal_paused); goal.isOverdue -> stringResource(R.string.goal_hero_overdue_goal); else -> stringResource(R.string.goal_hero_saving_goal) }
        val subtitleColor = when { goal.isCompleted -> SuccessColor; goal.isOverdue -> ExpenseRed; else -> AppPalette.textMuted }
        Text(subtitleText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = subtitleColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            val chipColor = when { goal.isCompleted -> SuccessColor; else -> goalColor }
            val chipText = when { goal.isCompleted -> stringResource(R.string.goal_hero_completed); else -> stringResource(R.string.goal_hero_pct_saved, goal.progressPercent.toInt()) }
            Surface(shape = RoundedCornerShape(20.dp), color = chipColor.copy(alpha = 0.12f), border = BorderStroke(1.dp, chipColor.copy(alpha = 0.15f))) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!goal.isCompleted) {
                        val miniProgress = (goal.progressPercent.toFloat() / 100f).coerceIn(0f, 1f)
                        Box(modifier = Modifier.width(48.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(chipColor.copy(alpha = 0.15f))) {
                            Box(modifier = Modifier.fillMaxWidth(miniProgress).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(chipColor))
                        }
                    } else { Icon(Icons.Outlined.CheckCircle, null, tint = chipColor, modifier = Modifier.size(14.dp)) }
                    Text(chipText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = chipColor)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
