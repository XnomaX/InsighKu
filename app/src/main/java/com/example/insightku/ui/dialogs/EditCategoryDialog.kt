package com.example.insightku.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.insightku.data.model.Category
import kotlin.math.roundToInt

@Composable
fun EditCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    category: Category?,
    onCategoryEdited: (Category) -> Unit,
    onCategoryDeleted: (String) -> Unit
) {
    if (category == null) return

    var name by remember { mutableStateOf(category.name) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var budgetLimit by remember { mutableStateOf(category.budgetLimit?.toFloat() ?: 500000f) }
    var alertThreshold by remember { mutableStateOf(category.alertThreshold.toFloat()) }
    var selectedIconName by remember { mutableStateOf(category.icon ?: "Food & Drinks") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val selectedIcon = defaultCategoryIcons.find { it.name == selectedIconName } ?: defaultCategoryIcons.first()

    // Sync state with category changes from outside
    LaunchedEffect(category) {
        name = category.name
        budgetLimit = category.budgetLimit?.toFloat() ?: 500000f
        alertThreshold = category.alertThreshold.toFloat()
        selectedIconName = category.icon ?: "Food & Drinks"
    }
    
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
                        Text("Edit Category", style = MaterialTheme.typography.titleLarge)
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
                                modifier = Modifier.height(140.dp)
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
                    }

                    // Action Buttons
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (validate()) {
                                        val updatedCategory = category.copy(
                                            name = name.trim(),
                                            color = "#" + selectedIcon.color.value.toString(16).substring(2, 8).uppercase(),
                                            icon = selectedIcon.name,
                                            budgetLimit = budgetLimit.toDouble(),
                                            alertThreshold = alertThreshold.roundToInt()
                                        )
                                        onCategoryEdited(updatedCategory)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Changes")
                            }
                        }
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Category")
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Warning") },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete \"${category.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onCategoryDeleted(category.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditCategoryDialogPreview() {
    MaterialTheme {
        EditCategoryDialog(
            isOpen = true,
            onDismiss = {},
            category = Category(
                id = "1",
                name = "Food",
                color = "#F59E0B",
                budgetLimit = 1000.0,
                icon = "Food & Drinks",
                alertThreshold = 90
            ),
            onCategoryEdited = {},
            onCategoryDeleted = {}
        )
    }
}
