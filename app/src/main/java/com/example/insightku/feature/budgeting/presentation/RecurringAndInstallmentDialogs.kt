package com.example.insightku.feature.budgeting.presentation
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.BudgetFrequency
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.components.PremiumDatePicker
import java.text.SimpleDateFormat
import java.util.*

private val SheetPurple: Color  @Composable get() = LocalAccent.current
private val SheetCyan    = Color(0xFF06B6D4)
private val SheetBorder: Color  @Composable get() = AppPalette.cardBorder
private val SheetBg: Color      @Composable get() = AppPalette.background

// ─── Add / Edit Recurring Payment — ModalBottomSheet ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringPaymentDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (RecurringBudget) -> Unit,
    editing: RecurringBudget? = null,
    availableCategories: List<Category> = emptyList(),
    accounts: List<Account> = emptyList()
) {
    if (!isOpen) return

    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name        by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var amountText  by remember(editing) { mutableStateOf(editing?.amount?.toLong()?.toString() ?: "") }
    var frequency   by remember(editing) { mutableStateOf(editing?.frequency ?: BudgetFrequency.MONTHLY) }
    var nextDue     by remember(editing) { mutableLongStateOf(editing?.nextDue ?: System.currentTimeMillis()) }
    var selectedCategoryId by remember(editing) { mutableStateOf(editing?.categoryId) }
    var selectedAccountId by remember(editing) { mutableStateOf(editing?.accountId) }
    var nameError   by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        PremiumDatePicker(
            initialMillis = nextDue,
            onDateSelected = { millis ->
                nextDue = millis
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest   = onDismiss,
        sheetState         = sheetState,
        containerColor     = AppPalette.card,
        dragHandle         = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(SheetBorder))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp)) {
                Surface(shape = RoundedCornerShape(50), color = SheetPurple.copy(alpha = 0.10f)) {
                    Text("Recurring Payment", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = SheetPurple)
                }
                Spacer(Modifier.height(6.dp))
                Text(if (editing != null) "Edit Payment" else "Add Recurring Payment",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                Text("Subscriptions, bills, memberships",
                    style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))

            // Form
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .background(SheetBg).padding(24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SheetFormField(label = "PAYMENT NAME", value = name,
                    onValueChange = { name = it; nameError = null },
                    placeholder = "e.g. Netflix, Spotify, Rent", error = nameError, accentColor = SheetPurple)

                SheetFormField(label = "AMOUNT",
                    value = com.example.insightku.core.utils.CurrencyUtils.formatInputThousands(amountText),
                    onValueChange = { amountText = com.example.insightku.core.utils.CurrencyUtils.stripThousands(it) },
                    placeholder = "e.g. 59.000", keyboardType = KeyboardType.Number,
                    prefix = "Rp", accentColor = SheetPurple)

                // Category selector
                if (availableCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CATEGORY", style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                        SheetCategorySelector(
                            categories = availableCategories,
                            selectedId = selectedCategoryId,
                            onSelect   = { selectedCategoryId = if (selectedCategoryId == it) null else it },
                            accentColor = SheetPurple
                        )
                    }
                }

                // Account selector
                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ACCOUNT", style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                        SheetAccountSelector(
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            onSelect = { selectedAccountId = if (selectedAccountId == it) null else it }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FREQUENCY", style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    SheetFrequencySelector(selected = frequency, onSelect = { frequency = it }, accentColor = SheetPurple)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NEXT DUE DATE", style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        shape    = RoundedCornerShape(14.dp),
                        color    = AppPalette.card,
                        border   = BorderStroke(1.dp, SheetBorder)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Date(nextDue)),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                            Icon(Icons.Default.EditCalendar, null, tint = Color(0xFFB39DDB), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp).clickable { focusManager.clearFocus(); keyboardController?.hide(); onDismiss() },
                        shape    = RoundedCornerShape(14.dp),
                        color    = AppPalette.card,
                        border   = BorderStroke(1.dp, SheetBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFF6B6B8A))
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp))
                            .background(SheetPurple).clickable {
                                if (name.trim().length < 2) { nameError = "Name must be at least 2 characters"; return@clickable }
                                focusManager.clearFocus(); keyboardController?.hide()
                                onSave(RecurringBudget(
                                    id         = editing?.id ?: 0,
                                    name       = name.trim(),
                                    amount     = amountText.filter { it.isDigit() }.toLongOrNull()?.toDouble() ?: 0.0,
                                    frequency  = frequency,
                                    nextDue    = nextDue,
                                    isActive   = true,
                                    categoryId = selectedCategoryId,
                                    accountId  = selectedAccountId ?: editing?.accountId
                                ))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (editing != null) "Save Changes" else "Add Payment",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ─── Add / Edit Installment — ModalBottomSheet ────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstallmentDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (Installment) -> Unit,
    editing: Installment? = null,
    availableCategories: List<Category> = emptyList(),
    accounts: List<Account> = emptyList()
) {
    if (!isOpen) return

    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name            by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var totalAmountText by remember(editing) { mutableStateOf(editing?.totalAmount?.toLong()?.toString() ?: "") }
    var monthlyText     by remember(editing) { mutableStateOf(editing?.monthlyPayment?.toLong()?.toString() ?: "") }
    var totalMonthsText by remember(editing) { mutableStateOf(editing?.totalMonths?.toString() ?: "") }
    var paidMonthsText  by remember(editing) { mutableStateOf(editing?.paidMonths?.toString() ?: "") }
    var nextDue         by remember(editing) { mutableLongStateOf(editing?.nextDueDate ?: System.currentTimeMillis()) }
    var selectedCategoryId by remember(editing) { mutableStateOf(editing?.categoryId) }
    var selectedAccountId by remember(editing) { mutableStateOf(editing?.accountId) }
    var nameError       by remember { mutableStateOf<String?>(null) }
    var showDatePicker  by remember { mutableStateOf(false) }

    val totalMonths = totalMonthsText.filter { it.isDigit() }.toIntOrNull() ?: 0
    val paidMonths  = paidMonthsText.filter { it.isDigit() }.toIntOrNull() ?: 0
    val monthly     = monthlyText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val remaining   = ((totalMonths - paidMonths).coerceAtLeast(0) * monthly).toDouble()

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = nextDue)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { nextDue = it }; showDatePicker = false }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = SheetCyan)
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = AppPalette.card,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(SheetBorder))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp)) {
                Surface(shape = RoundedCornerShape(50), color = SheetCyan.copy(alpha = 0.10f)) {
                    Text("Installment", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = SheetCyan)
                }
                Spacer(Modifier.height(6.dp))
                Text(if (editing != null) "Edit Installment" else "Add Installment",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                Text("Cicilan, PayLater, vehicle credit",
                    style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .background(SheetBg).padding(24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SheetFormField(label = "INSTALLMENT NAME", value = name,
                    onValueChange = { name = it; nameError = null },
                    placeholder = "e.g. MacBook, Phone, Car", error = nameError, accentColor = SheetCyan)

                SheetFormField(label = "TOTAL AMOUNT",
                    value = com.example.insightku.core.utils.CurrencyUtils.formatInputThousands(totalAmountText),
                    onValueChange = { totalAmountText = com.example.insightku.core.utils.CurrencyUtils.stripThousands(it) },
                    placeholder = "e.g. 12.000.000", keyboardType = KeyboardType.Number,
                    prefix = "Rp", accentColor = SheetCyan)

                SheetFormField(label = "MONTHLY PAYMENT",
                    value = com.example.insightku.core.utils.CurrencyUtils.formatInputThousands(monthlyText),
                    onValueChange = { monthlyText = com.example.insightku.core.utils.CurrencyUtils.stripThousands(it) },
                    placeholder = "e.g. 1.000.000", keyboardType = KeyboardType.Number,
                    prefix = "Rp", accentColor = SheetCyan)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        SheetFormField(label = "TOTAL MONTHS", value = totalMonthsText,
                            onValueChange = { totalMonthsText = it.filter { c -> c.isDigit() } },
                            placeholder = "12", keyboardType = KeyboardType.Number, accentColor = SheetCyan)
                    }
                    Column(Modifier.weight(1f)) {
                        SheetFormField(label = "PAID MONTHS", value = paidMonthsText,
                            onValueChange = { paidMonthsText = it.filter { c -> c.isDigit() } },
                            placeholder = "0", keyboardType = KeyboardType.Number, accentColor = SheetCyan)
                    }
                }

                // Category selector
                if (availableCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CATEGORY", style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                        SheetCategorySelector(
                            categories  = availableCategories,
                            selectedId  = selectedCategoryId,
                            onSelect    = { selectedCategoryId = if (selectedCategoryId == it) null else it },
                            accentColor = SheetCyan
                        )
                    }
                }

                // Account selector
                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ACCOUNT", style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                        SheetAccountSelector(
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            onSelect = { selectedAccountId = it }
                        )
                    }
                }

                // Auto-calculated summary
                if (totalMonths > 0 && monthly > 0) {
                    Surface(
                        shape  = RoundedCornerShape(14.dp),
                        color  = SheetCyan.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, SheetCyan.copy(alpha = 0.15f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Remaining", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                Text("Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id","ID")).format(remaining.toLong())}",
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetCyan)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Progress", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                Text("${if (totalMonths > 0) (paidMonths * 100 / totalMonths) else 0}%",
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NEXT DUE DATE", style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        shape    = RoundedCornerShape(14.dp),
                        color    = AppPalette.card,
                        border   = BorderStroke(1.dp, SheetBorder)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Date(nextDue)),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                            Icon(Icons.Default.EditCalendar, null, tint = Color(0xFFB39DDB), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp).clickable { focusManager.clearFocus(); keyboardController?.hide(); onDismiss() },
                        shape    = RoundedCornerShape(14.dp),
                        color    = AppPalette.card,
                        border   = BorderStroke(1.dp, SheetBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFF6B6B8A))
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp))
                            .background(SheetCyan).clickable {
                                if (name.trim().length < 2) { nameError = "Name must be at least 2 characters"; return@clickable }
                                focusManager.clearFocus(); keyboardController?.hide()
                                onSave(Installment(
                                    id          = editing?.id ?: java.util.UUID.randomUUID().toString(),
                                    name        = name.trim(),
                                    totalAmount = totalAmountText.filter { it.isDigit() }.toLongOrNull()?.toDouble() ?: 0.0,
                                    monthlyPayment = monthlyText.filter { it.isDigit() }.toLongOrNull()?.toDouble() ?: 0.0,
                                    totalMonths = totalMonths,
                                    paidMonths  = paidMonths,
                                    nextDueDate = nextDue,
                                    isActive    = true,
                                    categoryId  = selectedCategoryId,
                                    accountId   = selectedAccountId ?: editing?.accountId
                                ))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (editing != null) "Save Changes" else "Add Installment",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ─── Category Selector ────────────────────────────────────────────────────────

@Composable
private fun SheetCategorySelector(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    accentColor: Color
) {
    val rows = categories.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val isSelected = selectedId == cat.id
                    val resolved   = CategoryIconResolver.resolve(cat.icon ?: cat.name)
                    val catColor   = runCatching {
                        Color(android.graphics.Color.parseColor(cat.color.ifBlank { "#7C4DFF" }))
                    }.getOrDefault(resolved.color)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) catColor.copy(alpha = 0.10f) else AppPalette.card)
                            .border(1.5.dp, if (isSelected) catColor else SheetBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelect(cat.id) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(catColor.copy(alpha = if (isSelected) 0.18f else 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(resolved.icon, null, tint = catColor, modifier = Modifier.size(16.dp))
                        }
                        Text(cat.name,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) catColor else AppPalette.textMuted,
                            maxLines   = 2,
                            textAlign  = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ─── Account Selector for Sheet Dialogs ────────────────────────────────────────

@Composable
private fun SheetAccountSelector(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (String?) -> Unit
) {
    val rows = accounts.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { account ->
                    val isSelected = selectedAccountId == account.id
                    val accountColor = runCatching {
                        Color(android.graphics.Color.parseColor(account.color))
                    }.getOrDefault(SheetPurple)
                    val accountIcon = when (account.type) {
                        AccountType.CASH -> Icons.Default.Payments
                        AccountType.BANK_ACCOUNT -> Icons.Default.AccountBalance
                        AccountType.E_WALLET -> Icons.Default.AccountBalanceWallet
                        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accountColor.copy(alpha = 0.10f) else AppPalette.card)
                            .border(1.5.dp, if (isSelected) accountColor else SheetBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelect(if (isSelected) null else account.id) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(accountColor.copy(alpha = if (isSelected) 0.18f else 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(accountIcon, null, tint = accountColor, modifier = Modifier.size(16.dp))
                        }
                        Text(account.name,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) accountColor else AppPalette.textMuted,
                            maxLines   = 2,
                            textAlign  = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// --- Shared form helpers ------------------------------------------------------

@Composable
internal fun SheetFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accentColor: Color,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text(placeholder, color = Color(0xFFBDBDBD)) },
            prefix        = if (prefix != null) {{ Text(prefix, fontWeight = FontWeight.Bold, color = accentColor) }} else null,
            singleLine    = true,
            isError       = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape  = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = accentColor,
                unfocusedBorderColor    = SheetBorder,
                focusedContainerColor   = AppPalette.card,
                unfocusedContainerColor = AppPalette.card,
                errorBorderColor        = Color(0xFFEF4444)
            )
        )
        if (error != null) {
            Text(error, style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
        }
    }
}

@Composable
internal fun SheetFrequencySelector(
    selected: BudgetFrequency,
    onSelect: (BudgetFrequency) -> Unit,
    accentColor: Color
) {
    val options = listOf(
        BudgetFrequency.WEEKLY    to "Weekly",
        BudgetFrequency.BIWEEKLY  to "Biweekly",
        BudgetFrequency.MONTHLY   to "Monthly",
        BudgetFrequency.QUARTERLY to "Quarterly",
        BudgetFrequency.YEARLY    to "Yearly"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (freq, label) ->
            val isSelected = selected == freq
            Surface(
                modifier = Modifier.clickable { onSelect(freq) },
                shape    = RoundedCornerShape(50.dp),
                color    = if (isSelected) accentColor else AppPalette.card,
                border   = BorderStroke(1.dp, if (isSelected) accentColor else SheetBorder)
            ) {
                Text(
                    label,
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color      = if (isSelected) Color.White else Color(0xFF6B6B8A)
                )
            }
        }
    }
}






