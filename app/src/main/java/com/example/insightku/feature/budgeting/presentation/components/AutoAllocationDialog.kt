package com.example.insightku.feature.budgeting.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.budgeting.data.model.AllocationTriggerType
import com.example.insightku.feature.budgeting.data.model.AllocationValueType
import com.example.insightku.feature.budgeting.data.model.ConfirmationMode
import com.example.insightku.feature.budgeting.data.model.ScheduledFrequency
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule
import com.example.insightku.feature.budgeting.presentation.state.AutoAllocationRuleForm


/**
 * Enhanced Auto Allocation Rule Dialog.
 * Full configuration for Smart Auto Allocation with all trigger types,
 * source account, confirmation mode, category filtering, min income,
 * round-up, scheduling, and live preview card.
 */
@Composable
fun AutoAllocationDialog(
    rule: AutoAllocationRule?,
    goals: List<com.example.insightku.feature.budgeting.domain.model.Goal>,
    accounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (AutoAllocationRule) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEditing = rule != null

    // Initialize form from existing rule or defaults
    var form by remember {
        mutableStateOf(
            if (rule != null) {
                AutoAllocationRuleForm(
                    goalId = rule.goalId,
                    goalName = rule.goalName,
                    triggerType = rule.triggerType,
                    allocationType = rule.allocationType,
                    allocationValue = rule.allocationValue.toString(),
                    isEnabled = rule.isEnabled,
                    sourceAccountId = rule.sourceAccountId,
                    confirmationMode = rule.confirmationMode,
                    incomeCategoryIds = rule.incomeCategoryIds,
                    minIncomeAmount = if (rule.minIncomeAmount > 0) rule.minIncomeAmount.toString() else "",
                    roundUpEnabled = rule.roundUpEnabled,
                    roundUpIncrement = rule.roundUpIncrement,
                    scheduledFrequency = rule.scheduledFrequency,
                    scheduledDayOfWeek = rule.scheduledDayOfWeek,
                    scheduledDayOfMonth = rule.scheduledDayOfMonth,
                    categoryId = rule.triggerParams?.categoryId,
                    accountId = rule.triggerParams?.accountId,
                    threshold = rule.triggerParams?.threshold?.toString() ?: ""
                )
            } else {
                AutoAllocationRuleForm()
            }
        )
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Filter goals that are active and not completed
    val availableGoals = goals.filter {
        it.isActive && !it.isCompleted
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = AppPalette.card,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Allocation Rule" else "New Allocation Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                if (isEditing && onDelete != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                // ── Goal Selector ──────────────────────────────────────────────
                item {
                    SectionHeader("Goal")
                    GoalSelector(
                        goals = availableGoals,
                        selectedGoalId = form.goalId,
                        onSelect = { goal ->
                            form = form.copy(goalId = goal.id, goalName = goal.name)
                        }
                    )
                }

                // ── Trigger Type ──────────────────────────────────────────────
                item {
                    SectionHeader("Trigger")
                    TriggerTypeSelector(
                        selected = form.triggerType,
                        onSelect = { form = form.copy(triggerType = it) }
                    )
                }

                // ── Source Account ─────────────────────────────────────────────
                item {
                    SectionHeader("Source Account")
                    AccountSelector(
                        accounts = accounts,
                        selectedAccountId = form.sourceAccountId,
                        onSelect = { form = form.copy(sourceAccountId = it.id) }
                    )
                }

                // ── Allocation Value ──────────────────────────────────────────
                item {
                    SectionHeader("Allocation Amount")
                    AllocationValueSelector(
                        allocationType = form.allocationType,
                        allocationValue = form.allocationValue,
                        onTypeChange = { form = form.copy(allocationType = it) },
                        onValueChange = { form = form.copy(allocationValue = it) }
                    )
                }

                // ── Trigger-Specific Settings ─────────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = form.triggerType == AllocationTriggerType.INCOME_RECEIVED,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader("Income Settings")
                            // Minimum income threshold
                            OutlinedTextField(
                                value = form.minIncomeAmount,
                                onValueChange = { form = form.copy(minIncomeAmount = it.filter { c -> c.isDigit() || c == '.' }) },
                                label = { Text("Minimum Income (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                prefix = { Text("Rp ") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = form.triggerType == AllocationTriggerType.ROUND_UP,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader("Round-Up Settings")
                            Switch(
                                checked = form.roundUpEnabled,
                                onCheckedChange = { form = form.copy(roundUpEnabled = it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Round up expenses to save the difference",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.textMuted
                            )
                            AnimatedVisibility(visible = form.roundUpEnabled) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Round up to nearest:", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf(1000.0, 5000.0, 10000.0)) { increment ->
                                            FilterChip(
                                                selected = form.roundUpIncrement == increment,
                                                onClick = { form = form.copy(roundUpIncrement = increment) },
                                                label = { Text("Rp ${increment.toInt()}") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = form.triggerType in listOf(
                            AllocationTriggerType.DAILY,
                            AllocationTriggerType.WEEKLY,
                            AllocationTriggerType.BIWEEKLY,
                            AllocationTriggerType.MONTHLY
                        ),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader("Schedule")
                            Text(
                                text = "This rule will execute automatically on the configured schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.textMuted
                            )
                        }
                    }
                }

                // ── Confirmation Mode ──────────────────────────────────────────
                item {
                    SectionHeader("Execution Mode")
                    ConfirmationModeSelector(
                        selected = form.confirmationMode,
                        onSelect = { form = form.copy(confirmationMode = it) }
                    )
                }

                // ── Preview Card ──────────────────────────────────────────────
                item {
                    PreviewCard(form = form)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newRule = form.toRule()
                    if (newRule != null) {
                        val ruleWithId = if (isEditing) {
                            newRule.copy(id = rule!!.id)
                        } else {
                            newRule
                        }
                        onSave(ruleWithId)
                    }
                },
                enabled = form.isValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current)
            ) {
                Text(if (isEditing) "Update Rule" else "Create Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Delete Confirmation Dialog
    if (showDeleteConfirm && isEditing && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Rule?") },
            text = { Text("This auto-allocation rule will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(rule!!.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = AppPalette.textPrimary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun GoalSelector(
    goals: List<com.example.insightku.feature.budgeting.domain.model.Goal>,
    selectedGoalId: String,
    onSelect: (com.example.insightku.feature.budgeting.domain.model.Goal) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(goals) { goal ->
            val isSelected = goal.id == selectedGoalId
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(goal) },
                label = { Text(goal.name, maxLines = 1) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
    if (goals.isEmpty()) {
        Text(
            text = "Create a goal first to set up auto-allocation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun TriggerTypeSelector(
    selected: AllocationTriggerType,
    onSelect: (AllocationTriggerType) -> Unit
) {
    val options = listOf(
        AllocationTriggerType.INCOME_RECEIVED to ("Income-Based" to "Pay Yourself First"),
        AllocationTriggerType.ROUND_UP to ("Round-Up" to "Save from expenses"),
        AllocationTriggerType.DAILY to ("Daily" to "Fixed daily savings"),
        AllocationTriggerType.WEEKLY to ("Weekly" to "Fixed weekly savings"),
        AllocationTriggerType.BIWEEKLY to ("Biweekly" to "Every 2 weeks"),
        AllocationTriggerType.MONTHLY to ("Monthly" to "Fixed monthly savings"),
        AllocationTriggerType.BALANCE_ABOVE to ("Balance Above" to "When balance exceeds threshold")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(options) { (type, info) ->
            val isSelected = type == selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(type) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) LocalAccent.current.copy(alpha = 0.1f) else AppPalette.background,
                border = BorderStroke(1.dp, if (isSelected) LocalAccent.current else AppPalette.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(type) },
                        colors = RadioButtonDefaults.colors(selectedColor = LocalAccent.current)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.first,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppPalette.textPrimary
                        )
                        Text(
                            text = info.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSelector(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (Account) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accounts) { account ->
            val isSelected = account.id == selectedAccountId
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(account) },
                label = { Text(account.name, maxLines = 1) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
    if (accounts.isEmpty()) {
        Text(
            text = "Create an account first",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun AllocationValueSelector(
    allocationType: AllocationValueType,
    allocationValue: String,
    onTypeChange: (AllocationValueType) -> Unit,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Type toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = allocationType == AllocationValueType.PERCENT,
                onClick = { onTypeChange(AllocationValueType.PERCENT) },
                label = { Text("%") }
            )
            FilterChip(
                selected = allocationType == AllocationValueType.FIXED,
                onClick = { onTypeChange(AllocationValueType.FIXED) },
                label = { Text("Fixed Rp") }
            )
        }

        // Value input
        OutlinedTextField(
            value = allocationValue,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = {
                Text(
                    if (allocationType == AllocationValueType.PERCENT) "Percentage"
                    else "Fixed Amount"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = {
                Text(
                    if (allocationType == AllocationValueType.PERCENT) "%" else "Rp"
                )
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun ConfirmationModeSelector(
    selected: ConfirmationMode,
    onSelect: (ConfirmationMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == ConfirmationMode.AUTO,
            onClick = { onSelect(ConfirmationMode.AUTO) },
            label = {
                Column {
                    Text("Automatic", fontWeight = if (selected == ConfirmationMode.AUTO) FontWeight.Bold else FontWeight.Normal)
                    Text("Execute immediately", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                }
            }
        )
        FilterChip(
            selected = selected == ConfirmationMode.CONFIRMATION_REQUIRED,
            onClick = { onSelect(ConfirmationMode.CONFIRMATION_REQUIRED) },
            label = {
                Column {
                    Text("Confirm First", fontWeight = if (selected == ConfirmationMode.CONFIRMATION_REQUIRED) FontWeight.Bold else FontWeight.Normal)
                    Text("Show notification", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                }
            }
        )
    }
}

@Composable
private fun PreviewCard(form: AutoAllocationRuleForm) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalAccent.current.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, LocalAccent.current.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = LocalAccent.current
            )
            Spacer(modifier = Modifier.height(8.dp))

            val goalName = form.goalName.ifBlank { "Goal" }
            val triggerLabel = when (form.triggerType) {
                AllocationTriggerType.INCOME_RECEIVED -> "When Income Received"
                AllocationTriggerType.ROUND_UP -> "After Expense (Round-Up)"
                AllocationTriggerType.SPENDING_CATEGORY -> "On Category Spending"
                AllocationTriggerType.DAILY -> "Every Day"
                AllocationTriggerType.WEEKLY -> "Every Week"
                AllocationTriggerType.BIWEEKLY -> "Every 2 Weeks"
                AllocationTriggerType.MONTHLY -> "Every Month"
                AllocationTriggerType.BALANCE_ABOVE -> "When Balance Exceeds Threshold"
            }

            Text(
                text = triggerLabel,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
            Spacer(modifier = Modifier.height(4.dp))

            val amountText = when {
                form.allocationType == AllocationValueType.PERCENT -> "${form.allocationValue}% of amount"
                form.allocationType == AllocationValueType.FIXED -> "Rp ${form.allocationValue.toDoubleOrNull()?.toLong() ?: 0}"
                else -> ""
            }

            Text(
                text = "→ $goalName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary
            )
            Text(
                text = "  $amountText",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAccent.current
            )

            if (form.confirmationMode == ConfirmationMode.CONFIRMATION_REQUIRED) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠ Requires confirmation before executing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
