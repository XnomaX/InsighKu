package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.components.dialogs.PremiumArchiveConfirmDialog
import com.example.insightku.core.ui.components.dialogs.PremiumDeleteConfirmDialog
import com.example.insightku.core.ui.components.dialogs.PremiumSuccessOverlay

@Composable
internal fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
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

internal fun formatCurrencyFull(amount: Double): String {
    return NumberFormatter.formatCurrency(amount)
}
