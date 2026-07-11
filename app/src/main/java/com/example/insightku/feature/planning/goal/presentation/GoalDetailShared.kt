package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
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
internal fun GoalDetailSuccessOverlay(message: String) {
    PremiumSuccessOverlay(message = message)
}

@Composable
internal fun GoalDeleteConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PremiumDeleteConfirmDialog(itemName = goalName, onDismiss = onDismiss, onConfirm = onConfirm, message = stringResource(R.string.goal_confirm_delete_message, goalName))
}

@Composable
internal fun GoalArchiveConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PremiumArchiveConfirmDialog(itemName = goalName, onDismiss = onDismiss, onConfirm = onConfirm, message = stringResource(R.string.goal_confirm_archive_message, goalName))
}

@Composable
internal fun GoalDeleteAutoAllocationRuleConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PremiumDeleteConfirmDialog(itemName = stringResource(R.string.auto_alloc_rule), onDismiss = onDismiss, onConfirm = onConfirm, message = stringResource(R.string.goal_confirm_delete_rule_message))
}

internal fun formatCurrencyFull(amount: Double): String {
    return NumberFormatter.formatCurrency(amount)
}
