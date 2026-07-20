package com.example.insightku.feature.planning.budget.presentation

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.ui.theme.LocalAccent
import androidx.compose.ui.graphics.toArgb
import com.example.insightku.core.ui.components.PremiumDatePicker
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.core.utils.digitsToInt
import com.example.insightku.core.utils.digitsToLong
import com.example.insightku.core.utils.toAmountOrZero
import java.util.*

// ─── Curated Icon Set for Recurring Payments & Installments ───────────────────

data class QuickIcon(val icon: ImageVector, val label: String, val color: Color)

internal val recurringIcons = listOf(
    QuickIcon(Icons.Default.Repeat, "General", Color(0xFF7C4DFF)),
    QuickIcon(Icons.Default.Subscriptions, "Subscription", Color(0xFF8B5CF6)),
    QuickIcon(Icons.Default.Home, "Rent", Color(0xFFEF4444)),
    QuickIcon(Icons.Default.Wifi, "Internet", Color(0xFF3B82F6)),
    QuickIcon(Icons.Default.ElectricBolt, "Electricity", Color(0xFFF59E0B)),
    QuickIcon(Icons.Default.Water, "Water", Color(0xFF06B6D4)),
    QuickIcon(Icons.Default.PhoneAndroid, "Phone", Color(0xFF8B5CF6)),
    QuickIcon(Icons.Default.Security, "Insurance", Color(0xFF3B82F6)),
    QuickIcon(Icons.Default.School, "Education", Color(0xFF3B82F6)),
    QuickIcon(Icons.Default.FitnessCenter, "Fitness", Color(0xFF10B981)),
    QuickIcon(Icons.Default.LocalGasStation, "Fuel", Color(0xFFEF4444)),
    QuickIcon(Icons.Default.LocalParking, "Parking", Color(0xFF6366F1)),
    QuickIcon(Icons.Default.CleaningServices, "Cleaning", Color(0xFF10B981)),
    QuickIcon(Icons.Default.Pets, "Pet", Color(0xFFF59E0B)),
    QuickIcon(Icons.Default.CreditCard, "Card Fee", Color(0xFFEF4444)),
    QuickIcon(Icons.Default.AccountBalance, "Loan", Color(0xFFEF4444)),
    QuickIcon(Icons.Default.MoreHoriz, "Other", Color(0xFF79747E))
)

internal val installmentIcons = listOf(
    QuickIcon(Icons.Default.CreditScore, "General", Color(0xFF06B6D4)),
    QuickIcon(Icons.Default.PhoneAndroid, "Phone", Color(0xFF8B5CF6)),
    QuickIcon(Icons.Default.Laptop, "Laptop", Color(0xFF3B82F6)),
    QuickIcon(Icons.Default.Devices, "Electronics", Color(0xFF3B82F6)),
    QuickIcon(Icons.Default.DirectionsCar, "Car", Color(0xFFEF4444)),
    QuickIcon(Icons.Default.TwoWheeler, "Motorcycle", Color(0xFF3B82F6)),
    QuickIcon(Icons.Default.Flight, "Travel", Color(0xFF06B6D4)),
    QuickIcon(Icons.Default.Hotel, "Hotel", Color(0xFF06B6D4)),
    QuickIcon(Icons.Default.ShoppingBag, "Shopping", Color(0xFFEC4899)),
    QuickIcon(Icons.Default.Checkroom, "Clothing", Color(0xFFDB2777)),
    QuickIcon(Icons.Default.Home, "Furniture", Color(0xFFD97706)),
    QuickIcon(Icons.Default.School, "Education", Color(0xFF3B82F6)),
    QuickIcon(Icons.Default.AccountBalance, "Loan", Color(0xFFEF4444)),
    QuickIcon(Icons.Default.MoreHoriz, "Other", Color(0xFF79747E))
)

// ─── Icon Picker (Shared) ────────────────────────────────────────────────────

@Composable
fun SheetIconPicker(
    icons: List<QuickIcon>,
    selectedLabel: String?,
    onSelect: (QuickIcon) -> Unit,
    accentColor: Color
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(icons) { qi ->
            val isSelected = qi.label == selectedLabel
            Surface(
                modifier = Modifier.clickable { onSelect(qi) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) qi.color.copy(alpha = 0.15f) else AppPalette.card,
                border = BorderStroke(1.5.dp, if (isSelected) qi.color else SheetBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(qi.icon, null, tint = if (isSelected) qi.color else AppPalette.textMuted, modifier = Modifier.size(20.dp))
                    Text(qi.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) qi.color else AppPalette.textMuted, maxLines = 1)
                }
            }
        }
    }
}

// ─── Design tokens ────────────────────────────────────────────────────────────

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

    val context            = LocalContext.current
    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name        by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var amountText  by remember(editing) { mutableStateOf(editing?.amount?.toLong()?.toString() ?: "") }
    var frequency   by remember(editing) { mutableStateOf(editing?.frequency ?: BudgetFrequency.MONTHLY) }
    var nextDue     by remember(editing) { mutableLongStateOf(editing?.nextDue ?: System.currentTimeMillis()) }
    var selectedCategoryId by remember(editing) { mutableStateOf(editing?.categoryId) }
    var selectedAccountId by remember(editing) { mutableStateOf(editing?.accountId) }
    var selectedIconLabel by remember(editing) {
        mutableStateOf(
            editing?.iconName?.let { label ->
                recurringIcons.find { it.label == label }?.label
            }
        )
    }
    var selectedColorHex by remember(editing) { mutableStateOf(editing?.color) }
    var nameError   by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        PremiumDatePicker(
            initialMillis = nextDue,
            onDateSelected = { millis -> nextDue = millis; showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest   = onDismiss,
        containerColor     = AppPalette.card,
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle         = {             Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(SheetBorder))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp)) {
                Surface(shape = RoundedCornerShape(50), color = SheetPurple.copy(alpha = 0.10f)) {
                    Text(stringResource(R.string.recurring_sheet_chip), Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = SheetPurple)
                }
                Spacer(Modifier.height(6.dp))
                Text(if (editing != null) stringResource(R.string.recurring_sheet_edit) else stringResource(R.string.recurring_sheet_add),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                Text(stringResource(R.string.recurring_sheet_desc),
                    style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))

            // Form
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .background(SheetBg).padding(24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SheetFormField(label = stringResource(R.string.recurring_form_name), value = name,
                    onValueChange = { name = it; nameError = null },
                    placeholder = stringResource(R.string.recurring_name_placeholder), error = nameError, accentColor = SheetPurple)

                SheetFormField(label = stringResource(R.string.amount_label),
                    value = CurrencyUtils.formatInputThousands(amountText),
                    onValueChange = { amountText = CurrencyUtils.stripThousands(it); amountError = null },
                    placeholder = stringResource(R.string.recurring_amount_placeholder), keyboardType = KeyboardType.Number,
                    prefix = NumberFormatter.getCurrencySymbol(), error = amountError, accentColor = SheetPurple)

                // Icon picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.recurring_form_icon), style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    SheetIconPicker(
                        icons = recurringIcons,
                        selectedLabel = selectedIconLabel,
                        onSelect = { qi ->
                            selectedIconLabel = qi.label
                            selectedColorHex = String.format("#%06X", 0xFFFFFF and qi.color.toArgb())
                        },
                        accentColor = SheetPurple
                    )
                }

                // Category selector
                if (availableCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.category_label), style = MaterialTheme.typography.labelSmall,
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
                        Text(stringResource(R.string.account_label), style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                        SheetAccountSelector(
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            onSelect = { selectedAccountId = if (selectedAccountId == it) null else it }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.recurring_form_frequency), style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    SheetFrequencySelector(selected = frequency, onSelect = { frequency = it }, accentColor = SheetPurple)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.recurring_form_next_due), style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        shape    = RoundedCornerShape(14.dp),
                        color    = AppPalette.card,
                        border   = BorderStroke(1.dp, SheetBorder)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(DateFormatter.formatFullDate(nextDue),
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
                            Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFF6B6B8A))
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp))
                            .background(SheetPurple).clickable {
                                // Validate
                                var hasError = false
                                if (name.trim().length < 2) { nameError = context.getString(R.string.error_name_short); hasError = true }
                                val parsedAmount = amountText.toAmountOrZero()
                                if (parsedAmount <= 0) { amountError = context.getString(R.string.error_amount_zero); hasError = true }
                                if (hasError) return@clickable

                                focusManager.clearFocus(); keyboardController?.hide()
                                val selectedIcon = selectedIconLabel?.let { label -> recurringIcons.find { it.label == label } }
                                onSave(RecurringBudget(
                                    id         = editing?.id ?: 0,
                                    name       = name.trim(),
                                    amount     = parsedAmount,
                                    frequency  = frequency,
                                    nextDue    = nextDue,
                                    isActive   = true,
                                    categoryId = selectedCategoryId,
                                    accountId  = selectedAccountId ?: editing?.accountId,
                                    iconName   = selectedIcon?.label,
                                    color      = selectedColorHex
                                ))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (editing != null) stringResource(R.string.recurring_save_changes) else stringResource(R.string.recurring_add_payment_btn),
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

    val context            = LocalContext.current
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
    var selectedIconLabel by remember(editing) {
        mutableStateOf(
            editing?.iconName?.let { label ->
                installmentIcons.find { it.label == label }?.label
            }
        )
    }
    var selectedColorHex by remember(editing) { mutableStateOf(editing?.color) }
    var nameError       by remember { mutableStateOf<String?>(null) }
    var totalAmountError by remember { mutableStateOf<String?>(null) }
    var monthlyError    by remember { mutableStateOf<String?>(null) }
    var monthsError     by remember { mutableStateOf<String?>(null) }
    var showDatePicker  by remember { mutableStateOf(false) }

    // Auto-calculation state
    // Tracks which field was last edited to determine calculation direction
    var lastEditedField by remember(editing) { mutableStateOf<String?>(null) }

    val totalAmount = totalAmountText.digitsToLong()
    val monthly     = monthlyText.digitsToLong()
    val totalMonths = totalMonthsText.digitsToInt()
    val paidMonths  = paidMonthsText.digitsToInt()

    // Auto-calculate: when totalAmount and monthly are set, compute totalMonths
    LaunchedEffect(totalAmount, monthly, lastEditedField) {
        if (lastEditedField == "monthly" && totalAmount > 0 && monthly > 0) {
            val calculated = kotlin.math.ceil(totalAmount.toDouble() / monthly).toInt()
            if (calculated > 0 && calculated != totalMonths) {
                totalMonthsText = calculated.toString()
            }
        }
    }

    // Auto-calculate: when totalAmount and totalMonths are set, compute monthly
    LaunchedEffect(totalAmount, totalMonths, lastEditedField) {
        if (lastEditedField == "months" && totalAmount > 0 && totalMonths > 0) {
            val calculated = kotlin.math.ceil(totalAmount.toDouble() / totalMonths).toLong()
            if (calculated > 0 && calculated != monthly) {
                monthlyText = calculated.toString()
            }
        }
    }

    val remainingMonths = (totalMonths - paidMonths).coerceAtLeast(0)
    val remaining = remainingMonths * monthly
    val progressPercent = if (totalMonths > 0) (paidMonths * 100 / totalMonths) else 0

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = nextDue)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { nextDue = it }; showDatePicker = false }) {
                    Text(stringResource(R.string.dialog_ok), fontWeight = FontWeight.Bold, color = SheetCyan)
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = state) }
    }

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AppPalette.card,
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle = {             Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(SheetBorder))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp)) {
                Surface(shape = RoundedCornerShape(50), color = SheetCyan.copy(alpha = 0.10f)) {
                    Text(stringResource(R.string.installment_sheet_chip), Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = SheetCyan)
                }
                Spacer(Modifier.height(6.dp))
                Text(if (editing != null) stringResource(R.string.installment_sheet_edit) else stringResource(R.string.installment_sheet_add),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                Text(stringResource(R.string.installment_sheet_desc),
                    style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .background(SheetBg).padding(24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SheetFormField(label = stringResource(R.string.installment_form_name), value = name,
                    onValueChange = { name = it; nameError = null },
                    placeholder = stringResource(R.string.installment_name_placeholder), error = nameError, accentColor = SheetCyan)

                SheetFormField(label = stringResource(R.string.total_amount_label),
                    value = CurrencyUtils.formatInputThousands(totalAmountText),
                    onValueChange = { totalAmountText = CurrencyUtils.stripThousands(it); totalAmountError = null },
                    placeholder = stringResource(R.string.installment_total_placeholder), keyboardType = KeyboardType.Number,
                    prefix = NumberFormatter.getCurrencySymbol(), error = totalAmountError, accentColor = SheetCyan)

                // Icon picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.installment_form_icon), style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    SheetIconPicker(
                        icons = installmentIcons,
                        selectedLabel = selectedIconLabel,
                        onSelect = { qi ->
                            selectedIconLabel = qi.label
                            selectedColorHex = String.format("#%06X", 0xFFFFFF and qi.color.toArgb())
                        },
                        accentColor = SheetCyan
                    )
                }

                // Smart Monthly Payment / Total Months fields
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.installment_form_details), style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    Text(stringResource(R.string.installment_form_auto_hint),
                        style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted.copy(alpha = 0.7f))
                }

                SheetFormField(label = stringResource(R.string.installment_form_monthly),
                    value = CurrencyUtils.formatInputThousands(monthlyText),
                    onValueChange = {
                        monthlyText = CurrencyUtils.stripThousands(it); monthlyError = null
                        lastEditedField = "monthly"
                    },
                    placeholder = stringResource(R.string.installment_monthly_placeholder), keyboardType = KeyboardType.Number,
                    prefix = NumberFormatter.getCurrencySymbol(), error = monthlyError, accentColor = SheetCyan,
                    description = if (totalAmount > 0 && totalMonths > 0 && monthly == 0L) stringResource(R.string.installment_form_auto_hint) else null)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        SheetFormField(label = stringResource(R.string.installment_form_total_months), value = totalMonthsText,
                            onValueChange = {
                                totalMonthsText = it.filter { c -> c.isDigit() }; monthsError = null
                                lastEditedField = "months"
                            },
                            placeholder = "12", keyboardType = KeyboardType.Number, accentColor = SheetCyan,
                            description = if (totalAmount > 0 && monthly > 0 && totalMonths == 0) stringResource(R.string.installment_form_auto_hint) else null)
                    }
                    Column(Modifier.weight(1f)) {
                        SheetFormField(label = stringResource(R.string.installment_form_paid_months), value = paidMonthsText,
                            onValueChange = { paidMonthsText = it.filter { c -> c.isDigit() } },
                            placeholder = "0", keyboardType = KeyboardType.Number, accentColor = SheetCyan)
                    }
                }

                // Category selector
                if (availableCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.category_label), style = MaterialTheme.typography.labelSmall,
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
                        Text(stringResource(R.string.account_label), style = MaterialTheme.typography.labelSmall,
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
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(stringResource(R.string.recurring_remaining), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                    Text(NumberFormatter.formatCurrency(remaining.toDouble()),
                                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetCyan)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(stringResource(R.string.installment_progress_label), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                    Text("$progressPercent%",
                                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
                                }
                            }
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { if (totalMonths > 0) paidMonths.toFloat() / totalMonths else 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50.dp)),
                                color = Color(0xFF7C4DFF),
                                trackColor = SheetBorder
                            )
                            Text(stringResource(R.string.installment_months_summary, remainingMonths, paidMonths, totalMonths),
                                style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.next_due_date_label), style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        shape    = RoundedCornerShape(14.dp),
                        color    = AppPalette.card,
                        border   = BorderStroke(1.dp, SheetBorder)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(DateFormatter.formatFullDate(nextDue),
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
                            Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFF6B6B8A))
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp))
                            .background(SheetCyan).clickable {
                                // Validate
                                var hasError = false
                                if (name.trim().length < 2) { nameError = context.getString(R.string.error_name_short); hasError = true }
                                val parsedTotal = totalAmountText.toAmountOrZero()
                                val parsedMonthly = monthlyText.toAmountOrZero()
                                val parsedMonths = totalMonthsText.filter { it.isDigit() }.toIntOrNull() ?: 0
                                val parsedPaid = paidMonthsText.filter { it.isDigit() }.toIntOrNull() ?: 0

                                if (parsedTotal <= 0) { totalAmountError = context.getString(R.string.error_total_required); hasError = true }
                                if (parsedMonthly <= 0) { monthlyError = context.getString(R.string.error_monthly_required); hasError = true }
                                if (parsedMonths <= 0) { monthsError = context.getString(R.string.error_months_required); hasError = true }
                                if (parsedPaid > parsedMonths && parsedMonths > 0) { monthsError = context.getString(R.string.error_paid_exceeds); hasError = true }
                                if (hasError) return@clickable

                                focusManager.clearFocus(); keyboardController?.hide()
                                val selectedIcon = selectedIconLabel?.let { label -> installmentIcons.find { it.label == label } }
                                onSave(Installment(
                                    id          = editing?.id ?: java.util.UUID.randomUUID().toString(),
                                    name        = name.trim(),
                                    totalAmount = parsedTotal,
                                    monthlyPayment = parsedMonthly,
                                    totalMonths = parsedMonths,
                                    paidMonths  = parsedPaid,
                                    nextDueDate = nextDue,
                                    isActive    = true,
                                    categoryId  = selectedCategoryId,
                                    accountId   = selectedAccountId ?: editing?.accountId,
                                    iconName    = selectedIcon?.label,
                                    color       = selectedColorHex
                                ))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (editing != null) stringResource(R.string.recurring_save_changes) else stringResource(R.string.installment_add_btn),
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
                    val catColor = runCatching {
                        Color(android.graphics.Color.parseColor(cat.color.ifBlank { "#7C4DFF" }))
                    }.getOrDefault(accentColor)
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
                            Icon(Icons.Default.Category, null, tint = catColor, modifier = Modifier.size(16.dp))
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
    prefix: String? = null,
    description: String? = null
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
        if (description != null && error == null) {
            Text(description, style = MaterialTheme.typography.labelSmall, color = accentColor.copy(alpha = 0.6f))
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
        BudgetFrequency.WEEKLY    to stringResource(R.string.frequency_weekly),
        BudgetFrequency.BIWEEKLY  to stringResource(R.string.frequency_biweekly),
        BudgetFrequency.MONTHLY   to stringResource(R.string.frequency_monthly),
        BudgetFrequency.QUARTERLY to stringResource(R.string.frequency_quarterly),
        BudgetFrequency.YEARLY    to stringResource(R.string.frequency_yearly)
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
