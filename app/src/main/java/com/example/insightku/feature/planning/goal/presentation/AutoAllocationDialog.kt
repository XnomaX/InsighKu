package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.CurrencyUtils
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

// ─── Main Bottom Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoAllocationDialog(
    rule: AutoAllocationRule?,
    goal: Goal?,
    accounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (AutoAllocationRule) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEditing = rule != null
    val fallbackColor = LocalAccent.current
    val goalColor = remember(goal?.color) {
        try { Color(android.graphics.Color.parseColor(goal?.color ?: "")) }
        catch (_: Exception) { fallbackColor }
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                minIncomeAmount = if (rule.minIncomeAmount > 0) rule.minIncomeAmount.toLong().toString() else "",
                roundUpEnabled = rule.roundUpEnabled,
                roundUpIncrement = rule.roundUpIncrement,
                scheduledFrequency = rule.scheduledFrequency,
                scheduledDayOfWeek = rule.scheduledDayOfWeek,
                scheduledDayOfMonth = rule.scheduledDayOfMonth,
                categoryId = rule.triggerParams?.categoryId,
                accountId = rule.triggerParams?.accountId,
                threshold = rule.triggerParams?.threshold?.let { if (it > 0) it.toLong().toString() else "" } ?: ""
            ) else AutoAllocationRuleForm(
                goalId = goal?.id ?: "",
                goalName = goal?.name ?: ""
            )
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun handleDismiss() {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { handleDismiss() },
        sheetState = sheetState,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(
            topStart = Dimens.BottomSheetRadius,
            topEnd = Dimens.BottomSheetRadius
        ),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(AppPalette.cardBorder)
                )
            }
        }
    ) {
        // ── Header ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp)
        ) {
            Surface(shape = RoundedCornerShape(50), color = goalColor.copy(alpha = 0.10f)) {
                Text(
                    text = "Auto Allocation",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = goalColor
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) "Edit Allocation Rule" else "New Allocation Rule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        text = "Configure automatic savings",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
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
        }

        HorizontalDivider(color = AppPalette.cardBorder)

        // ── Scrollable Content ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(AppPalette.background)
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Goal Summary Card
            if (goal != null) {
                GoalSummaryCard(goal = goal, goalColor = goalColor)
            }

            // Trigger
            FormSectionLabel("TRIGGER")
            TriggerTypeSelector(
                selected = form.triggerType,
                onSelect = { form = form.copy(triggerType = it) },
                accentColor = goalColor
            )

            // Source Account
            FormSectionLabel("SOURCE ACCOUNT")
            AccountSelector(
                accounts = accounts,
                selectedAccountId = form.sourceAccountId,
                onSelect = { form = form.copy(sourceAccountId = it.id) },
                accentColor = goalColor
            )

            // Allocation Amount
            FormSectionLabel("ALLOCATION AMOUNT")
            AllocationValueSelector(
                allocationType = form.allocationType,
                allocationValue = form.allocationValue,
                onTypeChange = { form = form.copy(allocationType = it) },
                onValueChange = { form = form.copy(allocationValue = it) },
                accentColor = goalColor
            )

            // Income Settings (conditional)
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.INCOME_RECEIVED,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormSectionLabel("MINIMUM INCOME (OPTIONAL)")
                    OutlinedTextField(
                        value = if (form.minIncomeAmount.isEmpty()) ""
                            else CurrencyUtils.formatInputThousands(form.minIncomeAmount),
                        onValueChange = {
                            form = form.copy(minIncomeAmount = CurrencyUtils.stripThousands(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text("e.g. 5.000.000", color = AppPalette.placeholder)
                        },
                        prefix = {
                            Text("Rp ", fontWeight = FontWeight.Bold, color = goalColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goalColor,
                            unfocusedBorderColor = AppPalette.cardBorder,
                            focusedContainerColor = AppPalette.card,
                            unfocusedContainerColor = AppPalette.card
                        )
                    )
                }
            }

            // Threshold Settings (conditional)
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.BALANCE_ABOVE,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormSectionLabel("BALANCE THRESHOLD")
                    OutlinedTextField(
                        value = if (form.threshold.isNullOrEmpty()) ""
                            else CurrencyUtils.formatInputThousands(form.threshold!!),
                        onValueChange = {
                            form = form.copy(threshold = CurrencyUtils.stripThousands(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text("e.g. 10.000.000", color = AppPalette.placeholder)
                        },
                        prefix = {
                            Text("Rp ", fontWeight = FontWeight.Bold, color = goalColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goalColor,
                            unfocusedBorderColor = AppPalette.cardBorder,
                            focusedContainerColor = AppPalette.card,
                            unfocusedContainerColor = AppPalette.card
                        )
                    )
                    Text(
                        text = "Rule triggers when account balance exceeds this amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }

            // Schedule Info (conditional)
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPalette.cardElevated),
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = goalColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "This rule will execute automatically on the configured schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted
                        )
                    }
                }
            }

            // Execution Mode
            FormSectionLabel("EXECUTION MODE")
            ConfirmationModeSelector(
                selected = form.confirmationMode,
                onSelect = { form = form.copy(confirmationMode = it) },
                accentColor = goalColor
            )

            // Preview Card
            PreviewCard(form = form, accentColor = goalColor)

            Spacer(Modifier.height(4.dp))

            // ── Action Buttons ──────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable { handleDismiss() },
                    shape = RoundedCornerShape(14.dp),
                    color = AppPalette.card,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppPalette.textDialogMuted
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (form.isValid) goalColor else AppPalette.textMuted)
                        .clickable(enabled = form.isValid) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            val newRule = form.toRule()
                            if (newRule != null) {
                                val ruleWithId =
                                    if (isEditing) newRule.copy(id = rule!!.id) else newRule
                                onSave(ruleWithId)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isEditing) "Update Rule" else "Create Rule",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ────────────────────────────────────────
    if (showDeleteConfirm && isEditing && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text("Delete Rule?", fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
            },
            text = {
                Text(
                    "This auto-allocation rule will be permanently deleted. " +
                            "Automatic savings for this rule will stop.",
                    color = AppPalette.textMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(rule!!.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = AppPalette.card
        )
    }
}

// ─── Section Label ────────────────────────────────────────────────────────────

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppPalette.textMuted
    )
}

// ─── Goal Summary Card ────────────────────────────────────────────────────────

@Composable
private fun GoalSummaryCard(goal: Goal, goalColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = goalColor.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, goalColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(goalColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getGoalIcon(goal.iconName),
                    contentDescription = null,
                    tint = goalColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-allocation for",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppPalette.textMuted
                )
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (goal.targetAmount > 0) {
                    Text(
                        text = "${CurrencyUtils.formatAmountCompact(goal.currentAmount)} / ${CurrencyUtils.formatAmountCompact(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = goalColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${goal.progressPercent.toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = goalColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─── Trigger Type Selector ────────────────────────────────────────────────────

private data class TriggerOption(
    val type: AllocationTriggerType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val triggerOptions = listOf(
    TriggerOption(
        AllocationTriggerType.INCOME_RECEIVED,
        "Income-Based",
        "Pay Yourself First",
        Icons.Outlined.TrendingUp
    ),
    TriggerOption(
        AllocationTriggerType.ROUND_UP,
        "Round-Up",
        "Save from expenses",
        Icons.Outlined.ChangeHistory
    ),
    TriggerOption(
        AllocationTriggerType.DAILY,
        "Daily",
        "Fixed daily savings",
        Icons.Outlined.Schedule
    ),
    TriggerOption(
        AllocationTriggerType.WEEKLY,
        "Weekly",
        "Fixed weekly savings",
        Icons.Outlined.Schedule
    ),
    TriggerOption(
        AllocationTriggerType.BIWEEKLY,
        "Biweekly",
        "Every 2 weeks",
        Icons.Outlined.Schedule
    ),
    TriggerOption(
        AllocationTriggerType.MONTHLY,
        "Monthly",
        "Fixed monthly savings",
        Icons.Outlined.Schedule
    ),
    TriggerOption(
        AllocationTriggerType.BALANCE_ABOVE,
        "Balance Above",
        "When balance exceeds threshold",
        Icons.Outlined.AccountBalance
    ),
    TriggerOption(
        AllocationTriggerType.SPENDING_CATEGORY,
        "Category-Based",
        "Save from category spending",
        Icons.Outlined.Category
    )
)

@Composable
private fun TriggerTypeSelector(
    selected: AllocationTriggerType,
    onSelect: (AllocationTriggerType) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        triggerOptions.forEach { option ->
            val isSelected = option.type == selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option.type) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.08f) else AppPalette.card,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) accentColor else AppPalette.cardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.15f)
                                else AppPalette.cardElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) accentColor else AppPalette.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppPalette.textPrimary
                        )
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Account Selector ─────────────────────────────────────────────────────────

@Composable
private fun AccountSelector(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (Account) -> Unit,
    accentColor: Color
) {
    if (accounts.isEmpty()) {
        Text(
            text = "Create an account first",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts) { account ->
                val isSelected = account.id == selectedAccountId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(account) },
                    label = { Text(account.name, maxLines = 1) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.12f),
                        selectedLabelColor = accentColor
                    )
                )
            }
        }
    }
}

// ─── Allocation Value Selector ────────────────────────────────────────────────

@Composable
private fun AllocationValueSelector(
    allocationType: AllocationValueType,
    allocationValue: String,
    onTypeChange: (AllocationValueType) -> Unit,
    onValueChange: (String) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = allocationType == AllocationValueType.PERCENT,
                onClick = { onTypeChange(AllocationValueType.PERCENT) },
                label = { Text("Percentage %") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.12f),
                    selectedLabelColor = accentColor
                )
            )
            FilterChip(
                selected = allocationType == AllocationValueType.FIXED,
                onClick = { onTypeChange(AllocationValueType.FIXED) },
                label = { Text("Fixed Amount") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.12f),
                    selectedLabelColor = accentColor
                )
            )
        }
        OutlinedTextField(
            value = if (allocationType == AllocationValueType.FIXED) {
                if (allocationValue.isEmpty()) ""
                else CurrencyUtils.formatInputThousands(allocationValue)
            } else {
                allocationValue
            },
            onValueChange = { newValue ->
                if (allocationType == AllocationValueType.FIXED) {
                    onValueChange(CurrencyUtils.stripThousands(newValue))
                } else {
                    onValueChange(newValue.filter { c -> c.isDigit() || c == '.' })
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    if (allocationType == AllocationValueType.PERCENT) "e.g. 10"
                    else "e.g. 500.000",
                    color = AppPalette.placeholder
                )
            },
            prefix = if (allocationType == AllocationValueType.FIXED) {
                { Text("Rp ", fontWeight = FontWeight.Bold, color = accentColor) }
            } else null,
            suffix = if (allocationType == AllocationValueType.PERCENT) {
                { Text("%", fontWeight = FontWeight.Bold, color = accentColor) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = AppPalette.cardBorder,
                focusedContainerColor = AppPalette.card,
                unfocusedContainerColor = AppPalette.card
            )
        )
    }
}

// ─── Confirmation Mode Selector ───────────────────────────────────────────────

@Composable
private fun ConfirmationModeSelector(
    selected: ConfirmationMode,
    onSelect: (ConfirmationMode) -> Unit,
    accentColor: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val autoSelected = selected == ConfirmationMode.AUTO
        val confirmSelected = selected == ConfirmationMode.CONFIRMATION_REQUIRED

        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onSelect(ConfirmationMode.AUTO) },
            shape = RoundedCornerShape(14.dp),
            color = if (autoSelected) accentColor else AppPalette.card,
            border = BorderStroke(
                1.dp,
                if (autoSelected) accentColor else AppPalette.cardBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Automatic",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (autoSelected) Color.White else AppPalette.textPrimary
                )
                Text(
                    text = "Execute immediately",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (autoSelected) Color.White.copy(alpha = 0.7f)
                    else AppPalette.textMuted
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onSelect(ConfirmationMode.CONFIRMATION_REQUIRED) },
            shape = RoundedCornerShape(14.dp),
            color = if (confirmSelected) accentColor else AppPalette.card,
            border = BorderStroke(
                1.dp,
                if (confirmSelected) accentColor else AppPalette.cardBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Confirm First",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (confirmSelected) Color.White else AppPalette.textPrimary
                )
                Text(
                    text = "Show notification before executing",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (confirmSelected) Color.White.copy(alpha = 0.7f)
                    else AppPalette.textMuted
                )
            }
        }
    }
}

// ─── Preview Card ─────────────────────────────────────────────────────────────

@Composable
private fun PreviewCard(form: AutoAllocationRuleForm, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = RoundedCornerShape(50), color = accentColor.copy(alpha = 0.12f)) {
                Text(
                    text = "Preview",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }

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

            // Trigger row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = triggerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }

            // Destination row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "\u2192",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = goalName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary
                )
            }

            // Amount row
            val amountText = when {
                form.allocationType == AllocationValueType.PERCENT ->
                    "${form.allocationValue}% of amount"
                form.allocationType == AllocationValueType.FIXED -> {
                    val raw = form.allocationValue.toLongOrNull() ?: 0L
                    "Rp ${CurrencyUtils.formatInputThousands(raw.toString())}"
                }
                else -> ""
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )

            // Confirmation warning
            if (form.confirmationMode == ConfirmationMode.CONFIRMATION_REQUIRED) {
                HorizontalDivider(color = AppPalette.cardBorder)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Requires confirmation before executing",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
