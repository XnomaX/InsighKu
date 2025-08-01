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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.insightku.viewmodel.SettingsViewModel

// Data models for settings
data class SettingsItem(
    val label: String,
    val description: String,
    val type: SettingsItemType,
    val value: Any = false,
    val options: List<SettingsOption> = emptyList(),
    val onValueChange: (Any) -> Unit = {}
)

data class SettingsOption(
    val value: String,
    val label: String
)

enum class SettingsItemType {
    SWITCH, SELECT, WHATSAPP, ACTION
}

data class SettingsSection(
    val title: String,
    val icon: ImageVector,
    val items: List<SettingsItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {},
    onWhatsAppSetup: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Settings state from ViewModel
    val theme by viewModel.theme.collectAsState()
    val defaultInputMode by viewModel.defaultInputMode.collectAsState()
    val whatsappEnabled by viewModel.whatsappEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsState()
    val biometricsEnabled by viewModel.biometricsEnabled.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    
    // Settings sections
    val settingSections = remember(
        theme, defaultInputMode, whatsappEnabled, 
        notificationsEnabled, budgetAlertsEnabled, biometricsEnabled
    ) {
        listOf(
            SettingsSection(
                title = "Appearance",
                icon = if (theme == "dark") Icons.Default.
                DarkMode else Icons.Default.LightMode,
                items = listOf(
                    SettingsItem(
                        label = "Dark Mode",
                        description = "Switch between light and dark themes",
                        type = SettingsItemType.SWITCH,
                        value = theme == "dark",
                        onValueChange = { enabled ->
                            viewModel.updateTheme(if (enabled as Boolean) "dark" else "light")
                        }
                    )
                )
            ),
            SettingsSection(
                title = "Transaction Input",
                icon = Icons.Default.Edit,
                items = listOf(
                    SettingsItem(
                        label = "Default Input Mode",
                        description = "Choose your preferred method for adding transactions",
                        type = SettingsItemType.SELECT,
                        value = defaultInputMode,
                        options = listOf(
                            SettingsOption("ocr", "Scan Receipt (OCR)"),
                            SettingsOption("manual", "Manual Input")
                        ),
                        onValueChange = { mode ->
                            viewModel.updateDefaultInputMode(mode as String)
                        }
                    )
                )
            ),
            SettingsSection(
                title = "WhatsApp Integration",
                icon = Icons.Default.Chat,
                items = listOf(
                    SettingsItem(
                        label = "WhatsApp Bot",
                        description = if (whatsappEnabled) {
                            "Send transaction details via WhatsApp messages"
                        } else {
                            "Enable WhatsApp integration for automatic transaction input"
                        },
                        type = SettingsItemType.WHATSAPP,
                        value = whatsappEnabled,
                        onValueChange = { enabled ->
                            viewModel.updateWhatsAppEnabled(enabled as Boolean)
                        }
                    )
                )
            ),
            SettingsSection(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                items = listOf(
                    SettingsItem(
                        label = "Push Notifications",
                        description = "Receive notifications for app updates",
                        type = SettingsItemType.SWITCH,
                        value = notificationsEnabled,
                        onValueChange = { enabled ->
                            viewModel.updateNotificationsEnabled(enabled as Boolean)
                        }
                    ),
                    SettingsItem(
                        label = "Budget Alerts",
                        description = "Get notified when approaching budget limits",
                        type = SettingsItemType.SWITCH,
                        value = budgetAlertsEnabled,
                        onValueChange = { enabled ->
                            viewModel.updateBudgetAlertsEnabled(enabled as Boolean)
                        }
                    )
                )
            ),
            SettingsSection(
                title = "Security",
                icon = Icons.Default.Security,
                items = listOf(
                    SettingsItem(
                        label = "Biometric Authentication",
                        description = "Use fingerprint or face recognition to unlock",
                        type = SettingsItemType.SWITCH,
                        value = biometricsEnabled,
                        onValueChange = { enabled ->
                            viewModel.updateBiometricsEnabled(enabled as Boolean)
                        }
                    )
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5A2A82),
                            Color(0xFF7C3AED)
                        )
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 48.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Customize your app preferences",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Settings Sections
            settingSections.forEach { section ->
                SettingsSectionCard(
                    section = section,
                    whatsappEnabled = whatsappEnabled,
                    onWhatsAppSetup = onWhatsAppSetup
                )
            }
            
            // Account Management
            AccountManagementCard(
                userEmail = userEmail,
                onLogout = { showLogoutDialog = true }
            )
            
            // App Info
            AppInfoCard()
        }
        
        // Add bottom padding for bottom navigation
        Spacer(modifier = Modifier.height(100.dp))
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFFEF4444)
                )
            },
            title = {
                Text("Konfirmasi Logout")
            },
            text = {
                Text("Apakah Anda yakin ingin logout dari InsightKu? Anda akan kembali ke halaman login.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
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
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    section: SettingsSection,
    whatsappEnabled: Boolean,
    onWhatsAppSetup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color(0xFF5A2A82).copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        section.icon,
                        contentDescription = section.title,
                        tint = Color(0xFF5A2A82),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Section Items
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                section.items.forEachIndexed { index, item ->
                    SettingsItemRow(
                        item = item,
                        whatsappEnabled = whatsappEnabled,
                        onWhatsAppSetup = onWhatsAppSetup
                    )
                    
                    if (index < section.items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    whatsappEnabled: Boolean,
    onWhatsAppSetup: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Control based on type
            when (item.type) {
                SettingsItemType.SWITCH -> {
                    Switch(
                        checked = item.value as Boolean,
                        onCheckedChange = { item.onValueChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF5A2A82),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
                
                SettingsItemType.SELECT -> {
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = item.options.find { it.value == item.value }?.label ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .width(200.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5A2A82),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            item.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        item.onValueChange(option.value)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                SettingsItemType.WHATSAPP -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (whatsappEnabled) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFF10B981).copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                        
                        Switch(
                            checked = whatsappEnabled,
                            onCheckedChange = { item.onValueChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF5A2A82),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
                
                SettingsItemType.ACTION -> {
                    // For future action items
                }
            }
        }
        
        // WhatsApp Integration Details
        if (item.type == SettingsItemType.WHATSAPP) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (whatsappEnabled) {
                        Color(0xFF10B981).copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (whatsappEnabled) {
                        Color(0xFF10B981).copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (whatsappEnabled) Icons.Default.CheckCircle else Icons.Default.Chat,
                        contentDescription = null,
                        tint = if (whatsappEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (whatsappEnabled) {
                                "WhatsApp Integration Active"
                            } else {
                                "WhatsApp Integration Disabled"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (whatsappEnabled) {
                                Color(0xFF065F46)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        Text(
                            text = if (whatsappEnabled) {
                                "Send messages like \"Spent Rp15.000 at Starbucks\" to automatically add transactions"
                            } else {
                                "Enable to automatically add transactions from WhatsApp messages"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (whatsappEnabled) {
                                Color(0xFF047857)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = onWhatsAppSetup,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (whatsappEnabled) Color(0xFF047857) else Color(0xFF5A2A82)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (whatsappEnabled) Color(0xFF047857) else Color(0xFF5A2A82)
                            )
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (whatsappEnabled) "Manage Integration" else "Setup Integration"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountManagementCard(
    userEmail: String,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color(0xFF5A2A82).copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Account",
                        tint = Color(0xFF5A2A82),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current User
            Column {
                Text(
                    text = "Current User",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = userEmail.ifEmpty { "user@insightku.com" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logout Button
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout dari InsightKu")
            }
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "InsightKu",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Made with ❤️ for better financial management",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}