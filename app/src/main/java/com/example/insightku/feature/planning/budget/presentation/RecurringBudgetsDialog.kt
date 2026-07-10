package com.example.insightku.feature.planning.budget.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.BudgetFrequency
import com.example.insightku.core.data.model.RecurringBudget
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.components.PremiumDatePicker
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBudgetsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    recurringBudgets: List<RecurringBudget>,
    onBudgetAdded: (RecurringBudget) -> Unit,
    onBudgetEdited: (RecurringBudget) -> Unit,
    onBudgetDeleted: (RecurringBudget) -> Unit,
    accounts: List<Account> = emptyList(),
    showAddFormInitially: Boolean = false,
    editingBudgetInitially: RecurringBudget? = null
) {
    var showAddForm by remember { mutableStateOf(showAddFormInitially) }
    var editingBudget by remember { mutableStateOf(editingBudgetInitially) }
    var budgetToDelete by remember { mutableStateOf<RecurringBudget?>(null) }

    val categories = mapOf(
        stringResource(R.string.cat_subscriptions) to "cat_1",
        stringResource(R.string.cat_utilities) to "cat_2",
        stringResource(R.string.cat_insurance) to "cat_3",
        stringResource(R.string.cat_rent_mortgage) to "cat_4",
        stringResource(R.string.cat_loan_payments) to "cat_5",
        stringResource(R.string.cat_memberships) to "cat_6",
        stringResource(R.string.cat_donations) to "cat_7",
        stringResource(R.string.cat_others) to "cat_8"
    )

    fun handleEdit(budget: RecurringBudget) { editingBudget = budget; showAddForm = true }
    fun handleBackToList() { showAddForm = false; editingBudget = null }
    fun handleDeleteRequest(budget: RecurringBudget) { budgetToDelete = budget }
    fun handleDeleteConfirm() { budgetToDelete?.let { onBudgetDeleted(it); budgetToDelete = null } }

    // Localized reminder options
    val reminderOptions = listOf(
        1 to stringResource(R.string.recurring_reminder_1day),
        3 to stringResource(R.string.recurring_reminder_3days),
        7 to stringResource(R.string.recurring_reminder_7days),
        14 to stringResource(R.string.recurring_reminder_14days)
    )

    if (isOpen) {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.fillMaxSize().imePadding()) {
                    DialogHeader(showAddForm = showAddForm, isEditing = editingBudget != null, onBack = ::handleBackToList, onClose = onDismiss)
                    if (showAddForm) {
                        AddEditBudgetForm(editingBudget = editingBudget, onSave = { budget -> if (editingBudget != null) onBudgetEdited(budget) else onBudgetAdded(budget); handleBackToList() }, onCancel = ::handleBackToList, categories = categories, accounts = accounts)
                    } else {
                        BudgetList(budgets = recurringBudgets, onEdit = ::handleEdit, onDelete = ::handleDeleteRequest, onAdd = { showAddForm = true }, categories = categories)
                    }
                }
            }
        }
    }

    if (budgetToDelete != null) { DeleteConfirmationDialog(budgetName = budgetToDelete?.name ?: "", onConfirm = ::handleDeleteConfirm, onDismiss = { budgetToDelete = null }) }
}

@Composable
fun DialogHeader(showAddForm: Boolean, isEditing: Boolean, onBack: () -> Unit, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showAddForm) { IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }; Spacer(Modifier.width(8.dp)) }
            Icon(Icons.Default.Repeat, contentDescription = null, tint = AppPalette.accent)
            Spacer(Modifier.width(8.dp))
            Text(text = when { showAddForm && isEditing -> stringResource(R.string.recurring_budgets_edit_title); showAddForm -> stringResource(R.string.recurring_budgets_add_title); else -> stringResource(R.string.recurring_budgets_title) }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close)) }
    }
    HorizontalDivider()
}

@Composable
fun ColumnScope.BudgetList(budgets: List<RecurringBudget>, onEdit: (RecurringBudget) -> Unit, onDelete: (RecurringBudget) -> Unit, onAdd: () -> Unit, categories: Map<String, String>) {
    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.recurring_manage_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center)
        if (budgets.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.recurring_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(budgets) { budget -> RecurringBudgetItem(budget = budget, categoryName = categories.entries.find { it.value == budget.categoryId }?.key ?: "N/A", onEdit = { onEdit(budget) }, onDelete = { onDelete(budget) }) }
            }
        }
    }
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = AppPalette.accent)) {
        Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.recurring_add))
    }
}

@Composable
fun RecurringBudgetItem(budget: RecurringBudget, categoryName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(budget.name, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) { Text(text = when (budget.frequency) { BudgetFrequency.WEEKLY -> stringResource(R.string.period_weekly); BudgetFrequency.BIWEEKLY -> stringResource(R.string.recurring_biweekly); BudgetFrequency.MONTHLY -> stringResource(R.string.period_monthly); BudgetFrequency.QUARTERLY -> stringResource(R.string.recurring_quarterly); BudgetFrequency.YEARLY -> stringResource(R.string.recurring_yearly) }, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                    }
                    Text(categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = NumberFormatter.formatCurrency(budget.amount), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        if (budget.isActive) { Icon(Icons.Default.Notifications, contentDescription = null, tint = AppPalette.accent, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                        Text(formatNextDue(budget.nextDue), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.recurring_budgets_next, DateFormatter.formatShortDate(budget.nextDue)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = AppPalette.accent, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.AddEditBudgetForm(editingBudget: RecurringBudget?, onSave: (RecurringBudget) -> Unit, onCancel: () -> Unit, categories: Map<String, String>, accounts: List<Account> = emptyList()) {
    var name by remember { mutableStateOf(editingBudget?.name ?: "") }
    var amount by remember { mutableStateOf(editingBudget?.amount?.toString() ?: "") }
    var frequency by remember { mutableStateOf(editingBudget?.frequency ?: BudgetFrequency.MONTHLY) }
    var categoryId by remember { mutableStateOf(editingBudget?.categoryId) }
    var accountId by remember { mutableStateOf(editingBudget?.accountId) }
    var nextDueDate by remember { mutableLongStateOf(editingBudget?.nextDue ?: System.currentTimeMillis()) }
    var notifications by remember { mutableStateOf(editingBudget?.isActive ?: true) }
    var reminderDaysBefore by remember { mutableIntStateOf(editingBudget?.reminderDaysBefore ?: 3) }
    var showDatePicker by remember { mutableStateOf(false) }
    val reminderOptions = listOf(1 to stringResource(R.string.recurring_reminder_1day), 3 to stringResource(R.string.recurring_reminder_3days), 7 to stringResource(R.string.recurring_reminder_7days), 14 to stringResource(R.string.recurring_reminder_14days))

    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.recurring_budgets_name_label)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.recurring_budgets_amount_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        DropdownField(label = stringResource(R.string.recurring_budgets_category), options = categories.map { it.key to it.value }.toMap(), onValueSelected = { categoryId = it }, displayValue = { categories.entries.find { it.value == categoryId }?.key ?: stringResource(R.string.recurring_select_category) })
        Spacer(Modifier.height(16.dp))
        DropdownField(
            label = stringResource(R.string.recurring_budgets_frequency),
            options = BudgetFrequency.entries.associateBy({ it }, { freq ->
                when (freq) {
                    BudgetFrequency.WEEKLY -> stringResource(R.string.period_weekly)
                    BudgetFrequency.BIWEEKLY -> stringResource(R.string.recurring_biweekly)
                    BudgetFrequency.MONTHLY -> stringResource(R.string.period_monthly)
                    BudgetFrequency.QUARTERLY -> stringResource(R.string.recurring_quarterly)
                    BudgetFrequency.YEARLY -> stringResource(R.string.recurring_yearly)
                }
            }),
            onValueSelected = { frequency = it },
            displayValue = {
                when (frequency) {
                    BudgetFrequency.WEEKLY -> stringResource(R.string.period_weekly)
                    BudgetFrequency.BIWEEKLY -> stringResource(R.string.recurring_biweekly)
                    BudgetFrequency.MONTHLY -> stringResource(R.string.period_monthly)
                    BudgetFrequency.QUARTERLY -> stringResource(R.string.recurring_quarterly)
                    BudgetFrequency.YEARLY -> stringResource(R.string.recurring_yearly)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.account).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(8.dp))
        RecurringBudgetAccountSelector(accounts = accounts, selectedAccountId = accountId, onSelect = { accountId = it })
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Text(stringResource(R.string.recurring_budgets_reminder), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = DateFormatter.formatFullDate(nextDueDate), onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.recurring_budgets_reminder_start)) }, trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.recurring_select_date)) } }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        DropdownField(label = stringResource(R.string.recurring_budgets_reminder_time), options = reminderOptions.toMap(), onValueSelected = { reminderDaysBefore = it }, displayValue = { reminderOptions.find { it.first == reminderDaysBefore }?.second ?: stringResource(R.string.recurring_select_reminder) })
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(stringResource(R.string.recurring_budgets_notifications), style = MaterialTheme.typography.bodyLarge); Text(stringResource(R.string.recurring_budgets_notifications_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = notifications, onCheckedChange = { notifications = it })
        }
    }
    if (showDatePicker) { PremiumDatePicker(initialMillis = nextDueDate, onDateSelected = { millis -> nextDueDate = millis; showDatePicker = false }, onDismiss = { showDatePicker = false }) }
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
        Button(onClick = { val budget = RecurringBudget(id = editingBudget?.id ?: 0, name = name, amount = amount.toDoubleOrNull() ?: 0.0, frequency = frequency, categoryId = categoryId, accountId = accountId ?: editingBudget?.accountId, nextDue = nextDueDate, isActive = notifications, reminderDaysBefore = reminderDaysBefore); onSave(budget) }, enabled = name.isNotBlank() && amount.isNotBlank() && categoryId != null, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppPalette.accent)) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(if (editingBudget != null) stringResource(R.string.save) else stringResource(R.string.recurring_add)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(label: String, options: Map<T, String>, onValueSelected: (T) -> Unit, displayValue: @Composable () -> String) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = displayValue(), onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { (value, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onValueSelected(value); expanded = false }) } }
    }
}

@Composable
private fun RecurringBudgetAccountSelector(accounts: List<Account>, selectedAccountId: String?, onSelect: (String?) -> Unit) {
    if (accounts.isEmpty()) { Text(stringResource(R.string.recurring_budgets_no_accounts), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted); return }
    val rows = accounts.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { account ->
                    val isSelected = selectedAccountId == account.id
                    val accountColor = runCatching { Color(android.graphics.Color.parseColor(account.color)) }.getOrDefault(AppPalette.accent)
                    val accountIcon = when (account.type) { AccountType.CASH -> Icons.Default.Payments; AccountType.BANK_ACCOUNT -> Icons.Default.AccountBalance; AccountType.E_WALLET -> Icons.Default.AccountBalanceWallet; AccountType.CREDIT_CARD -> Icons.Default.CreditCard }
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isSelected) accountColor.copy(alpha = 0.10f) else AppPalette.card).border(1.5.dp, if (isSelected) accountColor else AppPalette.cardBorder, RoundedCornerShape(12.dp)).clickable { onSelect(if (isSelected) null else account.id) }.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(accountColor.copy(alpha = if (isSelected) 0.18f else 0.10f)), contentAlignment = Alignment.Center) { Icon(accountIcon, null, tint = accountColor, modifier = Modifier.size(16.dp)) }
                        Text(account.name, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) accountColor else AppPalette.textMuted, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun formatNextDue(nextDue: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = nextDue - now
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        diffDays < 0 -> stringResource(R.string.recurring_budgets_overdue)
        diffDays == 0L -> stringResource(R.string.recurring_budgets_today)
        diffDays == 1L -> stringResource(R.string.recurring_budgets_tomorrow)
        else -> stringResource(R.string.recurring_budgets_in_days, diffDays.toInt())
    }
}

@Composable
fun DeleteConfirmationDialog(budgetName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    com.example.insightku.core.ui.components.dialogs.PremiumDeleteConfirmDialog(
        itemName = budgetName,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        message = stringResource(R.string.recurring_budgets_delete_msg, budgetName)
    )
}


