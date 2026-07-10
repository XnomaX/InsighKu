package com.example.insightku.feature.accounts.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
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
        title = stringResource(R.string.account_delete_confirm),
        message = stringResource(R.string.account_delete_message, accountName),
        confirmText = stringResource(R.string.account_delete_button),
        dismissText = stringResource(R.string.cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
