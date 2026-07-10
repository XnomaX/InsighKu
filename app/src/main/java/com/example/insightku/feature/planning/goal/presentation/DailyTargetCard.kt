package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.planning.goal.domain.model.DailyTarget

@Composable
fun DailyTargetCard(goal: com.example.insightku.feature.planning.goal.domain.model.Goal, dailyTarget: DailyTarget, goalColor: androidx.compose.ui.graphics.Color, onSetTarget: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val progress = dailyTarget.progressPercent; val isCompleted = dailyTarget.isCompleted; val isSet = dailyTarget.isSet
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isCompleted) SuccessColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isCompleted) SuccessColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(imageVector = if (isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Flag, contentDescription = null, tint = if (isCompleted) SuccessColor else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column { Text(text = if (isCompleted) stringResource(R.string.daily_target_achieved) else stringResource(R.string.daily_saving_target), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); if (isSet) { Text(text = dailyTarget.date.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
                if (isSet) { IconButton(onClick = onEdit) { Icon(imageVector = Icons.Outlined.Edit, contentDescription = stringResource(R.string.daily_target_edit)) } }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isSet) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column { Text(text = "$${String.format("%.2f", dailyTarget.currentAmount).replace(".00", "")}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (isCompleted) SuccessColor else MaterialTheme.colorScheme.onSurface); Text(text = stringResource(R.string.daily_target_of_today, "${NumberFormatter.formatCurrency(dailyTarget.targetAmount)}"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(text = "${progress.toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (isCompleted) SuccessColor else MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                GoalProgressBar(progress = progress.toFloat(), progressColor = if (isCompleted) SuccessColor else MaterialTheme.colorScheme.primary, showPercentage = false)
                if (!isCompleted && dailyTarget.remainingAmount > 0) { Spacer(modifier = Modifier.height(8.dp)); Text(text = stringResource(R.string.daily_target_more_to_reach, NumberFormatter.formatCurrency(dailyTarget.remainingAmount)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                Text(text = stringResource(R.string.daily_target_set_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onSetTarget, modifier = Modifier.fillMaxWidth()) { Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.daily_target_set_button)) }
            }
        }
    }
}

@Composable fun CompactDailyTarget(dailyTarget: DailyTarget, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val progress = dailyTarget.progressPercent; val isCompleted = dailyTarget.isCompleted
    Surface(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)), color = if (isCompleted) SuccessColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant, onClick = onClick) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Flag, contentDescription = null, tint = if (isCompleted) SuccessColor else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(text = if (isCompleted) stringResource(R.string.daily_target_done) else stringResource(R.string.daily_target_of_today, "${NumberFormatter.formatCurrency(dailyTarget.currentAmount)} / ${NumberFormatter.formatCurrency(dailyTarget.targetAmount)}"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
            Text(text = "${progress.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isCompleted) SuccessColor else MaterialTheme.colorScheme.primary)
        }
    }
}
