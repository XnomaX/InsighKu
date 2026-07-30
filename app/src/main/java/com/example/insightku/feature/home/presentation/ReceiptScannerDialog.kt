package com.example.insightku.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter

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
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(formData.dateMillis)),
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
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Header
                    Text(
                        text = stringResource(R.string.scanner_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.scanner_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))

                    // Content based on state
                    when (scanningState) {
                        ScanningState.READY -> ReadyToScanContent(isIncome, onScan = handleScan)
                        ScanningState.SCANNING -> ProcessingContent(isIncome, text = stringResource(R.string.scanner_scanning))
                        ScanningState.SCANNED -> ScannedContent(
                            formData = formData,
                            onFormDataChange = { formData = it },
                            isIncome = isIncome,
                            onCancel = handleClose,
                            onSubmit = handleSubmit
                        )
                        ScanningState.PROCESSING -> ProcessingContent(isIncome, text = stringResource(R.string.scanner_processing))
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
                .background(com.example.insightku.core.ui.theme.AppPalette.primary.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = com.example.insightku.core.ui.theme.AppPalette.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(if (isIncome) R.string.scanner_scan_income_doc else R.string.scanner_scan_receipt),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(if (isIncome) R.string.scanner_point_camera_income else R.string.scanner_point_camera_receipt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = com.example.insightku.core.ui.theme.AppPalette.primary)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.scanner_take_photo))
        }
        OutlinedButton(
            onClick = onScan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.scanner_upload_gallery))
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
        CircularProgressIndicator(color = com.example.insightku.core.ui.theme.AppPalette.primary, strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text(text, fontWeight = FontWeight.Bold)
        Text(
            stringResource(if (isIncome) R.string.scanner_extracting_income else R.string.scanner_extracting_transaction),
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
    val categories = if (isIncome) listOf(stringResource(R.string.scanner_cat_salary), stringResource(R.string.scanner_cat_freelance), stringResource(R.string.scanner_cat_gift)) else listOf(stringResource(R.string.scanner_cat_food), stringResource(R.string.scanner_cat_transport), stringResource(R.string.scanner_cat_shopping))
    var showCategoryDropdown by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.example.insightku.core.ui.theme.AppPalette.success.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = com.example.insightku.core.ui.theme.AppPalette.success)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (isIncome) R.string.scanner_income_scanned else R.string.scanner_receipt_scanned),
                color = com.example.insightku.core.ui.theme.AppPalette.success,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formData.merchant,
                onValueChange = { onFormDataChange(formData.copy(merchant = it)) },
                label = { Text(stringResource(if (isIncome) R.string.income_source else R.string.merchant_store)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = formData.amount,
                onValueChange = { onFormDataChange(formData.copy(amount = it)) },
                label = { Text(stringResource(R.string.goal_amount_label)) },
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
                    label = { Text(stringResource(R.string.category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                label = { Text(stringResource(R.string.note_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = DateFormatter.formatNumericDate(formData.dateMillis),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.transaction_date_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = onSubmit,
                enabled = formData.merchant.isNotBlank() && formData.amount.isNotBlank() && formData.category.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncome) com.example.insightku.core.ui.theme.AppPalette.success else com.example.insightku.core.ui.theme.AppPalette.primary
                )
            ) {
                Text(stringResource(if (isIncome) R.string.add_income else R.string.add_expense))
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
    var amount   by remember { mutableStateOf(extractedData["amount"] ?: "") }
    var category by remember { mutableStateOf(extractedData["category"] ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.example.insightku.core.ui.theme.AppPalette.success.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = com.example.insightku.core.ui.theme.AppPalette.success)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.scanner_ai_extracted),
                color = com.example.insightku.core.ui.theme.AppPalette.success,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text(stringResource(R.string.scanner_merchant)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text(stringResource(R.string.goal_amount_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text(stringResource(R.string.category)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.try_again))
            }
            Button(
                onClick = {
                    onConfirm(
                        TransactionData(
                            id          = System.currentTimeMillis().toString(),
                            title       = merchant,
                            category    = category,
                            amount      = amount.toDoubleOrNull() ?: 0.0,
                            description = "",
                            date        = extractedData["date"] ?: "",
                            isIncome    = false
                        )
                    )
                },
                enabled = merchant.isNotBlank() && amount.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.insightku.core.ui.theme.AppPalette.primary)
            ) {
                Text(stringResource(R.string.transaction_confirm))
            }
        }
    }
}




