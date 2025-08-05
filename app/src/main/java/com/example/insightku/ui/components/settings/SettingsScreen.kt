package com.example.insightku.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.viewmodel.SettingsViewModel

// Data models for settings
data class SettingsItem(
    val label: String,
    val description: String,
    val type: SettingsItemType,
    val icon: ImageVector,
    val action: () -> Unit = {}
)

sealed class SettingsItemType {
    object Navigation : SettingsItemType()
    data class Toggle(val isEnabled: Boolean, val onToggle: (Boolean) -> Unit) : SettingsItemType()
    data class Selection(val currentValue: String, val onSelect: () -> Unit) : SettingsItemType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()

    // Theme colors
    val primaryPurple = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Handle logout success
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !uiState.showLogoutDialog && uiState.error == null) {
            // Check if logout was successful (user data cleared)
            if (uiState.userEmail.isEmpty() && uiState.userName.isEmpty()) {
                onLogout()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Manage your app preferences and account settings",
            style = MaterialTheme.typography.bodyMedium,
            color = onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // User Profile Card
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryPurple)
            }
        } else {
            UserProfileCard(
                userName = uiState.userName.ifEmpty { "User" },
                userEmail = uiState.userEmail.ifEmpty { "user@example.com" }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Items
        val settingsItems = listOf(
            SettingsItem(
                label = "Dark Mode",
                description = "Switch between light and dark theme",
                type = SettingsItemType.Toggle(
                    isEnabled = uiState.isDarkMode,
                    onToggle = { viewModel.onEvent(SettingsEvent.ToggleDarkMode(it)) }
                ),
                icon = Icons.Default.DarkMode
            ),
            SettingsItem(
                label = "Notifications",
                description = "Enable push notifications",
                type = SettingsItemType.Toggle(
                    isEnabled = uiState.notificationsEnabled,
                    onToggle = { viewModel.onEvent(SettingsEvent.ToggleNotifications(it)) }
                ),
                icon = Icons.Default.Notifications
            ),
            SettingsItem(
                label = "Biometric Authentication",
                description = "Use fingerprint or face recognition",
                type = SettingsItemType.Toggle(
                    isEnabled = uiState.biometricEnabled,
                    onToggle = { viewModel.onEvent(SettingsEvent.ToggleBiometric(it)) }
                ),
                icon = Icons.Default.Fingerprint
            ),
            SettingsItem(
                label = "Auto Backup",
                description = "Automatically backup your data",
                type = SettingsItemType.Toggle(
                    isEnabled = uiState.autoBackupEnabled,
                    onToggle = { viewModel.onEvent(SettingsEvent.ToggleAutoBackup(it)) }
                ),
                icon = Icons.Default.Backup
            ),
            SettingsItem(
                label = "Currency",
                description = "Current: ${uiState.currencyCode}",
                type = SettingsItemType.Selection(
                    currentValue = uiState.currencyCode,
                    onSelect = { /* TODO: Show currency picker */ }
                ),
                icon = Icons.Default.CurrencyExchange
            )
        )

        settingsItems.forEach { item ->
            SettingsItemRow(item = item)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = { viewModel.onEvent(SettingsEvent.ShowLogoutDialog) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    // Error Snackbar
    if (uiState.error != null) {
        LaunchedEffect(uiState.error) {
            // Show snackbar for errors
            // TODO: Implement snackbar similar to LoginScreen
            viewModel.onEvent(SettingsEvent.ClearError)
        }
    }

    // Logout Confirmation Dialog
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SettingsEvent.HideLogoutDialog) },
            title = {
                Text("Confirm Logout")
            },
            text = {
                Text("Are you sure you want to logout?")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(SettingsEvent.ConfirmLogout) }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(SettingsEvent.HideLogoutDialog) }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserProfileCard(
    userName: String,
    userEmail: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsItemRow(item: SettingsItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Render different controls based on type
            when (val type = item.type) {
                is SettingsItemType.Toggle -> {
                    Switch(
                        checked = type.isEnabled,
                        onCheckedChange = type.onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                is SettingsItemType.Selection -> {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is SettingsItemType.Navigation -> {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
