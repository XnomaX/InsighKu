package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingUp
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
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
import com.example.insightku.feature.planning.goal.data.model.CategoryBasedExecutionMode
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// ─── Account Type Icon Resolver ────────────────────────────────────────────────

private fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance
    AccountType.CASH -> Icons.Outlined.Money
    AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet
    AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
}

private fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.BANK_ACCOUNT -> "Bank"
    AccountType.CASH -> "Cash"
    AccountType.E_WALLET -> "E-Wallet"
    AccountType.CREDIT_CARD -> "Credit"
}

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
    val threshold: String? = null,
    // ── P1.1: Trigger-specific configuration fields ──────────────────────────
    val executionHour: Int = 8,
    val executionMinute: Int = 0,
    val biweeklyStartDate: Long = 0,
    val minRemainingBalance: String = "",
    val categoryBasedCategoryIds: List<String> = emptyList(),
    val categoryBasedExecutionMode: String = "every_transaction"
) {
    /** Per-trigger-type validation */
    val isValid: Boolean
        get() {
            if (goalId.isBlank()) return false
            if (allocationValue.toDoubleOrNull() == null || allocationValue.toDoubleOrNull()!! <= 0) return false
            if (sourceAccountId.isNullOrBlank()) return false

            // Trigger-specific validation
            return when (triggerType) {
                AllocationTriggerType.DAILY,
                AllocationTriggerType.WEEKLY,
                AllocationTriggerType.BIWEEKLY,
                AllocationTriggerType.MONTHLY -> {
                    // Scheduled rules require valid execution time
                    executionHour in 0..23 && executionMinute in 0..59
                }
                AllocationTriggerType.BALANCE_ABOVE -> {
                    // Threshold must be > 0
                    val t = threshold?.toDoubleOrNull() ?: 0.0
                    t > 0
                }
                AllocationTriggerType.SPENDING_CATEGORY -> {
                    // At least one category selected
                    categoryBasedCategoryIds.isNotEmpty()
                }
                AllocationTriggerType.INCOME_RECEIVED -> true
                AllocationTriggerType.ROUND_UP -> true
            }
        }

    fun toRule(): AutoAllocationRule? {
        val value = allocationValue.toDoubleOrNull() ?: return null
        val thresholdVal = threshold?.toDoubleOrNull()
        val minRemainingVal = minRemainingBalance.toDoubleOrNull() ?: 0.0
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
            scheduledDayOfMonth = scheduledDayOfMonth,
            // ── P1.1: Trigger-specific fields ──────────────────────────────
            executionHour = executionHour,
            executionMinute = executionMinute,
            biweeklyStartDate = biweeklyStartDate,
            minRemainingBalance = minRemainingVal,
            categoryBasedCategoryIds = categoryBasedCategoryIds,
            categoryBasedExecutionMode = com.example.insightku.feature.planning.goal.data.model.CategoryBasedExecutionMode.fromString(categoryBasedExecutionMode)
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
    expenseCategories: List<CategoryInfo> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (AutoAllocationRule) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onNavigateToAccounts: (() -> Unit)? = null
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

    // Filter to active accounts only
    val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }

    // Check if the previously selected account still exists
    val previousAccountStillExists = remember(rule?.sourceAccountId, activeAccounts) {
        val prevId = rule?.sourceAccountId
        prevId != null && activeAccounts.any { it.id == prevId }
    }

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
                threshold = rule.triggerParams?.threshold?.let { if (it > 0) it.toLong().toString() else "" } ?: "",
                // ── P1.1: Restore trigger-specific fields ─────────────────────
                executionHour = rule.executionHour,
                executionMinute = rule.executionMinute,
                biweeklyStartDate = rule.biweeklyStartDate,
                minRemainingBalance = if (rule.minRemainingBalance > 0) rule.minRemainingBalance.toLong().toString() else "",
                categoryBasedCategoryIds = rule.categoryBasedCategoryIds,
                categoryBasedExecutionMode = rule.categoryBasedExecutionMode.value
            ) else AutoAllocationRuleForm(
                goalId = goal?.id ?: "",
                goalName = goal?.name ?: ""
            )
        )
    }

    // Auto-select if only one account exists and no previous selection
    LaunchedEffect(activeAccounts.size) {
        if (form.sourceAccountId == null && activeAccounts.size == 1) {
            form = form.copy(sourceAccountId = activeAccounts.first().id)
        }
    }

    // Clear invalid selection if account was deleted
    LaunchedEffect(activeAccounts) {
        val selected = form.sourceAccountId
        if (selected != null && activeAccounts.none { it.id == selected }) {
            form = form.copy(sourceAccountId = null)
        }
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
                onSelect = { newTrigger ->
                    // Sync scheduledFrequency with the selected trigger type
                    val newFreq = when (newTrigger) {
                        AllocationTriggerType.DAILY -> ScheduledFrequency.DAILY
                        AllocationTriggerType.WEEKLY -> ScheduledFrequency.WEEKLY
                        AllocationTriggerType.BIWEEKLY -> ScheduledFrequency.BIWEEKLY
                        AllocationTriggerType.MONTHLY -> ScheduledFrequency.MONTHLY
                        else -> form.scheduledFrequency
                    }
                    form = form.copy(triggerType = newTrigger, scheduledFrequency = newFreq)
                },
                accentColor = goalColor
            )

            // Source Account
            FormSectionLabel("SOURCE ACCOUNT")
            AccountSelector(
                accounts = activeAccounts,
                selectedAccountId = form.sourceAccountId,
                isEditing = isEditing,
                previousAccountStillExists = previousAccountStillExists,
                onSelect = { form = form.copy(sourceAccountId = it.id) },
                onNavigateToAccounts = onNavigateToAccounts,
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
                            Text(NumberFormatter.getCurrencySymbol(), fontWeight = FontWeight.Bold, color = goalColor)
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

            // ── BALANCE ABOVE: Threshold + Min Remaining Balance ──────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.BALANCE_ABOVE,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                            Text(NumberFormatter.getCurrencySymbol(), fontWeight = FontWeight.Bold, color = goalColor)
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
                        text = "Allocate when account balance exceeds this amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )

                    FormSectionLabel("MINIMUM REMAINING BALANCE (OPTIONAL)")
                    OutlinedTextField(
                        value = if (form.minRemainingBalance.isEmpty()) ""
                            else CurrencyUtils.formatInputThousands(form.minRemainingBalance),
                        onValueChange = {
                            form = form.copy(minRemainingBalance = CurrencyUtils.stripThousands(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text("e.g. 1.000.000", color = AppPalette.placeholder)
                        },
                        prefix = {
                            Text(NumberFormatter.getCurrencySymbol(), fontWeight = FontWeight.Bold, color = goalColor)
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
                        text = "Keep at least this amount in the source account",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }

            // ── DAILY: Time Picker ───────────────────────────────────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.DAILY,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormSectionLabel("EXECUTION TIME")
                    TimePickerRow(
                        hour = form.executionHour,
                        minute = form.executionMinute,
                        onHourChange = { h -> form = form.copy(executionHour = h) },
                        onMinuteChange = { m -> form = form.copy(executionMinute = m) },
                        accentColor = goalColor,
                        frequencyLabel = "every day"
                    )
                }
            }

            // ── WEEKLY: Day Picker + Time Picker ─────────────────────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.WEEKLY,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormSectionLabel("EXECUTION DAY")
                    WeekDayPicker(
                        selectedDay = form.scheduledDayOfWeek,
                        onDaySelected = { d -> form = form.copy(scheduledDayOfWeek = d) },
                        accentColor = goalColor
                    )
                    FormSectionLabel("EXECUTION TIME")
                    val dayName = when (form.scheduledDayOfWeek) { 1->"Monday";2->"Tuesday";3->"Wednesday";4->"Thursday";5->"Friday";6->"Saturday";7->"Sunday";else->"Monday" }
                    TimePickerRow(
                        hour = form.executionHour,
                        minute = form.executionMinute,
                        onHourChange = { h -> form = form.copy(executionHour = h) },
                        onMinuteChange = { m -> form = form.copy(executionMinute = m) },
                        accentColor = goalColor,
                        frequencyLabel = "every $dayName"
                    )
                }
            }

            // ── BIWEEKLY: Start Date + Time Picker ───────────────────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.BIWEEKLY,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormSectionLabel("START DATE")
                    val startDateText = if (form.biweeklyStartDate > 0) {
                        DateFormatter.formatFullDate(form.biweeklyStartDate)
                    } else "Select start date"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                        border = BorderStroke(1.dp, if (form.biweeklyStartDate > 0) goalColor else AppPalette.cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Starting",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.textMuted
                            )
                            Text(
                                text = startDateText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (form.biweeklyStartDate > 0) goalColor else AppPalette.textMuted
                            )
                            // Simple date picker using day/month chips
                            val today = LocalDate.now()
                            val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val nextMonday = if (today.dayOfWeek.value == 1) {
                                    today.plusDays(7)
                                } else {
                                    val daysUntilMonday = (8 - today.dayOfWeek.value) % 7
                                    today.plusDays(daysUntilMonday.toLong().coerceAtLeast(1))
                                }
                                listOf("Today" to todayMillis, "Tomorrow" to (todayMillis + 86400000L), "Next Monday" to nextMonday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()).forEach { (label, millis) ->
                                    val ts = millis as Long
                                    val isSel = form.biweeklyStartDate == ts
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { form = form.copy(biweeklyStartDate = ts) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = goalColor.copy(alpha = 0.12f),
                                            selectedLabelColor = goalColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Repeats every 2 weeks from the selected date",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                    FormSectionLabel("EXECUTION TIME")
                    TimePickerRow(
                        hour = form.executionHour,
                        minute = form.executionMinute,
                        onHourChange = { h -> form = form.copy(executionHour = h) },
                        onMinuteChange = { m -> form = form.copy(executionMinute = m) },
                        accentColor = goalColor,
                        frequencyLabel = "every 2 weeks"
                    )
                }
            }

            // ── MONTHLY: Day of Month + Time Picker ──────────────────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.MONTHLY,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormSectionLabel("DAY OF MONTH")
                    MonthDayPicker(
                        selectedDay = form.scheduledDayOfMonth,
                        onDaySelected = { d -> form = form.copy(scheduledDayOfMonth = d) },
                        accentColor = goalColor
                    )
                    FormSectionLabel("EXECUTION TIME")
                    val dayText = if (form.scheduledDayOfMonth == -1) "last day"
                    else "the ${form.scheduledDayOfMonth}${when(form.scheduledDayOfMonth%10){1->"st";2->"nd";3->"rd";else->"th"}}"
                    TimePickerRow(
                        hour = form.executionHour,
                        minute = form.executionMinute,
                        onHourChange = { h -> form = form.copy(executionHour = h) },
                        onMinuteChange = { m -> form = form.copy(executionMinute = m) },
                        accentColor = goalColor,
                        frequencyLabel = "every $dayText of the month"
                    )
                }
            }

            // ── SPENDING CATEGORY: Category Selector + Execution Mode ─────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.SPENDING_CATEGORY,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormSectionLabel("CATEGORIES")
                    val categoriesToShow = remember(expenseCategories) {
                        if (expenseCategories.isNotEmpty()) expenseCategories
                        else listOf(
                            CategoryInfo("fallback-food", "Food"), CategoryInfo("fallback-transport", "Transport"),
                            CategoryInfo("fallback-shopping", "Shopping"), CategoryInfo("fallback-bills", "Bills"),
                            CategoryInfo("fallback-entertainment", "Entertainment"), CategoryInfo("fallback-health", "Health"),
                            CategoryInfo("fallback-education", "Education"), CategoryInfo("fallback-other", "Other")
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categoriesToShow.forEach { cat ->
                            val isSelected = form.categoryBasedCategoryIds.contains(cat.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updated = if (isSelected) form.categoryBasedCategoryIds - cat.id
                                        else form.categoryBasedCategoryIds + cat.id
                                        form = form.copy(categoryBasedCategoryIds = updated)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) goalColor.copy(alpha = 0.08f) else AppPalette.card,
                                border = BorderStroke(1.dp, if (isSelected) goalColor else AppPalette.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) goalColor else Color.Transparent)
                                            .then(if (!isSelected) Modifier.border(1.dp, AppPalette.cardBorder, RoundedCornerShape(6.dp)) else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = AppPalette.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    FormSectionLabel("EXECUTION MODE")
                    val modes = listOf(
                        "every_transaction" to "Every Transaction",
                        "after_daily_total" to "After Daily Total",
                        "after_monthly_total" to "After Monthly Total"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        modes.forEach { (value, label) ->
                            val isSel = form.categoryBasedExecutionMode == value
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { form = form.copy(categoryBasedExecutionMode = value) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) goalColor.copy(alpha = 0.08f) else AppPalette.card,
                                border = BorderStroke(1.dp, if (isSel) goalColor else AppPalette.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isSel) goalColor else Color.Transparent)
                                            .then(if (!isSel) Modifier.border(1.5.dp, AppPalette.cardBorder, RoundedCornerShape(9.dp)) else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSel) {
                                            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                                        }
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                        color = AppPalette.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Confirmation Mode
            FormSectionLabel("CONFIRMATION")
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
    isEditing: Boolean,
    previousAccountStillExists: Boolean,
    onSelect: (Account) -> Unit,
    onNavigateToAccounts: (() -> Unit)?,
    accentColor: Color
) {
    when {
        // Empty state — no accounts exist
        accounts.isEmpty() -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "Create an account first",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        text = "You need at least one account to set up auto-allocation rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                    if (onNavigateToAccounts != null) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onNavigateToAccounts()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Create Account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Editing mode — previously selected account no longer exists
        isEditing && selectedAccountId == null && !previousAccountStillExists -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.04f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Source account unavailable",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "The previously selected account was deleted. Please select another.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted
                        )
                    }
                }
            }
            // Show account picker below so user can select a new one
            AccountPickerChips(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onSelect = onSelect,
                accentColor = accentColor
            )
        }

        // Accounts exist — show the account picker
        else -> {
            AccountPickerChips(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onSelect = onSelect,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun AccountPickerChips(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (Account) -> Unit,
    accentColor: Color
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(accounts, key = { it.id }) { account ->
            val isSelected = account.id == selectedAccountId
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) accentColor else AppPalette.cardBorder,
                animationSpec = tween(durationMillis = 200),
                label = "accountBorder"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) accentColor.copy(alpha = 0.08f) else AppPalette.card,
                animationSpec = tween(durationMillis = 200),
                label = "accountBg"
            )

            Surface(
                modifier = Modifier
                    .width(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(account) },
                shape = RoundedCornerShape(14.dp),
                color = bgColor,
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Account icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(account.color))
                                        .copy(alpha = 0.12f)
                                } catch (_: Exception) {
                                    accentColor.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = accountTypeIcon(account.type),
                            contentDescription = null,
                            tint = try {
                                Color(android.graphics.Color.parseColor(account.color))
                            } catch (_: Exception) {
                                accentColor
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Account info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPalette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // Type badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = try {
                                    Color(android.graphics.Color.parseColor(account.color))
                                        .copy(alpha = 0.10f)
                                } catch (_: Exception) {
                                    accentColor.copy(alpha = 0.10f)
                                }
                            ) {
                                Text(
                                    text = accountTypeLabel(account.type),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = try {
                                        Color(android.graphics.Color.parseColor(account.color))
                                    } catch (_: Exception) {
                                        accentColor
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = CurrencyUtils.formatAmountCompact(account.balance),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted,
                            maxLines = 1
                        )
                    }

                    // Selected indicator
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
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
                { Text(NumberFormatter.getCurrencySymbol(), fontWeight = FontWeight.Bold, color = accentColor) }
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

// ─── Execution Mode Selector (P2.1: Reusable, Premium Segmented Selection) ─────

private data class ExecutionModeOption(
    val mode: ConfirmationMode,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val executionModeOptions = listOf(
    ExecutionModeOption(
        mode = ConfirmationMode.AUTO,
        title = "Automatic",
        description = "Execute immediately when triggered",
        icon = Icons.Outlined.AutoAwesome
    ),
    ExecutionModeOption(
        mode = ConfirmationMode.CONFIRMATION_REQUIRED,
        title = "Confirm First",
        description = "Show notification before executing",
        icon = Icons.Outlined.Warning
    )
)

@Composable
private fun ConfirmationModeSelector(
    selected: ConfirmationMode,
    onSelect: (ConfirmationMode) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        executionModeOptions.forEach { option ->
            val isSelected = selected == option.mode
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) accentColor else AppPalette.cardBorder,
                animationSpec = tween(durationMillis = 250),
                label = "execMode_border"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) accentColor.copy(alpha = 0.08f) else AppPalette.card,
                animationSpec = tween(durationMillis = 250),
                label = "execMode_bg"
            )
            val iconBgColor by animateColorAsState(
                targetValue = if (isSelected) accentColor.copy(alpha = 0.15f) else AppPalette.cardElevated,
                animationSpec = tween(durationMillis = 250),
                label = "execMode_iconBg"
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(option.mode) },
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── Top Row: Icon + Title + Indicator ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Icon container
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iconBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) accentColor else AppPalette.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Title
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) accentColor else AppPalette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Selection indicator
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isSelected) accentColor else Color.Transparent)
                                .then(
                                    if (!isSelected) Modifier.border(1.5.dp, AppPalette.cardBorder, RoundedCornerShape(11.dp))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // ── Bottom Section: Description ──
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) accentColor.copy(alpha = 0.8f) else AppPalette.textMuted,
                        modifier = Modifier.padding(start = 46.dp)
                    )
                }
            }
        }
    }
}

// ─── Time Picker Row ─────────────────────────────────────────────────────────

@Composable
private fun TimePickerRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    accentColor: Color,
    frequencyLabel: String = "every day"
) {
    val minutes = listOf(0, 15, 30, 45)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour selector
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Hour",
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Quick-select row for common hours
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(8, 12, 18, 21).forEach { h ->
                            FilterChip(
                                selected = hour == h,
                                onClick = { onHourChange(h) },
                                label = { Text("$h", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.12f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                    // Custom hour input
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Custom:", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                        // Decrease button
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onHourChange((hour - 1 + 24) % 24) },
                            shape = RoundedCornerShape(8.dp),
                            color = AppPalette.cardElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("−", fontWeight = FontWeight.Bold, color = accentColor)
                            }
                        }
                        Text(
                            text = hour.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.width(28.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        // Increase button
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onHourChange((hour + 1) % 24) },
                            shape = RoundedCornerShape(8.dp),
                            color = AppPalette.cardElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", fontWeight = FontWeight.Bold, color = accentColor)
                            }
                        }
                    }
                }
            }
        }

        // Minute selector
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Minute",
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        minutes.forEach { m ->
                            FilterChip(
                                selected = minute == m,
                                onClick = { onMinuteChange(m) },
                                label = { Text(":${m.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.12f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }            // Formatted time display
    val timeStr = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Schedule, null, tint = accentColor, modifier = Modifier.size(16.dp))
            Text(
                text = "Executes $frequencyLabel at $timeStr",
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }
    }
}

// ─── Week Day Picker ──────────────────────────────────────────────────────────

@Composable
private fun WeekDayPicker(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    accentColor: Color
) {
    val days = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { (dayNum, dayLabel) ->
            val isSelected = selectedDay == dayNum
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDaySelected(dayNum) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) accentColor else AppPalette.card,
                border = BorderStroke(1.dp, if (isSelected) accentColor else AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else AppPalette.textMuted
                    )
                }
            }
        }
    }
}

// ─── Month Day Picker ─────────────────────────────────────────────────────────

@Composable
private fun MonthDayPicker(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Grid of days 1–28
        val rows = (1..28).chunked(7)
        rows.forEach { rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowDays.forEach { day ->
                    val isSelected = selectedDay == day
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDaySelected(day) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) accentColor else AppPalette.card,
                        border = BorderStroke(1.dp, if (isSelected) accentColor else AppPalette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$day",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else AppPalette.textMuted
                            )
                        }
                    }
                }
            }
        }
        // Extra options: 29, 30, 31, Last Day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(29, 30, 31).forEach { day ->
                val isSelected = selectedDay == day
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDaySelected(day) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) accentColor else AppPalette.card,
                    border = BorderStroke(1.dp, if (isSelected) accentColor else AppPalette.cardBorder)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$day",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else AppPalette.textMuted
                        )
                    }
                }
            }
            // Last Day of Month
            val isLastDay = selectedDay == -1
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDaySelected(-1) },
                shape = RoundedCornerShape(8.dp),
                color = if (isLastDay) accentColor else AppPalette.card,
                border = BorderStroke(1.dp, if (isLastDay) accentColor else AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Last",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isLastDay) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLastDay) Color.White else AppPalette.textMuted
                    )
                }
            }
        }
    }
}

// ─── Preview Card ─────────────────────────────────────────────────────────────

@Composable
private fun PreviewCard(form: AutoAllocationRuleForm, accentColor: Color) {
    val timeStr = "${form.executionHour.toString().padStart(2, '0')}:${form.executionMinute.toString().padStart(2, '0')}"
    val summary = when (form.triggerType) {
        AllocationTriggerType.DAILY -> "Every day at $timeStr"
        AllocationTriggerType.WEEKLY -> {
            val dayName = when (form.scheduledDayOfWeek) { 1->"Monday";2->"Tuesday";3->"Wednesday";4->"Thursday";5->"Friday";6->"Saturday";7->"Sunday";else->"Monday" }
            "Every $dayName at $timeStr"
        }
        AllocationTriggerType.BIWEEKLY -> {
            val dateText = if (form.biweeklyStartDate > 0) {
                DateFormatter.formatShortDate(form.biweeklyStartDate)
            } else "not set"
            "Every two weeks starting $dateText at $timeStr"
        }
        AllocationTriggerType.MONTHLY -> {
            val dayText = if (form.scheduledDayOfMonth == -1) "Last day"
            else "${form.scheduledDayOfMonth}${when(form.scheduledDayOfMonth%10){1->"st";2->"nd";3->"rd";else->"th"}}"
            "Every $dayText at $timeStr"
        }
        AllocationTriggerType.BALANCE_ABOVE -> {
            val thresh = form.threshold?.toDoubleOrNull() ?: 0.0
            val base = "When balance exceeds Rp ${"%,.0f".format(thresh).replace(",", ".")}"
            val minRem = form.minRemainingBalance.toDoubleOrNull() ?: 0.0
            if (minRem > 0) "$base (keep Rp ${"%,.0f".format(minRem).replace(",", ".")})" else base
        }
        AllocationTriggerType.SPENDING_CATEGORY -> {
            val modeText = when (form.categoryBasedExecutionMode) {
                "every_transaction" -> "On every transaction"
                "after_daily_total" -> "After daily total"
                "after_monthly_total" -> "After monthly total"
                else -> "On every transaction"
            }
            "$modeText in ${form.categoryBasedCategoryIds.size} categories"
        }
        AllocationTriggerType.INCOME_RECEIVED -> "When income is received"
        AllocationTriggerType.ROUND_UP -> "After every expense (round-up)"
    }

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
                    text = "Configuration Summary",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }

            val goalName = form.goalName.ifBlank { "Goal" }

            // Live summary
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
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
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
                    "${NumberFormatter.getCurrencySymbol()} ${CurrencyUtils.formatInputThousands(raw.toString())}"
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
