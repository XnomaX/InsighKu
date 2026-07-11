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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.components.dialogs.PremiumDeleteConfirmDialog
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.feature.planning.goal.data.model.AllocationTriggerType
import com.example.insightku.feature.planning.goal.data.model.RoundUpMode
import com.example.insightku.feature.planning.goal.data.model.AllocationValueType
import com.example.insightku.feature.planning.goal.data.model.CategoryBasedExecutionMode
import com.example.insightku.feature.planning.goal.data.model.ConfirmationMode
import com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency
import com.example.insightku.feature.planning.goal.domain.model.AllocationTriggerParams
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
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
    val roundUpMode: RoundUpMode = RoundUpMode.ROUND_UP,
    val scheduledFrequency: ScheduledFrequency = ScheduledFrequency.DAILY,
    val scheduledDayOfWeek: Int = 1,
    val scheduledDayOfMonth: Int = 1,
    val categoryId: String? = null,
    val accountId: String? = null,
    val threshold: String? = null,
    val executionHour: Int = 8,
    val executionMinute: Int = 0,
    val biweeklyStartDate: Long = 0,
    val minRemainingBalance: String = "",
    val categoryBasedCategoryIds: List<String> = emptyList(),
    val categoryBasedExecutionMode: String = "every_transaction"
) {
    val isValid: Boolean
        get() {
            if (goalId.isBlank()) return false
            // ROUND_UP uses rounding logic, not a user-specified allocation value
            if (triggerType != AllocationTriggerType.ROUND_UP) {
                if (allocationValue.toDoubleOrNull() == null || allocationValue.toDoubleOrNull()!! <= 0) return false
            }
            if (sourceAccountId.isNullOrBlank()) return false
            return when (triggerType) {
                AllocationTriggerType.DAILY,
                AllocationTriggerType.WEEKLY,
                AllocationTriggerType.MONTHLY -> {
                    executionHour in 0..23 && executionMinute in 0..59
                }
                AllocationTriggerType.BIWEEKLY -> {
                    biweeklyStartDate > 0 && executionHour in 0..23 && executionMinute in 0..59
                }
                AllocationTriggerType.BALANCE_ABOVE -> {
                    val t = threshold?.toDoubleOrNull() ?: 0.0
                    val minRem = minRemainingBalance.toDoubleOrNull() ?: 0.0
                    t > 0 && (minRem <= 0 || minRem < t)
                }
                AllocationTriggerType.SPENDING_CATEGORY -> {
                    categoryBasedCategoryIds.isNotEmpty()
                }
                AllocationTriggerType.INCOME_RECEIVED -> true
                AllocationTriggerType.ROUND_UP -> true
            }
        }

    fun toRule(): AutoAllocationRule? {
        // ROUND_UP uses rounding logic, not a user-specified allocation value
        val value = if (triggerType == AllocationTriggerType.ROUND_UP) {
            1.0 // Default value - not used by round-up engine
        } else {
            allocationValue.toDoubleOrNull() ?: return null
        }
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
            roundUpMode = roundUpMode,
            scheduledFrequency = scheduledFrequency,
            scheduledDayOfWeek = scheduledDayOfWeek,
            scheduledDayOfMonth = scheduledDayOfMonth,
            executionHour = executionHour,
            executionMinute = executionMinute,
            biweeklyStartDate = biweeklyStartDate,
            minRemainingBalance = minRemainingVal,
            categoryBasedCategoryIds = categoryBasedCategoryIds,
            categoryBasedExecutionMode = CategoryBasedExecutionMode.fromString(categoryBasedExecutionMode)
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
        try { Color((goal?.color ?: "").toColorInt()) }
        catch (_: Exception) { fallbackColor }
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }

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
                roundUpMode = rule.roundUpMode,
                scheduledFrequency = rule.scheduledFrequency,
                scheduledDayOfWeek = rule.scheduledDayOfWeek,
                scheduledDayOfMonth = rule.scheduledDayOfMonth,
                categoryId = rule.triggerParams?.categoryId,
                accountId = rule.triggerParams?.accountId,
                threshold = rule.triggerParams?.threshold?.let { if (it > 0) it.toLong().toString() else "" } ?: "",
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

    LaunchedEffect(activeAccounts.size) {
        if (form.sourceAccountId == null && activeAccounts.size == 1) {
            form = form.copy(sourceAccountId = activeAccounts.first().id)
        }
    }

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

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = { handleDismiss() },
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(
            topStart = Dimens.BottomSheetRadius,
            topEnd = Dimens.BottomSheetRadius
        ),
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
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
                .padding(horizontal = 28.dp)
                .padding(bottom = 20.dp)
        ) {
            Surface(shape = RoundedCornerShape(50), color = goalColor.copy(alpha = 0.10f)) {
                Text(
                    text = stringResource(R.string.auto_alloc_chip),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = goalColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (isEditing) R.string.auto_alloc_edit_rule else R.string.auto_alloc_new_rule),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.auto_alloc_configure),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
                if (isEditing && onDelete != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = AppPalette.cardBorder)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(AppPalette.background)
                .padding(horizontal = 24.dp)
                .padding(vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactTriggerSelector(
                selected = form.triggerType,
                onSelect = { newTrigger ->
                    val newFreq = when (newTrigger) {
                        AllocationTriggerType.DAILY -> ScheduledFrequency.DAILY
                        AllocationTriggerType.WEEKLY -> ScheduledFrequency.WEEKLY
                        AllocationTriggerType.BIWEEKLY -> ScheduledFrequency.BIWEEKLY
                        AllocationTriggerType.MONTHLY -> ScheduledFrequency.MONTHLY
                        else -> form.scheduledFrequency
                    }
                    // Auto-set roundUpEnabled when ROUND_UP trigger is selected
                    val newRoundUp = newTrigger == AllocationTriggerType.ROUND_UP
                    form = form.copy(
                        triggerType = newTrigger,
                        scheduledFrequency = newFreq,
                        roundUpEnabled = newRoundUp
                    )
                },
                accentColor = goalColor
            )

            // ── 2. Source Account: horizontal scrollable chips ──────────
            AccountSelector(
                accounts = activeAccounts,
                selectedAccountId = form.sourceAccountId,
                isEditing = isEditing,
                previousAccountStillExists = previousAccountStillExists,
                onSelect = { form = form.copy(sourceAccountId = it.id) },
                onNavigateToAccounts = onNavigateToAccounts,
                accentColor = goalColor
            )

            // ── 3. Allocation Amount: inline section ────────────────────
            // Hide for ROUND_UP trigger - amount is determined by rounding logic
            AnimatedVisibility(
                visible = form.triggerType != AllocationTriggerType.ROUND_UP,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                AllocationValueSelector(
                    allocationType = form.allocationType,
                    allocationValue = form.allocationValue,
                    onTypeChange = { form = form.copy(allocationType = it) },
                    onValueChange = { form = form.copy(allocationValue = it) },
                    accentColor = goalColor
                )
            }

            // ── 4. Round-Up Configuration ──────────────────────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.ROUND_UP,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Rounding Mode Selector
                    Text(
                        text = stringResource(R.string.auto_alloc_rounding_mode),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textMuted
                    )
                    val roundingModes = listOf(
                        RoundUpMode.ROUND_UP to stringResource(R.string.auto_alloc_round_up),
                        RoundUpMode.ROUND_DOWN to stringResource(R.string.auto_alloc_round_down),
                        RoundUpMode.ROUND_NEAREST to stringResource(R.string.auto_alloc_round_nearest)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        roundingModes.forEach { (mode, label) ->
                            val isSel = form.roundUpMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { form = form.copy(roundUpMode = mode) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) goalColor.copy(alpha = 0.08f) else AppPalette.card,
                                border = BorderStroke(1.dp, if (isSel) goalColor else AppPalette.cardBorder)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSel) goalColor else AppPalette.textMuted,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    // Increment input
                    InlineField(
                        label = stringResource(R.string.auto_alloc_increment),
                        value = CurrencyUtils.formatInputThousands(form.roundUpIncrement.toLong().toString()),
                        onValueChange = { newValue ->
                            val cleaned = CurrencyUtils.stripThousands(newValue)
                            val parsed = cleaned.toDoubleOrNull() ?: return@InlineField
                            if (parsed > 0) form = form.copy(roundUpIncrement = parsed)
                        },
                        placeholder = "e.g. 5.000",
                        prefix = NumberFormatter.getCurrencySymbol(),
                        accentColor = goalColor,
                        hint = stringResource(R.string.auto_alloc_increment_desc)
                    )
                }
            }

            // ── 5. Dynamic Trigger-Specific Config ──────────────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.INCOME_RECEIVED,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                InlineField(
                    label = stringResource(R.string.auto_alloc_min_income),
                    value = if (form.minIncomeAmount.isEmpty()) ""
                    else CurrencyUtils.formatInputThousands(form.minIncomeAmount),
                    onValueChange = { form = form.copy(minIncomeAmount = CurrencyUtils.stripThousands(it)) },
                    placeholder = "e.g. 5.000.000",
                    prefix = NumberFormatter.getCurrencySymbol(),
                    accentColor = goalColor
                )
            }

            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.BALANCE_ABOVE,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InlineField(
                        label = stringResource(R.string.auto_alloc_balance_threshold),
                        value = if (form.threshold.isNullOrEmpty()) ""
                        else CurrencyUtils.formatInputThousands(form.threshold!!),
                        onValueChange = { form = form.copy(threshold = CurrencyUtils.stripThousands(it)) },
                        placeholder = "e.g. 10.000.000",
                        prefix = NumberFormatter.getCurrencySymbol(),
                        accentColor = goalColor,
                        hint = stringResource(R.string.auto_alloc_balance_desc)
                    )
                    InlineField(
                        label = stringResource(R.string.auto_alloc_min_remaining),
                        value = if (form.minRemainingBalance.isEmpty()) ""
                        else CurrencyUtils.formatInputThousands(form.minRemainingBalance),
                        onValueChange = { form = form.copy(minRemainingBalance = CurrencyUtils.stripThousands(it)) },
                        placeholder = "e.g. 1.000.000",
                        prefix = NumberFormatter.getCurrencySymbol(),
                        accentColor = goalColor,
                        hint = stringResource(R.string.auto_alloc_min_remaining_desc)
                    )
                }
            }

            // ── Scheduled Triggers: compact time picker ─────────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.DAILY,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                CompactTimePicker(
                    hour = form.executionHour,
                    minute = form.executionMinute,
                    onHourChange = { h -> form = form.copy(executionHour = h) },
                    onMinuteChange = { m -> form = form.copy(executionMinute = m) },
                    accentColor = goalColor
                )
            }

            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.WEEKLY,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeekDayPicker(
                        selectedDay = form.scheduledDayOfWeek,
                        onDaySelected = { d -> form = form.copy(scheduledDayOfWeek = d) },
                        accentColor = goalColor
                    )
                    CompactTimePicker(
                        hour = form.executionHour,
                        minute = form.executionMinute,
                        onHourChange = { h -> form = form.copy(executionHour = h) },
                        onMinuteChange = { m -> form = form.copy(executionMinute = m) },
                        accentColor = goalColor
                    )
                }
            }

            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.BIWEEKLY,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactBiweeklyDatePicker(
                        selectedDate = form.biweeklyStartDate,
                        onDateSelected = { ts -> form = form.copy(biweeklyStartDate = ts) },
                        accentColor = goalColor
                    )
                    CompactTimePicker(
                        hour = form.executionHour,
                        minute = form.executionMinute,
                        onHourChange = { h -> form = form.copy(executionHour = h) },
                        onMinuteChange = { m -> form = form.copy(executionMinute = m) },
                        accentColor = goalColor
                    )
                }
            }

            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.MONTHLY,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonthDayPickerCompact(
                        selectedDay = form.scheduledDayOfMonth,
                        onDaySelected = { d -> form = form.copy(scheduledDayOfMonth = d) },
                        accentColor = goalColor
                    )
                    CompactTimePicker(
                        hour = form.executionHour,
                        minute = form.executionMinute,
                        onHourChange = { h -> form = form.copy(executionHour = h) },
                        onMinuteChange = { m -> form = form.copy(executionMinute = m) },
                        accentColor = goalColor
                    )
                }
            }

            // ── Spending Category ───────────────────────────────────────
            AnimatedVisibility(
                visible = form.triggerType == AllocationTriggerType.SPENDING_CATEGORY,
                enter = expandVertically(), exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val categoriesToShow = remember(expenseCategories) {
                        expenseCategories.ifEmpty {
                            listOf(
                                CategoryInfo("fallback-food", "Food"), CategoryInfo("fallback-transport", "Transport"),
                                CategoryInfo("fallback-shopping", "Shopping"), CategoryInfo("fallback-bills", "Bills"),
                                CategoryInfo("fallback-entertainment", "Entertainment"), CategoryInfo("fallback-health", "Health"),
                                CategoryInfo("fallback-education", "Education"), CategoryInfo("fallback-other", "Other")
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.auto_alloc_categories),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textMuted
                    )
                    // Compact chip grid
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categoriesToShow.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { cat ->
                                    val isSelected = form.categoryBasedCategoryIds.contains(cat.id)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val updated = if (isSelected) form.categoryBasedCategoryIds - cat.id
                                                else form.categoryBasedCategoryIds + cat.id
                                                form = form.copy(categoryBasedCategoryIds = updated)
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) goalColor.copy(alpha = 0.08f) else AppPalette.card,
                                        border = BorderStroke(1.dp, if (isSelected) goalColor else AppPalette.cardBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(if (isSelected) goalColor else Color.Transparent)
                                                    .then(if (!isSelected) Modifier.border(1.dp, AppPalette.cardBorder, RoundedCornerShape(5.dp)) else Modifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = AppPalette.textPrimary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    // Execution mode - compact inline
                    Text(
                        text = stringResource(R.string.auto_alloc_execution_mode),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textMuted
                    )
                    val modes = listOf(
                        "every_transaction" to stringResource(R.string.auto_alloc_every_transaction),
                        "after_daily_total" to stringResource(R.string.auto_alloc_after_daily_total),
                        "after_monthly_total" to stringResource(R.string.auto_alloc_after_monthly_total)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { (value, label) ->
                            val isSel = form.categoryBasedExecutionMode == value
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { form = form.copy(categoryBasedExecutionMode = value) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) goalColor.copy(alpha = 0.08f) else AppPalette.card,
                                border = BorderStroke(1.dp, if (isSel) goalColor else AppPalette.cardBorder)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSel) goalColor else AppPalette.textMuted,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // ── 5. Confirmation Mode: inline toggle ─────────────────────
            InlineConfirmationToggle(
                selected = form.confirmationMode,
                onSelect = { form = form.copy(confirmationMode = it) },
                accentColor = goalColor
            )

            Spacer(Modifier.height(4.dp))

            // ── 6. Action Buttons ──────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { handleDismiss() },
                    shape = RoundedCornerShape(14.dp),
                    color = AppPalette.card,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.cancel),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppPalette.textDialogMuted
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (form.isValid) goalColor else AppPalette.textMuted)
                        .clickable(enabled = form.isValid) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            val newRule = form.toRule()
                            if (newRule != null) {
                                val ruleWithId =
                                    if (isEditing) newRule.copy(id = rule.id) else newRule
                                onSave(ruleWithId)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(if (isEditing) R.string.auto_alloc_update_rule else R.string.auto_alloc_create_rule),
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
        PremiumDeleteConfirmDialog(
            itemName = stringResource(R.string.auto_alloc_rule),
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                onDelete(rule.id)
                showDeleteConfirm = false
            },
            message = stringResource(R.string.auto_alloc_delete_rule_desc)
        )
    }
}

// ─── Trigger Selector (expandable selection field) ───────────────────────────
// DESIGN RATIONALE: Compact expandable field that shows only the selected trigger
// in collapsed state, with a vertical list of all options when expanded.
// Feels like an expandable preference selector, not a dropdown or chip grid.

private data class TriggerOption(
    val type: AllocationTriggerType,
    val label: String,
    val description: String,
    val icon: ImageVector
)

@Composable
private fun getTriggerOptions(): List<TriggerOption> = listOf(
    TriggerOption(AllocationTriggerType.INCOME_RECEIVED, stringResource(R.string.auto_alloc_trigger_income), stringResource(R.string.auto_alloc_trigger_income_desc),
        Icons.AutoMirrored.Outlined.TrendingUp
    ),
    TriggerOption(AllocationTriggerType.ROUND_UP, stringResource(R.string.auto_alloc_trigger_roundup), stringResource(R.string.auto_alloc_trigger_roundup_desc), Icons.Outlined.ChangeHistory),
    TriggerOption(AllocationTriggerType.DAILY, stringResource(R.string.auto_alloc_trigger_daily), stringResource(R.string.auto_alloc_trigger_daily_desc), Icons.Outlined.Schedule),
    TriggerOption(AllocationTriggerType.WEEKLY, stringResource(R.string.auto_alloc_trigger_weekly), stringResource(R.string.auto_alloc_trigger_weekly_desc), Icons.Outlined.Schedule),
    TriggerOption(AllocationTriggerType.BIWEEKLY, stringResource(R.string.auto_alloc_trigger_biweekly), stringResource(R.string.auto_alloc_trigger_biweekly_desc), Icons.Outlined.Schedule),
    TriggerOption(AllocationTriggerType.MONTHLY, stringResource(R.string.auto_alloc_trigger_monthly), stringResource(R.string.auto_alloc_trigger_monthly_desc), Icons.Outlined.Schedule),
    TriggerOption(AllocationTriggerType.BALANCE_ABOVE, stringResource(R.string.auto_alloc_trigger_balance), stringResource(R.string.auto_alloc_trigger_balance_desc), Icons.Outlined.AccountBalance),
    TriggerOption(AllocationTriggerType.SPENDING_CATEGORY, stringResource(R.string.auto_alloc_trigger_category), stringResource(R.string.auto_alloc_trigger_category_desc), Icons.Outlined.Category)
)

@Composable
private fun CompactTriggerSelector(
    selected: AllocationTriggerType,
    onSelect: (AllocationTriggerType) -> Unit,
    accentColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    val options = getTriggerOptions()
    val selectedOption = options.first { it.type == selected }
    val cornerRadius = 12.dp

    Column {
        Text(
            text = stringResource(R.string.auto_alloc_trigger),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        Spacer(Modifier.height(8.dp))
        
        // Collapsed state: shows selected trigger
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            color = AppPalette.card,
            border = BorderStroke(1.dp, if (isExpanded) accentColor else AppPalette.cardBorder),
            onClick = { isExpanded = !isExpanded }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = selectedOption.icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedOption.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        text = selectedOption.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Expanded state: shows all options
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(tween(200)),
            exit = shrinkVertically(tween(200))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(cornerRadius),
                color = AppPalette.card,
                border = BorderStroke(1.dp, accentColor)
            ) {
                Column {
                    options.forEachIndexed { index, option ->
                        val isSelected = option.type == selected
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent,
                            onClick = {
                                onSelect(option.type)
                                isExpanded = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) accentColor else AppPalette.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) accentColor else AppPalette.textPrimary
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppPalette.textMuted,
                                        maxLines = 1
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        // Divider between options (except last)
                        if (index < options.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = AppPalette.cardBorder
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── NEW: Inline Field (replaces FormSectionLabel + OutlinedTextField pattern) ─
// DESIGN RATIONALE: The original pattern used a separate label + large text field.
// InlineField combines them into a compact single-unit reducing visual noise.

@Composable
private fun InlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    prefix: String,
    accentColor: Color,
    hint: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder, color = AppPalette.placeholder) },
            prefix = { Text(prefix, fontWeight = FontWeight.Bold, color = accentColor) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = AppPalette.cardBorder,
                focusedContainerColor = AppPalette.card,
                unfocusedContainerColor = AppPalette.card
            )
        )
        if (hint != null) {
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.auto_alloc_source_account),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        when {
            accounts.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.AccountBalance, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.auto_alloc_create_account_first), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
                            Text(stringResource(R.string.auto_alloc_need_account), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                        }
                        if (onNavigateToAccounts != null) {
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onNavigateToAccounts() },
                                shape = RoundedCornerShape(8.dp),
                                color = accentColor.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.Add, null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Text(stringResource(R.string.auto_alloc_create_account), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = accentColor)
                                }
                            }
                        }
                    }
                }
            }
            isEditing && selectedAccountId == null && !previousAccountStillExists -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.04f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.auto_alloc_account_unavailable), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                AccountPickerChips(accounts, selectedAccountId, onSelect, accentColor)
            }
            else -> AccountPickerChips(accounts, selectedAccountId, onSelect, accentColor)
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        accounts.forEach { account ->
            val isSelected = account.id == selectedAccountId
            val borderColor by animateColorAsState(targetValue = if (isSelected) accentColor else AppPalette.cardBorder, animationSpec = tween(200), label = "ab")
            val bgColor by animateColorAsState(targetValue = if (isSelected) accentColor.copy(alpha = 0.08f) else AppPalette.card, animationSpec = tween(200), label = "bg")

            Surface(
                modifier = Modifier.width(140.dp).clip(RoundedCornerShape(12.dp)).clickable { onSelect(account) },
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                            .background(try { Color(account.color.toColorInt()).copy(alpha = 0.12f) } catch (_: Exception) { accentColor.copy(alpha = 0.12f) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(accountTypeIcon(account.type), null, tint = try {
                            Color(account.color.toColorInt()) } catch (_: Exception) { accentColor }, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(CurrencyUtils.formatAmountCompact(account.balance), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, maxLines = 1)
                    }
                    if (isSelected) {
                        Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(accentColor), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.auto_alloc_amount),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = allocationType == AllocationValueType.PERCENT,
                onClick = { onTypeChange(AllocationValueType.PERCENT) },
                label = { Text(stringResource(R.string.auto_alloc_percentage)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.12f),
                    selectedLabelColor = accentColor
                )
            )
            FilterChip(
                selected = allocationType == AllocationValueType.FIXED,
                onClick = { onTypeChange(AllocationValueType.FIXED) },
                label = { Text(stringResource(R.string.auto_alloc_fixed)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.12f),
                    selectedLabelColor = accentColor
                )
            )
        }
        OutlinedTextField(
            value = if (allocationType == AllocationValueType.FIXED) {
                if (allocationValue.isEmpty()) "" else CurrencyUtils.formatInputThousands(allocationValue)
            } else allocationValue,
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
                    if (allocationType == AllocationValueType.PERCENT) "e.g. 10" else "e.g. 500.000",
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
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = AppPalette.cardBorder,
                focusedContainerColor = AppPalette.card,
                unfocusedContainerColor = AppPalette.card
            )
        )
    }
}

// ─── NEW: Inline Confirmation Toggle ─────────────────────────────────────────
// DESIGN RATIONALE: The original ConfirmationModeSelector used two large cards
// with icons, titles, and descriptions consuming ~200dp. An inline switch+label
// reduces this to ~56dp while preserving clarity.

@Composable
private fun InlineConfirmationToggle(
    selected: ConfirmationMode,
    onSelect: (ConfirmationMode) -> Unit,
    accentColor: Color
) {
    val isAuto = selected == ConfirmationMode.AUTO
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppPalette.card,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isAuto) Icons.Outlined.AutoAwesome else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (isAuto) accentColor else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.auto_alloc_confirmation),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary
                )
                Text(
                    text = if (isAuto) stringResource(R.string.auto_alloc_automatic_desc)
                    else stringResource(R.string.auto_alloc_confirm_first_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            Switch(
                checked = !isAuto,
                onCheckedChange = {
                    onSelect(if (it) ConfirmationMode.CONFIRMATION_REQUIRED else ConfirmationMode.AUTO)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = AppPalette.textMuted.copy(alpha = 0.4f)
                )
            )
        }
    }
}
@Composable
private fun CompactTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.auto_alloc_execution_time),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppPalette.card,
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Hour
                TimeAdjuster(value = hour, onDecrement = { onHourChange((hour - 1 + 24) % 24) }, onIncrement = { onHourChange((hour + 1) % 24) }, accentColor = accentColor)
                Text(" : ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                // Minute
                TimeAdjuster(value = minute, onDecrement = { onMinuteChange((minute - 15 + 60) % 60) }, onIncrement = { onMinuteChange((minute + 15) % 60) }, accentColor = accentColor)
            }
        }
    }
}

@Composable
private fun TimeAdjuster(value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onDecrement() },
            shape = RoundedCornerShape(8.dp),
            color = AppPalette.cardElevated
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("-", fontWeight = FontWeight.Bold, color = accentColor)
            }
        }
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Surface(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onIncrement() },
            shape = RoundedCornerShape(8.dp),
            color = AppPalette.cardElevated
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("+", fontWeight = FontWeight.Bold, color = accentColor)
            }
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
    val days = listOf(
        1 to stringResource(R.string.auto_alloc_day_mon),
        2 to stringResource(R.string.auto_alloc_day_tue),
        3 to stringResource(R.string.auto_alloc_day_wed),
        4 to stringResource(R.string.auto_alloc_day_thu),
        5 to stringResource(R.string.auto_alloc_day_fri),
        6 to stringResource(R.string.auto_alloc_day_sat),
        7 to stringResource(R.string.auto_alloc_day_sun)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.auto_alloc_execution_day),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEach { (dayNum, dayLabel) ->
                val isSelected = selectedDay == dayNum
                Surface(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onDaySelected(dayNum) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) accentColor else AppPalette.card,
                    border = BorderStroke(1.dp, if (isSelected) accentColor else AppPalette.cardBorder)
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(dayLabel, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else AppPalette.textMuted)
                    }
                }
            }
        }
    }
}

// ─── NEW: Compact Month Day Picker ───────────────────────────────────────────
// DESIGN RATIONALE: The original MonthDayPicker showed a 28-day grid + extra row
// of 29, 30, 31, Last = huge vertical footprint. Compact version uses horizontal
// scroll with common presets + "Last Day" option.

@Composable
private fun MonthDayPickerCompact(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.auto_alloc_day_of_month),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        // Common day presets in a grid
        val commonDays = listOf(1, 5, 10, 15, 20, 25, -1)
        val lastDayLabel = stringResource(R.string.auto_alloc_day_last)
        val dayLabels = mapOf(1 to "1", 5 to "5", 10 to "10", 15 to "15", 20 to "20", 25 to "25", -1 to lastDayLabel)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            commonDays.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { day ->
                        val isSelected = selectedDay == day
                        Surface(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onDaySelected(day) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) accentColor else AppPalette.card,
                            border = BorderStroke(1.dp, if (isSelected) accentColor else AppPalette.cardBorder)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(dayLabels[day] ?: "$day", style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.White else AppPalette.textMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── NEW: Compact Biweekly Date Picker ───────────────────────────────────────

@Composable
private fun CompactBiweeklyDatePicker(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.auto_alloc_start_date),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        val today = LocalDate.now()
        val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nextMonday = if (today.dayOfWeek.value == 1) today.plusDays(7)
        else today.plusDays(((8 - today.dayOfWeek.value) % 7).toLong().coerceAtLeast(1))
        val nextMondayMillis = nextMonday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                stringResource(R.string.auto_alloc_today) to todayMillis,
                stringResource(R.string.auto_alloc_tomorrow) to (todayMillis + 86400000L),
                stringResource(R.string.auto_alloc_next_monday) to nextMondayMillis
            ).forEach { (label, millis) ->
                val isSel = selectedDate == millis
                FilterChip(
                    selected = isSel,
                    onClick = { onDateSelected(millis) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.12f),
                        selectedLabelColor = accentColor
                    )
                )
            }
        }
        Text(
            text = stringResource(R.string.auto_alloc_repeats_biweekly),
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted
        )
    }
}
