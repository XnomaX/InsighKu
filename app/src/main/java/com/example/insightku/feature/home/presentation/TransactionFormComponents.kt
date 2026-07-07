package com.example.insightku.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.CurrencyUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

// ─── Design Tokens ────────────────────────────────────────────────────────────

internal val IncomeGreen = com.example.insightku.core.ui.theme.AppPalette.success
internal val ExpenseRed = com.example.insightku.core.ui.theme.AppPalette.deleteRed
internal val GlassSurface: Color @Composable get() = AppPalette.card
internal val GlassBorder: Color @Composable get() = AppPalette.cardBorder

// ─── Form Section Card ────────────────────────────────────────────────────────

@Composable
internal fun FormSectionCard(content: @Composable ColumnScope.() -> Unit) {
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
internal fun FormSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = AppPalette.textPrimary
    )
}

// ─── Amount Hero Card ─────────────────────────────────────────────────────────

@Composable
internal fun AmountHeroCard(
    amount: String,
    isIncome: Boolean,
    onAmountChange: (String) -> Unit,
    onImeAction: () -> Unit,
    onFocusChange: (Boolean) -> Unit = {}
) {
    val accentColor = if (isIncome) IncomeGreen else LocalAccent.current
    val cardBg = androidx.compose.ui.graphics.lerp(AppPalette.card, accentColor, 0.06f)
    var isFocused by remember { mutableStateOf(false) }

    var fieldValue by remember(amount) {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = amount,
                selection = androidx.compose.ui.text.TextRange(amount.length)
            )
        )
    }

    LaunchedEffect(amount) {
        if (fieldValue.text != amount) {
            fieldValue = androidx.compose.ui.text.input.TextFieldValue(
                text = amount,
                selection = androidx.compose.ui.text.TextRange(amount.length)
            )
        }
    }

    val formatted = CurrencyUtils.formatInputThousands(amount)
    val displayText = if (amount.isBlank()) "0" else formatted

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else AppPalette.cardBorder,
        animationSpec = tween(180),
        label = "amount_border"
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
        Text(
            text = "Rp $displayText",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isIncome) "Income amount" else "Expense amount",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val rawDigits = newValue.text.filter { it.isDigit() }
                val newCursor = rawDigits.length
                val next = newValue.copy(
                    text = rawDigits,
                    selection = androidx.compose.ui.text.TextRange(newCursor)
                )
                fieldValue = next
                onAmountChange(rawDigits)
            },
            placeholder = { Text("0", color = accentColor.copy(alpha = 0.35f)) },
            label = { Text("Amount (Rp)", color = accentColor.copy(alpha = 0.7f)) },
            leadingIcon = {
                Text(
                    "Rp",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    isFocused = it.isFocused
                    onFocusChange(it.isFocused)
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onImeAction() }),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = accentColor.copy(alpha = 0.3f),
                focusedContainerColor = AppPalette.card,
                unfocusedContainerColor = AppPalette.card,
                cursorColor = accentColor,
                focusedLabelColor = accentColor,
                focusedTextColor = accentColor,
                unfocusedTextColor = accentColor
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

// ─── Finance Field ────────────────────────────────────────────────────────────

@Composable
internal fun FinanceField(
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
    val primary = MaterialTheme.colorScheme.primary
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) primary else GlassBorder,
        animationSpec = tween(180),
        label = "ff_border"
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
                tint = primary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        BasicFinanceTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            readOnly = readOnly,
            singleLine = singleLine,
            imeAction = imeAction,
            onImeAction = onImeAction,
            onFocusChange = {
                isFocused = it
                onFocusChange(it)
            },
            modifier = Modifier.weight(1f)
        )

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
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        },
        modifier = modifier.onFocusChanged { onFocusChange(it.isFocused) },
        readOnly = readOnly,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        shape = RoundedCornerShape(0.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
    )
}

// ─── Category Chip Selector ───────────────────────────────────────────────────

@Composable
internal fun CategoryChipSelector(
    categories: List<Category>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onCreateCategory: () -> Unit
) {
    val primary = AppPalette.accent

    if (categories.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppPalette.background,
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = primary.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                }
                Text("No categories yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                Text("Create a category to get started.", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                Surface(
                    modifier = Modifier.clickable(onClick = onCreateCategory),
                    shape = RoundedCornerShape(50.dp),
                    color = AppPalette.card,
                    border = BorderStroke(1.dp, primary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
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
        val rows = categories.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    CategoryItemCard(
                        category = category,
                        isSelected = selectedCategory == category.name,
                        onSelect = { onCategorySelected(if (selectedCategory == category.name) "" else category.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier
                .clickable(onClick = onCreateCategory)
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
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
    val catColor = com.example.insightku.core.ui.components.parseCategoryColor(category.color.ifBlank { "#7C4DFF" })
    val iconInfo = CategoryIconResolver.resolve(category.icon ?: category.name)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "cat_scale_${category.id}"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) catColor.copy(alpha = 0.10f) else AppPalette.card,
        animationSpec = tween(200),
        label = "cat_bg_${category.id}"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) catColor else AppPalette.cardBorder,
        animationSpec = tween(200),
        label = "cat_border_${category.id}"
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
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = if (isSelected) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconInfo.icon, contentDescription = null, tint = catColor, modifier = Modifier.size(17.dp))
        }
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) catColor else AppPalette.textMuted,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Account Chip Selector ────────────────────────────────────────────────────

@Composable
internal fun AccountChipSelector(
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (String) -> Unit
) {
    val accent = LocalAccent.current

    if (accounts.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppPalette.background,
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(accent.copy(alpha = 0.08f)),
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

    val cashAndBank = accounts.filter { it.type in listOf(AccountType.CASH, AccountType.BANK_ACCOUNT) }
    val eWallets = accounts.filter { it.type == AccountType.E_WALLET }
    val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (cashAndBank.isNotEmpty()) {
            AccountChipGrid(accounts = cashAndBank, selectedAccountId = selectedAccountId, onAccountSelected = onAccountSelected, accent = accent)
        }
        if (eWallets.isNotEmpty()) {
            AccountChipGrid(accounts = eWallets, selectedAccountId = selectedAccountId, onAccountSelected = onAccountSelected, accent = accent)
        }
        if (creditCards.isNotEmpty()) {
            AccountChipGrid(accounts = creditCards, selectedAccountId = selectedAccountId, onAccountSelected = onAccountSelected, accent = accent)
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
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
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "account_scale_${account.id}"
    )

    val accountColor = com.example.insightku.core.ui.components.parseCategoryColor(account.color)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accountColor.copy(alpha = 0.10f) else AppPalette.card,
        animationSpec = tween(200),
        label = "account_bg_${account.id}"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accountColor else AppPalette.cardBorder,
        animationSpec = tween(200),
        label = "account_border_${account.id}"
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
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accountColor.copy(alpha = if (isSelected) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(accountIcon, contentDescription = account.name, tint = accountColor, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = account.name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) accountColor else AppPalette.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
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
    val pillFraction by animateFloatAsState(
        targetValue = if (isIncome) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "seg_pill"
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

        Box(
            modifier = Modifier
                .width(halfWidthDp)
                .fillMaxHeight()
                .offset(x = halfWidthDp * pillFraction)
                .padding(3.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (isIncome) IncomeGreen else ExpenseRed)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelectionChanged(false) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "Expense",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (!isIncome) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isIncome) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
    icon = Icons.Default.Category,
    value = value,
    onValueChange = onValueChange,
    placeholder = placeholder,
    modifier = modifier,
    readOnly = readOnly,
    singleLine = singleLine,
    imeAction = imeAction,
    onImeAction = onImeAction,
    trailingIcon = trailingContent
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
    icon = icon,
    value = value,
    onValueChange = onValueChange,
    placeholder = placeholder,
    modifier = modifier,
    readOnly = readOnly,
    singleLine = singleLine,
    imeAction = imeAction,
    onImeAction = onImeAction,
    trailingIcon = trailingIcon
)
