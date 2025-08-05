package com.example.insightku.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LogoutConfirmationDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Konfirmasi Logout",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin logout dari InsightKu? Anda akan kembali ke halaman login.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmLogout()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("Ya, Logout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
