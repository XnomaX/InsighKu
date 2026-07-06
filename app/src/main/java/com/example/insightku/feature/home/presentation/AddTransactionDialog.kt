package com.example.insightku.feature.home.presentation
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.TextFieldValue
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.ui.components.PremiumDatePicker
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── Design tokens ────────────────────────────────────────────────────────────
private val IncomeGreen  = Color(0xFF10B981)
private val ExpenseRed   = Color(0xFFE57373)
private val GlassSurface: Color  @Composable get() = AppPalette.card
private val GlassBorder: Color   @Composable get() = AppPalette.cardBorder

// ─── Date helpers ─────────────────────────────────────────────────────────────

/** Epoch ms → "12 May 2026" */
private fun Long.toDisplayDate(): String =
    SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date(this))

/** Epoch ms dari saat ini — dipakai sebagai default date untuk transaksi baru */
private fun todayMillis(): Long = System.currentTimeMillis()

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
    /**
     * ID of the selected Account.
     * Every transaction must belong to exactly one Account.
     */
    val accountId: String = ""
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
    onOpenScanner: () -> Unit,
    categories: List<Category> = emptyList(),
    expenseCategories: List<Category> = emptyList(),
    incomeCategories: List<Category> = emptyList(),
    accounts: List<Account> = emptyList(),
    /** Default account ID to preselect (e.g., from auto-detection or last used) */
    defaultAccountId: String? = null,
    notificationData: com.example.insightku.core.notification.NotificationTransactionData? = null,
    onCreateCategory: () -> Unit = {}
) {
    var currentStep by remember { mutableStateOf<AddTransactionStep>(AddTransactionStep.ModeSelection) }
    var formData    by remember { mutableStateOf(TransactionFormData()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isAnyFieldFocused by remember { mutableStateOf(false) }

    // Get default account - prefer defaultAccountId param, then first active account
    val effectiveDefaultAccountId = remember(accounts, defaultAccountId) {
        defaultAccountId ?: accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id ?: ""
    }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            isAnyFieldFocused = false
            if (notificationData != null) {
                // Pre-fill from notification — skip ModeSelection, go straight to ManualForm
                formData = TransactionFormData(
                    merchant      = notificationData.title,
                    amount        = if (notificationData.amount > 0) notificationData.amount.toLong().toString() else "",
                    isIncome      = notificationData.typeHint.uppercase() == "INCOME",
                    dateMillis    = notificationData.timestamp,
                    description   = notificationData.description,
                    accountId     = effectiveDefaultAccountId,
                    category      = "" // user picks manually
                )
                currentStep = AddTransactionStep.ManualForm
            } else {
                currentStep = AddTransactionStep.ModeSelection
                formData    = TransactionFormData(accountId = effectiveDefaultAccountId)
            }
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
                            categories = if (formData.isIncome) incomeCategories
                                         else expenseCategories,
                            accounts = accounts,
                            onCreateCategory = onCreateCategory,
                            onShowDatePicker = { showDatePicker = true },
                            onSubmit = {
                                val amount = formData.amount.toLongOrNull()?.toDouble() ?: 0.0
                                onTransactionAdded(
                                    Transaction(
                                        title         = formData.merchant,
                                        amount        = amount,
                                        category      = formData.category,
                                        description   = formData.description,
                                        date          = formData.dateMillis,
                                        accountId     = formData.accountId,
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

    // PremiumDatePicker uses a Dialog window internally — renders above any ModalBottomSheet
    if (showDatePicker) {
        PremiumDatePicker(
            initialMillis  = formData.dateMillis,
            onDateSelected = { millis ->
                formData = formData.copy(dateMillis = millis)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// ─── Premium Header (no gradient) ────────────────────────────────────────────

@Composable
private fun DialogGradientHeader(
    step: AddTransactionStep,
    isIncome: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val isManual = step == AddTransactionStep.ManualForm
    val accentColor = if (isIncome && isManual) Color(0xFF10B981) else Color(0xFF7C4DFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPalette.card)
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = if (isManual) 20.dp else 16.dp,
                bottom = if (isManual) 20.dp else 16.dp
            )
    ) {
        // Back button (ManualForm only)
        if (isManual) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppPalette.cardBorder)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Center content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    !isManual -> "Add Transaction"
                    isIncome  -> "Add Income"
                    else      -> "Add Expense"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
            Text(
                text = when {
                    !isManual -> "Track every mindful spending"
                    isIncome  -> "Record your income source"
                    else      -> "Record your spending"
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }

        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(AppPalette.cardBorder)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = AppPalette.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    // Thin divider
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppPalette.cardBorder)
    )
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
            iconBg      = Color(0xFF7C4DFF).copy(alpha = 0.1f),
            iconTint    = Color(0xFF7C4DFF),
            title       = "Scan Receipt",
            description = "Foto struk, AI ekstrak detailnya",
            badge       = "AI",
            badgeColor  = Color(0xFF7C4DFF),
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

@Composable
fun ColumnScope.ManualFormContent(
    formData: TransactionFormData,
    onFormDataChanged: (TransactionFormData) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    categories: List<Category> = emptyList(),
    accounts: List<Account> = emptyList(),
    onCreateCategory: () -> Unit = {},
    onShowDatePicker: () -> Unit = {},
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val primary      = MaterialTheme.colorScheme.primary

    // Form is valid only when all required fields are filled including account selection
    val isFormValid = formData.merchant.isNotBlank()
            && (formData.amount.toDoubleOrNull() ?: -1.0) > 0
            && formData.category.isNotBlank()
            && formData.accountId.isNotBlank()

    val saveBgColor = when {
        !isFormValid -> Color(0xFFE0E0E0)
        formData.isIncome -> Color(0xFF10B981)
        else -> Color(0xFF7C4DFF)
    }

    // ── Scrollable form content ───────────────────────────────────────────
    Column(
        modifier = Modifier
            .weight(1f)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .background(AppPalette.background)
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Transaction Type ───────────────────────────────────────────
        PremiumSegmentedControl(
            isIncome           = formData.isIncome,
            onSelectionChanged = { isIncome ->
                onFormDataChanged(formData.copy(isIncome = isIncome, category = ""))
            }
        )

        // ── 2. Amount card ────────────────────────────────────────────────
        AmountHeroCard(
            amount         = formData.amount,
            isIncome       = formData.isIncome,
            onAmountChange = { onFormDataChanged(formData.copy(amount = it)) },
            onImeAction    = { focusManager.moveFocus(FocusDirection.Down) },
            onFocusChange  = { onFocusChanged(it) }
        )

        // ── 3. Details card ───────────────────────────────────────────────
        FormSectionCard {
            FormSectionTitle("Transaction Details")
            Spacer(Modifier.height(12.dp))
            FinanceField(
                icon          = if (formData.isIncome) Icons.Default.Work else Icons.Default.Store,
                value         = formData.merchant,
                onValueChange = { onFormDataChanged(formData.copy(merchant = it)) },
                placeholder   = if (formData.isIncome) "Income Source" else "Merchant / Store",
                imeAction     = ImeAction.Next,
                onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                onFocusChange = { onFocusChanged(it) }
            )
        }

        // ── 4. Category card ──────────────────────────────────────────────
        FormSectionCard {
            FormSectionTitle("Category")
            Spacer(Modifier.height(12.dp))
            CategoryChipSelector(
                categories         = categories,
                selectedCategory   = formData.category,
                onCategorySelected = { onFormDataChanged(formData.copy(category = it)) },
                onCreateCategory   = onCreateCategory
            )
        }

        // ── 5. Account card ─────────────────────────────────────────────
        FormSectionCard {
            FormSectionTitle("Account")
            Spacer(Modifier.height(12.dp))
            AccountChipSelector(
                accounts = accounts,
                selectedAccountId = formData.accountId,
                onAccountSelected = { onFormDataChanged(formData.copy(accountId = it)) }
            )
        }

        // ── 6. Date & Note card ───────────────────────────────────────────
        FormSectionCard {
            FormSectionTitle("Date & Note")
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Date row — tappable surface
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowDatePicker() },
                    shape = RoundedCornerShape(14.dp),
                    color = AppPalette.background,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val accentAdd = LocalAccent.current
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentAdd.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint     = accentAdd,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text      = formData.dateMillis.toDisplayDate(),
                            style     = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color     = AppPalette.textPrimary,
                            modifier  = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.EditCalendar,
                            contentDescription = "Change date",
                            tint     = AppPalette.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Note field
                FinanceField(
                    icon          = Icons.AutoMirrored.Filled.Notes,
                    value         = formData.description,
                    onValueChange = { onFormDataChanged(formData.copy(description = it)) },
                    placeholder   = "What was this for? (optional)",
                    singleLine    = false,
                    imeAction     = ImeAction.Done,
                    onImeAction   = { focusManager.clearFocus() },
                    onFocusChange = { onFocusChanged(it) },
                    modifier      = Modifier.height(80.dp)
                )
            }
        }

        // ── 7. Save button ────────────────────────────────────────────────
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(saveBgColor)
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
                    tint               = if (isFormValid) Color.White else AppPalette.textMuted,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text          = if (formData.isIncome) "Save Income" else "Save Expense",
                    style         = MaterialTheme.typography.bodyLarge,
                    fontWeight    = FontWeight.Bold,
                    color         = if (isFormValid) Color.White else AppPalette.textMuted,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Back ghost button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(50.dp))
                .border(1.dp, AppPalette.cardBorder, RoundedCornerShape(50.dp))
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
                    tint     = AppPalette.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Back",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = AppPalette.textMuted
                )
            }
        }
    }
}

// ─── Category Selector ────────────────────────────────────────────────────────

@Composable
private fun CategoryChipSelector(
    categories: List<Category>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onCreateCategory: () -> Unit
) {
    val primary = Color(0xFF7C4DFF)

    if (categories.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            color    = AppPalette.background,
            border   = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(48.dp).clip(CircleShape).background(primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = primary.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                }
                Text("No categories yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                Text("Create a category to get started.", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                Surface(
                    modifier = Modifier.clickable(onClick = onCreateCategory),
                    shape    = RoundedCornerShape(50.dp),
                    color    = AppPalette.card,
                    border   = BorderStroke(1.dp, primary)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = primary)
                        Text("Create Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = primary)
                    }
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Vertical adaptive grid — 4 columns
        val rows = categories.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    CategoryItemCard(
                        category         = category,
                        isSelected       = selectedCategory == category.name,
                        onSelect         = { onCategorySelected(if (selectedCategory == category.name) "" else category.name) },
                        modifier         = Modifier.weight(1f)
                    )
                }
                // Fill remaining slots in last row
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Add category shortcut
        Row(
            modifier              = Modifier
                .clickable(onClick = onCreateCategory)
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = primary.copy(alpha = 0.7f))
            Text("Add category", style = MaterialTheme.typography.labelSmall, color = primary.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun CategoryItemCard(
    category: Category,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(category.color.ifBlank { "#7C4DFF" }))
    }.getOrDefault(Color(0xFF7C4DFF))
    val iconInfo          = CategoryIconResolver.resolve(category.icon ?: category.name)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label         = "cat_scale_${category.id}"
    )
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) catColor.copy(alpha = 0.10f) else AppPalette.card,
        animationSpec = tween(200),
        label         = "cat_bg_${category.id}"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) catColor else AppPalette.cardBorder,
        animationSpec = tween(200),
        label         = "cat_border_${category.id}"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = if (isSelected) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconInfo.icon, contentDescription = null, tint = catColor, modifier = Modifier.size(17.dp))
        }
        Text(
            text       = category.name,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = if (isSelected) catColor else AppPalette.textMuted,
            maxLines   = 2,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─── Form Section Card ────────────────────────────────────────────────────────

@Composable
private fun FormSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            content = content
        )
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color      = AppPalette.textPrimary
    )
}

// ─── Section Label ────────────────────────────────────────────────────────────

@Composable
private fun AmountHeroCard(
    amount: String,
    isIncome: Boolean,
    onAmountChange: (String) -> Unit,
    onImeAction: () -> Unit,
    onFocusChange: (Boolean) -> Unit = {}
) {
    val accentColor = if (isIncome) IncomeGreen else LocalAccent.current
    // Dark focal card: the accent shows as a soft glow over the themed surface, not a bright pastel fill.
    val cardBg      = androidx.compose.ui.graphics.lerp(AppPalette.card, accentColor, 0.06f)
    var isFocused   by remember { mutableStateOf(false) }

    // TextFieldValue preserves cursor position — prevents jumping cursor bug.
    // We keep the field value as raw digits only. The formatted display is shown
    // in the hero Text above, not inside the field.
    var fieldValue by remember(amount) {
        mutableStateOf(TextFieldValue(text = amount, selection = androidx.compose.ui.text.TextRange(amount.length)))
    }

    // Sync external state → field only when the raw digits actually differ
    // (avoids overwriting cursor position on every recomposition)
    LaunchedEffect(amount) {
        if (fieldValue.text != amount) {
            fieldValue = TextFieldValue(
                text      = amount,
                selection = androidx.compose.ui.text.TextRange(amount.length)
            )
        }
    }

    val formatted   = com.example.insightku.core.utils.CurrencyUtils.formatInputThousands(amount)
    val displayText = if (amount.isBlank()) "0" else formatted

    val borderColor by animateColorAsState(
        targetValue   = if (isFocused) accentColor else AppPalette.cardBorder,
        animationSpec = tween(180),
        label         = "amount_border"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Formatted display — purely visual, not editable
        Text(
            text       = "Rp $displayText",
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

        // Input field — raw digits only, cursor position preserved via TextFieldValue
        OutlinedTextField(
            value         = fieldValue,
            onValueChange = { newValue ->
                // Strip all non-digits from whatever was typed
                val rawDigits = newValue.text.filter { it.isDigit() }
                // Clamp cursor to end of raw digits (safe position)
                val newCursor = rawDigits.length
                val next = newValue.copy(
                    text      = rawDigits,
                    selection = androidx.compose.ui.text.TextRange(newCursor)
                )
                fieldValue = next
                onAmountChange(rawDigits)
            },
            placeholder   = { Text("0", color = accentColor.copy(alpha = 0.35f)) },
            label         = { Text("Amount (Rp)", color = accentColor.copy(alpha = 0.7f)) },
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
            singleLine      = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction    = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onImeAction() }),
            shape  = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = accentColor,
                unfocusedBorderColor    = accentColor.copy(alpha = 0.3f),
                focusedContainerColor   = AppPalette.card,
                unfocusedContainerColor = AppPalette.card,
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

// ─── Account Chip Selector ─────────────────────────────────────────────────────

/**
 * Account selector chip grid.
 * Shows all active accounts in a scrollable grid.
 * Users must select an account before saving a transaction.
 */
@Composable
private fun AccountChipSelector(
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (String) -> Unit
) {
    val accent = LocalAccent.current

    if (accounts.isEmpty()) {
        // Show message when no accounts exist
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppPalette.background,
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(48.dp).clip(CircleShape).background(accent.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = accent.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                }
                Text("No accounts yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                Text("Create an account in Settings to start tracking transactions.", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            }
        }
        return
    }

    // Group accounts by type for better organization
    val cashAndBank = accounts.filter { it.type in listOf(AccountType.CASH, AccountType.BANK_ACCOUNT) }
    val eWallets = accounts.filter { it.type == AccountType.E_WALLET }
    val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cash & Bank accounts
        if (cashAndBank.isNotEmpty()) {
            AccountChipGrid(
                accounts = cashAndBank,
                selectedAccountId = selectedAccountId,
                onAccountSelected = onAccountSelected,
                accent = accent
            )
        }

        // E-Wallets
        if (eWallets.isNotEmpty()) {
            AccountChipGrid(
                accounts = eWallets,
                selectedAccountId = selectedAccountId,
                onAccountSelected = onAccountSelected,
                accent = accent
            )
        }

        // Credit Cards
        if (creditCards.isNotEmpty()) {
            AccountChipGrid(
                accounts = creditCards,
                selectedAccountId = selectedAccountId,
                onAccountSelected = onAccountSelected,
                accent = accent
            )
        }
    }
}

@Composable
private fun AccountChipGrid(
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (String) -> Unit,
    accent: Color
) {
    val rows = accounts.chunked(3)
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { account ->
                    AccountChipCard(
                        account = account,
                        isSelected = selectedAccountId == account.id,
                        onSelect = { onAccountSelected(account.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccountChipCard(
    account: Account,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label         = "account_scale_${account.id}"
    )

    val accountColor = runCatching {
        Color(android.graphics.Color.parseColor(account.color))
    }.getOrDefault(Color(0xFF7C4DFF))

    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) accountColor.copy(alpha = 0.10f) else AppPalette.card,
        animationSpec = tween(200),
        label         = "account_bg_${account.id}"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) accountColor else AppPalette.cardBorder,
        animationSpec = tween(200),
        label         = "account_border_${account.id}"
    )

    val accountIcon: ImageVector = when (account.type) {
        AccountType.CASH -> Icons.Default.Payments
        AccountType.BANK_ACCOUNT -> Icons.Default.AccountBalance
        AccountType.E_WALLET -> Icons.Default.AccountBalanceWallet
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
    }

    Column(
        modifier = modifier
            .scale(scale)
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier         = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accountColor.copy(alpha = if (isSelected) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(accountIcon, contentDescription = account.name, tint = accountColor, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text       = account.name,
            style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = if (isSelected) accountColor else AppPalette.textMuted,
            maxLines   = 2,
            overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center
        )
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

    var containerWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .onSizeChanged { containerWidthPx = it.width }
    ) {
        val halfWidthDp = with(density) { (containerWidthPx / 2).toDp() }

        // Animated pill — solid colors
        Box(
            modifier = Modifier
                .width(halfWidthDp)
                .fillMaxHeight()
                .offset(x = halfWidthDp * pillFraction)
                .padding(3.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (isIncome) Color(0xFF10B981) else Color(0xFFE57373))
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


