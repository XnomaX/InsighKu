package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.planning.goal.data.model.AllocationTriggerType
import com.example.insightku.feature.planning.goal.data.model.AllocationValueType
import com.example.insightku.feature.planning.goal.data.model.ConfirmationMode
import com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency
import com.example.insightku.feature.planning.goal.domain.model.AllocationTriggerParams
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.Instant
import java.util.UUID

// ─── Form State ───────────────────────────────────────────────────────────────

data class AutoAllocationRuleForm(
    val goalId: String = "",
    val goalName: String = "",
    val triggerType: AllocationTriggerType = AllocationTriggerType.INCOME_RECEIVED,
    val allocationType: AllocationValueType = AllocationValueType.PERCENT,
    val allocationValue: String = "10",
    val isEnabled: Boolean = true,
    val sourceAccountId: String? = null,
    val confirmationMode: ConfirmationMode = ConfirmationMode.AUTO,
    val incomeCategoryIds: List<String> = emptyList(),
    val minIncomeAmount: String = "",
    val roundUpEnabled: Boolean = false,
    val roundUpIncrement: Double = 5000.0,
    val scheduledFrequency: ScheduledFrequency = ScheduledFrequency.DAILY,
    val scheduledDayOfWeek: Int = 1,
    val scheduledDayOfMonth: Int = 1,
    val categoryId: String? = null,
    val accountId: String? = null,
    val threshold: String? = null
) {
    val isValid: Boolean
        get() = goalId.isNotBlank() &&
                allocationValue.toDoubleOrNull() != null &&
                allocationValue.toDoubleOrNull()!! > 0

    fun toRule(): AutoAllocationRule? {
        val value = allocationValue.toDoubleOrNull() ?: return null
        val thresholdVal = threshold?.toDoubleOrNull()
        return AutoAllocationRule(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            goalName = goalName,
            triggerType = triggerType,
            triggerParams = AllocationTriggerParams(
                categoryId = categoryId,
                accountId = accountId,
                threshold = thresholdVal
            ),
            allocationType = allocationType,
            allocationValue = value,
            isEnabled = isEnabled,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            sourceAccountId = sourceAccountId,
            confirmationMode = confirmationMode,
            incomeCategoryIds = incomeCategoryIds,
            minIncomeAmount = minIncomeAmount.toDoubleOrNull() ?: 0.0,
            roundUpEnabled = roundUpEnabled,
            roundUpIncrement = roundUpIncrement,
            scheduledFrequency = scheduledFrequency,
            scheduledDayOfWeek = scheduledDayOfWeek,
            scheduledDayOfMonth = scheduledDayOfMonth
        )
    }
}

// ─── Main Dialog ──────────────────────────────────────────────────────────────

@Composable
fun AutoAllocationDialog(
    rule: AutoAllocationRule?,
    goals: List<Goal>,
    accounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (AutoAllocationRule) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEditing = rule != null
    var form by remember {
        mutableStateOf(
            if (rule != null) AutoAllocationRuleForm(
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
            ) else AutoAllocationRuleForm()
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val availableGoals = goals.filter { it.isActive && !it.isCompleted }

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

                item {
                    SectionHeader("Trigger")
                    TriggerTypeSelector(
                        selected = form.triggerType,
                        onSelect = { form = form.copy(triggerType = it) }
                    )
                }

                item {
                    SectionHeader("Source Account")
                    AccountSelector(
                        accounts = accounts,
                        selectedAccountId = form.sourceAccountId,
                        onSelect = { form = form.copy(sourceAccountId = it.id) }
                    )
                }

                item {
                    SectionHeader("Allocation Amount")
                    AllocationValueSelector(
                        allocationType = form.allocationType,
                        allocationValue = form.allocationValue,
                        onTypeChange = { form = form.copy(allocationType = it) },
                        onValueChange = { form = form.copy(allocationValue = it) }
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = form.triggerType == AllocationTriggerType.INCOME_RECEIVED,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader("Income Settings")
                            OutlinedTextField(
                                value = form.minIncomeAmount,
                                onValueChange = {
                                    form = form.copy(
                                        minIncomeAmount = it.filter { c -> c.isDigit() || c == '.' }
                                    )
                                },
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

                item {
                    SectionHeader("Execution Mode")
                    ConfirmationModeSelector(
                        selected = form.confirmationMode,
                        onSelect = { form = form.copy(confirmationMode = it) }
                    )
                }

                item {
                    PreviewCard(form = form)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    val newRule = form.toRule()
                    if (newRule != null) {
                        val ruleWithId = if (isEditing) newRule.copy(id = rule!!.id) else newRule
                        onSave(ruleWithId)
                    }
                },
                enabled = form.isValid,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = LocalAccent.current
                )
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

    if (showDeleteConfirm && isEditing && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Rule?") },
            text = { Text("This auto-allocation rule will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(rule!!.id); showDeleteConfirm = false },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

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
private fun GoalSelector(goals: List<Goal>, selectedGoalId: String, onSelect: (Goal) -> Unit) {
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
private fun TriggerTypeSelector(selected: AllocationTriggerType, onSelect: (AllocationTriggerType) -> Unit) {
    val options = listOf(
        AllocationTriggerType.INCOME_RECEIVED to ("Income-Based" to "Pay Yourself First"),
        AllocationTriggerType.ROUND_UP to ("Round-Up" to "Save from expenses"),
        AllocationTriggerType.DAILY to ("Daily" to "Fixed daily savings"),
        AllocationTriggerType.WEEKLY to ("Weekly" to "Fixed weekly savings"),
        AllocationTriggerType.BIWEEKLY to ("Biweekly" to "Every 2 weeks"),
        AllocationTriggerType.MONTHLY to ("Monthly" to "Fixed monthly savings"),
        AllocationTriggerType.BALANCE_ABOVE to ("Balance Above" to "When balance exceeds threshold")
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (type, info) ->
            val isSelected = type == selected
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(type) },
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
private fun AccountSelector(accounts: List<Account>, selectedAccountId: String?, onSelect: (Account) -> Unit) {
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
        OutlinedTextField(
            value = allocationValue,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = { Text(if (allocationType == AllocationValueType.PERCENT) "Percentage" else "Fixed Amount") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text(if (allocationType == AllocationValueType.PERCENT) "%" else "Rp") },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun ConfirmationModeSelector(selected: ConfirmationMode, onSelect: (ConfirmationMode) -> Unit) {
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
    val accentColor = LocalAccent.current
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
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
            Text(text = triggerLabel, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            Spacer(modifier = Modifier.height(4.dp))
            val amountText = when {
                form.allocationType == AllocationValueType.PERCENT -> "${form.allocationValue}% of amount"
                form.allocationType == AllocationValueType.FIXED -> "Rp ${form.allocationValue.toDoubleOrNull()?.toLong() ?: 0}"
                else -> ""
            }
            Text(text = "\u2192 $goalName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
            Text(text = "  $amountText", style = MaterialTheme.typography.bodySmall, color = accentColor)
            if (form.confirmationMode == ConfirmationMode.CONFIRMATION_REQUIRED) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\u26A0 Requires confirmation before executing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


