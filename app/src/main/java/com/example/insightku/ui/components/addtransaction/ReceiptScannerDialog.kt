package com.example.insightku.ui.components.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.insightku.ui.dialogs.TransactionData

enum class ScanningState {
    READY, SCANNING, SCANNED, PROCESSING, RESULT
}

@Composable
fun ReceiptScannerDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTransactionConfirmed: (TransactionData) -> Unit,
    isIncome: Boolean = false
) {
    var scanningState by remember { mutableStateOf(ScanningState.READY) }
    var formData by remember { mutableStateOf(TransactionFormData()) }

    // Mock functions for button clicks
    val handleScan = { scanningState = ScanningState.SCANNING }
    val handleClose = { onDismiss() }
    val handleSubmit = {
        val amount = formData.amount.toDoubleOrNull() ?: 0.0
        val transaction = TransactionData(
            id = System.currentTimeMillis().toString(),
            title = formData.merchant,
            category = formData.category,
            amount = if (formData.isIncome) amount else -amount,
            description = formData.description,
            date = formData.date,
            isIncome = formData.isIncome
        )
        onTransactionConfirmed(transaction)
        onDismiss()
    }

    if (isOpen) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    // Header
                    Text(
                        text = "Scan Receipt",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Use our AI to automatically extract details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))

                    // Content based on state
                    when (scanningState) {
                        ScanningState.READY -> ReadyToScanContent(isIncome, onScan = handleScan)
                        ScanningState.SCANNING -> ProcessingContent(isIncome, text = "Scanning...")
                        ScanningState.SCANNED -> ScannedContent(
                            formData = formData,
                            onFormDataChange = { formData = it },
                            isIncome = isIncome,
                            onCancel = handleClose,
                            onSubmit = handleSubmit
                        )
                        ScanningState.PROCESSING -> ProcessingContent(isIncome, text = "Processing...")
                        ScanningState.RESULT -> ResultContent(
                            extractedData = mapOf(
                                "merchant" to "Starbucks",
                                "amount" to "15.00",
                                "category" to "Food & Drinks",
                                "date" to "2023-10-27"
                            ),
                            onConfirm = onTransactionConfirmed,
                            onRetry = { scanningState = ScanningState.READY }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadyToScanContent(isIncome: Boolean, onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Color(0xFF5A2A82).copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color(0xFF5A2A82),
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Scan Your ${if (isIncome) "Income Document" else "Receipt"}",
            fontWeight = FontWeight.SemiBold
        )
        Text(
            if (isIncome) "Point your camera at invoices, payment confirmations, or income documents"
            else "Point your camera at the receipt and our AI will extract the details",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A2A82))
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Take Photo")
        }
        OutlinedButton(
            onClick = onScan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Upload from Gallery")
        }
    }
}

@Composable
fun ProcessingContent(isIncome: Boolean, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color(0xFF5A2A82), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text(text, fontWeight = FontWeight.Bold)
        Text(
            "AI is extracting ${if (isIncome) "income" else "transaction"} details...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedContent(
    formData: TransactionFormData,
    onFormDataChange: (TransactionFormData) -> Unit,
    isIncome: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    val categories = if (isIncome) listOf("Salary", "Freelance", "Gift") else listOf("Food", "Transport", "Shopping")
    var showCategoryDropdown by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
            Spacer(Modifier.width(8.dp))
            Text(
                "${if (isIncome) "Income document" else "Receipt"} scanned! Review and add details.",
                color = Color(0xFF166534),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formData.merchant,
                onValueChange = { onFormDataChange(formData.copy(merchant = it)) },
                label = { Text(if (isIncome) "Income Source" else "Merchant/Store") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = formData.amount,
                onValueChange = { onFormDataChange(formData.copy(amount = it)) },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
            ) {
                OutlinedTextField(
                    value = formData.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                onFormDataChange(formData.copy(category = category))
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = formData.description,
                onValueChange = { onFormDataChange(formData.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = formData.date,
                onValueChange = { onFormDataChange(formData.copy(date = it)) },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = onSubmit,
                enabled = formData.merchant.isNotBlank() && formData.amount.isNotBlank() && formData.category.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncome) Color(0xFF10B981) else Color(0xFF5A2A82)
                )
            ) {
                Text("Add ${if (isIncome) "Income" else "Transaction"}")
            }
        }
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
        // ... (Implementation from your provided code)
    }
}

@Preview(showBackground = true)
@Composable
fun ReceiptScannerDialogPreview() {
    MaterialTheme {
        ReceiptScannerDialog(isOpen = true, onDismiss = {}, onTransactionConfirmed = {})
    }
}