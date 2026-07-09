package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.components.dialogs.PremiumSuccessOverlay
import com.example.insightku.core.ui.components.dialogs.PremiumDeleteConfirmDialog
import com.example.insightku.core.ui.components.dialogs.PremiumArchiveConfirmDialog
import com.example.insightku.core.i18n.NumberFormatter

@Composable
internal fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.example.insightku.core.ui.theme.AppPalette.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = com.example.insightku.core.ui.theme.AppPalette.textMuted)
    }
}

@Composable
internal fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = com.example.insightku.core.ui.theme.AppPalette.textMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
internal fun GoalDetailSuccessOverlay(message: String) {
    PremiumSuccessOverlay(message = message)
}

@Composable
internal fun GoalDeleteConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PremiumDeleteConfirmDialog(itemName = goalName, onDismiss = onDismiss, onConfirm = onConfirm, message = "Are you sure you want to permanently delete \"$goalName\"? This action cannot be undone.")
}

@Composable
internal fun GoalArchiveConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PremiumArchiveConfirmDialog(itemName = goalName, onDismiss = onDismiss, onConfirm = onConfirm, message = "Are you sure you want to archive \"$goalName\"? You can view archived goals in settings.")
}

@Composable
internal fun GoalDeleteAutoAllocationRuleConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PremiumDeleteConfirmDialog(itemName = "auto-allocation rule", onDismiss = onDismiss, onConfirm = onConfirm, message = "Are you sure you want to delete this auto-allocation rule? Automatic savings for this rule will stop.")
}

internal fun formatCurrencyFull(amount: Double): String {
    return NumberFormatter.formatCurrency(amount)
}
