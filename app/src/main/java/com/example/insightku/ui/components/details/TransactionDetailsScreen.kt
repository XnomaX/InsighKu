package com.example.insightku.ui.components.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
private val CardSurface    = Color(0xFFF3EEFF)
private val AmountCardExpense = Color(0xFFF0EBFF)
private val AmountCardIncome  = Color(0xFFECFDF5)

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
                    ).forEach { (type, label) ->
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

// ─── Premium Transaction Detail Overlay ──────────────────────────────────────

@Composable
fun TransactionDetailOverlay(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var dragOffsetY      by remember { mutableFloatStateOf(0f) }

    BackHandler { onDismiss() }

    val isIncome    = transaction.type == TransactionType.INCOME
    val catColor    = getCategoryColor(transaction.category)
    val amountColor = if (isIncome) TxIncomeGreen else TxExpenseRed
    val prefix      = if (isIncome) "+" else "-"

    val headerGradient = if (isIncome)
        Brush.linearGradient(listOf(IncomeDeep, IncomeMid, TxIncomeGreen))
    else
        Brush.linearGradient(listOf(PurpleDark, PurpleMid, PurpleViolet))

    // Animate in: scrim fades, card slides up from below
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scrimAlpha by animateFloatAsState(
        targetValue   = if (visible) 0.6f else 0f,
        animationSpec = tween(280),
        label         = "scrim"
    )

    val cardOffsetY by animateFloatAsState(
        targetValue   = if (visible) 0f else 300f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label         = "card_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating card — not full width, with horizontal margin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 16.dp)
                .graphicsLayer {
                    translationY = cardOffsetY + dragOffsetY.coerceAtLeast(0f)
                }
                .shadow(
                    elevation    = 32.dp,
                    shape        = RoundedCornerShape(32.dp),
                    ambientColor = PurpleViolet.copy(alpha = 0.25f),
                    spotColor    = PurpleViolet.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(GlassSurface)
                .pointerInput(Unit) {
                    detectTapGestures { /* consume — don't dismiss */ }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > 120f) onDismiss()
                            else dragOffsetY = 0f
                        },
                        onDragCancel = { dragOffsetY = 0f },
                        onVerticalDrag = { _, delta ->
                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Drag handle ───────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(PurpleLavender.copy(alpha = 0.4f))
                    )
                }

                // ── Gradient hero header ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(headerGradient)
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 24.dp)
                ) {
                    // Close button top-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null,
                            tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(4.dp))

                        // Circular category icon
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(12.dp, CircleShape, ambientColor = catColor.copy(alpha = 0.4f))
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(catColor.copy(alpha = 0.9f), catColor.copy(alpha = 0.6f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                getCategoryIcon(transaction.category),
                                contentDescription = null,
                                tint     = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Transaction title
                        Text(
                            text       = transaction.title,
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            textAlign  = TextAlign.Center,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(6.dp))

                        // Category pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Text(
                                    transaction.category,
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Amount hero
                        Text(
                            text       = "$prefix ${formatCurrencyRp(transaction.amount)}",
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(Modifier.height(4.dp))

                        // Type badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isIncome) TxIncomeGreen else TxExpenseRed).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text     = if (isIncome) "INCOME" else "EXPENSE",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // ── Detail info cards ─────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date & Time row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailInfoCard(
                            icon    = Icons.Default.CalendarMonth,
                            label   = "Date",
                            value   = formatFullDate(transaction.date),
                            color   = PurpleViolet,
                            modifier = Modifier.weight(1f)
                        )
                        DetailInfoCard(
                            icon    = Icons.Default.AccessTime,
                            label   = "Time",
                            value   = transaction.time,
                            color   = PurpleViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Payment method & sync status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailInfoCard(
                            icon    = Icons.Default.CreditCard,
                            label   = "Payment",
                            value   = transaction.paymentMethod ?: "Not specified",
                            color   = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        DetailInfoCard(
                            icon    = if (transaction.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            label   = "Status",
                            value   = if (transaction.isSynced) "Synced" else "Pending",
                            color   = if (transaction.isSynced) TxIncomeGreen else Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Location (if present)
                    if (!transaction.location.isNullOrBlank()) {
                        DetailInfoCardWide(
                            icon  = Icons.Default.LocationOn,
                            label = "Location",
                            value = transaction.location,
                            color = Color(0xFFEC4899)
                        )
                    }

                    // Notes (if present)
                    if (!transaction.description.isNullOrBlank()) {
                        DetailInfoCardWide(
                            icon  = Icons.Default.Notes,
                            label = "Notes",
                            value = transaction.description,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }

                // ── Soft divider ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GlassBorder, Color.Transparent)
                            )
                        )
                )

                // ── Action buttons ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Edit button — outlined purple
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(PurpleMid, PurpleViolet)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(PurpleViolet.copy(alpha = 0.06f))
                            .pointerInput(Unit) { detectTapGestures { onEdit(transaction) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Edit, null,
                                tint = PurpleViolet, modifier = Modifier.size(16.dp))
                            Text("Edit",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color      = PurpleViolet)
                        }
                    }

                    // Delete button — gradient red
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp),
                                ambientColor = TxExpenseRed.copy(alpha = 0.3f),
                                spotColor    = TxExpenseRed.copy(alpha = 0.3f))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFE53E3E), TxExpenseRed)
                                )
                            )
                            .pointerInput(Unit) { detectTapGestures { showDeleteDialog = true } },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Delete, null,
                                tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Delete",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White)
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TxExpenseRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteForever, null,
                        tint = TxExpenseRed, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text("Delete Transaction?",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge)
            },
            text  = {
                Text(
                    "\"${transaction.title}\" will be permanently deleted. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFE53E3E), TxExpenseRed)))
                        .pointerInput(Unit) {
                            detectTapGestures { showDeleteDialog = false; onDelete(transaction.id) }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) { detectTapGestures { showDeleteDialog = false } }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Cancel",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = GlassSurface
        )
    }
}

// ─── Detail Info Cards ────────────────────────────────────────────────────────

@Composable
private fun DetailInfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                }
                Text(
                    label,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines  = 1
                )
            }
            Text(
                value,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailInfoCardWide(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    label,
                    style  = MaterialTheme.typography.labelSmall,
                    color  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    value,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurface
                )
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
