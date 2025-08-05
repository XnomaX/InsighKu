package com.example.insightku.ui.dialogs

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun WhatsAppSetupDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSetupComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    
    val steps = listOf(
        WhatsAppSetupStep(
            title = "Connect Your Phone",
            description = "We'll send you a code to verify your phone number",
            icon = Icons.Default.Phone,
            action = "Send Code"
        ),
        WhatsAppSetupStep(
            title = "Enter Verification Code", 
            description = "Enter the 6-digit code we sent to your WhatsApp",
            icon = Icons.Default.Message,
            action = "Verify Code"
        ),
        WhatsAppSetupStep(
            title = "Grant Permissions",
            description = "Allow InsightKu to read your WhatsApp messages for transaction detection",
            icon = Icons.Default.Security,
            action = "Grant Permission"
        ),
        WhatsAppSetupStep(
            title = "Setup Complete!",
            description = "You can now send transaction messages to automatically add expenses",
            icon = Icons.Default.CheckCircle,
            action = "Start Using"
        )
    )

    LaunchedEffect(isOpen) {
        if (isOpen) {
            currentStep = 0
        }
    }

    if (isOpen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp, 16.dp, 20.dp, 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "WhatsApp Setup",
                                tint = Color(0xFF25D366), // WhatsApp green
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Text(
                                text = "WhatsApp Integration",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Progress Indicator
                    LinearProgressIndicator(
                        progress = { (currentStep + 1).toFloat() / steps.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFF25D366),
                        trackColor = Color(0xFF25D366).copy(alpha = 0.2f)
                    )
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        val step = steps[currentStep]
                        
                        // Step Icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Color(0xFF25D366).copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                step.icon,
                                contentDescription = step.title,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        // Step Title & Description
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Step-specific content
                        when (currentStep) {
                            0 -> PhoneNumberStep()
                            1 -> VerificationCodeStep()
                            2 -> PermissionStep()
                            3 -> CompletionStep()
                        }
                    }
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 0 && currentStep < steps.size - 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (currentStep < steps.size - 1) {
                                    currentStep++
                                } else {
                                    onSetupComplete()
                                }
                            },
                            modifier = Modifier.weight(if (currentStep > 0 && currentStep < steps.size - 1) 1f else 1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            )
                        ) {
                            Text(steps[currentStep].action)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneNumberStep() {
    var phoneNumber by remember { mutableStateOf("") }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            placeholder = { Text("+62 812 3456 7890") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Phone",
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF25D366),
                focusedLabelColor = Color(0xFF25D366),
                focusedLeadingIconColor = Color(0xFF25D366)
            )
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF25D366).copy(alpha = 0.1f)
            )
        ) {
            Text(
                text = "💡 Make sure this is the same number you use for WhatsApp",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF065F46)
            )
        }
    }
}

@Composable
private fun VerificationCodeStep() {
    var code by remember { mutableStateOf("") }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Verification Code") },
            placeholder = { Text("123456") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF25D366),
                focusedLabelColor = Color(0xFF25D366)
            )
        )
        
        TextButton(
            onClick = {
                // TODO: Implement resend code functionality
            }
        ) {
            Text(
                text = "Didn't receive code? Resend",
                color = Color(0xFF25D366)
            )
        }
    }
}

@Composable
private fun PermissionStep() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Required Permissions:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                
                PermissionItem(
                    icon = Icons.Default.Message,
                    title = "Read Messages",
                    description = "To detect transaction keywords"
                )
                
                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = "To confirm when transactions are added"
                )
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF25D366).copy(alpha = 0.1f)
            )
        ) {
            Text(
                text = "🔒 Your privacy is protected. We only read messages with transaction keywords and never store personal conversations.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF065F46)
            )
        }
    }
}

@Composable
private fun CompletionStep() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF25D366).copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How to use:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                
                Text(
                    text = "Send messages like:",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "• \"Spent Rp25.000 at Starbucks\"\n• \"Paid Rp150.000 for groceries\"\n• \"Coffee Rp15.000\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF065F46)
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Color(0xFF25D366),
            modifier = Modifier.size(20.dp)
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class WhatsAppSetupStep(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val action: String
)
