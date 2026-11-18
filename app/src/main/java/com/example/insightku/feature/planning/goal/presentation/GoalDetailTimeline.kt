package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import java.time.ZoneId

@Composable
internal fun TimelineSection(events: List<GoalTimelineEvent>, goalColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.goal_savings_journey), subtitle = stringResource(R.string.goal_savings_journey_desc))
        Spacer(Modifier.height(Dimens.PaddingLarge))
        if (events.isEmpty()) {
            EmptyTimelineCard(goalColor)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
                    events.forEachIndexed { index, event ->
                        TimelineEventItem(event = event, goalColor = goalColor, isLast = index == events.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TimelineEventItem(event: GoalTimelineEvent, goalColor: androidx.compose.ui.graphics.Color, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(Dimens.PaddingMedium)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppPalette.textMuted)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(Dimens.PaddingExtraLarge + Dimens.PaddingLarge)
                        .background(AppPalette.cardBorder)
                )
            }
        }
        Spacer(Modifier.width(Dimens.PaddingMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
            Text(event.description, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            Text(DateFormatter.formatShortDate(event.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
        }
        event.amount?.let { amount ->
            Text(
                "${if (amount > 0) "+" else ""}${NumberFormatter.formatCurrencyCompact(kotlin.math.abs(amount))}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textPrimary
            )
        }
    }
    if (!isLast) Spacer(Modifier.height(Dimens.PaddingLarge))
}

@Composable
internal fun EmptyTimelineCard(goalColor: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingExtraLarge + Dimens.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.goal_contrib_no_milestones),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(Dimens.PaddingSmall))
            Text(
                stringResource(R.string.goal_contrib_start_saving),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
