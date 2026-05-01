package com.example.insightku.ui.components.addtransaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- DATA & STATE ---

fun getCurrentDateAsString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

data class TransactionFormData(
    val merchant: String = "",
    val amount: String = "",
    val category: String = "",
    val description: String = "",
    val date: String = getCurrentDateAsString(),
    val isIncome: Boolean = false
)

sealed class AddTransactionStep {
    object ModeSelection : AddTransactionStep()
    object ManualForm : AddTransactionStep()
}

// --- MAIN DIALOG ---

@Composable
fun AddTransactionDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTransactionAdded: (Transaction) -> Unit,
    onOpenScanner: () -> Unit
) {
    var currentStep by remember { mutableStateOf<AddTransactionStep>(AddTransactionStep.ModeSelection) }
    var formData by remember { mutableStateOf(TransactionFormData()) }

    // Reset state when dialog is opened
    LaunchedEffect(isOpen) {
        if (isOpen) {
            currentStep = AddTransactionStep.ModeSelection
            formData = TransactionFormData()
        }
    }

    if (isOpen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.fillMaxSize()) {
                    DialogHeader(
                        showBackButton = currentStep == AddTransactionStep.ManualForm,
                        onBack = { currentStep = AddTransactionStep.ModeSelection },
                        onClose = onDismiss
                    )

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
                                onFormDataChanged = { formData = it },
                                onSubmit = {
                                    val amount = formData.amount.toDoubleOrNull() ?: 0.0
                                    val transaction = Transaction(
                                        title = formData.merchant,
                                        amount = amount,
                                        category = formData.category,
                                        description = formData.description,
                                        date = System.currentTimeMillis(), // TODO: Parse from formData.date
                                        type = if (formData.isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                                    )
                                    onTransactionAdded(transaction)
                                    onDismiss()
                                },
                                onBack = {
                                    currentStep = AddTransactionStep.ModeSelection
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- DIALOG COMPONENTS ---

@Composable
fun DialogHeader(showBackButton: Boolean, onBack: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = Color(0xFF5A2A82))
            Spacer(Modifier.width(8.dp))
            Text("Add Transaction", style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
    HorizontalDivider()
}

@Composable
fun ModeSelectionContent(onOCRSelected: () -> Unit, onManualSelected: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Choose how you'd like to add your transaction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
        ModeButton(
            text = "Scan Receipt (OCR)",
            description = "Use AI to extract details from receipt photos",
            icon = Icons.Default.CameraAlt,
            onClick = onOCRSelected
        )
        Spacer(Modifier.height(12.dp))
        ModeButton(
            text = "Manual Input",
            description = "Enter transaction details manually",
            icon = Icons.Default.Edit,
            onClick = onManualSelected
        )
    }
}

@Composable
fun ModeButton(text: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF5A2A82).copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = text, tint = Color(0xFF5A2A82))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.ManualFormContent(
    formData: TransactionFormData,
    onFormDataChanged: (TransactionFormData) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val expenseCategories = listOf("Food & Drinks", "Transportation", "Shopping", "Entertainment", "Healthcare", "Utilities", "Housing", "Others")
    val incomeCategories = listOf("Salary", "Freelance", "Investment", "Business", "Gift", "Others")
    val categories = if (formData.isIncome) incomeCategories else expenseCategories

    var showCategoryDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Income/Expense Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (formData.isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (formData.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                Text(if (formData.isIncome) "Recording Income" else "Recording Expense", fontWeight = FontWeight.Medium)
            }
            Switch(
                checked = formData.isIncome,
                onCheckedChange = { isIncome -> onFormDataChanged(formData.copy(isIncome = isIncome, category = "")) }
            )
        }

        // Form Fields
        LabeledTextField(
            label = if (formData.isIncome) "Income Source" else "Merchant/Store",
            icon = Icons.Default.Store,
            value = formData.merchant,
            onValueChange = { onFormDataChanged(formData.copy(merchant = it)) },
            placeholder = if (formData.isIncome) "e.g., Salary, Client" else "e.g., Starbucks"
        )
        LabeledTextField(
            label = "Amount",
            icon = Icons.Default.AttachMoney,
            value = formData.amount,
            onValueChange = { onFormDataChanged(formData.copy(amount = it)) },
            placeholder = "0.00",
            keyboardType = KeyboardType.Decimal
        )
        
        // Category Dropdown
        ExposedDropdownMenuBox(
            expanded = showCategoryDropdown,
            onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
        ) {
            LabeledTextField(
                label = "Category",
                icon = Icons.Default.Category,
                value = formData.category,
                onValueChange = {},
                placeholder = "Select category",
                readOnly = true,
                modifier = Modifier.menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) }
            )
            ExposedDropdownMenu(expanded = showCategoryDropdown, onDismissRequest = { showCategoryDropdown = false }) {
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
        
        LabeledTextField(
            label = "Description (Optional)",
            icon = Icons.Default.Description,
            value = formData.description,
            onValueChange = { onFormDataChanged(formData.copy(description = it)) },
            placeholder = "e.g., Lunch with colleagues",
            singleLine = false,
            modifier = Modifier.height(100.dp)
        )

        // Date Picker (simplified for now, can be replaced with a proper DatePickerDialog)
        LabeledTextField(
            label = "Date",
            icon = Icons.Default.DateRange,
            value = formData.date,
            onValueChange = { onFormDataChanged(formData.copy(date = it)) },
            placeholder = "YYYY-MM-DD"
        )
    }
    
    // Action Buttons
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
        Button(
            onClick = onSubmit,
            enabled = formData.merchant.isNotBlank() && formData.amount.isNotBlank() && formData.category.isNotBlank(),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (formData.isIncome) Color(0xFF10B981) else Color(0xFF5A2A82)
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add ${if (formData.isIncome) "Income" else "Expense"}")
        }
    }
}

@Composable
fun LabeledTextField(
    label: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = modifier.fillMaxWidth(),
            readOnly = readOnly,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF5A2A82),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
}


// --- PREVIEWS ---

@Preview(name = "Mode Selection", showBackground = true, widthDp = 360, heightDp = 480)
@Composable
fun AddTransactionDialogModeSelectionPreview() {
    MaterialTheme {
        Surface(color = Color.Gray) {
             AddTransactionDialog(isOpen = true, onDismiss = {}, onTransactionAdded = {}, onOpenScanner = {})
        }
    }
}

@Preview(name = "Expense Form", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun AddTransactionDialogExpenseFormPreview() {
    MaterialTheme {
        // This preview is complex due to internal state. 
        // For accurate preview, we directly display the content part.
        Surface(modifier = Modifier.fillMaxSize()) {
            var formData by remember { mutableStateOf(TransactionFormData(isIncome = false)) }
             Column(Modifier.fillMaxSize()) {
                DialogHeader(showBackButton = true, onBack = {}, onClose = { })
                ManualFormContent(
                    formData = formData,
                    onFormDataChanged = { formData = it },
                    onSubmit = { },
                    onBack = { }
                )
            }
        }
    }
}

@Preview(name = "Income Form", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun AddTransactionDialogIncomeFormPreview() {
     MaterialTheme {
        // This preview is complex due to internal state. 
        // For accurate preview, we directly display the content part.
        Surface(modifier = Modifier.fillMaxSize()) {
            var formData by remember { mutableStateOf(TransactionFormData(isIncome = true, category = "Salary")) }
             Column(Modifier.fillMaxSize()) {
                DialogHeader(showBackButton = true, onBack = {}, onClose = { })
                ManualFormContent(
                    formData = formData,
                    onFormDataChanged = { formData = it },
                    onSubmit = { },
                    onBack = { }
                )
            }
        }
    }
}