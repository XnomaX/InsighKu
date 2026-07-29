package com.example.insightku.feature.home.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.home.presentation.TransactionCategoryIcon
import com.example.insightku.feature.home.presentation.resolveCategoryIcon
import com.example.insightku.core.utils.TimeUtils
import kotlin.math.abs

// --- Premium List View --------------------------------------------------------

@Composable
fun PremiumTransactionListView(
    transactions: List<Transaction>,
    allCount: Int,
    categoryMap: Map<String, Category> = emptyMap(),
    accountMap: Map<String, Account> = emptyMap(),
    searchTerm: String,
    onSearchTermChanged: (String) -> Unit,
    filterType: FilterType,
    onFilterTypeChanged: (FilterType) -> Unit,
    sortBy: SortType,
    onSortByChanged: (SortType) -> Unit,
    onTransactionSelected: (Transaction) -> Unit,
    onBack: () -> Unit
) {
    val totalIncome  = remember(transactions) { transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }
    val totalExpense = remember(transactions) { transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { abs(it.amount) } }

    // Group transactions by time period
    val grouped = remember(transactions) {
        transactions
            .sortedByDescending { it.date }
            .groupBy { getTxGroup(it.date) }
            .entries
            .sortedBy { it.key.ordinal }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TxBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Premium header
            item {
                PremiumTxHeader(
                    onBack   = onBack,
                    income   = totalIncome,
                    expense  = totalExpense,
                    total    = allCount
                )
            }

            // Search + filters
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumSearchBar(
                        value    = searchTerm,
                        onChange = onSearchTermChanged
                    )
                    PremiumFilterRow(
                        filterType          = filterType,
                        onFilterTypeChanged = onFilterTypeChanged
                    )
                    PremiumSortRow(
                        sortBy          = sortBy,
                        onSortByChanged = onSortByChanged,
                        count           = transactions.size
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    PremiumEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 40.dp)
                    )
                }
            } else {
                grouped.forEach { (group, txList) ->
                    // Sticky group header
                    item(key = "header-${group.name}") {
                        TxGroupHeader(
                            label    = group.label(),
                            count    = txList.size,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(txList, key = { it.id }) { tx ->
                        PremiumTransactionCard(
                            transaction = tx,
                            categoryMap = categoryMap,
                            accountMap = accountMap,
                            onClick     = { onTransactionSelected(tx) },
                            modifier    = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Premium Header -----------------------------------------------------------

@Composable
private fun PremiumTxHeader(
    onBack: () -> Unit,
    income: Double,
    expense: Double,
    total: Int
) {
    val accent = TxAccent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TxCard)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 20.dp)
    ) {
        // Back + title row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.10f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    stringResource(R.string.tx_list_title),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = TxTextPrimary
                )
                Text(
                    stringResource(R.string.tx_list_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TxTextMuted
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Summary cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TxSummaryCard(
                label  = stringResource(R.string.tx_summary_income),
                amount = income,
                color  = TxIncomeGreen,
                bg     = TxIncomeGreen.copy(alpha = 0.10f),
                icon   = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            TxSummaryCard(
                label  = stringResource(R.string.tx_summary_expenses),
                amount = expense,
                color  = TxExpenseRed,
                bg     = TxExpenseRed.copy(alpha = 0.10f),
                icon   = Icons.AutoMirrored.Filled.TrendingDown,
                modifier = Modifier.weight(1f)
            )
            TxSummaryCard(
                label  = stringResource(R.string.tx_summary_total),
                amount = null,
                count  = total,
                color  = accent,
                bg     = TxTint,
                icon   = Icons.Outlined.Receipt,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TxSummaryCard(
    label: String,
    amount: Double?,
    count: Int? = null,
    color: Color,
    bg: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        color     = bg,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            }
            if (amount != null) {
                Text(
                    formatCurrencyRp(amount),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = color,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    "$count",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

// --- Premium Search Bar -------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSearchBar(value: String, onChange: (String) -> Unit) {
    val accent = TxAccent
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        placeholder   = {
            Text(
                stringResource(R.string.tx_list_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TxTextMuted
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search, null,
                tint     = TxTextMuted,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = TxTextMuted)
                }
            }
        },
        singleLine = true,
        shape      = RoundedCornerShape(16.dp),
        modifier   = Modifier.fillMaxWidth(),
        colors     = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = accent.copy(alpha = 0.5f),
            unfocusedBorderColor    = TxCardBorder,
            unfocusedContainerColor = TxCard,
            focusedContainerColor   = TxCard,
            cursorColor             = accent,
            focusedTextColor        = TxTextPrimary,
            unfocusedTextColor      = TxTextPrimary
        )
    )
}

// --- Premium Filter Row -------------------------------------------------------

@Composable
private fun PremiumFilterRow(
    filterType: FilterType,
    onFilterTypeChanged: (FilterType) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val filters = listOf(
        FilterType.ALL         to context.getString(R.string.tx_filter_all),
        FilterType.INCOME      to context.getString(R.string.tx_filter_income),
        FilterType.EXPENSE     to context.getString(R.string.tx_filter_expense),
        FilterType.TRANSFER    to context.getString(R.string.tx_filter_transfer),
        FilterType.GOAL        to context.getString(R.string.tx_filter_goals),
        FilterType.AUTO_ALLOC  to context.getString(R.string.tx_filter_auto),
        FilterType.TODAY       to context.getString(R.string.tx_filter_today),
        FilterType.WEEK        to context.getString(R.string.tx_filter_this_week),
        FilterType.MONTH       to context.getString(R.string.tx_filter_this_month)
    )

    val accent = TxAccent
    Row(
        modifier              = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (type, label) ->
            val selected = filterType == type
            Surface(
                modifier = Modifier.clickable { onFilterTypeChanged(type) },
                shape    = RoundedCornerShape(50.dp),
                color    = if (selected) accent else TxCard,
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) accent else TxCardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selected) {
                        Icon(
                            Icons.Default.Check, null,
                            modifier = Modifier.size(12.dp),
                            tint     = Color.White
                        )
                    }
                    Text(
                        label,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color      = if (selected) Color.White else TxTextMuted
                    )
                }
            }
        }
    }
}

// --- Sort Row -----------------------------------------------------------------

@Composable
private fun PremiumSortRow(
    sortBy: SortType,
    onSortByChanged: (SortType) -> Unit,
    count: Int
) {
    var expanded by remember { mutableStateOf(false) }
    val accent = TxAccent

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            "$count transaction${if (count != 1) "s" else ""}",
            style = MaterialTheme.typography.labelMedium,
            color = TxTextMuted
        )
        Box {
            Surface(
                onClick = { expanded = true },
                shape   = RoundedCornerShape(10.dp),
                color   = accent.copy(alpha = 0.06f),
                border  = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, null, tint = accent, modifier = Modifier.size(14.dp))
                    Text(
                        when (sortBy) {
                            SortType.NEWEST   -> stringResource(R.string.tx_sort_newest_short)
                            SortType.OLDEST   -> stringResource(R.string.tx_sort_oldest_short)
                            SortType.HIGHEST  -> stringResource(R.string.tx_sort_highest_short)
                            SortType.LOWEST   -> stringResource(R.string.tx_sort_lowest_short)
                            SortType.CATEGORY -> stringResource(R.string.tx_sort_category)
                        },
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = accent
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = accent, modifier = Modifier.size(14.dp)
                    )
                }
            }
            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
                modifier         = Modifier.background(TxCard, RoundedCornerShape(14.dp))
            ) {
                listOf(
                    SortType.NEWEST   to stringResource(R.string.tx_sort_newest),
                    SortType.OLDEST   to stringResource(R.string.tx_sort_oldest),
                    SortType.HIGHEST  to stringResource(R.string.tx_sort_highest),
                    SortType.LOWEST   to stringResource(R.string.tx_sort_lowest),
                    SortType.CATEGORY to stringResource(R.string.tx_sort_category)
                ).forEach { (type, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (sortBy == type) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (sortBy == type) accent else TxTextPrimary
                            )
                        },
                        onClick      = { onSortByChanged(type); expanded = false },
                        leadingIcon  = {
                            if (sortBy == type) Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}

// --- Group Header -------------------------------------------------------------

@Composable
fun TxGroupHeader(label: String, count: Int, modifier: Modifier = Modifier) {
    val accent = TxAccent
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = TxTextPrimary
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = TxTint
        ) {
            Text(
                "$count",
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color      = accent
            )
        }
    }
}

// --- Premium Transaction Card -------------------------------------------------

@Composable
fun PremiumTransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    categoryMap: Map<String, Category> = emptyMap(),
    accountMap: Map<String, Account> = emptyMap(),
    modifier: Modifier = Modifier
) {
    // Resolve category color for badge (icon is handled by shared TransactionCategoryIcon)
    val resolved = resolveCategoryIcon(transaction.category, transaction.type, categoryMap)
    val catColor = resolved.color
    val amountColor = txTypeColor(transaction.type)
    val prefix      = txAmountPrefix(transaction.type)
    val showCat     = txShowCategory(transaction.type)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label         = "card_scale"
    )

    Surface(
        modifier      = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape         = RoundedCornerShape(20.dp),
        color         = TxCard,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border        = androidx.compose.foundation.BorderStroke(1.dp, TxCardBorder),
        onClick       = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category icon (shared composable — consistent with Home screen)
            // Use title as fallback when category is blank — the icon resolver
            // has fuzzy keyword matching that can resolve icons from titles.
            TransactionCategoryIcon(
                categoryName = transaction.category.ifBlank { transaction.title },
                transactionType = transaction.type,
                categoryMap = categoryMap,
                containerSize = 46.dp,
                iconSize = 22.dp
            )

            // Title + type badge + time
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    transaction.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = TxTextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(6.dp)
                ) {
                    // Type badge (always shown)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = amountColor.copy(alpha = 0.10f)
                    ) {
                        Text(
                            txTypeLabel(transaction.type),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = amountColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                    // Category badge (only for Income/Expense)
                    if (showCat && transaction.category.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catColor.copy(alpha = 0.10f)
                        ) {
                            Text(
                                transaction.category,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = catColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        TimeUtils.toShortRelativeTime(transaction.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = TxTextMuted
                    )
                }
                // Show context line: goal name for goals, account name otherwise
                val contextLabel = when {
                    (transaction.type == TransactionType.GOAL_CONTRIBUTION || transaction.type == TransactionType.GOAL_WITHDRAWAL || transaction.type == TransactionType.AUTO_ALLOCATION) && !transaction.goalName.isNullOrBlank() -> transaction.goalName
                    else -> accountMap[transaction.accountId]?.name
                }
                if (contextLabel != null) {
                    Text(
                        contextLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TxTextMuted
                    )
                }
            }

            // Amount
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "$prefix ${formatCurrencyRp(transaction.amount)}",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color      = amountColor
                )
                // Sync status badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (transaction.isSynced) TxIncomeGreen.copy(alpha = 0.10f) else TxAdjustOrange.copy(alpha = 0.10f)
                ) {
                    Text(
                        if (transaction.isSynced) stringResource(R.string.tx_detail_synced) else stringResource(R.string.tx_detail_syncing),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = if (transaction.isSynced) TxIncomeGreen else TxAdjustOrange,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// --- Premium Empty State ------------------------------------------------------

@Composable
private fun PremiumEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val accent = TxAccent
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(TxTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ReceiptLong, null,
                tint     = accent.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            stringResource(R.string.tx_list_empty),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = TxTextPrimary,
            textAlign  = TextAlign.Center
        )
        Text(
            stringResource(R.string.tx_list_empty_desc),
            style     = MaterialTheme.typography.bodySmall,
            color     = TxTextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// Keep old TransactionListItem for backward compat (used in previews)
@Composable
fun TransactionListItem(transaction: Transaction, onClick: () -> Unit) {
    PremiumTransactionCard(transaction = transaction, onClick = onClick)
}
