package com.example.insightku.feature.budgeting.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.Category
import com.example.insightku.feature.budgeting.data.model.AllocationTriggerType
import com.example.insightku.feature.budgeting.data.model.AllocationValueType
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule

/**
 * Dialog for creating/editing auto-allocation rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoAllocationDialog(
    rule: AutoAllocationRule? = null,
    goals: List<com.example.insightku.feature.budgeting.domain.model.Goal>,
    categories: List<Category> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (AutoAllocationRule) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var selectedGoalId by remember { mutableStateOf(rule?.goalId ?: goals.firstOrNull()?.id ?: "") }
    var triggerType by remember { mutableStateOf(rule?.triggerType ?: AllocationTriggerType.INCOME_RECEIVED) }
    var categoryId by remember { mutableStateOf(rule?.triggerParams?.categoryId) }
    var threshold by remember { mutableStateOf(rule?.triggerParams?.threshold?.toString() ?: "") }
    var allocationType by remember { mutableStateOf(rule?.allocationType ?: AllocationValueType.PERCENT) }
    var allocationValue by remember { mutableStateOf(rule?.allocationValue?.toString() ?: "10") }
    var isEnabled by remember { mutableStateOf(rule?.isEnabled ?: true) }

    val selectedGoal = goals.find { it.id == selectedGoalId }
    val isValid = selectedGoalId.isNotBlank() &&
                  allocationValue.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (rule != null) "Edit Auto-Allocation" else "Add Auto-Allocation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Goal Selection
                Text(
                    text = "Save to Goal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                goals.forEach { goal ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGoalId == goal.id,
                            onClick = { selectedGoalId = goal.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                HorizontalDivider()

                // Trigger Type
                Text(
                    text = "When to Save",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Column {
                    AllocationTriggerOption(
                        title = "Income Received",
                        description = "Save a percentage when you receive money",
                        icon = Icons.Outlined.Payments,
                        selected = triggerType == AllocationTriggerType.INCOME_RECEIVED,
                        onClick = { triggerType = AllocationTriggerType.INCOME_RECEIVED }
                    )

                    AllocationTriggerOption(
                        title = "Daily",
                        description = "Save a set amount every day",
                        icon = Icons.Outlined.Today,
                        selected = triggerType == AllocationTriggerType.DAILY,
                        onClick = { triggerType = AllocationTriggerType.DAILY }
                    )

                    AllocationTriggerOption(
                        title = "Balance Above Threshold",
                        description = "Save when balance exceeds a limit",
                        icon = Icons.Outlined.AccountBalance,
                        selected = triggerType == AllocationTriggerType.BALANCE_ABOVE,
                        onClick = { triggerType = AllocationTriggerType.BALANCE_ABOVE }
                    )
                }

                // Threshold Input (for BALANCE_ABOVE)
                if (triggerType == AllocationTriggerType.BALANCE_ABOVE) {
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { threshold = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Balance Threshold") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = {
                            Icon(Icons.Outlined.AttachMoney, contentDescription = null)
                        },
                        singleLine = true
                    )
                }

                HorizontalDivider()

                // Allocation Amount
                Text(
                    text = "How Much to Save",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = allocationType == AllocationValueType.PERCENT,
                        onClick = { allocationType = AllocationValueType.PERCENT },
                        label = { Text("Percentage") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = allocationType == AllocationValueType.FIXED,
                        onClick = { allocationType = AllocationValueType.FIXED },
                        label = { Text("Fixed Amount") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = allocationValue,
                    onValueChange = { allocationValue = it.filter { c -> c.isDigit() || c == '.' } },
                    label = {
                        Text(
                            if (allocationType == AllocationValueType.PERCENT) "Percentage (%)"
                            else "Amount ($)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = null
                        )
                    },
                    singleLine = true
                )

                // Preview
                if (selectedGoal != null && allocationValue.toDoubleOrNull() != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildPreviewText(triggerType, allocationType, allocationValue.toDouble()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Delete Button
                if (rule != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(rule.id) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Rule")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newRule = AutoAllocationRule(
                        id = rule?.id ?: "",
                        goalId = selectedGoalId,
                        goalName = selectedGoal?.name ?: "",
                        triggerType = triggerType,
                        triggerParams = com.example.insightku.feature.budgeting.domain.model.AllocationTriggerParams(
                            categoryId = categoryId,
                            threshold = threshold.toDoubleOrNull()
                        ),
                        allocationType = allocationType,
                        allocationValue = allocationValue.toDouble() ?: 0.0,
                        isEnabled = isEnabled,
                        createdAt = rule?.createdAt ?: java.time.Instant.now(),
                        updatedAt = java.time.Instant.now()
                    )
                    onSave(newRule)
                },
                enabled = isValid
            ) {
                Text(if (rule != null) "Update" else "Add Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AllocationTriggerOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun buildPreviewText(
    trigger: AllocationTriggerType,
    allocationType: AllocationValueType,
    value: Double
): String {
    val amountText = when (allocationType) {
        AllocationValueType.PERCENT -> "${value.toInt()}%"
        AllocationValueType.FIXED -> "$${value.toInt()}"
    }

    return when (trigger) {
        AllocationTriggerType.INCOME_RECEIVED -> "Save $amountText from each income"
        AllocationTriggerType.DAILY -> "Save $amountText every day"
        AllocationTriggerType.BALANCE_ABOVE -> "Save $amountText when balance exceeds threshold"
        AllocationTriggerType.SPENDING_CATEGORY -> "Save $amountText from category spending"
    }
}
