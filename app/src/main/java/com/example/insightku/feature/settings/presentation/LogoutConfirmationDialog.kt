package com.example.insightku.feature.settings.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.components.dialogs.PremiumDialog
import com.example.insightku.core.ui.components.dialogs.PremiumDialogType

/** Logout uses the shared destructive dialog, like DeleteAccountConfirmDialog. */
@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PremiumDialog(
        type = PremiumDialogType.DELETE,
        customIcon = Icons.AutoMirrored.Filled.ExitToApp,
        title = stringResource(R.string.settings_logout_title),
        message = stringResource(R.string.settings_logout_message),
        confirmText = stringResource(R.string.settings_logout_button),
        dismissText = stringResource(R.string.settings_logout_stay),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
