package com.example.insightku.feature.accounts.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.Composable
import com.example.insightku.core.ui.components.dialogs.PremiumDialog
import com.example.insightku.core.ui.components.dialogs.PremiumDialogType

/**
 * Delete account confirmation dialog.
 * Used by both AccountDetailSheet and AccountRow in AccountsScreen,
 * and by EditAccountDialog.
 */
@Composable
fun DeleteAccountConfirmDialog(
    accountName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PremiumDialog(
        type = PremiumDialogType.DELETE,
        customIcon = Icons.Filled.DeleteForever,
        title = "Delete Account?",
        message = "Are you sure you want to delete \"$accountName\"? This action cannot be undone and all transactions in this account will also be permanently deleted.",
        confirmText = "Delete",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
