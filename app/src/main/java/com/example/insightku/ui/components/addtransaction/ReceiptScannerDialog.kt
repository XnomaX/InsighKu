package com.example.insightku.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.insightku.ui.dialogs.TransactionData
import kotlinx.coroutines.delay

@Composable
fun ReceiptScannerDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTransactionAdded: (TransactionData) -> Unit
) {
    var scanningState by remember { mutableStateOf(ScanningState.READY) }
    var extractedData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            scanningState = ScanningState.READY
            extractedData = emptyMap()
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
                                Icons.Default.CameraAlt,
                                contentDescription = "Receipt Scanner",
                                tint = Color(0xFF5A2A82),
                                modifier = Modifier.size(24.dp)
                            )

                            Text(
                                text = "AI Receipt Scanner",
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

                    // Content based on scanning state
                    when (scanningState) {
                        ScanningState.READY -> {
                            ScannerReadyContent(
                                onStartScan = {
                                    scanningState = ScanningState.SCANNING
                                }
                            )
                        }

                        ScanningState.SCANNING -> {
                            ScanningContent(
                                onScanComplete = { data ->
                                    extractedData = data
                                    scanningState = ScanningState.PROCESSING
                                }
                            )
                        }

                        ScanningState.PROCESSING -> {
                            ProcessingContent(
                                onProcessComplete = {
                                    scanningState = ScanningState.RESULT
                                }
                            )
                        }

                        ScanningState.RESULT -> {
                            ResultContent(
                                extractedData = extractedData,
                                onConfirm = { transaction ->
                                    onTransactionAdded(transaction)
                                    onDismiss()
                                },
                                onRetry = {
                                    scanningState = ScanningState.READY
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerReadyContent(
    onStartScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Camera Preview Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Position receipt in camera view",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Instructions
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF5A2A82).copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📸 Tips for best results:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = "• Ensure good lighting\n• Keep receipt flat and fully visible\n• Avoid shadows and glare\n• Make sure text is clear and readable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scan Button
        Button(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5A2A82),
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Scanning")
        }
    }
}

@Composable
private fun ScanningContent(
    onScanComplete: (Map<String, String>) -> Unit
) {
    LaunchedEffect(Unit) {
        // Simulate scanning process
        delay(3000)

        // Mock extracted data
        val mockData = mapOf(
            "merchant" to "Starbucks Coffee",
            "amount" to "25000",
            "date" to "2024-01-15",
            "category" to "Food & Drinks"
        )
        onScanComplete(mockData)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Scanning Animation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF5A2A82).copy(alpha = 0.1f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF5A2A82)
                    )

                    Text(
                        text = "Scanning receipt...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Text(
                        text = "AI is extracting transaction details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingContent(
    onProcessComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        // Simulate processing
        delay(2000)
        onProcessComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF5A2A82)
        )

        Text(
            text = "Processing data...",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            )
        )

        Text(
            text = "Analyzing and categorizing transaction",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResultContent(
    extractedData: Map<String, String>,
    onConfirm: (TransactionData) -> Unit,
    onRetry: () -> Unit
) {
    var merchant by remember { mutableStateOf(extractedData["merchant"] ?: "") }
    var amount by remember { mutableStateOf(extractedData["amount"] ?: "") }
    var category by remember { mutableStateOf(extractedData["category"] ?: "") }
    var date by remember { mutableStateOf(extractedData["date"] ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = "Receipt scanned successfully!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF10B981)
            )
        }

        Text(
            text = "Please review and confirm the extracted details:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Extracted data form
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5A2A82)
                )
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Text("Rp", style = MaterialTheme.typography.bodyMedium)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5A2A82)
                )
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5A2A82)
                )
            )

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5A2A82)
                )
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text("Scan Again")
            }

            Button(
                onClick = {
                    val transaction = TransactionData(
                        id = System.currentTimeMillis().toString(),
                        title = merchant,
                        category = category,
                        amount = -(amount.toDoubleOrNull() ?: 0.0),
                        description = "Scanned from receipt",
                        date = date,
                        isIncome = false
                    )
                    onConfirm(transaction)
                },
                enabled = merchant.isNotBlank() && amount.isNotBlank() && category.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5A2A82),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Transaction")
            }
        }
    }
}

enum class ScanningState {
    READY, SCANNING, PROCESSING, RESULT
}
