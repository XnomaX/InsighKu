package com.example.insightku.feature.home.presentation
import com.example.insightku.core.ui.components.TransactionCategoryIcon
import com.example.insightku.core.ui.components.TransactionTypePresentation
import com.example.insightku.core.ui.components.resolveCategoryIcon

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.ui.components.PremiumDatePicker
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.TimeUtils
import com.example.insightku.feature.home.presentation.TransactionDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// --- Design tokens ------------------------------------------------------------
// Semantic colors delegated to TransactionTypePresentation for single source of truth.
private val TxIncomeGreen @Composable get() = TransactionTypePresentation.INCOME.color
private val TxExpenseRed @Composable get() = TransactionTypePresentation.EXPENSE.color
private val TxTransferBlue @Composable get() = TransactionTypePresentation.TRANSFER_OUT.color
private val TxGoalPurple @Composable get() = TransactionTypePresentation.GOAL_CONTRIBUTION.color
private val TxWithdrawalTeal @Composable get() = TransactionTypePresentation.GOAL_WITHDRAWAL.color
private val TxAutoAllocIndigo @Composable get() = TransactionTypePresentation.AUTO_ALLOCATION.color
private val TxAdjustOrange @Composable get() = TransactionTypePresentation.BALANCE_ADJUSTMENT.color

// All surface/text/border tokens are resolved at runtime from AppPalette or
// LocalAccent so they adapt to dark mode and the user's chosen accent.
private val TxAccent:      Color @Composable get() = LocalAccent.current
private val TxBackground:  Color @Composable get() = AppPalette.background
private val TxCard:        Color @Composable get() = AppPalette.card
private val TxCardBorder:  Color @Composable get() = AppPalette.cardBorder
private val TxTextPrimary: Color @Composable get() = AppPalette.textPrimary
private val TxTextMuted:   Color @Composable get() = AppPalette.textMuted
private val TxTint:        Color @Composable get() = AppPalette.cardElevated

// --- Type-based helpers -------------------------------------------------------

/** Get the accent color for a transaction type. */
private fun txTypeColor(type: TransactionType): Color = TransactionTypePresentation.colorForType(type)

/** Get the display label for a transaction type (e.g. "Transfer", "Goal Contribution"). */
private fun txTypeLabel(type: TransactionType): String = TransactionTypePresentation.labelForType(type)

/** Get the amount prefix/sign for display. */
private fun txAmountPrefix(type: TransactionType): String = when (type) {
    TransactionType.INCOME -> "+"
    TransactionType.EXPENSE -> "-"
    else -> ""
}

/** Whether this type should show category info. */
private fun txShowCategory(type: TransactionType): Boolean = TransactionTypePresentation.forType(type).showCategory

/** Whether this type allows editing. */
private fun txAllowsEdit(type: TransactionType): Boolean = TransactionTypePresentation.forType(type).allowsEdit

/** Whether this type allows deletion. */
private fun txAllowsDelete(type: TransactionType): Boolean = TransactionTypePresentation.forType(type).allowsDelete

// --- Enums --------------------------------------------------------------------
enum class FilterType { ALL, INCOME, EXPENSE, TRANSFER, GOAL, AUTO_ALLOC, TODAY, WEEK, MONTH }
enum class SortType   { NEWEST, OLDEST, HIGHEST, LOWEST, CATEGORY }

// --- Transaction group label --------------------------------------------------
private enum class TxGroup { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, OLDER }

private fun getTxGroup(dateMillis: Long): TxGroup {
    val now   = Calendar.getInstance()
    val txCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val diffDays = ((now.timeInMillis - dateMillis) / 86_400_000L).toInt()
    return when {
        now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) -> TxGroup.TODAY
        diffDays == 1 -> TxGroup.YESTERDAY
        diffDays <= 7 -> TxGroup.THIS_WEEK
        now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        now.get(Calendar.MONTH) == txCal.get(Calendar.MONTH) -> TxGroup.THIS_MONTH
        else -> TxGroup.OLDER
    }
}

private fun TxGroup.label(): String = when (this) {
    TxGroup.TODAY      -> "Today"
    TxGroup.YESTERDAY  -> "Yesterday"
    TxGroup.THIS_WEEK  -> "This Week"
    TxGroup.THIS_MONTH -> "This Month"
    TxGroup.OLDER      -> "Earlier"
}

// --- Helpers ------------------------------------------------------------------



private fun formatDateClean(dateMillis: Long): String {
    val date      = Date(dateMillis)
    val today     = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val txCal     = Calendar.getInstance().apply { time = date }
    val timeFmt   = SimpleDateFormat("HH:mm", Locale.getDefault())
    return when {
        today.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) ->
            "Today, ${timeFmt.format(date)}"
        yesterday.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) ->
            "Yesterday, ${timeFmt.format(date)}"
        today.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) ->
            SimpleDateFormat("d MMM", Locale.ENGLISH).format(date)
        else ->
            SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(date)
    }
}

private fun formatFullDate(dateMillis: Long): String =
    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(Date(dateMillis))

private fun formatCurrencyRp(amount: Double): String =
    "Rp " + String.format(Locale.getDefault(), "%,.0f", abs(amount))

// --- Main Screen --------------------------------------------------------------

@Composable
fun TransactionDetailsScreen(
    viewModel: TransactionDetailsViewModel = hiltViewModel(),
    initialTransactions: List<Transaction> = emptyList(),
    accounts: List<Account> = emptyList(),
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit = {},
    onDeleteTransaction: (String) -> Unit = {}
) {
    val dbTransactions by viewModel.transactions.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val categories     by viewModel.categories.collectAsState()
    val allTransactions = dbTransactions.ifEmpty { initialTransactions }

    // Build category icon/color lookup: name.lowercase() ? Category
    val categoryMap = remember(categories) {
        categories.associateBy { it.name.trim().lowercase() }
    }

    // Build account lookup by ID
    val accountMap = remember(accounts) {
        accounts.associateBy { it.id }
    }

    var searchTerm          by remember { mutableStateOf("") }
    var filterType          by remember { mutableStateOf(FilterType.ALL) }
    var sortBy              by remember { mutableStateOf(SortType.NEWEST) }
    // Hold only IDs; resolve the live Transaction from the Room-backed list each recomposition so an
    // open detail/edit sheet reflects saved changes instantly (no need to close and reopen).
    var selectedTransactionId by remember { mutableStateOf<String?>(null) }
    var transactionToEditId   by remember { mutableStateOf<String?>(null) }
    val selectedTransaction = remember(selectedTransactionId, allTransactions) {
        allTransactions.firstOrNull { it.id == selectedTransactionId }
    }
    val transactionToEdit = remember(transactionToEditId, allTransactions) {
        allTransactions.firstOrNull { it.id == transactionToEditId }
    }

    val filtered = remember(allTransactions, searchTerm, filterType, sortBy) {
        val cal = Calendar.getInstance()
        val f = allTransactions.filter { tx ->
            val matchSearch = tx.title.contains(searchTerm, ignoreCase = true) ||
                tx.category.contains(searchTerm, ignoreCase = true) ||
                (tx.description ?: "").contains(searchTerm, ignoreCase = true)
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            val matchFilter = when (filterType) {
                FilterType.INCOME      -> tx.type == TransactionType.INCOME
                FilterType.EXPENSE     -> tx.type == TransactionType.EXPENSE
                FilterType.TRANSFER    -> tx.type == TransactionType.TRANSFER_IN || tx.type == TransactionType.TRANSFER_OUT
                FilterType.GOAL        -> tx.type == TransactionType.GOAL_CONTRIBUTION || tx.type == TransactionType.GOAL_WITHDRAWAL
                FilterType.AUTO_ALLOC  -> tx.type == TransactionType.AUTO_ALLOCATION
                FilterType.TODAY       -> cal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) &&
                                          cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
                FilterType.WEEK        -> tx.date >= Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }.timeInMillis
                FilterType.MONTH       -> tx.date >= Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
                FilterType.ALL         -> true
            }
            matchSearch && matchFilter
        }
        when (sortBy) {
            SortType.NEWEST   -> f.sortedByDescending { it.date }
            SortType.OLDEST   -> f.sortedBy { it.date }
            SortType.HIGHEST  -> f.sortedByDescending { abs(it.amount) }
            SortType.LOWEST   -> f.sortedBy { abs(it.amount) }
            SortType.CATEGORY -> f.sortedBy { it.category }
        }
    }

    val accent = TxAccent
    if (isLoading) {
        Box(Modifier.fillMaxSize().background(TxBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
        }
        return
    }

    PremiumTransactionListView(
        transactions          = filtered,
        allCount              = allTransactions.size,
        categoryMap           = categoryMap,
        accountMap            = accountMap,
        searchTerm            = searchTerm,
        onSearchTermChanged   = { searchTerm = it },
        filterType            = filterType,
        onFilterTypeChanged   = { filterType = it },
        sortBy                = sortBy,
        onSortByChanged       = { sortBy = it },
        onTransactionSelected = { selectedTransactionId = it.id },
        onBack                = onBack
    )

    selectedTransaction?.let { tx ->
        TransactionDetailOverlay(
            transaction = tx,
            categoryMap = categoryMap,
            accountMap = accountMap,
            onDismiss   = { selectedTransactionId = null },
            onEdit      = { transactionToEditId = it.id },
            onDelete    = { id ->
                onDeleteTransaction(id)
                selectedTransactionId = null
            }
        )
    }

    transactionToEdit?.let { tx ->
        EditTransactionDetail(
            transaction  = tx,
            categoryMap  = categoryMap,
            categories   = categories,
            accounts     = accounts,
            onDismiss    = { transactionToEditId = null },
            onSave       = { updated ->
                // Persist immediately; Room re-emits ? list AND any still-open sheet update live.
                onEditTransaction(updated)
                transactionToEditId = null
            }
        )
    }
}

// --- Premium List View --------------------------------------------------------

@Composable
fun PremiumTransactionListView(
    transactions: List<Transaction>,
    allCount: Int,
    categoryMap: Map<String, com.example.insightku.core.data.model.Category> = emptyMap(),
    accountMap: Map<String, com.example.insightku.core.data.model.Account> = emptyMap(),
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
            .background(TxBackground)   // AppPalette.background — adapts to dark mode
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
                    contentDescription = "Back",
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    "All Transactions",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = TxTextPrimary
                )
                Text(
                    "Track every mindful financial movement",
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
                label  = "Income",
                amount = income,
                color  = TxIncomeGreen,
                bg     = TxIncomeGreen.copy(alpha = 0.10f),
                icon   = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            TxSummaryCard(
                label  = "Expenses",
                amount = expense,
                color  = TxExpenseRed,
                bg     = TxExpenseRed.copy(alpha = 0.10f),
                icon   = Icons.AutoMirrored.Filled.TrendingDown,
                modifier = Modifier.weight(1f)
            )
            TxSummaryCard(
                label  = "Total",
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
                "Search transactions…",
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
    val filters = listOf(
        FilterType.ALL         to "All",
        FilterType.INCOME      to "Income",
        FilterType.EXPENSE     to "Expense",
        FilterType.TRANSFER    to "Transfer",
        FilterType.GOAL        to "Goals",
        FilterType.AUTO_ALLOC  to "Auto",
        FilterType.TODAY       to "Today",
        FilterType.WEEK        to "This Week",
        FilterType.MONTH       to "This Month"
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
                            SortType.NEWEST   -> "Newest"
                            SortType.OLDEST   -> "Oldest"
                            SortType.HIGHEST  -> "Highest"
                            SortType.LOWEST   -> "Lowest"
                            SortType.CATEGORY -> "Category"
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
                    SortType.NEWEST   to "Newest First",
                    SortType.OLDEST   to "Oldest First",
                    SortType.HIGHEST  to "Highest Amount",
                    SortType.LOWEST   to "Lowest Amount",
                    SortType.CATEGORY to "By Category"
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
    categoryMap: Map<String, com.example.insightku.core.data.model.Category> = emptyMap(),
    accountMap: Map<String, com.example.insightku.core.data.model.Account> = emptyMap(),
    modifier: Modifier = Modifier
) {
    // Resolve category color for badge (icon is handled by shared TransactionCategoryIcon)
    val resolved = resolveCategoryIcon(transaction.category, transaction.type, categoryMap)
    val catColor = resolved.color
    val amountColor = txTypeColor(transaction.type)
    val prefix      = txAmountPrefix(transaction.type)
    val showCat     = txShowCategory(transaction.type)

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
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
            TransactionCategoryIcon(
                categoryName = transaction.category,
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
                        if (transaction.isSynced) "Synced" else "Syncing",
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
                Icons.Outlined.ReceiptLong, null,
                tint     = accent.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            "No transactions found",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = TxTextPrimary,
            textAlign  = TextAlign.Center
        )
        Text(
            "Try adjusting your search or filter\nto find what you're looking for.",
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

// --- Premium Transaction Detail Overlay --------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailOverlay(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: ((Transaction) -> Unit)? = null,
    categoryMap: Map<String, com.example.insightku.core.data.model.Category> = emptyMap(),
    accountMap: Map<String, com.example.insightku.core.data.model.Account> = emptyMap()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val txType      = transaction.type
    val amountColor = txTypeColor(txType)
    val typeLabel   = txTypeLabel(txType)
    val prefix      = txAmountPrefix(txType)
    val showCat     = txShowCategory(txType)
    val canEdit     = txAllowsEdit(txType)
    val canDelete   = txAllowsDelete(txType)

    // Resolve category color for badge (icon is handled by shared TransactionCategoryIcon)
    val resolved = resolveCategoryIcon(transaction.category, transaction.type, categoryMap)
    val catColor = resolved.color

    val accent = TxAccent
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = TxCard,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(TxCardBorder))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // -- Hero section ----------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TransactionCategoryIcon(
                    categoryName = transaction.category,
                    transactionType = transaction.type,
                    categoryMap = categoryMap,
                    containerSize = 68.dp,
                    iconSize = 32.dp,
                    cornerRadius = 20.dp,
                    borderColor = catColor.copy(alpha = 0.2f),
                    borderWidth = 1.dp
                )
                Text(transaction.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TxTextPrimary, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Category badge (only for Income/Expense)
                    if (showCat && transaction.category.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(50.dp), color = catColor.copy(alpha = 0.10f)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(catColor))
                                Text(transaction.category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = catColor)
                            }
                        }
                    }
                    // Type badge (always shown)
                    Surface(shape = RoundedCornerShape(8.dp), color = amountColor.copy(alpha = 0.10f)) {
                        Text(typeLabel.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = amountColor, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                Text("$prefix ${formatCurrencyRp(transaction.amount)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = amountColor, letterSpacing = (-0.5).sp)
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(TxCardBorder))

            // -- Info grid -------------------------------------------------
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumInfoTile(Icons.Default.CalendarMonth, "Date", formatFullDate(transaction.date), accent, Modifier.weight(1f))
                    PremiumInfoTile(Icons.Default.AccessTime, "Time", transaction.time, accent, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Account info
                    val account = accountMap[transaction.accountId]
                    val accountIcon = when (account?.type) {
                        AccountType.CASH -> Icons.Default.Payments
                        AccountType.BANK_ACCOUNT -> Icons.Default.AccountBalance
                        AccountType.E_WALLET -> Icons.Default.AccountBalanceWallet
                        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                        null -> Icons.Default.AccountBalance
                    }
                    val accountColor = account?.let { runCatching { Color(android.graphics.Color.parseColor(it.color)) }.getOrDefault(AppPalette.defaultBlue) } ?: AppPalette.defaultBlue
                    PremiumInfoTile(accountIcon, "Account", account?.name ?: "No account", accountColor, Modifier.weight(1f))
                    // Sync status
                    PremiumInfoTile(
                        if (transaction.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        "Status", if (transaction.isSynced) "Synced" else "Syncing",
                        if (transaction.isSynced) TxIncomeGreen else TxAdjustOrange, Modifier.weight(1f)
                    )
                }
                // Transfer info: show related account
                if (txType == TransactionType.TRANSFER_OUT || txType == TransactionType.TRANSFER_IN) {
                    transaction.relatedAccountId?.let { relatedId ->
                        val relatedAccount = accountMap[relatedId]
                        val label = if (txType == TransactionType.TRANSFER_OUT) "To Account" else "From Account"
                        PremiumInfoTile(
                            Icons.Default.SwapHoriz, label,
                            relatedAccount?.name ?: relatedId,
                            TxTransferBlue, Modifier.fillMaxWidth()
                        )
                    }
                }
                // Goal info: show goal name
                if (txType == TransactionType.GOAL_CONTRIBUTION || txType == TransactionType.GOAL_WITHDRAWAL || txType == TransactionType.AUTO_ALLOCATION) {
                    transaction.goalName?.let { goalName ->
                        val goalIcon = if (txType == TransactionType.GOAL_WITHDRAWAL) Icons.Default.ArrowUpward else Icons.Default.Flag
                        val goalColor = if (txType == TransactionType.GOAL_WITHDRAWAL) TxWithdrawalTeal else TxGoalPurple
                        PremiumInfoTile(goalIcon, "Goal", goalName, goalColor, Modifier.fillMaxWidth())
                    }
                }
                // Contribution type info for Goal Contribution
                if (txType == TransactionType.GOAL_CONTRIBUTION) {
                    val contribType = if (transaction.isAuto) "Auto Allocation" else "Manual"
                    PremiumInfoTile(
                        if (transaction.isAuto) Icons.Default.AutoAwesome else Icons.Default.TouchApp,
                        "Contribution Type", contribType,
                        TxGoalPurple, Modifier.fillMaxWidth()
                    )
                }
                // Allocation rule info for Auto Allocation
                if (txType == TransactionType.AUTO_ALLOCATION) {
                    transaction.referenceId?.let { ruleId ->
                        PremiumInfoTile(
                            Icons.Default.AutoAwesome, "Allocation Rule", ruleId,
                            TxAutoAllocIndigo, Modifier.fillMaxWidth()
                        )
                    }
                }
                if (!transaction.description.isNullOrBlank()) PremiumInfoTileWide(Icons.Default.Notes, "Notes", transaction.description, AppPalette.notesPurple)
                if (!transaction.location.isNullOrBlank()) PremiumInfoTileWide(Icons.Default.LocationOn, "Location", transaction.location, AppPalette.locationPink)
            }

            // -- Action buttons (type-dependent) --------------------------------
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Edit + Delete for editable types (Income, Expense, Balance Adjustment)
                if (canEdit) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(Modifier.weight(1f).height(50.dp).clickable { onEdit(transaction) }, RoundedCornerShape(16.dp), TxTint, border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Edit, null, tint = accent, modifier = Modifier.size(16.dp))
                                    Text("Edit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = accent)
                                }
                            }
                        }
                        Surface(Modifier.weight(1f).height(50.dp).clickable { showDeleteDialog = true }, RoundedCornerShape(16.dp), TxExpenseRed.copy(alpha = 0.10f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = TxExpenseRed, modifier = Modifier.size(16.dp))
                                    Text("Delete", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TxExpenseRed)
                                }
                            }
                        }
                    }
                }
                // Goal Contribution/Withdrawal: View Goal button
                if (txType == TransactionType.GOAL_CONTRIBUTION || txType == TransactionType.GOAL_WITHDRAWAL || txType == TransactionType.AUTO_ALLOCATION) {
                    Surface(Modifier.fillMaxWidth().height(50.dp).clickable { onDismiss() }, RoundedCornerShape(16.dp), TxTint, border = androidx.compose.foundation.BorderStroke(1.dp, TxGoalPurple.copy(alpha = 0.3f))) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Flag, null, tint = TxGoalPurple, modifier = Modifier.size(16.dp))
                                Text("View Goal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TxGoalPurple)
                            }
                        }
                    }
                }
                // Transfer: View Transfer Details button
                if (txType == TransactionType.TRANSFER_OUT || txType == TransactionType.TRANSFER_IN) {
                    Surface(Modifier.fillMaxWidth().height(50.dp).clickable { onDismiss() }, RoundedCornerShape(16.dp), TxTint, border = androidx.compose.foundation.BorderStroke(1.dp, TxTransferBlue.copy(alpha = 0.3f))) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.SwapHoriz, null, tint = TxTransferBlue, modifier = Modifier.size(16.dp))
                                Text("View Transfer Details", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TxTransferBlue)
                            }
                        }
                    }
                }
                // Duplicate only for editable types
                if (canEdit && onDuplicate != null) {
                    Surface(
                        Modifier.fillMaxWidth().height(46.dp).clickable {
                            onDuplicate(transaction.copy(id = java.util.UUID.randomUUID().toString(), date = System.currentTimeMillis()))
                            onDismiss()
                        }, RoundedCornerShape(16.dp), TxTint,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TxCardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ContentCopy, null, tint = accent, modifier = Modifier.size(15.dp))
                                Text("Duplicate Transaction", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = accent)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        com.example.insightku.core.ui.components.dialogs.PremiumDialog(
            type = com.example.insightku.core.ui.components.dialogs.PremiumDialogType.ERROR,
            customIcon = Icons.Default.DeleteForever,
            title = "Delete Transaction?",
            message = "\"${transaction.title}\" will be permanently deleted.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                showDeleteDialog = false
                onDelete(transaction.id)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// --- Premium Info Tiles -------------------------------------------------------

@Composable
private fun PremiumInfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        color     = color.copy(alpha = 0.06f),
        border    = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
            }
            Text(
                value,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = TxTextPrimary,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PremiumInfoTileWide(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = color.copy(alpha = 0.06f),
        border   = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TxTextPrimary)
            }
        }
    }
}








