package com.example.insightku.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.TimeUtils
import kotlin.math.abs

// --- Main list view -----------------------------------------------------------

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
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { abs(it.amount) }
    }

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
            .background(AppPalette.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimens.ContentBottomPadding)
        ) {
            item {
                TxListHeader(
                    onBack = onBack,
                    income = totalIncome,
                    expense = totalExpense,
                    total = allCount
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ScreenHorizontalPadding)
                        .padding(top = Dimens.PaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    TxSearchBar(value = searchTerm, onChange = onSearchTermChanged)
                    TxFilterRow(filterType = filterType, onFilterTypeChanged = onFilterTypeChanged)
                    TxSortRow(
                        sortBy = sortBy,
                        onSortByChanged = onSortByChanged,
                        count = transactions.size
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    TxEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenHorizontalPadding)
                            .padding(top = Dimens.PaddingExtraLarge + Dimens.PaddingLarge)
                    )
                }
            } else {
                grouped.forEach { (group, txList) ->
                    item(key = "header-${group.name}") {
                        TxGroupHeader(
                            label = group.label(),
                            count = txList.size,
                            modifier = Modifier.padding(
                                horizontal = Dimens.ScreenHorizontalPadding,
                                vertical = Dimens.PaddingMedium
                            )
                        )
                    }
                    items(txList, key = { it.id }) { tx ->
                        PremiumTransactionCard(
                            transaction = tx,
                            categoryMap = categoryMap,
                            accountMap = accountMap,
                            onClick = { onTransactionSelected(tx) },
                            modifier = Modifier.padding(
                                horizontal = Dimens.ScreenHorizontalPadding,
                                vertical = Dimens.PaddingSmall
                            )
                        )
                    }
                }
            }
        }
    }
}

// --- Header -------------------------------------------------------------------

@Composable
private fun TxListHeader(
    onBack: () -> Unit,
    income: Double,
    expense: Double,
    total: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = Dimens.PaddingMedium, bottom = Dimens.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.tx_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary
                )
                Text(
                    stringResource(R.string.tx_list_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
        }

        // Three figures as quiet text, not three identical tinted cards.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TxFigure(
                label = stringResource(R.string.tx_summary_income),
                value = formatCurrencyRp(income),
                modifier = Modifier.weight(1f)
            )
            TxFigure(
                label = stringResource(R.string.tx_summary_expenses),
                value = formatCurrencyRp(expense),
                modifier = Modifier.weight(1f)
            )
            TxFigure(
                label = stringResource(R.string.tx_summary_total),
                value = "$total",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TxFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = AppPalette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- Search -------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxSearchBar(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = {
            Text(
                stringResource(R.string.tx_list_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = AppPalette.textMuted,
                modifier = Modifier.size(Dimens.IconSizeMedium)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSizeSmall),
                        tint = AppPalette.textMuted
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LocalAccent.current,
            unfocusedBorderColor = AppPalette.cardBorder,
            unfocusedContainerColor = AppPalette.card,
            focusedContainerColor = AppPalette.card,
            cursorColor = LocalAccent.current,
            focusedTextColor = AppPalette.textPrimary,
            unfocusedTextColor = AppPalette.textPrimary
        )
    )
}

// --- Filters ------------------------------------------------------------------

@Composable
private fun TxFilterRow(
    filterType: FilterType,
    onFilterTypeChanged: (FilterType) -> Unit
) {
    val filters = listOf(
        FilterType.ALL to stringResource(R.string.tx_filter_all),
        FilterType.INCOME to stringResource(R.string.tx_filter_income),
        FilterType.EXPENSE to stringResource(R.string.tx_filter_expense),
        FilterType.TRANSFER to stringResource(R.string.tx_filter_transfer),
        FilterType.GOAL to stringResource(R.string.tx_filter_goals),
        FilterType.AUTO_ALLOC to stringResource(R.string.tx_filter_auto),
        FilterType.TODAY to stringResource(R.string.tx_filter_today),
        FilterType.WEEK to stringResource(R.string.tx_filter_this_week),
        FilterType.MONTH to stringResource(R.string.tx_filter_this_month)
    )

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        filters.forEach { (type, label) ->
            val selected = filterType == type
            // Active = small surface step + hairline, not a colored pill.
            Surface(
                modifier = Modifier,
                onClick = { onFilterTypeChanged(type) },
                shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                color = if (selected) AppPalette.cardElevated else Color.Transparent,
                border = BorderStroke(1.dp, if (selected) AppPalette.cardBorder else Color.Transparent)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) AppPalette.textPrimary else AppPalette.textMuted,
                    modifier = Modifier.padding(
                        horizontal = Dimens.PaddingLarge,
                        vertical = Dimens.PaddingMedium
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

// --- Sort ---------------------------------------------------------------------

@Composable
private fun TxSortRow(
    sortBy: SortType,
    onSortByChanged: (SortType) -> Unit,
    count: Int
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            pluralStringResource(R.plurals.tx_list_count, count, count),
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.textMuted
        )
        Box {
            TextButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(Dimens.IconSizeSmall)
                )
                Spacer(Modifier.size(Dimens.PaddingSmall))
                Text(
                    when (sortBy) {
                        SortType.NEWEST -> stringResource(R.string.tx_sort_newest_short)
                        SortType.OLDEST -> stringResource(R.string.tx_sort_oldest_short)
                        SortType.HIGHEST -> stringResource(R.string.tx_sort_highest_short)
                        SortType.LOWEST -> stringResource(R.string.tx_sort_lowest_short)
                        SortType.CATEGORY -> stringResource(R.string.tx_sort_category)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = AppPalette.textPrimary
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppPalette.textMuted,
                    modifier = Modifier.size(Dimens.IconSizeSmall)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(AppPalette.card, RoundedCornerShape(Dimens.CornerRadiusMedium))
            ) {
                listOf(
                    SortType.NEWEST to stringResource(R.string.tx_sort_newest),
                    SortType.OLDEST to stringResource(R.string.tx_sort_oldest),
                    SortType.HIGHEST to stringResource(R.string.tx_sort_highest),
                    SortType.LOWEST to stringResource(R.string.tx_sort_lowest),
                    SortType.CATEGORY to stringResource(R.string.tx_sort_category)
                ).forEach { (type, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (sortBy == type) FontWeight.Medium else FontWeight.Normal,
                                color = AppPalette.textPrimary
                            )
                        },
                        onClick = { onSortByChanged(type); expanded = false },
                        leadingIcon = {
                            if (sortBy == type) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppPalette.textMuted,
                                    modifier = Modifier.size(Dimens.IconSizeSmall)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- Group header -------------------------------------------------------------

@Composable
fun TxGroupHeader(label: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = AppPalette.textPrimary
        )
        Text("$count", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
    }
}

// --- Transaction row ----------------------------------------------------------

@Composable
fun PremiumTransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryMap: Map<String, Category> = emptyMap(),
    accountMap: Map<String, Account> = emptyMap()
) {
    val context = LocalContext.current
    val prefix = txAmountPrefix(transaction.type)
    val showCat = txShowCategory(transaction.type)

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardInnerPaddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
            ) {
                Text(
                    txDisplayTitle(transaction, context),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildList {
                    add(txTypeLabel(transaction.type))
                    if (showCat && transaction.category.isNotBlank()) add(transaction.category)
                    add(TimeUtils.toShortRelativeTime(transaction.date))
                }.joinToString(" · ")
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val contextLabel = when {
                    (transaction.type == TransactionType.GOAL_CONTRIBUTION ||
                        transaction.type == TransactionType.GOAL_WITHDRAWAL ||
                        transaction.type == TransactionType.AUTO_ALLOCATION) &&
                        !transaction.goalName.isNullOrBlank() -> transaction.goalName
                    else -> accountMap[transaction.accountId]?.name
                }
                if (contextLabel != null) {
                    Text(
                        contextLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
            ) {
                Text(
                    "$prefix ${formatCurrencyRp(transaction.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary,
                    maxLines = 1
                )
                Text(
                    if (transaction.isSynced) {
                        stringResource(R.string.tx_detail_synced)
                    } else {
                        stringResource(R.string.tx_detail_syncing)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}

// --- Empty state --------------------------------------------------------------

@Composable
private fun TxEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        Text(
            stringResource(R.string.tx_list_empty),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = AppPalette.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.tx_list_empty_desc),
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center
        )
    }
}
