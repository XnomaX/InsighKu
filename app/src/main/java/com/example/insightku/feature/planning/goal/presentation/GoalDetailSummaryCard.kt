package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.ZoneId

@Composable
internal fun GoalSummaryCard(goal: Goal, goalColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
            Text(
                text = stringResource(R.string.goal_summary_saved),
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
            Text(
                text = NumberFormatter.formatCurrencyCompact(goal.currentAmount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary
            )
            Spacer(Modifier.height(Dimens.PaddingMedium))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = stringResource(R.string.goal_summary_target),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = NumberFormatter.formatCurrencyCompact(goal.targetAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textPrimary
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.goal_summary_remaining),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = NumberFormatter.formatCurrencyCompact(goal.remainingAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textPrimary
                    )
                }
                goal.deadline?.let { deadline ->
                    Column {
                        Text(
                            text = stringResource(R.string.goal_summary_deadline),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.textMuted
                        )
                        Text(
                            text = DateFormatter.formatShortDate(
                                deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppPalette.textPrimary
                        )
                    }
                }
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
    fontWeight: FontWeight = FontWeight.Medium
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = fontWeight, color = valueColor)
    }
}
