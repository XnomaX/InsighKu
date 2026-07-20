package com.example.insightku.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.home.presentation.resolveCategoryIcon
import java.util.*
import kotlin.math.abs

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dbTransactions = uiState.transactions
    val isLoading      = uiState.isLoading
    val categories     = uiState.categories
    val allTransactions = dbTransactions.ifEmpty { initialTransactions }

    // Build category icon/color lookup: name.lowercase() → Category
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
                // Persist immediately; Room re-emits → list AND any still-open sheet update live.
                onEditTransaction(updated)
                transactionToEditId = null
            }
        )
    }
}
