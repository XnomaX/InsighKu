package com.example.insightku.ui.components.details

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import com.example.insightku.ui.theme.GlassBorder
import com.example.insightku.ui.theme.IncomeGreen
import com.example.insightku.ui.theme.InsightKuTheme
import com.example.insightku.ui.theme.PurpleDark
import com.example.insightku.ui.theme.PurpleViolet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
    var isRecurring by remember { mutableStateOf(false) }

    var titleFocused by remember { mutableStateOf(false) }
    var amountFocused by remember { mutableStateOf(false) }
    var notesFocused by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPaymentMethod by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val isIncome = type == TransactionType.INCOME
    val accentColor = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.primary
    val headerGradient = if (isIncome) {
        Brush.horizontalGradient(listOf(Color(0xFF064E3B), IncomeGreen))
    } else {
        Brush.horizontalGradient(listOf(PurpleDark, PurpleViolet))
    }

    val categories = if (isIncome) {
        listOf("Salary", "Business", "Investment", "Freelance", "Bonus", "Others")
    } else {
        listOf("Food & Dining", "Transportation", "Shopping", "Bills & Utilities", "Entertainment", "Health", "Education", "Others")
    }
    val paymentMethods = listOf("Cash", "Debit Card", "Credit Card", "Bank Transfer", "E-Wallet", "Others")

    fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = date }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = Calendar.getInstance().apply { set(year, month, dayOfMonth) }.time
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Gradient header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            headerGradient,
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Edit Transaction",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isIncome) "Income transaction" else "Expense transaction",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        IconButton(onClick = onDismiss) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Type toggle — animated pill
                    val incomeColor by animateColorAsState(
                        targetValue = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(200), label = "incomeColor"
                    )
                    val expenseColor by animateColorAsState(
                        targetValue = if (!isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(200), label = "expenseColor"
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(expenseColor)
                                    .clickable { type = TransactionType.EXPENSE },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Expense",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(incomeColor)
                                    .clickable { type = TransactionType.INCOME },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Title field
                    val titleBorder by animateColorAsState(
                        targetValue = if (titleFocused) accentColor else GlassBorder,
                        animationSpec = tween(180), label = "titleBorder"
                    )
                    DesignTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "TITLE",
                        placeholder = "e.g. Lunch at Warteg",
                        leadingIcon = { Icon(Icons.Outlined.Title, contentDescription = null, modifier = Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.7f)) },
                        borderColor = titleBorder,
                        accentColor = accentColor,
                        modifier = Modifier.onFocusChanged { titleFocused = it.isFocused }
                    )

                    // Amount field
                    val amountBorder by animateColorAsState(
                        targetValue = if (amountFocused) accentColor else GlassBorder,
                        animationSpec = tween(180), label = "amountBorder"
                    )
                    DesignTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "AMOUNT",
                        placeholder = "0",
                        leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.7f)) },
                        borderColor = amountBorder,
                        accentColor = accentColor,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.onFocusChanged { amountFocused = it.isFocused }
                    )

                    // Category dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory }
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = GlassBorder
                                ),
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accentColor.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Category, contentDescription = null, modifier = Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.7f))
                                    }
                                },
                                trailingIcon = {
                                    Icon(
                                        if (expandedCategory) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = accentColor.copy(alpha = 0.5f)
                                    )
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                categories.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = { category = option; expandedCategory = false }
                                    )
                                }
                            }
                        }
                    }

                    // Date field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "DATE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        OutlinedTextField(
                            value = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(date),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = GlassBorder
                            ),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentColor.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.DateRange, contentDescription = null, modifier = Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.7f))
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker() }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select Date", modifier = Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.5f))
                                }
                            }
                        )
                    }

                    // Payment method dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PAYMENT METHOD",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedPaymentMethod,
                            onExpandedChange = { expandedPaymentMethod = !expandedPaymentMethod }
                        ) {
                            OutlinedTextField(
                                value = paymentMethod,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = GlassBorder
                                ),
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accentColor.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Payment, contentDescription = null, modifier = Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.7f))
                                    }
                                },
                                trailingIcon = {
                                    Icon(
                                        if (expandedPaymentMethod) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = accentColor.copy(alpha = 0.5f)
                                    )
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedPaymentMethod,
                                onDismissRequest = { expandedPaymentMethod = false }
                            ) {
                                paymentMethods.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = { paymentMethod = option; expandedPaymentMethod = false }
                                    )
                                }
                            }
                        }
                    }

                    // Notes field
                    val notesBorder by animateColorAsState(
                        targetValue = if (notesFocused) accentColor else GlassBorder,
                        animationSpec = tween(180), label = "notesBorder"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "NOTES",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { notesFocused = it.isFocused },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = notesBorder,
                                unfocusedBorderColor = notesBorder
                            ),
                            placeholder = {
                                Text(
                                    "Optional notes…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentColor.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null, modifier = Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.7f))
                                }
                            },
                            minLines = 3
                        )
                    }

                    // Recurring toggle
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recurring Transaction",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Repeat this transaction automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentColor
                                )
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel — outlined ghost
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable(onClick = onDismiss),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Save — gradient primary
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(headerGradient)
                            .clickable {
                                val finalAmount = amount.toDoubleOrNull() ?: 0.0
                                onSave(
                                    transaction.copy(
                                        title = title,
                                        amount = if (isIncome) finalAmount else -finalAmount,
                                        type = type,
                                        category = category,
                                        date = date.time,
                                        description = notes,
                                        paymentMethod = paymentMethod
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save Changes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    borderColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor
            ),
            placeholder = {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    leadingIcon()
                }
            },
            singleLine = true,
            keyboardOptions = keyboardOptions
        )
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
