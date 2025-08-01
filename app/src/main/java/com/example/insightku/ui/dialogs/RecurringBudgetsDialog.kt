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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.insightku.viewmodel.RecurringBudget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBudgetsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    recurringBudgets: List<RecurringBudget>,
    onBudgetAdded: (RecurringBudget) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Monthly") }
    var category by remember { mutableStateOf("") }
    var nextDate by remember { mutableStateOf("") }

    val frequencies = listOf("Weekly", "Monthly", "Quarterly", "Yearly")
    val categories = listOf("Entertainment", "Housing", "Health", "Utilities", "Transport", "Others")

    LaunchedEffect(isOpen) {
        if (isOpen && !showAddForm) {
            // Reset form when dialog opens
            name = ""
            amount = ""
            frequency = "Monthly"
            category = ""
            nextDate = ""
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
                            if (showAddForm) {
                                IconButton(
                                    onClick = { showAddForm = false },
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
                                Icons.Default.Repeat,
                                contentDescription = "Recurring Budgets",
                                tint = Color(0xFF5A2A82),
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Text(
                                text = if (showAddForm) "Add Recurring Budget" else "Recurring Budgets",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!showAddForm) {
                                IconButton(
                                    onClick = { showAddForm = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = Color(0xFF5A2A82),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
                    }
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Content
                    if (showAddForm) {
                        // Add Form
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Budget Name
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Budget Name",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("e.g., Netflix Subscription") },
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
                                Text(
                                    text = "Amount",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = { amount = it },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    leadingIcon = {
                                        Text(
                                            text = "Rp",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF5A2A82),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                            
                            // Frequency
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Frequency",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                
                                var expandedFrequency by remember { mutableStateOf(false) }
                                
                                ExposedDropdownMenuBox(
                                    expanded = expandedFrequency,
                                    onExpandedChange = { expandedFrequency = !expandedFrequency }
                                ) {
                                    OutlinedTextField(
                                        value = frequency,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency)
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
                                        expanded = expandedFrequency,
                                        onDismissRequest = { expandedFrequency = false }
                                    ) {
                                        frequencies.forEach { freq ->
                                            DropdownMenuItem(
                                                text = { Text(freq) },
                                                onClick = {
                                                    frequency = freq
                                                    expandedFrequency = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Category
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Category",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                
                                var expandedCategory by remember { mutableStateOf(false) }
                                
                                ExposedDropdownMenuBox(
                                    expanded = expandedCategory,
                                    onExpandedChange = { expandedCategory = !expandedCategory }
                                ) {
                                    OutlinedTextField(
                                        value = category,
                                        onValueChange = {},
                                        readOnly = true,
                                        placeholder = { Text("Select category") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory)
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
                                        expanded = expandedCategory,
                                        onDismissRequest = { expandedCategory = false }
                                    ) {
                                        categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat) },
                                                onClick = {
                                                    category = cat
                                                    expandedCategory = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Next Date
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Next Payment Date",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = nextDate,
                                    onValueChange = { nextDate = it },
                                    placeholder = { Text("e.g., Jul 1") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF5A2A82),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }
                        
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        
                        // Add Form Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddForm = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = {
                                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                                    val budget = RecurringBudget(
                                        id = System.currentTimeMillis().toString(),
                                        name = name,
                                        amount = amountValue,
                                        frequency = frequency,
                                        nextDate = nextDate,
                                        category = category,
                                        isActive = true
                                    )
                                    onBudgetAdded(budget)
                                    showAddForm = false
                                },
                                enabled = name.isNotBlank() && amount.isNotBlank() && category.isNotBlank() && nextDate.isNotBlank(),
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
                                Text("Add Budget")
                            }
                        }
                    } else {
                        // Budget List
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (recurringBudgets.isEmpty()) {
                                // Empty state
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "No recurring budgets yet",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Add your first recurring payment",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                recurringBudgets.forEach { budget ->
                                    RecurringBudgetItem(budget = budget)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringBudgetItem(
    budget: RecurringBudget
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF5A2A82).copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = budget.name,
                        tint = Color(0xFF5A2A82),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${budget.frequency} • Due ${budget.nextDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Rp ${budget.amount.toInt()}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (budget.isActive) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}