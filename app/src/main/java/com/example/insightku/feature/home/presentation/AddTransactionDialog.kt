package com.example.insightku.feature.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.ui.components.PremiumDatePicker
import com.example.insightku.core.utils.toAmountOrZero
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent

// ─── Date helpers ─────────────────────────────────────────────────────────────

/** Epoch ms → "12 May 2026" */
private fun Long.toDisplayDate(): String =
    DateFormatter.formatShortDate(this)

/** Epoch ms dari saat ini — dipakai sebagai default date untuk transaksi baru */
private fun todayMillis(): Long = System.currentTimeMillis()

// Note: Shared form components (AmountHeroCard, FormSectionCard, FormSectionTitle,
// FinanceField, CategoryChipSelector, AccountChipSelector) are imported from
// TransactionFormComponents.kt in the same package

// ─── Data & State ─────────────────────────────────────────────────────────────

// Tetap expose getCurrentDateAsString() agar tidak break kode lain yang mungkin pakai
fun getCurrentDateAsString(): String =
    DateFormatter.formatNumericDate(System.currentTimeMillis())

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
    onCreateCategory: () -> Unit = {},
    viewModel: AddTransactionViewModel? = null
) {
    // PERF FIX: Collect state internally to avoid MainScreen recompositions
    // If viewModel is provided, use it; otherwise fall back to parameters
    val collectedExpenseCategories = viewModel?.expenseCategories?.collectAsStateWithLifecycle()
    val collectedIncomeCategories = viewModel?.incomeCategories?.collectAsStateWithLifecycle()
    val effectiveExpenseCategories = collectedExpenseCategories?.value ?: expenseCategories
    val effectiveIncomeCategories = collectedIncomeCategories?.value ?: incomeCategories

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
                formData    = TransactionFormData(accountId = effectiveDefaultAccountId)    }
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
                            categories = if (formData.isIncome) effectiveIncomeCategories
                                         else effectiveExpenseCategories,
                            accounts = accounts,
                            onCreateCategory = onCreateCategory,
                            onShowDatePicker = { showDatePicker = true },
                            onSubmit = {
                                val amount = formData.amount.toAmountOrZero()
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
                }    }
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
    val accentColor = if (isIncome && isManual) AppPalette.success else AppPalette.accent

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
                    contentDescription = stringResource(R.string.back),
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(18.dp)
                )    }
}

        // Center content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {                Text(
                text = when {
                    !isManual -> stringResource(R.string.add_transaction_title)
                    isIncome  -> stringResource(R.string.add_income)
                    else      -> stringResource(R.string.add_expense)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
            Text(
                text = when {
                    !isManual -> stringResource(R.string.add_transaction_subtitle)
                    isIncome  -> stringResource(R.string.add_income_subtitle)
                    else      -> stringResource(R.string.add_expense_subtitle)
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
                contentDescription = stringResource(R.string.close),
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
            iconBg      = AppPalette.accent.copy(alpha = 0.1f),
            iconTint    = AppPalette.accent,
            title       = stringResource(R.string.scan_receipt),
            description = stringResource(R.string.scan_receipt_desc),
            badge       = "AI",
            badgeColor  = AppPalette.accent,
            onClick     = onOCRSelected
        )
        ModeOptionCard(
            icon        = Icons.Default.EditNote,
            iconBg      = IncomeGreen.copy(alpha = 0.1f),
            iconTint    = IncomeGreen,
            title       = stringResource(R.string.manual_entry),
            description = stringResource(R.string.manual_entry_desc),
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
        !isFormValid -> AppPalette.placeholder
        formData.isIncome -> AppPalette.success
        else -> AppPalette.accent
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
            FormSectionTitle(stringResource(R.string.transaction_details_section))
            Spacer(Modifier.height(12.dp))
            FinanceField(
                icon          = if (formData.isIncome) Icons.Default.Work else Icons.Default.Store,
                value         = formData.merchant,
                onValueChange = { onFormDataChanged(formData.copy(merchant = it)) },
                placeholder   = if (formData.isIncome) stringResource(R.string.income_source) else stringResource(R.string.merchant_store),
                imeAction     = ImeAction.Next,
                onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                onFocusChange = { onFocusChanged(it) }
            )
        }

        // ── 4. Category card ──────────────────────────────────────────────
        FormSectionCard {
            FormSectionTitle(stringResource(R.string.category))
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
            FormSectionTitle(stringResource(R.string.account))
            Spacer(Modifier.height(12.dp))
            AccountChipSelector(
                accounts = accounts,
                selectedAccountId = formData.accountId,
                onAccountSelected = { onFormDataChanged(formData.copy(accountId = it)) }
            )
        }

        // ── 6. Date & Note card ───────────────────────────────────────────
        FormSectionCard {
            FormSectionTitle(stringResource(R.string.date_and_note))
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
                            contentDescription = stringResource(R.string.change_date),
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
                    placeholder   = stringResource(R.string.note_placeholder),
                    singleLine    = false,
                    imeAction     = ImeAction.Done,
                    onImeAction   = { focusManager.clearFocus() },
                    onFocusChange = { onFocusChanged(it) },
                    modifier      = Modifier.height(80.dp)
                )    }
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
                    text          = if (formData.isIncome) stringResource(R.string.save_income) else stringResource(R.string.save_expense),
                    style         = MaterialTheme.typography.bodyLarge,
                    fontWeight    = FontWeight.Bold,
                    color         = if (isFormValid) Color.White else AppPalette.textMuted,
                    letterSpacing = 0.5.sp
                )    }
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
                    stringResource(R.string.back),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = AppPalette.textMuted
                )    }
}
    }
}













