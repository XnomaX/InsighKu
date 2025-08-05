package com.example.insightku.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.insightku.data.model.Category
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCategoryAdded: (Category) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var budgetLimit by remember { mutableStateOf(500000f) }
    var alertThreshold by remember { mutableStateOf(80f) }
    var selectedIconName by remember { mutableStateOf("Food & Drinks") }

    val selectedIcon = defaultCategoryIcons.find { it.name == selectedIconName } ?: defaultCategoryIcons.first()
    
    fun validate() : Boolean {
        return if (name.trim().length < 2) {
            nameError = "Category name must be at least 2 characters"
            false
        } else {
            nameError = null
            true
        }
    }

    if (isOpen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 650.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp, 16.dp, 20.dp, 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add New Category", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider()

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Category Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; nameError = null },
                            label = { Text("Category Name") },
                            placeholder = { Text("e.g., Groceries, Bills") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = nameError != null,
                            singleLine = true
                        )
                        if (nameError != null) {
                            Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        // Icon & Color Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Icon & Color", style = MaterialTheme.typography.titleMedium)
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(60.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(140.dp) // Adjust height for 2 rows
                            ) {
                                items(defaultCategoryIcons) { iconData ->
                                    IconOption(
                                        iconData = iconData,
                                        isSelected = selectedIconName == iconData.name,
                                        onClick = { selectedIconName = iconData.name }
                                    )
                                }
                            }
                        }
                        
                        // Budget Limit
                        SliderSection(
                            title = "Budget Limit",
                            label = "Monthly Budget",
                            value = budgetLimit,
                            onValueChange = { budgetLimit = it },
                            range = 50000f..5000000f,
                            steps = 99,
                            prefix = "Rp "
                        )

                        // Alert Threshold
                        SliderSection(
                            title = "Alert Threshold",
                            label = "Alert at",
                            value = alertThreshold,
                            onValueChange = { alertThreshold = it },
                            range = 50f..100f,
                            steps = 9,
                            suffix = "% of budget"
                        )
                        
                        // Preview
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                           Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                               Text("Preview", style = MaterialTheme.typography.titleMedium)
                               Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(selectedIcon.color.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(selectedIcon.icon, contentDescription = null, tint = selectedIcon.color, modifier = Modifier.size(14.dp))
                                    }
                                   Text(name.ifEmpty { "Category Name" }, fontWeight = FontWeight.Bold)
                               }
                               PreviewRow("Budget:", "Rp ${budgetLimit.roundToInt().formatCurrency()}")
                               PreviewRow("Alert At:", "${alertThreshold.roundToInt()}% (Rp ${(budgetLimit * alertThreshold / 100).roundToInt().formatCurrency()})")
                           }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (validate()) {
                                    val newCategory = Category(
                                        name = name.trim(),
                                        color = "#" + selectedIcon.color.value.toString(16).substring(2, 8).uppercase(),
                                        icon = selectedIcon.name,
                                        budgetLimit = budgetLimit.toDouble(),
                                        alertThreshold = alertThreshold.roundToInt()
                                    )
                                    onCategoryAdded(newCategory)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Preview
@Composable
fun AddCategoryDialogPreview() {
    MaterialTheme {
        AddCategoryDialog(
            isOpen = true,
            onDismiss = {},
            onCategoryAdded = {}
        )
    }
}
