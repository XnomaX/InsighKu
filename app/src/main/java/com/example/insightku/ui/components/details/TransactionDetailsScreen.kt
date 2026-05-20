package com.example.insightku.ui.components.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.insightku.utils.TimeUtils
import com.example.insightku.viewmodel.TransactionDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ─── Design tokens ────────────────────────────────────────────────────────────
private val TxIncomeGreen = Color(0xFF10B981)
private val TxExpenseRed  = Color(0xFFEF4444)
private val GlassBorder   = Color(0xFFE8DDFF)

// ─── Enums ────────────────────────────────────────────────────────────────────
enum class FilterType { ALL, INCOME, EXPENSE, TODAY, WEEK, MONTH, YEAR }
enum class SortType   { NEWEST, OLDEST, HIGHEST, LOWEST, CATEGORY }

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun getCategoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "food & dining", "food", "food & drinks"    -> Icons.Default.Fastfood
    "transportation"                             -> Icons.Default.DirectionsCar
    "shopping"                                   -> Icons.Default.ShoppingBag
    "entertainment"                              -> Icons.Default.Movie
    "healthcare", "health"                       -> Icons.Default.HealthAndSafety
    "utilities", "bills", "housing"              -> Icons.Default.Home
    "salary"                                     -> Icons.Default.AccountBalance
    "freelance", "work"                          -> Icons.Default.Work
    "investment"                                 -> Icons.AutoMirrored.Filled.TrendingUp
    "business"                                   -> Icons.Default.BusinessCenter
    "gift"                                       -> Icons.Default.CardGiftcard
    "education"                                  -> Icons.Default.School
    "others"                                     -> Icons.Default.Category
    else                                         -> Icons.Default.AttachMoney
}

private fun getCategoryColor(category: String): Color = when (category.lowercase()) {
    "food & dining", "food", "food & drinks"    -> Color(0xFFF59E0B)
    "transportation"                             -> Color(0xFF3B82F6)
    "shopping"                                   -> Color(0xFFEC4899)
    "entertainment"                              -> Color(0xFF8B5CF6)
    "healthcare", "health"                       -> Color(0xFF10B981)
    "utilities", "bills", "housing"              -> Color(0xFFEF4444)
    "salary", "freelance", "work", "business"   -> Color(0xFF10B981)
    "investment"                                 -> Color(0xFF06B6D4)
    "gift"                                       -> Color(0xFFF59E0B)
    "education"                                  -> Color(0xFF8B5CF6)
    else                                         -> Color(0xFF6B7280)
}

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
    val allTransactions = dbTransactions.ifEmpty { initialTransactions }

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
                FilterType.INCOME  -> tx.type == TransactionType.INCOME
                FilterType.EXPENSE -> tx.type == TransactionType.EXPENSE
                FilterType.TODAY   -> cal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) &&
                                      cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
                FilterType.WEEK    -> tx.date >= Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }.timeInMillis
                FilterType.MONTH   -> tx.date >= Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
                FilterType.YEAR    -> tx.date >= Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
                FilterType.ALL     -> true
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // List view — always rendered
    TransactionListView(
        transactions          = filtered,
        searchTerm            = searchTerm,
        onSearchTermChanged   = { searchTerm = it },
        filterType            = filterType,
        onFilterTypeChanged   = { filterType = it },
        sortBy                = sortBy,
        onSortByChanged       = { sortBy = it },
        onTransactionSelected = { selectedTransaction = it },
        onBack                = onBack
    )

    // Detail overlay — rendered on top when transaction selected
    selectedTransaction?.let { tx ->
        TransactionDetailOverlay(
            transaction = tx,
            onDismiss   = { selectedTransaction = null },
            onEdit      = { transactionToEdit = it },
            onDelete    = { id ->
                onDeleteTransaction(id)
                selectedTransaction = null
            }
        )
    }

    // Edit overlay
    transactionToEdit?.let { tx ->
        EditTransactionDetail(
            transaction = tx,
            onDismiss   = { transactionToEdit = null },
            onSave      = { updated ->
                onEditTransaction(updated)
                transactionToEdit = null
            }
        )
    }
}

// ─── List View ────────────────────────────────────────────────────────────────

@Composable
fun TransactionListView(
    transactions: List<Transaction>,
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
    val netAmount    = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TxListHeader(onBack = onBack, income = totalIncome, expense = totalExpense, net = netAmount)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                TxFilterControls(
                    searchTerm          = searchTerm,
                    onSearchTermChanged = onSearchTermChanged,
                    filterType          = filterType,
                    onFilterTypeChanged = onFilterTypeChanged,
                    sortBy              = sortBy,
                    onSortByChanged     = onSortByChanged
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "${transactions.size} transaction${if (transactions.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }

            if (transactions.isEmpty()) {
                item { TxEmptyState() }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionListItem(transaction = tx, onClick = { onTransactionSelected(tx) })
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ─── Gradient Header ──────────────────────────────────────────────────────────

@Composable
private fun TxListHeader(onBack: () -> Unit, income: Double, expense: Double, net: Double) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.85f))))
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column {
                Text(
                    "All Transactions",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    "Your complete financial history",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TxStatCard("Income",  income,  Icons.AutoMirrored.Filled.TrendingUp,   Color(0xFF4ADE80), Modifier.weight(1f))
            TxStatCard("Expense", expense, Icons.AutoMirrored.Filled.TrendingDown, Color(0xFFF87171), Modifier.weight(1f))
            TxStatCard(
                label       = "Net",
                amount      = net,
                icon        = Icons.Default.AccountBalance,
                amountColor = if (net >= 0) Color(0xFFC6F6D5) else Color(0xFFFED7D7),
                modifier    = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TxStatCard(
    label: String,
    amount: Double,
    icon: ImageVector,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
            }
            Spacer(Modifier.height(3.dp))
            Text(
                formatCurrencyRp(amount),
                style      = MaterialTheme.typography.labelMedium,
                color      = amountColor,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1
            )
        }
    }
}

// ─── Filter Controls ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxFilterControls(
    searchTerm: String,
    onSearchTermChanged: (String) -> Unit,
    filterType: FilterType,
    onFilterTypeChanged: (FilterType) -> Unit,
    sortBy: SortType,
    onSortByChanged: (SortType) -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value         = searchTerm,
            onValueChange = onSearchTermChanged,
            placeholder   = { Text("Search transactions…", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon  = {
                if (searchTerm.isNotEmpty()) {
                    IconButton(onClick = { onSearchTermChanged("") }) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape      = RoundedCornerShape(16.dp),
            modifier   = Modifier.fillMaxWidth(),
            colors     = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = primary,
                unfocusedBorderColor    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                cursorColor             = primary
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            listOf(
                FilterType.ALL     to "All",
                FilterType.INCOME  to "Income",
                FilterType.EXPENSE to "Expense",
                FilterType.TODAY   to "Today",
                FilterType.WEEK    to "This Week",
                FilterType.MONTH   to "This Month",
                FilterType.YEAR    to "This Year"
            ).forEach { (type, label) ->
                val selected = filterType == type
                FilterChip(
                    selected = selected,
                    onClick  = { onFilterTypeChanged(type) },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor   = primary,
                        selectedLabelColor       = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Text("Sort:", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Box {
                Surface(
                    onClick = { sortExpanded = true },
                    shape   = RoundedCornerShape(10.dp),
                    color   = primary.copy(alpha = 0.08f),
                    border  = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (sortBy) {
                                SortType.NEWEST   -> "Newest"
                                SortType.OLDEST   -> "Oldest"
                                SortType.HIGHEST  -> "Highest"
                                SortType.LOWEST   -> "Lowest"
                                SortType.CATEGORY -> "Category"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = primary, maxLines = 1
                        )
                        Icon(
                            if (sortExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null, tint = primary, modifier = Modifier.size(16.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false },
                    modifier = Modifier.widthIn(min = 180.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                ) {
                    listOf(
                        SortType.NEWEST   to "Newest First",
                        SortType.OLDEST   to "Oldest First",
                        SortType.HIGHEST  to "Highest Amount",
                        SortType.LOWEST   to "Lowest Amount",
                        SortType.CATEGORY to "By Category"
                    ).forEachIndexed { index, (type, label) ->
                        DropdownMenuItem(
                            text = {
                                Text(label, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (sortBy == type) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (sortBy == type) primary else MaterialTheme.colorScheme.onSurface)
                            },
                            onClick = { onSortByChanged(type); sortExpanded = false },
                            leadingIcon = {
                                if (sortBy == type) Icon(Icons.Default.Check, null,
                                    tint = primary, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Transaction List Item ────────────────────────────────────────────────────

@Composable
fun TransactionListItem(transaction: Transaction, onClick: () -> Unit) {
    val catColor    = getCategoryColor(transaction.category)
    val amountColor = if (transaction.type == TransactionType.INCOME) TxIncomeGreen else TxExpenseRed
    val prefix      = if (transaction.type == TransactionType.INCOME) "+ " else "- "

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick   = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getCategoryIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint     = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.title,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = catColor.copy(alpha = 0.1f)) {
                        Text(
                            transaction.category,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = catColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                    Text("·", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        TimeUtils.toShortRelativeTime(transaction.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = prefix + formatCurrencyRp(transaction.amount),
                    color      = amountColor,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatDateClean(transaction.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun TxEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SearchOff, null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp))
        }
        Text("No Transactions Found", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text("Try adjusting your search or filter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}

// ─── Transaction Detail Overlay (Dialog style, same window as activity) ───────

@Composable
fun TransactionDetailOverlay(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(tween(200)) + slideInVertically(tween(300)) { it / 4 },
        exit    = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Bottom sheet style card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .pointerInput(Unit) { detectTapGestures { } }, // consume taps
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 24.dp
            ) {
                Column(Modifier.fillMaxSize()) {
                    // ── Handle bar ────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        )
                    }

                    // ── Gradient header ───────────────────────────────────
                    val catColor    = getCategoryColor(transaction.category)
                    val amountColor = if (transaction.type == TransactionType.INCOME) TxIncomeGreen else TxExpenseRed
                    val prefix      = if (transaction.type == TransactionType.INCOME) "+ " else "- "
                    val isIncome    = transaction.type == TransactionType.INCOME

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isIncome)
                                    Brush.horizontalGradient(listOf(Color(0xFF064E3B), Color(0xFF10B981)))
                                else
                                    Brush.horizontalGradient(listOf(Color(0xFF2D0A5E), Color(0xFF7C3AED)))
                            )
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getCategoryIcon(transaction.category),
                                    contentDescription = null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    transaction.title,
                                    style      = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White,
                                    maxLines   = 2,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        transaction.category,
                                        style      = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Color.White,
                                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            // Close button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null,
                                    tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Amount row
                        Spacer(Modifier.height(16.dp))
                    }

                    // Amount hero
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isIncome) Color(0xFFECFDF5) else Color(0xFFF0EBFF)
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Amount", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                prefix + formatCurrencyRp(transaction.amount),
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color      = amountColor
                            )
                        }
                    }

                    // ── Info rows ─────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        TxDetailRow(Icons.Default.CalendarMonth, "Date", formatDateClean(transaction.date))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        TxDetailRow(Icons.Default.AccessTime, "Time", transaction.time)
                        if (!transaction.paymentMethod.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            TxDetailRow(Icons.Default.CreditCard, "Payment Method", transaction.paymentMethod)
                        }
                        if (!transaction.location.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            TxDetailRow(Icons.Default.LocationOn, "Location", transaction.location)
                        }
                        if (!transaction.description.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            TxDetailRow(Icons.Default.Notes, "Description", transaction.description)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (transaction.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                null,
                                tint     = if (transaction.isSynced) TxIncomeGreen
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                if (transaction.isSynced) "Synced to cloud" else "Pending sync",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (transaction.isSynced) TxIncomeGreen
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // ── Action buttons ────────────────────────────────────
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { onEdit(transaction) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick  = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = TxExpenseRed)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = TxExpenseRed) },
            title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) },
            text  = {
                Text("\"${transaction.title}\" will be permanently deleted. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete(transaction.id) },
                    colors  = ButtonDefaults.buttonColors(containerColor = TxExpenseRed),
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun TxDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null,
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
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
