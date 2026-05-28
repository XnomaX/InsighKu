package com.example.insightku.ui.components.details

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import com.example.insightku.ui.dialogs.CategoryIconResolver
import com.example.insightku.ui.components.common.PremiumDatePicker
import com.example.insightku.utils.TimeUtils
import com.example.insightku.viewmodel.TransactionDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ─── Design tokens ────────────────────────────────────────────────────────────
private val TxIncomeGreen  = Color(0xFF10B981)
private val TxExpenseRed   = Color(0xFFEF4444)
private val GlassBorder    = Color(0xFFE8DDFF)
private val PurpleDark     = Color(0xFF2D0A5E)
private val PurpleMid      = Color(0xFF5A2A82)
private val PurpleViolet   = Color(0xFF7C3AED)
private val PurpleLavender = Color(0xFFAB8FD4)
private val PurpleTint     = Color(0xFFEDE9FE)
private val IncomeDeep     = Color(0xFF064E3B)
private val IncomeMid      = Color(0xFF065F46)
private val GlassSurface   = Color(0xFFFAF8FF)
private val TxBackground   = Color(0xFFFAF9FE)

// ─── Enums ────────────────────────────────────────────────────────────────────
enum class FilterType { ALL, INCOME, EXPENSE, RECURRING, INSTALLMENT, TODAY, WEEK, MONTH }
enum class SortType   { NEWEST, OLDEST, HIGHEST, LOWEST, CATEGORY }

// ─── Transaction group label ──────────────────────────────────────────────────
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

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun getCategoryIcon(category: String): ImageVector =
    CategoryIconResolver.resolveIcon(category)

private fun getCategoryColor(category: String): Color =
    CategoryIconResolver.resolveColor(category)

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

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun TransactionDetailsScreen(
    viewModel: TransactionDetailsViewModel = hiltViewModel(),
    initialTransactions: List<Transaction> = emptyList(),
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit = {},
    onDeleteTransaction: (String) -> Unit = {}
) {
    val dbTransactions by viewModel.transactions.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val categories     by viewModel.categories.collectAsState()
    val allTransactions = dbTransactions.ifEmpty { initialTransactions }

    // Build category icon/color lookup: name.lowercase() → Category
    val categoryMap = remember(categories) {
        categories.associateBy { it.name.trim().lowercase() }
    }

    var searchTerm          by remember { mutableStateOf("") }
    var filterType          by remember { mutableStateOf(FilterType.ALL) }
    var sortBy              by remember { mutableStateOf(SortType.NEWEST) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToEdit   by remember { mutableStateOf<Transaction?>(null) }

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
                FilterType.RECURRING   -> tx.description?.contains("recurring", ignoreCase = true) == true
                FilterType.INSTALLMENT -> tx.description?.contains("installment", ignoreCase = true) == true
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

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(TxBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PurpleViolet, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
        }
        return
    }

    PremiumTransactionListView(
        transactions          = filtered,
        allCount              = allTransactions.size,
        categoryMap           = categoryMap,
        searchTerm            = searchTerm,
        onSearchTermChanged   = { searchTerm = it },
        filterType            = filterType,
        onFilterTypeChanged   = { filterType = it },
        sortBy                = sortBy,
        onSortByChanged       = { sortBy = it },
        onTransactionSelected = { selectedTransaction = it },
        onBack                = onBack
    )

    selectedTransaction?.let { tx ->
        TransactionDetailOverlay(
            transaction = tx,
            categoryMap = categoryMap,
            onDismiss   = { selectedTransaction = null },
            onEdit      = { transactionToEdit = it },
            onDelete    = { id ->
                onDeleteTransaction(id)
                selectedTransaction = null
            }
        )
    }

    transactionToEdit?.let { tx ->
        EditTransactionDetail(
            transaction  = tx,
            categoryMap  = categoryMap,
            categories   = categories,
            onDismiss    = { transactionToEdit = null },
            onSave       = { updated ->
                onEditTransaction(updated)
                transactionToEdit = null
            }
        )
    }
}

// ─── Premium List View ────────────────────────────────────────────────────────

@Composable
fun PremiumTransactionListView(
    transactions: List<Transaction>,
    allCount: Int,
    categoryMap: Map<String, com.example.insightku.data.model.Category> = emptyMap(),
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
                            onClick     = { onTransactionSelected(tx) },
                            modifier    = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Premium Header ───────────────────────────────────────────────────────────

@Composable
private fun PremiumTxHeader(
    onBack: () -> Unit,
    income: Double,
    expense: Double,
    total: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
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
                    .background(Color(0xFFF5F3FF))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PurpleViolet,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    "All Transactions",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1A1A2E)
                )
                Text(
                    "Track every mindful financial movement",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
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
                bg     = Color(0xFFECFDF5),
                icon   = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            TxSummaryCard(
                label  = "Expenses",
                amount = expense,
                color  = TxExpenseRed,
                bg     = Color(0xFFFFF1F2),
                icon   = Icons.AutoMirrored.Filled.TrendingDown,
                modifier = Modifier.weight(1f)
            )
            TxSummaryCard(
                label  = "Total",
                amount = null,
                count  = total,
                color  = PurpleViolet,
                bg     = PurpleTint,
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

// ─── Premium Search Bar ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSearchBar(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        placeholder   = {
            Text(
                "Search transactions…",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0AABF)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search, null,
                tint     = Color(0xFFB0AABF),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color(0xFFB0AABF))
                }
            }
        },
        singleLine = true,
        shape      = RoundedCornerShape(16.dp),
        modifier   = Modifier.fillMaxWidth(),
        colors     = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = PurpleViolet.copy(alpha = 0.5f),
            unfocusedBorderColor    = Color(0xFFECE7F6),
            unfocusedContainerColor = Color.White,
            focusedContainerColor   = Color.White,
            cursorColor             = PurpleViolet
        )
    )
}

// ─── Premium Filter Row ───────────────────────────────────────────────────────

@Composable
private fun PremiumFilterRow(
    filterType: FilterType,
    onFilterTypeChanged: (FilterType) -> Unit
) {
    val filters = listOf(
        FilterType.ALL         to "All",
        FilterType.EXPENSE     to "Expense",
        FilterType.INCOME      to "Income",
        FilterType.RECURRING   to "Recurring",
        FilterType.INSTALLMENT to "Installments",
        FilterType.TODAY       to "Today",
        FilterType.WEEK        to "This Week",
        FilterType.MONTH       to "This Month"
    )

    Row(
        modifier              = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (type, label) ->
            val selected = filterType == type
            Surface(
                modifier = Modifier.clickable { onFilterTypeChanged(type) },
                shape    = RoundedCornerShape(50.dp),
                color    = if (selected) PurpleViolet else Color.White,
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) PurpleViolet else Color(0xFFECE7F6)
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
                        color      = if (selected) Color.White else Color(0xFF6B6B8A)
                    )
                }
            }
        }
    }
}

// ─── Sort Row ─────────────────────────────────────────────────────────────────

@Composable
private fun PremiumSortRow(
    sortBy: SortType,
    onSortByChanged: (SortType) -> Unit,
    count: Int
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            "$count transaction${if (count != 1) "s" else ""}",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9E9E9E)
        )
        Box {
            Surface(
                onClick = { expanded = true },
                shape   = RoundedCornerShape(10.dp),
                color   = PurpleViolet.copy(alpha = 0.06f),
                border  = androidx.compose.foundation.BorderStroke(1.dp, PurpleViolet.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, null, tint = PurpleViolet, modifier = Modifier.size(14.dp))
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
                        color      = PurpleViolet
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = PurpleViolet, modifier = Modifier.size(14.dp)
                    )
                }
            }
            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
                modifier         = Modifier.background(Color.White, RoundedCornerShape(14.dp))
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
                                color      = if (sortBy == type) PurpleViolet else Color(0xFF1A1A2E)
                            )
                        },
                        onClick      = { onSortByChanged(type); expanded = false },
                        leadingIcon  = {
                            if (sortBy == type) Icon(Icons.Default.Check, null, tint = PurpleViolet, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}

// ─── Group Header ─────────────────────────────────────────────────────────────

@Composable
fun TxGroupHeader(label: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF1A1A2E)
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = PurpleTint
        ) {
            Text(
                "$count",
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color      = PurpleViolet
            )
        }
    }
}

// ─── Premium Transaction Card ─────────────────────────────────────────────────

@Composable
fun PremiumTransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    categoryMap: Map<String, com.example.insightku.data.model.Category> = emptyMap(),
    modifier: Modifier = Modifier
) {
    // Resolve icon: try category.icon field first (most accurate), fall back to name fuzzy match
    val matchedCat  = categoryMap[transaction.category.trim().lowercase()]
    val iconKey     = matchedCat?.icon?.ifBlank { null } ?: transaction.category
    val resolvedByIcon = CategoryIconResolver.resolve(iconKey)
    val resolvedByName = CategoryIconResolver.resolve(transaction.category)
    val resolved    = if (resolvedByIcon.name != "Others") resolvedByIcon else resolvedByName
    val catColor    = if (!matchedCat?.color.isNullOrBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(matchedCat!!.color)) }.getOrDefault(resolved.color)
    } else resolved.color
    val catIcon     = resolved.icon
    val isIncome    = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) TxIncomeGreen else TxExpenseRed
    val prefix      = if (isIncome) "+" else "-"

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
        color         = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border        = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECE7F6)),
        onClick       = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(catIcon, null, tint = catColor, modifier = Modifier.size(22.dp))
            }

            // Title + category + time
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    transaction.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1A1A2E),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(6.dp)
                ) {
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
                    Text(
                        "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0AABF)
                    )
                    Text(
                        TimeUtils.toShortRelativeTime(transaction.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0AABF)
                    )
                }
                if (!transaction.paymentMethod.isNullOrBlank()) {
                    Text(
                        transaction.paymentMethod,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0AABF)
                    )
                }
            }

            // Amount + type indicator
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
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = amountColor.copy(alpha = 0.10f)
                ) {
                    Text(
                        if (isIncome) "IN" else "OUT",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = amountColor,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ─── Premium Empty State ──────────────────────────────────────────────────────

@Composable
private fun PremiumEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(PurpleTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ReceiptLong, null,
                tint     = PurpleViolet.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            "No transactions found",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF1A1A2E),
            textAlign  = TextAlign.Center
        )
        Text(
            "Try adjusting your search or filter\nto find what you're looking for.",
            style     = MaterialTheme.typography.bodySmall,
            color     = Color(0xFF9E9E9E),
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

// ─── Premium Transaction Detail Overlay ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailOverlay(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: ((Transaction) -> Unit)? = null,
    categoryMap: Map<String, com.example.insightku.data.model.Category> = emptyMap()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isIncome    = transaction.type == TransactionType.INCOME
    // Resolve icon: try category.icon field first, fall back to fuzzy name match
    val matchedCat  = categoryMap[transaction.category.trim().lowercase()]
    val iconKey     = matchedCat?.icon?.ifBlank { null } ?: transaction.category
    val resolvedByIcon = CategoryIconResolver.resolve(iconKey)
    val resolvedByName = CategoryIconResolver.resolve(transaction.category)
    val resolved    = if (resolvedByIcon.name != "Others") resolvedByIcon else resolvedByName
    val catColor    = if (!matchedCat?.color.isNullOrBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(matchedCat!!.color)) }.getOrDefault(resolved.color)
    } else resolved.color
    val catIcon     = resolved.icon
    val amountColor = if (isIncome) TxIncomeGreen else TxExpenseRed
    val prefix      = if (isIncome) "+" else "-"

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
                .navigationBarsPadding()
        ) {
            // ── Hero section ──────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(68.dp).clip(RoundedCornerShape(20.dp))
                        .background(catColor.copy(alpha = 0.12f))
                        .border(1.dp, catColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(catIcon, null, tint = catColor, modifier = Modifier.size(32.dp))
                }
                Text(transaction.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E), textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50.dp), color = catColor.copy(alpha = 0.10f)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(catColor))
                            Text(transaction.category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = catColor)
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = amountColor.copy(alpha = 0.10f)) {
                        Text(if (isIncome) "INCOME" else "EXPENSE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = amountColor, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                Text("$prefix ${formatCurrencyRp(transaction.amount)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = amountColor, letterSpacing = (-0.5).sp)
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0EBF8)))

            // ── Info grid ─────────────────────────────────────────────────
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumInfoTile(Icons.Default.CalendarMonth, "Date", formatFullDate(transaction.date), PurpleViolet, Modifier.weight(1f))
                    PremiumInfoTile(Icons.Default.AccessTime, "Time", transaction.time, PurpleViolet, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumInfoTile(Icons.Default.CreditCard, "Payment", transaction.paymentMethod ?: "Not specified", Color(0xFF3B82F6), Modifier.weight(1f))
                    PremiumInfoTile(
                        if (transaction.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        "Status", if (transaction.isSynced) "Synced" else "Pending",
                        if (transaction.isSynced) TxIncomeGreen else Color(0xFFF59E0B), Modifier.weight(1f)
                    )
                }
                if (!transaction.description.isNullOrBlank()) PremiumInfoTileWide(Icons.Default.Notes, "Notes", transaction.description, Color(0xFF8B5CF6))
                if (!transaction.location.isNullOrBlank()) PremiumInfoTileWide(Icons.Default.LocationOn, "Location", transaction.location, Color(0xFFEC4899))
            }

            // ── Action buttons ────────────────────────────────────────────
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(Modifier.weight(1f).height(50.dp).clickable { onEdit(transaction) }, RoundedCornerShape(16.dp), PurpleTint, border = androidx.compose.foundation.BorderStroke(1.dp, PurpleViolet.copy(alpha = 0.3f))) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Edit, null, tint = PurpleViolet, modifier = Modifier.size(16.dp))
                                Text("Edit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PurpleViolet)
                            }
                        }
                    }
                    Surface(Modifier.weight(1f).height(50.dp).clickable { showDeleteDialog = true }, RoundedCornerShape(16.dp), Color(0xFFFFF1F2)) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Delete, null, tint = TxExpenseRed, modifier = Modifier.size(16.dp))
                                Text("Delete", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TxExpenseRed)
                            }
                        }
                    }
                }
                if (onDuplicate != null) {
                    Surface(
                        Modifier.fillMaxWidth().height(46.dp).clickable {
                            onDuplicate(transaction.copy(id = java.util.UUID.randomUUID().toString(), date = System.currentTimeMillis()))
                            onDismiss()
                        }, RoundedCornerShape(16.dp), Color(0xFFF5F3FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECE7F6))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(15.dp))
                                Text("Duplicate Transaction", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF7C4DFF))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Box(Modifier.size(48.dp).clip(CircleShape).background(TxExpenseRed.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DeleteForever, null, tint = TxExpenseRed, modifier = Modifier.size(26.dp))
                }
            },
            title = { Text("Delete Transaction?", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge) },
            text  = { Text("\"${transaction.title}\" will be permanently deleted.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(TxExpenseRed)
                        .pointerInput(Unit) { detectTapGestures { showDeleteDialog = false; onDelete(transaction.id) } }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) { Text("Delete", fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) { detectTapGestures { showDeleteDialog = false } }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) { Text("Cancel", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            shape = RoundedCornerShape(24.dp), containerColor = GlassSurface
        )
    }
}

// ─── Premium Info Tiles ───────────────────────────────────────────────────────

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
                color      = Color(0xFF1A1A2E),
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
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A2E))
            }
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private val SAMPLE_TRANSACTIONS = listOf(
    Transaction(id = "1", title = "Monthly Salary", amount = 8500000.0, category = "Salary",
        type = TransactionType.INCOME, date = System.currentTimeMillis() - 3_600_000L,
        description = "April salary", paymentMethod = "Bank Transfer"),
    Transaction(id = "2", title = "Starbucks Coffee", amount = 65000.0, category = "Food & Drinks",
        type = TransactionType.EXPENSE, date = System.currentTimeMillis() - 7_200_000L,
        description = "Iced latte", location = "Starbucks Sudirman", paymentMethod = "GoPay"),
    Transaction(id = "3", title = "Freelance Project", amount = 2500000.0, category = "Freelance",
        type = TransactionType.INCOME, date = System.currentTimeMillis() - 86_400_000L * 2),
    Transaction(id = "4", title = "Netflix", amount = 54000.0, category = "Entertainment",
        type = TransactionType.EXPENSE, date = System.currentTimeMillis() - 86_400_000L * 5,
        paymentMethod = "Credit Card"),
    Transaction(id = "5", title = "Grab Ride", amount = 32000.0, category = "Transportation",
        type = TransactionType.EXPENSE, date = System.currentTimeMillis() - 86_400_000L * 10)
)

@Preview(showBackground = true, name = "Transaction List")
@Composable
fun TransactionDetailsScreenListPreview() {
    MaterialTheme {
        TransactionDetailsScreen(initialTransactions = SAMPLE_TRANSACTIONS, onBack = {})
    }
}

@Preview(showBackground = true, name = "Detail Overlay - Expense", widthDp = 400, heightDp = 800)
@Composable
private fun PreviewDetailOverlayExpense() {
    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            TransactionDetailOverlay(
                transaction = SAMPLE_TRANSACTIONS[1],
                onDismiss   = {},
                onEdit      = {},
                onDelete    = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Detail Overlay - Income", widthDp = 400, heightDp = 800)
@Composable
private fun PreviewDetailOverlayIncome() {
    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            TransactionDetailOverlay(
                transaction = SAMPLE_TRANSACTIONS[0],
                onDismiss   = {},
                onEdit      = {},
                onDelete    = {}
            )
        }
    }
}
