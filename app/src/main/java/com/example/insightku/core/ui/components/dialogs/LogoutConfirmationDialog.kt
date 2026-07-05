package com.example.insightku.core.ui.components.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable

@Composable
fun LogoutConfirmationDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    if (isOpen) {
        PremiumDialog(
            type = PremiumDialogType.WARNING,
            customIcon = Icons.Filled.Logout,
            title = "Konfirmasi Logout",
            message = "Apakah Anda yakin ingin logout dari InsightKu? Anda akan kembali ke halaman login.",
            confirmText = "Ya, Logout",
            dismissText = "Batal",
            onConfirm = {
                onConfirmLogout()
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}
