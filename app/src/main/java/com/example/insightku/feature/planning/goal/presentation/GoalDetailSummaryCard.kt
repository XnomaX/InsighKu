package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.ZoneId

@Composable
internal fun GoalSummaryCard(goal: Goal, goalColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryItem(label = stringResource(R.string.goal_summary_target), value = NumberFormatter.formatCurrencyCompact(goal.targetAmount), modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(AppPalette.cardBorder)
                )
                SummaryItem(label = stringResource(R.string.goal_summary_saved), value = NumberFormatter.formatCurrencyCompact(goal.currentAmount), valueColor = if (goal.isCompleted) SuccessColor else goalColor, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(AppPalette.cardBorder)
                )
                SummaryItem(label = stringResource(R.string.goal_summary_remaining), value = NumberFormatter.formatCurrencyCompact(goal.remainingAmount), valueColor = AppPalette.textMuted, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = AppPalette.cardBorder, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                goal.deadline?.let { deadline ->
                    val daysText = when {
                        goal.isCompleted -> stringResource(R.string.goal_summary_done); goal.isOverdue -> stringResource(
                            R.string.goal_summary_overdue
                        ); goal.daysRemaining == 0 -> stringResource(R.string.goal_summary_today); else -> pluralStringResource(
                            R.plurals.goal_summary_days,
                            goal.daysRemaining ?: 0
                        )
                    }
                    val daysColor = when { goal.isOverdue -> ExpenseRed; (goal.daysRemaining ?: 0) <= 7 -> WarningYellow; else -> AppPalette.textPrimary }
                    SummaryItem(label = stringResource(R.string.goal_summary_deadline), value = DateFormatter.formatShortDate(deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()), modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(AppPalette.cardBorder)
                    )
                    SummaryItem(label = stringResource(R.string.goal_summary_days_left), value = daysText, valueColor = daysColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                } ?: SummaryItem(label = stringResource(R.string.goal_summary_started), value = DateFormatter.formatShortDate(goal.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun SummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = AppPalette.textPrimary,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = fontWeight, color = valueColor)
    }
}
