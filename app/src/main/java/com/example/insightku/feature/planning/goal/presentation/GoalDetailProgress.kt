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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.formatCurrencyCompactIDR
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
internal fun ProgressSection(goal: Goal, goalColor: Color, animatedProgress: Float, modifier: Modifier = Modifier) {
    val barColor = if (goal.isCompleted) SuccessColor else goalColor
    val progress = animatedProgress.coerceIn(0f, 1f)
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = barColor.copy(alpha = 0.12f)) {
                    Text("${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = barColor, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                }
                Text("${formatCurrencyCompactIDR(goal.currentAmount)} / ${formatCurrencyCompactIDR(goal.targetAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
            }
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(AppPalette.cardBorder))
                Box(modifier = Modifier.fillMaxWidth(progress).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.7f), barColor))))
                listOf(0.25f, 0.5f, 0.75f).forEach { milestone ->
                    val isPassed = progress >= milestone
                    Box(modifier = Modifier.fillMaxWidth(milestone).height(14.dp), contentAlignment = Alignment.CenterEnd) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isPassed) Color.White else AppPalette.cardBorder))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (goal.isCompleted) "Goal achieved!" else "${formatCurrencyCompactIDR(goal.remainingAmount)} remaining", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = if (goal.isCompleted) SuccessColor else AppPalette.textMuted)
                if (goal.isCompleted) {
                    Surface(shape = RoundedCornerShape(8.dp), color = SuccessColor.copy(alpha = 0.12f)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Check, null, tint = SuccessColor, modifier = Modifier.size(12.dp))
                            Text("Done", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SuccessColor)
                        }
                    }
                }
            }
        }
    }
}
