package com.example.insightku.ui.components.details

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import com.example.insightku.ui.theme.InsightKuTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDetail(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(abs(transaction.amount).toString()) }
    var type by remember { mutableStateOf(transaction.type) }
    var category by remember { mutableStateOf(transaction.category) }
    var date by remember { mutableStateOf(Date(transaction.date)) }
    var notes by remember { mutableStateOf(transaction.description ?: "") }
    var paymentMethod by remember { mutableStateOf(transaction.paymentMethod ?: "Cash") }
    var isRecurring by remember { mutableStateOf(false) } // Assuming this property exists

    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPaymentMethod by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val categories = when (type) {
        TransactionType.INCOME -> listOf("Salary", "Business", "Investment", "Freelance", "Bonus", "Others")
        TransactionType.EXPENSE -> listOf("Food & Dining", "Transportation", "Shopping", "Bills & Utilities", "Entertainment", "Health", "Education", "Others")
    }

    val paymentMethods = listOf("Cash", "Debit Card", "Credit Card", "Bank Transfer", "E-Wallet", "Others")

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = date
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, month, dayOfMonth)
                date = newDate.time
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 650.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Transaction")
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Transaction", style = MaterialTheme.typography.titleLarge)
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Type Toggle
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                            onClick = { type = TransactionType.EXPENSE },
                            selected = type == TransactionType.EXPENSE,
                            label = { Text("Expense") }
                        )
                        SegmentedButton(
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                            onClick = { type = TransactionType.INCOME },
                            selected = type == TransactionType.INCOME,
                            label = { Text("Income") }
                        )
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Title, contentDescription = null) }
                    )

                    // Amount
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Category
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = !expandedCategory }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            leadingIcon = { Icon(Icons.Outlined.Category, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            categories.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        category = selectionOption
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }

                    // Date
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(date),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Date")
                            }
                        }
                    )
                    
                    // Payment Method
                     ExposedDropdownMenuBox(
                        expanded = expandedPaymentMethod,
                        onExpandedChange = { expandedPaymentMethod = !expandedPaymentMethod }
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            leadingIcon = { Icon(Icons.Outlined.Payment, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaymentMethod) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPaymentMethod,
                            onDismissRequest = { expandedPaymentMethod = false }
                        ) {
                            paymentMethods.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        paymentMethod = selectionOption
                                        expandedPaymentMethod = false
                                    }
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null) },
                        minLines = 3
                    )
                    
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recurring Transaction", modifier = Modifier.weight(1f))
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val finalAmount = amount.toDoubleOrNull() ?: 0.0
                        val updatedTransaction = transaction.copy(
                            title = title,
                            amount = if (type == TransactionType.INCOME) finalAmount else -finalAmount,
                            type = type,
                            category = category,
                            date = date.time,
                            description = notes,
                            paymentMethod = paymentMethod
                        )
                        onSave(updatedTransaction)
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditTransactionDetailPreview() {
    InsightKuTheme {
        val sampleTransaction = Transaction(
            id = "1",
            title = "Lunch at Warteg",
            amount = -50000.0,
            category = "Food & Dining",
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            description = "A very delicious lunch",
            paymentMethod = "Cash"
        )
        EditTransactionDetail(
            transaction = sampleTransaction,
            onDismiss = {},
            onSave = {}
        )
    }
}
