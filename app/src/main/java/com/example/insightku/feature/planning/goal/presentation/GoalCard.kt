package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.core.ui.theme.formatCurrencyCompactIDR
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.format.DateTimeFormatter

@Composable
fun GoalCard(goal: Goal, onClick: () -> Unit, onContribute: () -> Unit, onWithdraw: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val goalColor = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { AppPalette.textPrimary }
    val isCompleted = goal.status == GoalStatus.COMPLETED
    val isPaused = goal.status == GoalStatus.PAUSED
    val isOverdue = goal.isOverdue
    val animatedProgress by animateFloatAsState(targetValue = goal.progressPercent.toFloat() / 100f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f), label = "progress")
    val statusColor = when { isCompleted -> SuccessColor; goal.progressPercent >= 75 -> goalColor; goal.progressPercent >= 50 -> goalColor; else -> AppPalette.textMuted }
    Card(modifier = modifier.fillMaxWidth().animateContentSize().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(goalColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(imageVector = getGoalIcon(goal.iconName), contentDescription = null, tint = goalColor, modifier = Modifier.size(22.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (goal.deadline != null && !isCompleted) { Spacer(modifier = Modifier.height(2.dp)); DeadlineBadge(daysLeft = goal.daysRemaining ?: 0, deadline = goal.deadline.format(DateTimeFormatter.ofPattern("d MMM yyyy")), isOverdue = isOverdue) }
                }
                if (isCompleted || isPaused) { StatusBadge(isCompleted = isCompleted, isPaused = isPaused) } else { IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "Options", tint = AppPalette.textMuted, modifier = Modifier.size(18.dp)) } }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column { Text(text = formatCurrencyCompactIDR(goal.currentAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isCompleted) SuccessColor else AppPalette.textPrimary); Text(text = "of ${formatCurrencyCompactIDR(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted) }
                Text(text = "${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = statusColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppPalette.cardBorder)) {
                Box(modifier = Modifier.fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(brush = Brush.horizontalGradient(colors = listOf(if (isCompleted) SuccessColor else goalColor.copy(alpha = 0.8f), if (isCompleted) SuccessColor else goalColor))))
            }
            if (!isPaused) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!isCompleted && goal.remainingAmount > 0) { Surface(shape = RoundedCornerShape(8.dp), color = goalColor.copy(alpha = 0.08f)) { Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(imageVector = Icons.Outlined.Flag, contentDescription = null, tint = goalColor, modifier = Modifier.size(11.dp)); Text(text = "${formatCurrencyCompactIDR(goal.remainingAmount)} left", style = MaterialTheme.typography.labelSmall, color = goalColor, fontWeight = FontWeight.Medium) } } }
                    Spacer(modifier = Modifier.weight(1f))
                    if (goal.currentAmount > 0) { Surface(onClick = onWithdraw, shape = RoundedCornerShape(50.dp), color = AppPalette.cardElevated, border = BorderStroke(1.dp, AppPalette.cardBorder)) { Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(imageVector = Icons.Outlined.ArrowUpward, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(13.dp)); Text(text = "Withdraw", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted) } } }
                    Button(onClick = onContribute, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp), shape = RoundedCornerShape(50.dp), colors = ButtonDefaults.buttonColors(containerColor = goalColor), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) { Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Save", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable private fun DeadlineBadge(daysLeft: Int, deadline: String, isOverdue: Boolean) { val badgeColor = when { isOverdue -> ExpenseRed; daysLeft <= 7 -> WarningYellow; else -> AppPalette.textMuted }; Surface(shape = RoundedCornerShape(6.dp), color = badgeColor.copy(alpha = 0.1f)) { Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null, tint = badgeColor, modifier = Modifier.size(10.dp)); Text(text = when { isOverdue -> "Overdue"; daysLeft <= 0 -> "Due today"; daysLeft <= 7 -> "$daysLeft days left"; else -> deadline }, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.Medium) } } }

@Composable private fun StatusBadge(isCompleted: Boolean, isPaused: Boolean) { val (color, text, icon) = when { isCompleted -> Triple(SuccessColor, "Completed", Icons.Outlined.CheckCircle); else -> Triple(AppPalette.textMuted, "Paused", Icons.Outlined.Pause) }; Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) { Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp)); Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color) } } }

@Composable fun CompactGoalCard(goal: Goal, onClick: () -> Unit, modifier: Modifier = Modifier) { val goalColor = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { AppPalette.textPrimary }; val isCompleted = goal.status == GoalStatus.COMPLETED; Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(goalColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(imageVector = getGoalIcon(goal.iconName), contentDescription = null, tint = goalColor, modifier = Modifier.size(20.dp)) }; Text(text = "${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isCompleted) SuccessColor else goalColor) }; Spacer(modifier = Modifier.height(12.dp)); Text(text = goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(modifier = Modifier.height(4.dp)); Text(text = formatCurrencyCompactIDR(goal.currentAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary); Text(text = "of ${formatCurrencyCompactIDR(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted); Spacer(modifier = Modifier.height(12.dp)); Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppPalette.cardBorder)) { Box(modifier = Modifier.fillMaxWidth((goal.progressPercent / 100).toFloat()).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (isCompleted) SuccessColor else goalColor)) } } } }
