package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.PurpleViolet
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import java.time.ZoneId

@Composable
internal fun TimelineSection(events: List<GoalTimelineEvent>, goalColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Savings Journey", subtitle = "Track your milestones")
        Spacer(Modifier.height(12.dp))
        if (events.isEmpty()) {
            EmptyTimelineCard(goalColor)
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                Column(modifier = Modifier.padding(20.dp)) {
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
    val iconColor = when (event.type) {
        TimelineEventType.GOAL_COMPLETED -> SuccessColor
        TimelineEventType.WITHDRAWAL -> ExpenseRed
        TimelineEventType.MILESTONE_25, TimelineEventType.MILESTONE_50, TimelineEventType.MILESTONE_75, TimelineEventType.MILESTONE_90 -> goalColor
        else -> PurpleViolet
    }
    val icon = when (event.type) {
        TimelineEventType.GOAL_CREATED -> Icons.Outlined.Flag
        TimelineEventType.FIRST_CONTRIBUTION -> Icons.Outlined.Star
        TimelineEventType.MILESTONE_25, TimelineEventType.MILESTONE_50, TimelineEventType.MILESTONE_75, TimelineEventType.MILESTONE_90 -> Icons.Outlined.Stars
        TimelineEventType.GOAL_COMPLETED -> Icons.Outlined.CheckCircle
        TimelineEventType.WITHDRAWAL -> Icons.Outlined.ArrowDownward
        else -> Icons.Outlined.Event
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(iconColor))
            if (!isLast) { Box(modifier = Modifier.width(2.dp).height(40.dp).background(AppPalette.cardBorder)) }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text(event.description, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
            Text(DateFormatter.formatShortDate(event.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
        }
        event.amount?.let { amount ->
            Text("${if (amount > 0) "+" else ""}${NumberFormatter.formatCurrencyCompact(kotlin.math.abs(amount))}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (amount > 0) SuccessColor else ExpenseRed)
        }
    }
    if (!isLast) Spacer(Modifier.height(12.dp))
}

@Composable
internal fun EmptyTimelineCard(goalColor: androidx.compose.ui.graphics.Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Flag, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("No milestones yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Start saving to unlock your first milestone", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
