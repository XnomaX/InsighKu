package com.example.insightku.ui.components.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.data.model.Category
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import com.example.insightku.ui.components.common.PremiumDatePicker
import com.example.insightku.ui.dialogs.CategoryIconResolver
import com.example.insightku.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// â”€â”€â”€ Design tokens â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
private val EditPurple     = Color(0xFF7C4DFF)
private val EditPurpleTint = Color(0xFFEDE9FE)
private val EditBorder     = Color(0xFFECE7F6)
private val EditSurface    = Color(0xFFFAF9FE)
private val EditTextPrimary = Color(0xFF1A1A2E)
private val EditTextMuted   = Color(0xFF9E9E9E)
private val EditIncomeGreen = Color(0xFF10B981)
private val EditExpenseRed  = Color(0xFFEF4444)

private val paymentOptions = listOf(
    "Cash" to Icons.Default.Payments,
    "QRIS" to Icons.Default.QrCode,
    "Debit Card" to Icons.Default.CreditCard,
    "Credit Card" to Icons.Default.CreditScore,
    "Bank Transfer" to Icons.Default.AccountBalance,
    "E-Wallet" to Icons.Default.AccountBalanceWallet,
    "GoPay" to Icons.Default.AccountBalanceWallet,
    "OVO" to Icons.Default.AccountBalanceWallet,
    "DANA" to Icons.Default.AccountBalanceWallet,
    "ShopeePay" to Icons.Default.AccountBalanceWallet,
    "PayLater" to Icons.Default.AccessTime,
    "Other" to Icons.Default.MoreHoriz
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDetail(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    categoryMap: Map<String, com.example.insightku.data.model.Category> = emptyMap(),
    categories: List<com.example.insightku.data.model.Category> = emptyList(),
    expenseCategories: List<com.example.insightku.data.model.Category> = emptyList(),
    incomeCategories: List<com.example.insightku.data.model.Category> = emptyList()
) {
    val focusManager = LocalFocusManager.current

    var title         by remember { mutableStateOf(transaction.title) }
    var amountRaw     by remember { mutableStateOf(abs(transaction.amount).toLong().toString()) }
    var amountFieldValue by remember {
        val initial = abs(transaction.amount).toLong().toString()
        mutableStateOf(TextFieldValue(text = initial, selection = androidx.compose.ui.text.TextRange(initial.length)))
    }
    var type          by remember { mutableStateOf(transaction.type) }
    var category      by remember { mutableStateOf(transaction.category) }
    var dateMillis    by remember { mutableStateOf(transaction.date) }
    var notes         by remember { mutableStateOf(transaction.description ?: "") }
    var paymentMethod by remember { mutableStateOf(transaction.paymentMethod ?: "Cash") }
    var showDatePicker    by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val isIncome    = type == TransactionType.INCOME
    val accentColor = if (isIncome) EditIncomeGreen else EditPurple
    val amountColor = if (isIncome) EditIncomeGreen else EditExpenseRed

    // Use type-filtered lists (same as AddTransaction). Fall back to unfiltered `categories`
    // only if the caller didn't provide the split lists (backward compat).
    val pickerCategories = when {
        isIncome  && incomeCategories.isNotEmpty()  -> incomeCategories
        !isIncome && expenseCategories.isNotEmpty() -> expenseCategories
        else -> categories
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler { onDismiss() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color(0xFFE0D9F5)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Edit Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EditTextPrimary)
                    Text("Update your transaction details", style = MaterialTheme.typography.bodySmall, color = EditTextMuted)
                }
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(EditPurpleTint).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = EditPurple, modifier = Modifier.size(18.dp))
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(EditBorder))

            // Scrollable form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // â”€â”€ Type toggle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                EditTypeToggle(isIncome = isIncome, onToggle = { type = if (it) TransactionType.INCOME else TransactionType.EXPENSE })

                // â”€â”€ Amount hero â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                EditAmountCard(
                    amountRaw        = amountRaw,
                    amountFieldValue = amountFieldValue,
                    isIncome         = isIncome,
                    onAmountChange   = { raw, fv -> amountRaw = raw; amountFieldValue = fv }
                )

                // â”€â”€ Title â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                EditFormCard {
                    EditFieldLabel("TITLE")
                    EditTextField(
                        value         = title,
                        onValueChange = { title = it },
                        placeholder   = "Transaction title",
                        icon          = Icons.Default.Edit,
                        accentColor   = accentColor,
                        imeAction     = ImeAction.Next,
                        onImeAction   = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    )
                }

                // â”€â”€ Category â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                EditFormCard {
                    EditFieldLabel("CATEGORY")
                    Spacer(Modifier.height(8.dp))
                    EditCategoryDisplay(
                        category    = category,
                        categoryMap = categoryMap,
                        accentColor = accentColor,
                        onClick     = { showCategoryPicker = true }
                    )
                }

                // â”€â”€ Payment method â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                EditFormCard {
                    EditFieldLabel("PAYMENT METHOD")
                    Spacer(Modifier.height(10.dp))
                    EditPaymentChips(selected = paymentMethod, onSelect = { paymentMethod = it })
                }

                // â”€â”€ Date â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                EditFormCard {
                    EditFieldLabel("DATE")
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        shape    = RoundedCornerShape(14.dp),
                        color    = EditSurface,
                        border   = BorderStroke(1.dp, EditBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarMonth, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(Date(dateMillis)),
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color      = EditTextPrimary,
                                modifier   = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.EditCalendar, null, tint = Color(0xFFB39DDB), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // â”€â”€ Notes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                EditFormCard {
                    EditFieldLabel("NOTES (OPTIONAL)")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = notes,
                        onValueChange = { notes = it },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = { Text("Add a noteâ€¦", color = EditTextMuted.copy(alpha = 0.5f)) },
                        leadingIcon   = {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Notes, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            }
                        },
                        minLines = 2,
                        shape    = RoundedCornerShape(14.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = accentColor,
                            unfocusedBorderColor    = EditBorder,
                            focusedContainerColor   = Color.White,
                            unfocusedContainerColor = EditSurface
                        )
                    )
                }
            }

            // â”€â”€ Save / Cancel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(Modifier.fillMaxWidth().height(1.dp).background(EditBorder))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    border   = BorderStroke(1.dp, EditBorder)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold, color = EditTextMuted)
                }
                Button(
                    onClick = {
                        val finalAmount = amountRaw.toLongOrNull()?.toDouble() ?: 0.0
                        onSave(
                            transaction.copy(
                                title         = title.trim().ifBlank { transaction.title },
                                amount        = if (isIncome) finalAmount else finalAmount,
                                type          = type,
                                category      = category,
                                date          = dateMillis,
                                description   = notes.trim().ifBlank { null },
                                paymentMethod = paymentMethod.ifBlank { null }
                            )
                        )
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePicker) {
        PremiumDatePicker(
            initialMillis  = dateMillis,
            onDateSelected = { millis -> dateMillis = millis; showDatePicker = false },
            onDismiss      = { showDatePicker = false }
        )
    }

    if (showCategoryPicker) {
        val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false },
            sheetState       = pickerSheetState,
            containerColor   = Color.White,
            dragHandle = {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFFE0D9F5)))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Select Category",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = EditTextPrimary
                )
                if (pickerCategories.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No categories available", style = MaterialTheme.typography.bodyMedium, color = EditTextMuted)
                    }
                } else {
                    val rows = pickerCategories.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { cat ->
                                    val catColor = runCatching {
                                        Color(android.graphics.Color.parseColor(cat.color.ifBlank { "#7C4DFF" }))
                                    }.getOrDefault(Color(0xFF7C4DFF))
                                    val iconInfo  = CategoryIconResolver.resolve(cat.icon ?: cat.name)
                                    val isSelected = category == cat.name
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val scale by animateFloatAsState(
                                        targetValue   = if (isPressed) 0.92f else 1f,
                                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                                        label         = "cat_scale_${cat.id}"
                                    )
                                    val bgColor by animateColorAsState(
                                        targetValue   = if (isSelected) catColor.copy(alpha = 0.10f) else Color.White,
                                        animationSpec = tween(200), label = "cat_bg_${cat.id}"
                                    )
                                    val borderColor by animateColorAsState(
                                        targetValue   = if (isSelected) catColor else Color(0xFFECE7F6),
                                        animationSpec = tween(200), label = "cat_border_${cat.id}"
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .graphicsLayer { scaleX = scale; scaleY = scale }
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(bgColor)
                                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                            .clickable(interactionSource = interactionSource, indication = null) {
                                                category = cat.name
                                                showCategoryPicker = false
                                            }
                                            .padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(catColor.copy(alpha = if (isSelected) 0.18f else 0.10f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(iconInfo.icon, null, tint = catColor, modifier = Modifier.size(17.dp))
                                        }
                                        Text(
                                            text       = cat.name,
                                            style      = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color      = if (isSelected) catColor else EditTextMuted,
                                            maxLines   = 2,
                                            textAlign  = TextAlign.Center
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€â”€ Sub-composables â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun EditTypeToggle(isIncome: Boolean, onToggle: (Boolean) -> Unit) {
    val expenseColor by animateColorAsState(
        targetValue   = if (!isIncome) EditExpenseRed else Color(0xFFEEEEEE),
        animationSpec = tween(200), label = "exp_color"
    )
    val incomeColor by animateColorAsState(
        targetValue   = if (isIncome) EditIncomeGreen else Color(0xFFEEEEEE),
        animationSpec = tween(200), label = "inc_color"
    )
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = Color(0xFFF5F3FF),
        border = BorderStroke(1.dp, EditBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            Box(
                modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(10.dp))
                    .background(expenseColor).clickable { onToggle(false) },
                contentAlignment = Alignment.Center
            ) {
                Text("Expense", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = if (!isIncome) Color.White else EditTextMuted)
            }
            Box(
                modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(10.dp))
                    .background(incomeColor).clickable { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Text("Income", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = if (isIncome) Color.White else EditTextMuted)
            }
        }
    }
}

// VisualTransformation that formats raw digits with Indonesian thousand separators (.)
// while keeping the underlying TextField value as raw digits only.
private object ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val raw       = text.text
        val formatted = if (raw.isEmpty()) "" else {
            val n = raw.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
            java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID")).apply {
                maximumFractionDigits = 0; minimumFractionDigits = 0; isGroupingUsed = true
            }.format(n)
        }

        val offsetMapping = object : OffsetMapping {
            // raw index → formatted index: count how many separator chars appear before this raw position
            override fun originalToTransformed(offset: Int): Int {
                var rawCount = 0
                var fmtIdx   = 0
                while (fmtIdx < formatted.length && rawCount < offset) {
                    if (formatted[fmtIdx].isDigit()) rawCount++
                    fmtIdx++
                }
                return fmtIdx
            }
            // formatted index → raw index: count only digit chars up to this formatted position
            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it.isDigit() }
        }

        return TransformedText(
            androidx.compose.ui.text.AnnotatedString(formatted),
            offsetMapping
        )
    }
}

@Composable
private fun EditAmountCard(
    amountRaw: String,
    amountFieldValue: TextFieldValue,
    isIncome: Boolean,
    onAmountChange: (String, TextFieldValue) -> Unit
) {
    val accentColor = if (isIncome) EditIncomeGreen else EditPurple
    val cardBg      = if (isIncome) Color(0xFFF0FFF4) else Color(0xFFF3EEFF)
    var isFocused   by remember { mutableStateOf(false) }

    // Internal field value — raw digits only, cursor managed here
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(
            text      = amountRaw,
            selection = androidx.compose.ui.text.TextRange(amountRaw.length)
        ))
    }
    // Sync only when external raw value changes from outside (e.g. initial load)
    LaunchedEffect(amountRaw) {
        if (fieldValue.text != amountRaw) {
            fieldValue = TextFieldValue(
                text      = amountRaw,
                selection = androidx.compose.ui.text.TextRange(amountRaw.length)
            )
        }
    }

    val formatted   = CurrencyUtils.formatInputThousands(amountRaw)
    val displayText = if (amountRaw.isBlank()) "0" else formatted

    val borderColor by animateColorAsState(
        targetValue   = if (isFocused) accentColor else EditBorder,
        animationSpec = tween(180), label = "amt_border"
    )

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(cardBg).border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text       = "Rp $displayText",
            style      = MaterialTheme.typography.headlineMedium,
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
        OutlinedTextField(
            value         = fieldValue,
            onValueChange = { newVal ->
                // Strip non-digits — raw value is always pure digits
                val raw  = newVal.text.filter { it.isDigit() }
                // Clamp cursor to end of raw string (safe, no jumping)
                val next = newVal.copy(
                    text      = raw,
                    selection = androidx.compose.ui.text.TextRange(raw.length)
                )
                fieldValue = next
                onAmountChange(raw, next)
            },
            placeholder         = { Text("0", color = accentColor.copy(alpha = 0.35f)) },
            label               = { Text("Amount (Rp)", color = accentColor.copy(alpha = 0.7f)) },
            leadingIcon         = {
                Text("Rp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = accentColor, modifier = Modifier.padding(start = 4.dp))
            },
            visualTransformation = ThousandSeparatorTransformation,
            modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            shape  = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = accentColor,
                unfocusedBorderColor    = accentColor.copy(alpha = 0.3f),
                focusedContainerColor   = Color.White.copy(alpha = 0.6f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.4f),
                cursorColor             = accentColor,
                focusedTextColor        = accentColor,
                unfocusedTextColor      = accentColor
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun EditCategoryDisplay(
    category: String,
    accentColor: Color,
    categoryMap: Map<String, com.example.insightku.data.model.Category> = emptyMap(),
    onClick: () -> Unit = {}
) {
    val matchedCat     = categoryMap[category.trim().lowercase()]
    val iconKey        = matchedCat?.icon?.ifBlank { null } ?: category
    val resolvedByIcon = CategoryIconResolver.resolve(iconKey)
    val resolvedByName = CategoryIconResolver.resolve(category)
    val resolved       = if (resolvedByIcon.name != "Others") resolvedByIcon else resolvedByName
    val catColor       = if (!matchedCat?.color.isNullOrBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(matchedCat!!.color)) }.getOrDefault(resolved.color)
    } else resolved.color
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = catColor.copy(alpha = 0.08f),
        border   = BorderStroke(1.dp, catColor.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(resolved.icon, null, tint = catColor, modifier = Modifier.size(20.dp))
            }
            Text(
                text       = category.ifBlank { "Select category" },
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = if (category.isBlank()) EditTextMuted else EditTextPrimary,
                modifier   = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, null, tint = EditTextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EditPaymentChips(selected: String, onSelect: (String) -> Unit) {
    val rows = paymentOptions.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, icon) ->
                    val isSelected = selected == label
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue   = if (isPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                        label         = "pay_scale_$label"
                    )
                    val bgColor by animateColorAsState(
                        targetValue   = if (isSelected) EditPurple.copy(alpha = 0.10f) else Color.White,
                        animationSpec = tween(180), label = "pay_bg_$label"
                    )
                    val borderColor by animateColorAsState(
                        targetValue   = if (isSelected) EditPurple else EditBorder,
                        animationSpec = tween(180), label = "pay_border_$label"
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .height(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(interactionSource = interactionSource, indication = null) {
                                onSelect(if (selected == label) "" else label)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(if (isSelected) EditPurple.copy(alpha = 0.15f) else Color(0xFFF5F3FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = if (isSelected) EditPurple else EditTextMuted, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) EditPurple else EditTextMuted,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
                // Fill empty slots
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun EditFormCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        color    = Color.White,
        border   = BorderStroke(1.dp, EditBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            content  = content
        )
    }
}

@Composable
private fun EditFieldLabel(text: String) {
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp,
        color         = EditTextMuted.copy(alpha = 0.7f),
        fontWeight    = FontWeight.SemiBold
    )
}

@Composable
private fun EditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    accentColor: Color,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue   = if (isFocused) accentColor else EditBorder,
        animationSpec = tween(180), label = "field_border"
    )
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused },
        placeholder   = { Text(placeholder, color = EditTextMuted.copy(alpha = 0.5f)) },
        leadingIcon   = {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
        },
        singleLine      = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
        shape  = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = borderColor,
            unfocusedBorderColor    = borderColor,
            focusedContainerColor   = Color.White,
            unfocusedContainerColor = EditSurface
        )
    )
}
