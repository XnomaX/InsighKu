package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
internal fun ActionButtonsSection(goal: Goal, goalColor: androidx.compose.ui.graphics.Color, onContribute: () -> Unit, onWithdraw: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Goal Actions", subtitle = "Add or withdraw from this goal")
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (goal.currentAmount > 0) {
                OutlinedButton(onClick = onWithdraw, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textMuted), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                    Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Withdraw", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(onClick = onContribute, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = goalColor), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun DangerZoneSection(onArchive: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding)) {
                Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onArchive).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Archive, null, tint = WarningYellow, modifier = Modifier.size(24.dp))
                        Column { Text("Archive Goal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary); Text("Hide this goal from your active list", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted) }
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted)
                }
                HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
                        Column { Text("Delete Goal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary); Text("Permanently remove this goal and all its data", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted) }
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = AppPalette.textMuted)
                }
            }
        }
    }
}
