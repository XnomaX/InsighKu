package com.example.insightku.ui.dialogs

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.insightku.ui.dialogs.TransactionData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class TransactionFormData(
    val merchant: String = "",
    val amount: String = "",
    val category: String = "",
    val description: String = "",
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isIncome: Boolean = false
)

sealed class AddTransactionStep {
    object ModeSelection : AddTransactionStep()
    object ManualForm : AddTransactionStep()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTransactionAdded: (TransactionData) -> Unit,
    onOpenScanner: () -> Unit,
    defaultMode: String = "ocr"
) {
    var currentStep by remember { mutableStateOf<AddTransactionStep>(AddTransactionStep.ModeSelection) }
    var formData by remember { mutableStateOf(TransactionFormData()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val expenseCategories = listOf(
        "Food & Drinks",
        "Transportation", 
        "Shopping",
        "Entertainment",
        "Healthcare",
        "Utilities",
        "Housing",
        "Others"
    )

    val incomeCategories = listOf(
        "Salary",
        "Freelance",
        "Investment",
        "Business",
        "Gift",
        "Others"
    )

    val categories = if (formData.isIncome) incomeCategories else expenseCategories

    LaunchedEffect(isOpen) {
        if (isOpen) {
            currentStep = AddTransactionStep.ModeSelection
            formData = TransactionFormData()
        }
    }

    if (isOpen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
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
                            if (currentStep == AddTransactionStep.ManualForm) {
                                IconButton(
                                    onClick = {
                                        currentStep = AddTransactionStep.ModeSelection
                                        formData = TransactionFormData()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Transaction",
                                tint = Color(0xFF5A2A82),
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Text(
                                text = "Add Transaction",
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
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        when (currentStep) {
                            AddTransactionStep.ModeSelection -> {
                                ModeSelectionContent(
                                    onOCRSelected = {
                                        onDismiss()
                                        onOpenScanner()
                                    },
                                    onManualSelected = {
                                        currentStep = AddTransactionStep.ManualForm
                                    }
                                )
                            }
                            
                            AddTransactionStep.ManualForm -> {
                                ManualFormContent(
                                    formData = formData,
                                    categories = categories,
                                    onFormDataChanged = { formData = it },
                                    onSubmit = {
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
                                        onTransactionAdded(transaction)
                                        onDismiss()
                                    },
                                    onBack = {
                                        currentStep = AddTransactionStep.ModeSelection
                                        formData = TransactionFormData()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelectionContent(
    onOCRSelected: () -> Unit,
    onManualSelected: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose how you'd like to add your transaction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // OCR Mode Button
        OutlinedButton(
            onClick = onOCRSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFF5A2A82).copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "OCR",
                        tint = Color(0xFF5A2A82),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Scan Receipt (OCR)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Use AI to extract details from receipt photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Manual Mode Button
        OutlinedButton(
            onClick = onManualSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFF5A2A82).copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Manual",
                        tint = Color(0xFF5A2A82),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Manual Input",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter transaction details manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualFormContent(
    formData: TransactionFormData,
    categories: List<String>,
    onFormDataChanged: (TransactionFormData) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Income/Expense Toggle
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (formData.isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (formData.isIncome) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (formData.isIncome) "Recording Income" else "Recording Expense",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                Switch(
                    checked = formData.isIncome,
                    onCheckedChange = { isIncome ->
                        onFormDataChanged(formData.copy(isIncome = isIncome, category = ""))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF5A2A82),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
        
        // Merchant/Source
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (formData.isIncome) "Income Source" else "Merchant/Store",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            OutlinedTextField(
                value = formData.merchant,
                onValueChange = { onFormDataChanged(formData.copy(merchant = it)) },
                placeholder = {
                    Text(
                        if (formData.isIncome) "e.g., Salary, Freelance Client, Investment" 
                        else "e.g., Starbucks, Target, Gas Station"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5A2A82),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        
        // Amount
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            OutlinedTextField(
                value = formData.amount,
                onValueChange = { onFormDataChanged(formData.copy(amount = it)) },
                placeholder = { Text("0") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5A2A82),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        
        // Category
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
            ) {
                OutlinedTextField(
                    value = formData.category,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Select category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF5A2A82),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                onFormDataChanged(formData.copy(category = category))
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }
        }
        
        // Description
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Description (Optional)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            OutlinedTextField(
                value = formData.description,
                onValueChange = { onFormDataChanged(formData.copy(description = it)) },
                placeholder = {
                    Text(
                        if (formData.isIncome) "e.g., monthly salary, consulting project payment"
                        else "e.g., bought groceries, lunch with colleagues"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5A2A82),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        
        // Date
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            OutlinedTextField(
                value = formData.date,
                onValueChange = { onFormDataChanged(formData.copy(date = it)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5A2A82),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            
            Button(
                onClick = onSubmit,
                enabled = formData.merchant.isNotBlank() && 
                         formData.amount.isNotBlank() && 
                         formData.category.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (formData.isIncome) Color(0xFF10B981) else Color(0xFF5A2A82),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add ${if (formData.isIncome) "Income" else "Expense"}")
            }
        }
    }
}
