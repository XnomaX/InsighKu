package com.example.insightku.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import com.example.insightku.viewmodel.TransactionDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// --- DATA & STATE MANAGEMENT ---

enum class FilterType {
    ALL, INCOME, EXPENSE, TODAY, WEEK, MONTH, YEAR
}

enum class SortType {
    NEWEST, OLDEST, HIGHEST, LOWEST, CATEGORY
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food & dining", "food" -> Icons.Default.Fastfood
        "transportation" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.ShoppingBag
        "entertainment" -> Icons.Default.Movie
        "salary", "freelance", "work" -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.Default.AttachMoney
    }
}

private fun getCategoryColor(category: String): Color {
     return when (category.lowercase()) {
        "food & dining", "food" -> Color(0xFFF59E0B)
        "transportation" -> Color(0xFF3B82F6)
        "shopping" -> Color(0xFFEC4899)
        "entertainment" -> Color(0xFF8B5CF6)
        "salary", "freelance", "work" -> Color(0xFF10B981)
        else -> Color(0xFF6B7280)
    }
}

private fun formatDate(dateMillis: Long): String {
    val date = Date(dateMillis)
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val transactionDate = Calendar.getInstance().apply { time = date }

    return when {
        today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR) -> "Today"
        yesterday.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) && yesterday.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}

// --- MAIN SCREEN COMPOSABLE ---

@Composable
fun TransactionDetailsScreen(
    viewModel: TransactionDetailsViewModel = hiltViewModel(),
    initialTransactions: List<Transaction> = emptyList(),
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit = {},
    onDeleteTransaction: (String) -> Unit = {}
) {
    val dbTransactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Use DB transactions if available, else fallback to initialTransactions
    val allTransactions = dbTransactions.ifEmpty { initialTransactions }

    var searchTerm by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(FilterType.ALL) }
    var sortBy by remember { mutableStateOf(SortType.NEWEST) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    val filteredAndSortedTransactions = remember(allTransactions, searchTerm, filterType, sortBy) {
        val filtered = allTransactions.filter { transaction ->
            val matchesSearch = transaction.title.contains(searchTerm, ignoreCase = true) ||
                    transaction.category.contains(searchTerm, ignoreCase = true) ||
                    (transaction.description ?: "").contains(searchTerm, ignoreCase = true)
            val calendar = Calendar.getInstance()
            val transactionDate = Calendar.getInstance().apply { timeInMillis = transaction.date }
            val matchesFilter = when (filterType) {
                FilterType.INCOME  -> transaction.type == TransactionType.INCOME
                FilterType.EXPENSE -> transaction.type == TransactionType.EXPENSE
                FilterType.TODAY   -> calendar.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR) && calendar.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR)
                FilterType.WEEK    -> transactionDate.after(Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) })
                FilterType.MONTH   -> transactionDate.after(Calendar.getInstance().apply { add(Calendar.MONTH, -1) })
                FilterType.YEAR    -> transactionDate.after(Calendar.getInstance().apply { add(Calendar.YEAR, -1) })
                FilterType.ALL     -> true
            }
            matchesSearch && matchesFilter
        }
        when (sortBy) {
            SortType.NEWEST   -> filtered.sortedByDescending { it.date }
            SortType.OLDEST   -> filtered.sortedBy { it.date }
            SortType.HIGHEST  -> filtered.sortedByDescending { abs(it.amount) }
            SortType.LOWEST   -> filtered.sortedBy { abs(it.amount) }
            SortType.CATEGORY -> filtered.sortedBy { it.category }
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (transactionToEdit != null) {
        EditTransactionDetail(
            transaction = transactionToEdit!!,
            onDismiss = { transactionToEdit = null },
            onSave = { updatedTransaction ->
                onEditTransaction(updatedTransaction)
                transactionToEdit = null
            }
        )
    } else if (selectedTransaction != null) {
        TransactionDetailView(
            transaction = selectedTransaction!!,
            onBack = { selectedTransaction = null },
            onEdit = { transactionToEdit = it },
            onDelete = onDeleteTransaction
        )
    } else {
        TransactionListView(
            transactions = filteredAndSortedTransactions,
            searchTerm = searchTerm,
            onSearchTermChanged = { searchTerm = it },
            filterType = filterType,
            onFilterTypeChanged = { filterType = it },
            sortBy = sortBy,
            onSortByChanged = { sortBy = it },
            onTransactionSelected = { selectedTransaction = it },
            onBack = onBack
        )
    }
}

// --- LIST VIEW & COMPONENTS ---

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
    val statistics = remember(transactions) {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { abs(it.amount) }
        val netAmount = totalIncome - totalExpense
        mapOf("income" to totalIncome, "expense" to totalExpense, "net" to netAmount)
    }

    Scaffold(
        topBar = {
            ListHeader(
                onBack = onBack,
                income = statistics["income"] ?: 0.0,
                expense = statistics["expense"] ?: 0.0,
                net = statistics["net"] ?: 0.0
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                FilterControls(
                    searchTerm = searchTerm,
                    onSearchTermChanged = onSearchTermChanged,
                    filterType = filterType,
                    onFilterTypeChanged = onFilterTypeChanged,
                    sortBy = sortBy,
                    onSortByChanged = onSortByChanged
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${transactions.size} transaction${if (transactions.size != 1) "s" else ""} found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { onTransactionSelected(transaction) }
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ListHeader(onBack: () -> Unit, income: Double, expense: Double, net: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF5A2A82), Color(0xFF7C3AED))
                )
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "All Transactions",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatisticCard(
                label = "Income",
                amount = income,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            StatisticCard(
                label = "Expense",
                amount = expense,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                modifier = Modifier.weight(1f)
            )
            StatisticCard(
                label = "Net",
                amount = net,
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f),
                amountColor = if (net >= 0) Color(0xFFC6F6D5) else Color(0xFFFED7D7)
            )
        }
    }
}

@Composable
fun StatisticCard(label: String, amount: Double, icon: ImageVector, modifier: Modifier = Modifier, amountColor: Color = Color.White) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
            }
            Text(
                "Rp${String.format(Locale.getDefault(), "%,.0f", abs(amount))}",
                style = MaterialTheme.typography.bodyLarge,
                color = amountColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterControls(
    searchTerm: String, onSearchTermChanged: (String) -> Unit,
    filterType: FilterType, onFilterTypeChanged: (FilterType) -> Unit,
    sortBy: SortType, onSortByChanged: (SortType) -> Unit
) {
    var filterExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search bar
        OutlinedTextField(
            value = searchTerm,
            onValueChange = onSearchTermChanged,
            placeholder = { Text("Search transactions...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchTerm.isNotEmpty()) {
                    IconButton(onClick = { onSearchTermChanged("") }) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        // Filter chips — horizontally scrollable so they never overflow
        Text(
            "Filter",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
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
                    onClick = { onFilterTypeChanged(type) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF7C3AED),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Sort dropdown
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sort:", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(
                expanded = sortExpanded,
                onExpandedChange = { sortExpanded = it }
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF7C3AED).copy(alpha = 0.1f),
                    modifier = Modifier.menuAnchor()
                ) {
                    Row(
                        modifier = Modifier.clickable { sortExpanded = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                        Text(
                            sortBy.name.lowercase().replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF7C3AED),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                    }
                }
                ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    listOf(
                        SortType.NEWEST to "Newest First",
                        SortType.OLDEST to "Oldest First",
                        SortType.HIGHEST to "Highest Amount",
                        SortType.LOWEST to "Lowest Amount",
                        SortType.CATEGORY to "By Category"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = { onSortByChanged(type); sortExpanded = false },
                            leadingIcon = if (sortBy == type) {
                                { Icon(Icons.Default.Check, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun TransactionListItem(transaction: Transaction, onClick: () -> Unit) {
    val categoryColor = getCategoryColor(transaction.category)
    val amountColor = if (transaction.type == TransactionType.INCOME) Color(0xFF10B981) else Color(0xFFEF4444)
    val amountPrefix = if (transaction.type == TransactionType.INCOME) "+ " else "- "

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(50.dp)
                    .background(categoryColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getCategoryIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = categoryColor.copy(alpha = 0.1f), contentColor = categoryColor) {
                        Text(transaction.category, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDate(transaction.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(transaction.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = amountPrefix + "Rp" + String.format(Locale.getDefault(), "%,.0f", abs(transaction.amount)),
                color = amountColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.SearchOff, contentDescription = "", tint = Color.Gray, modifier = Modifier.size(64.dp))
        Text("No Transactions Found", style = MaterialTheme.typography.titleMedium)
        Text(
            "Try adjusting your search or filter criteria.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// --- DETAIL VIEW ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailView(
    transaction: Transaction,
    onBack: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit
) {
    val categoryColor = getCategoryColor(transaction.category)
    val amountColor = if (transaction.type == TransactionType.INCOME) Color(0xFF10B981) else Color(0xFFEF4444)
    val amountPrefix = if (transaction.type == TransactionType.INCOME) "+ " else "- "

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(
                                getCategoryIcon(transaction.category),
                                contentDescription = transaction.category,
                                tint = categoryColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                             Text(transaction.title, style = MaterialTheme.typography.headlineSmall)
                             Badge(containerColor = categoryColor.copy(alpha = 0.1f), contentColor = categoryColor) {
                                 Text(transaction.category)
                             }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                             Text(
                                text = amountPrefix + "Rp" + String.format(Locale.getDefault(), "%,.0f", abs(transaction.amount)),
                                color = amountColor,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(formatDate(transaction.date), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailItem("Time", transaction.time, modifier = Modifier.weight(1f))
                        transaction.paymentMethod?.let {
                            DetailItem("Payment Method", it, modifier = Modifier.weight(1f))
                        }
                    }
                    transaction.location?.let {
                        Spacer(Modifier.height(16.dp))
                        DetailItem("Location", it)
                    }
                     transaction.description?.let {
                        if (it.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            DetailItem("Description", it)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onEdit(transaction) }, modifier = Modifier.weight(1f)) {
                     Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                     Spacer(Modifier.width(8.dp))
                     Text("Edit")
                }
                Button(
                    onClick = { onDelete(transaction.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                     Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                     Spacer(Modifier.width(8.dp))
                     Text("Delete")
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

// --- PREVIEW ---

private val SAMPLE_TRANSACTIONS = listOf(
    Transaction(id = "1", title = "Salary", amount = 5000.0, category = "Work", type = TransactionType.INCOME, date = System.currentTimeMillis() - 86400000L * 1, description = "Monthly Salary", location = "Bank Transfer", paymentMethod = "Direct Deposit"),
    Transaction(id = "2", title = "Groceries", amount = 150.0, category = "Food", type = TransactionType.EXPENSE, date = System.currentTimeMillis() - 86400000L * 2, description = "Weekly groceries", location = "Supermarket", paymentMethod = "Credit Card"),
    Transaction(id = "3", title = "Freelance Project", amount = 1200.0, category = "Work", type = TransactionType.INCOME, date = System.currentTimeMillis() - 86400000L * 5, description = "Web design project", location = "Online", paymentMethod = "Bank Transfer"),
    Transaction(id = "4", title = "Netflix", amount = 15.0, category = "Entertainment", type = TransactionType.EXPENSE, date = System.currentTimeMillis() - 86400000L * 10, description = "Monthly subscription", location = "Online Payment", paymentMethod = "Credit Card"),
    Transaction(id = "5", title = "Uber Ride", amount = 25.5, category = "Transportation", type = TransactionType.EXPENSE, date = System.currentTimeMillis() - 86400000L * 12, description = "Ride to the airport", location = "City", paymentMethod = "Digital Wallet")
)

@Preview(showBackground = true, name = "Transaction List View")
@Composable
fun TransactionDetailsScreenListPreview() {
    MaterialTheme {
        TransactionDetailsScreen(
            initialTransactions = SAMPLE_TRANSACTIONS,
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Transaction Detail View")
@Composable
fun TransactionDetailsScreenDetailPreview() {
    MaterialTheme {
       var selectedTransaction by remember { mutableStateOf<Transaction?>(SAMPLE_TRANSACTIONS.first()) }
       if (selectedTransaction != null) {
            TransactionDetailView(
                transaction = selectedTransaction!!,
                onBack = { selectedTransaction = null },
                onEdit = {},
                onDelete = {}
            )
       }
    }
}
