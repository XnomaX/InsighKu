package com.example.insightku.ui.components.addtransaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── Design tokens ────────────────────────────────────────────────────────────
private val IncomeGreen  = Color(0xFF10B981)
private val ExpenseRed   = Color(0xFFEF4444)

// Premium purple system — dark → mid → electric violet
private val GradPurpleDark    = Color(0xFF2D0A5E)
private val GradPurpleMid     = Color(0xFF5A2A82)
private val GradPurpleViolet  = Color(0xFF7C3AED)
private val GradPurpleLavender = Color(0xFFAB8FD4)

// Expense accent — coral/red premium
private val ExpenseAccent     = Color(0xFFE53E3E)
private val ExpenseAccentLight = Color(0xFFFC8181)

// Income accent
private val GradIncomeDeep  = Color(0xFF064E3B)
private val GradIncomeMid   = Color(0xFF065F46)
private val GradIncomeLight = Color(0xFF10B981)

// Surface system
private val GlassSurface = Color(0xFFFAF8FF)
private val GlassBorder  = Color(0xFFE8DDFF)
private val CardSurface  = Color(0xFFF3EEFF)

// Amount hero tint
private val AmountCardExpense = Color(0xFFF0EBFF)
private val AmountCardIncome  = Color(0xFFECFDF5)

// ─── Payment method chips ─────────────────────────────────────────────────────
private data class PaymentChip(val label: String, val icon: ImageVector)
private val paymentChips = listOf(
    PaymentChip("Cash",     Icons.Default.Payments),
    PaymentChip("QRIS",     Icons.Default.QrCode),
    PaymentChip("Debit",    Icons.Default.CreditCard),
    PaymentChip("E-Wallet", Icons.Default.AccountBalanceWallet),
    PaymentChip("Transfer", Icons.Default.AccountBalance)
)

// ─── Date helpers ─────────────────────────────────────────────────────────────

/** Epoch ms → "12 May 2026" */
private fun Long.toDisplayDate(): String =
    SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date(this))

/** Epoch ms → "yyyy-MM-dd" untuk disimpan ke Transaction.date (Long) */
private fun todayMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

// ─── Data & State ─────────────────────────────────────────────────────────────

// Tetap expose getCurrentDateAsString() agar tidak break kode lain yang mungkin pakai
fun getCurrentDateAsString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

data class TransactionFormData(
    val merchant: String = "",
    val amount: String = "",
    val category: String = "",
    val description: String = "",
    /** Epoch ms dari tanggal yang dipilih user (bukan System.currentTimeMillis()) */
    val dateMillis: Long = todayMillis(),
    val isIncome: Boolean = false,
    val paymentMethod: String = ""
)

sealed class AddTransactionStep {
    object ModeSelection : AddTransactionStep()
    object ManualForm    : AddTransactionStep()
}

// ─── Main Dialog ──────────────────────────────────────────────────────────────

@Composable
fun AddTransactionDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTransactionAdded: (Transaction) -> Unit,
    onOpenScanner: () -> Unit
) {
    var currentStep by remember { mutableStateOf<AddTransactionStep>(AddTransactionStep.ModeSelection) }
    var formData    by remember { mutableStateOf(TransactionFormData()) }

    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Track apakah ada field yang sedang fokus (keyboard terbuka)
    var isAnyFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            currentStep = AddTransactionStep.ModeSelection
            formData    = TransactionFormData()
            isAnyFieldFocused = false
        }
    }

    // Satu fungsi terpusat untuk handle back — dipanggil dari BackHandler DAN onDismissRequest
    fun handleBack() {
        when {
            // Prioritas 1: keyboard terbuka → tutup keyboard saja
            isAnyFieldFocused -> {
                isAnyFieldFocused = false
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            // Prioritas 2: di ManualForm → kembali ke ModeSelection
            currentStep == AddTransactionStep.ManualForm -> {
                focusManager.clearFocus()
                keyboardController?.hide()
                currentStep = AddTransactionStep.ModeSelection
            }
            // Prioritas 3: di ModeSelection → tutup dialog
            else -> onDismiss()
        }
    }

    BackHandler(enabled = isOpen) { handleBack() }

    if (!isOpen) return

    // Gunakan Box overlay di atas composable tree (bukan Dialog window terpisah)
    // Ini memastikan BackHandler dikontrol penuh oleh activity's OnBackPressedDispatcher
    // dan predictive back gesture tidak bypass ke window Dialog terpisah
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(200)),
        exit  = fadeOut(tween(200))
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .then(
                    if (currentStep == AddTransactionStep.ModeSelection)
                        Modifier.wrapContentHeight()
                    else
                        Modifier.fillMaxHeight(0.92f)
                )
                .clip(RoundedCornerShape(28.dp))
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp), clip = false)
                // Konsumsi tap di dalam surface agar tidak propagate ke scrim
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                },
            color           = GlassSurface,
            tonalElevation  = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = if (currentStep == AddTransactionStep.ModeSelection)
                    Modifier.fillMaxWidth()
                else
                    Modifier.fillMaxSize()
            ) {
                // Gradient header — sama persis dengan DashboardHeader
                DialogGradientHeader(
                    step = currentStep,
                    isIncome = formData.isIncome,
                    onBack = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        currentStep = AddTransactionStep.ModeSelection
                    },
                    onClose = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDismiss()
                    }
                )

                // Content dengan animasi slide
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState == AddTransactionStep.ManualForm) {
                            slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)) togetherWith
                            slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280))
                        } else {
                            slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280)) togetherWith
                            slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280))
                        }
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        AddTransactionStep.ModeSelection -> ModeSelectionContent(
                            onOCRSelected = { onDismiss(); onOpenScanner() },
                            onManualSelected = { currentStep = AddTransactionStep.ManualForm }
                        )
                        AddTransactionStep.ManualForm -> ManualFormContent(
                            formData = formData,
                            onFormDataChanged = { formData = it },
                            onFocusChanged = { isAnyFieldFocused = it },
                            onSubmit = {
                                val amount = formData.amount.toDoubleOrNull() ?: 0.0
                                onTransactionAdded(
                                    Transaction(
                                        title         = formData.merchant,
                                        amount        = amount,
                                        category      = formData.category,
                                        description   = formData.description,
                                        date          = formData.dateMillis,
                                        paymentMethod = formData.paymentMethod.ifBlank { null },
                                        type          = if (formData.isIncome) TransactionType.INCOME
                                                        else TransactionType.EXPENSE
                                    )
                                )
                                onDismiss()
                            },
                            onBack = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                currentStep = AddTransactionStep.ModeSelection
                            }
                        )
                    }
                }
            }
        }
    }
    } // end AnimatedVisibility
}

// ─── Gradient Header ──────────────────────────────────────────────────────────

@Composable
private fun DialogGradientHeader(
    step: AddTransactionStep,
    isIncome: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val isManual = step == AddTransactionStep.ManualForm
    val gradColors = if (isIncome && isManual)
        listOf(GradIncomeDeep, GradIncomeMid, GradIncomeLight)
    else
        listOf(GradPurpleDark, GradPurpleMid, GradPurpleViolet)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(gradColors))
            .padding(
                start  = 8.dp,
                end    = 8.dp,
                top    = if (isManual) 18.dp else 14.dp,
                bottom = if (isManual) 18.dp else 14.dp
            )
    ) {
        // Back button (hanya ManualForm)
        if (isManual) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center content
        Column(
            modifier            = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    !isManual        -> "Add Your Transaction"
                    isIncome         -> "Add Income"
                    else             -> "Add Expense"
                },
                style         = MaterialTheme.typography.titleLarge,
                fontWeight    = FontWeight.ExtraBold,
                color         = Color.White,
                letterSpacing = 0.2.sp
            )
            Text(
                text = when {
                    !isManual -> "Choose how to record it"
                    isIncome  -> "Record your income source"
                    else      -> "Record your spending"
                },
                style  = MaterialTheme.typography.bodySmall,
                color  = Color.White.copy(alpha = 0.65f)
            )
        }

        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint     = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Mode Selection ───────────────────────────────────────────────────────────

@Composable
private fun ModeSelectionContent(
    onOCRSelected: () -> Unit,
    onManualSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeOptionCard(
            icon        = Icons.Default.CameraAlt,
            iconBg      = GradPurpleMid.copy(alpha = 0.1f),
            iconTint    = GradPurpleMid,
            title       = "Scan Receipt",
            description = "Foto struk, AI ekstrak detailnya",
            badge       = "AI",
            badgeColor  = GradPurpleMid,
            onClick     = onOCRSelected
        )
        ModeOptionCard(
            icon        = Icons.Default.EditNote,
            iconBg      = IncomeGreen.copy(alpha = 0.1f),
            iconTint    = IncomeGreen,
            title       = "Manual Entry",
            description = "Isi sendiri nominal dan kategori",
            onClick     = onManualSelected
        )
    }
}

@Composable
private fun ModeOptionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
    badge: String? = null,
    badgeColor: Color = Color.Unspecified
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                badge,
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color      = badgeColor,
                                modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Manual Form ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.ManualFormContent(
    formData: TransactionFormData,
    onFormDataChanged: (TransactionFormData) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val primary      = MaterialTheme.colorScheme.primary

    val expenseCategories = listOf(
        "Food & Drinks", "Transportation", "Shopping", "Entertainment",
        "Healthcare", "Utilities", "Housing", "Education", "Others"
    )
    val incomeCategories = listOf(
        "Salary", "Freelance", "Investment", "Business", "Gift", "Others"
    )
    val categories = if (formData.isIncome) incomeCategories else expenseCategories

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showDatePicker       by remember { mutableStateOf(false) }

    val isFormValid = formData.merchant.isNotBlank()
            && (formData.amount.toDoubleOrNull() ?: -1.0) > 0
            && formData.category.isNotBlank()

    val saveGradient = if (formData.isIncome)
        Brush.horizontalGradient(listOf(GradIncomeMid, GradIncomeLight))
    else
        Brush.horizontalGradient(listOf(GradPurpleDark, GradPurpleMid, GradPurpleViolet))

    // ── DatePickerDialog ──────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formData.dateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onFormDataChanged(formData.copy(dateMillis = millis))
                    }
                    showDatePicker = false
                }) { Text("OK", fontWeight = FontWeight.Bold, color = primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = primary,
                    todayDateBorderColor      = primary,
                    todayContentColor         = primary
                )
            )
        }
    }

    // ── Scrollable form content ───────────────────────────────────────────
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Transaction Type ───────────────────────────────────────────
        SectionLabel("TRANSACTION TYPE")
        PremiumSegmentedControl(
            isIncome           = formData.isIncome,
            onSelectionChanged = { isIncome ->
                onFormDataChanged(formData.copy(isIncome = isIncome, category = ""))
            }
        )

        // ── 2. Amount ─────────────────────────────────────────────────────
        SectionLabel("AMOUNT")
        AmountHeroCard(
            amount         = formData.amount,
            isIncome       = formData.isIncome,
            onAmountChange = { onFormDataChanged(formData.copy(amount = it)) },
            onImeAction    = { focusManager.moveFocus(FocusDirection.Down) },
            onFocusChange  = { onFocusChanged(it) }
        )

        // ── 3. Transaction Details ────────────────────────────────────────
        SectionLabel("TRANSACTION DETAILS")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceField(
                icon          = if (formData.isIncome) Icons.Default.Work else Icons.Default.Store,
                value         = formData.merchant,
                onValueChange = { onFormDataChanged(formData.copy(merchant = it)) },
                placeholder   = if (formData.isIncome) "Income Source" else "Merchant / Store",
                imeAction     = ImeAction.Next,
                onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                onFocusChange = { onFocusChanged(it) }
            )

            ExposedDropdownMenuBox(
                expanded         = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
            ) {
                FinanceField(
                    icon          = Icons.Default.Category,
                    value         = formData.category,
                    onValueChange = {},
                    placeholder   = "Category",
                    readOnly      = true,
                    modifier      = Modifier.menuAnchor(),
                    trailingIcon  = {
                        Icon(
                            if (showCategoryDropdown) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint     = primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                ExposedDropdownMenu(
                    expanded         = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onFormDataChanged(formData.copy(category = cat))
                                showCategoryDropdown = false
                            },
                            leadingIcon = {
                                if (formData.category == cat) {
                                    Icon(Icons.Default.Check, null, tint = primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── 4. Payment Method ─────────────────────────────────────────────
        SectionLabel("PAYMENT METHOD")
        PaymentMethodChips(
            selected = formData.paymentMethod,
            onSelect = { onFormDataChanged(formData.copy(paymentMethod = it)) }
        )

        // ── 5. Additional Info ────────────────────────────────────────────
        SectionLabel("ADDITIONAL INFO")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceField(
                icon          = Icons.Default.CalendarMonth,
                value         = formData.dateMillis.toDisplayDate(),
                onValueChange = {},
                placeholder   = "Date",
                readOnly      = true,
                trailingIcon  = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.08f))
                            .clickable { showDatePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EditCalendar,
                            contentDescription = "Pick date",
                            tint     = primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            )
            FinanceField(
                icon          = Icons.AutoMirrored.Filled.Notes,
                value         = formData.description,
                onValueChange = { onFormDataChanged(formData.copy(description = it)) },
                placeholder   = "Note (optional)",
                singleLine    = false,
                imeAction     = ImeAction.Done,
                onImeAction   = { focusManager.clearFocus() },
                onFocusChange = { onFocusChanged(it) },
                modifier      = Modifier.height(80.dp)
            )
        }

        // ── 6. Save Button (inside scroll — always visible above keyboard) ─
        Spacer(Modifier.height(8.dp))

        // Save CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation    = if (isFormValid) 8.dp else 0.dp,
                    shape        = RoundedCornerShape(16.dp),
                    ambientColor = if (formData.isIncome) GradIncomeLight.copy(alpha = 0.4f)
                                   else GradPurpleViolet.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFormValid) saveGradient
                    else Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    )
                )
                .clickable(enabled = isFormValid) { onSubmit() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = if (formData.isIncome) Icons.AutoMirrored.Filled.TrendingUp
                                         else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint               = if (isFormValid) Color.White
                                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text          = if (formData.isIncome) "Save Income" else "Save Expense",
                    style         = MaterialTheme.typography.bodyLarge,
                    fontWeight    = FontWeight.Bold,
                    color         = if (isFormValid) Color.White
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Back ghost button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Back",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Section Label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelSmall,
        fontWeight    = FontWeight.Bold,
        color         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        letterSpacing = 1.2.sp,
        modifier      = Modifier.padding(start = 2.dp)
    )
}

// ─── Amount Hero Card ─────────────────────────────────────────────────────────
// Focal point utama — angka besar seperti fintech dashboard

@Composable
private fun AmountHeroCard(
    amount: String,
    isIncome: Boolean,
    onAmountChange: (String) -> Unit,
    onImeAction: () -> Unit,
    onFocusChange: (Boolean) -> Unit = {}
) {
    val primary      = MaterialTheme.colorScheme.primary
    val cardBg       = if (isIncome) AmountCardIncome else AmountCardExpense
    val accentColor  = if (isIncome) GradIncomeLight else GradPurpleViolet
    val displayAmount = amount.ifBlank { "0" }
    var isFocused    by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(
                width = 1.5.dp,
                brush = if (isFocused)
                    Brush.horizontalGradient(
                        if (isIncome) listOf(GradIncomeMid, GradIncomeLight)
                        else listOf(GradPurpleMid, GradPurpleViolet)
                    )
                else Brush.horizontalGradient(listOf(GlassBorder, GlassBorder)),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Preview amount besar — focal point
        Text(
            text       = "Rp ${if (displayAmount == "0") "0" else
                String.format(Locale.getDefault(), "%,.0f", displayAmount.toDoubleOrNull() ?: 0.0)}",
            style      = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color      = accentColor,
            textAlign  = TextAlign.Center
        )
        Text(
            text  = if (isIncome) "Income amount" else "Expense amount",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(4.dp))

        // Input field compact di bawah preview
        OutlinedTextField(
            value         = amount,
            onValueChange = onAmountChange,
            placeholder   = { Text("0", color = accentColor.copy(alpha = 0.35f)) },
            label         = { Text("Amount Spent", color = accentColor.copy(alpha = 0.7f)) },
            leadingIcon   = {
                Text(
                    "Rp",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor,
                    modifier   = Modifier.padding(start = 4.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    isFocused = it.isFocused
                    onFocusChange(it.isFocused)
                },
            singleLine    = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction    = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onImeAction() }),
            shape  = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = accentColor,
                unfocusedBorderColor    = accentColor.copy(alpha = 0.3f),
                focusedContainerColor   = Color.White.copy(alpha = 0.6f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.4f),
                cursorColor             = accentColor,
                focusedLabelColor       = accentColor,
                focusedTextColor        = accentColor,
                unfocusedTextColor      = accentColor
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

// ─── Finance Field ────────────────────────────────────────────────────────────
// Field compact dengan icon container di kiri — mirip referensi

@Composable
private fun FinanceField(
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val primary   = MaterialTheme.colorScheme.primary
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue   = if (isFocused) primary else GlassBorder,
        animationSpec = tween(180),
        label         = "ff_border"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container kiri
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = primary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        // TextField tanpa border (border ada di Row)
        BasicFinanceTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = placeholder,
            readOnly      = readOnly,
            singleLine    = singleLine,
            imeAction     = imeAction,
            onImeAction   = onImeAction,
            onFocusChange = {
                isFocused = it
                onFocusChange(it)
            },
            modifier      = Modifier.weight(1f)
        )

        // Trailing icon
        if (trailingIcon != null) {
            Box(modifier = Modifier.padding(end = 12.dp)) {
                trailingIcon()
            }
        }
    }
}

@Composable
private fun BasicFinanceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    readOnly: Boolean,
    singleLine: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        },
        modifier = modifier
            .onFocusChanged { onFocusChange(it.isFocused) },
        readOnly      = readOnly,
        singleLine    = singleLine,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        shape  = RoundedCornerShape(0.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = Color.Transparent,
            unfocusedBorderColor    = Color.Transparent,
            focusedContainerColor   = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor             = MaterialTheme.colorScheme.primary
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
    )
}

// ─── Payment Method Chips ─────────────────────────────────────────────────────

@Composable
private fun PaymentMethodChips(
    selected: String,
    onSelect: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    // Wrap chips dalam Row yang bisa scroll horizontal
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(horizontal = 2.dp)
    ) {
        items(paymentChips) { chip ->
            val isSelected = selected == chip.label

            val chipBg by animateColorAsState(
                targetValue   = if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                animationSpec = tween(200),
                label         = "chip_bg_${chip.label}"
            )
            val chipContent by animateColorAsState(
                targetValue   = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label         = "chip_content_${chip.label}"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) primary else GlassBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(if (isSelected) "" else chip.label) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        chip.icon,
                        contentDescription = null,
                        tint     = chipContent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        chip.label,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = chipContent
                    )
                }
            }
        }
    }
}

// ─── Premium Segmented Control ────────────────────────────────────────────────

@Composable
fun PremiumSegmentedControl(
    isIncome: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated pill: 0f = expense (kiri), 1f = income (kanan)
    val pillFraction by animateFloatAsState(
        targetValue   = if (isIncome) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label         = "seg_pill"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
    ) {
        val halfWidth = maxWidth / 2

        // Animated pill — coral/red untuk expense, green untuk income
        Box(
            modifier = Modifier
                .width(halfWidth)
                .fillMaxHeight()
                .offset(x = halfWidth * pillFraction)
                .padding(3.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    if (isIncome)
                        Brush.horizontalGradient(listOf(GradIncomeMid, GradIncomeLight))
                    else
                        Brush.horizontalGradient(listOf(ExpenseAccent, ExpenseAccentLight))
                )
        )

        // Tap targets — Row di atas pill, tidak ada Button (fix overlap bug)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelectionChanged(false) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint     = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "Expense",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = if (!isIncome) FontWeight.ExtraBold else FontWeight.Normal,
                        color      = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelectionChanged(true) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint     = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "Income",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isIncome) FontWeight.ExtraBold else FontWeight.Normal,
                        color      = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// Alias backward compat
@Composable
fun IncomeExpenseSegmentedControl(
    isIncome: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) = PremiumSegmentedControl(isIncome, onSelectionChanged, modifier)

// ─── Backward compat aliases ──────────────────────────────────────────────────

@Composable
fun PremiumTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) = FinanceField(
    icon          = Icons.Default.Edit,
    value         = value,
    onValueChange = onValueChange,
    placeholder   = placeholder,
    modifier      = modifier,
    readOnly      = readOnly,
    singleLine    = singleLine,
    imeAction     = imeAction,
    onImeAction   = onImeAction,
    trailingIcon  = trailingContent
)

@Composable
fun FormTextField(
    label: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null
) = FinanceField(
    icon          = icon,
    value         = value,
    onValueChange = onValueChange,
    placeholder   = placeholder,
    modifier      = modifier,
    readOnly      = readOnly,
    singleLine    = singleLine,
    imeAction     = imeAction,
    onImeAction   = onImeAction,
    trailingIcon  = trailingIcon
)

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Mode Selection", showBackground = true, widthDp = 360, heightDp = 520)
@Composable
private fun PreviewModeSelection() {
    MaterialTheme {
        AddTransactionDialog(isOpen = true, onDismiss = {}, onTransactionAdded = {}, onOpenScanner = {})
    }
}

@Preview(name = "Expense Form", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun PreviewExpenseForm() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            var formData by remember { mutableStateOf(TransactionFormData(isIncome = false)) }
            Column(Modifier.fillMaxSize()) {
                DialogGradientHeader(
                    step = AddTransactionStep.ManualForm,
                    isIncome = false,
                    onBack = {},
                    onClose = {}
                )
                ManualFormContent(
                    formData = formData,
                    onFormDataChanged = { formData = it },
                    onSubmit = {},
                    onBack = {}
                )
            }
        }
    }
}

@Preview(name = "Income Form", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun PreviewIncomeForm() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            var formData by remember { mutableStateOf(TransactionFormData(isIncome = true, category = "Salary")) }
            Column(Modifier.fillMaxSize()) {
                DialogGradientHeader(
                    step = AddTransactionStep.ManualForm,
                    isIncome = true,
                    onBack = {},
                    onClose = {}
                )
                ManualFormContent(
                    formData = formData,
                    onFormDataChanged = { formData = it },
                    onSubmit = {},
                    onBack = {}
                )
            }
        }
    }
}
