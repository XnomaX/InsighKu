package com.example.insightku.feature.accounts.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.core.ui.components.DetailRow
import com.example.insightku.core.ui.components.SectionHeaderWithCount
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.feature.accounts.presentation.components.AllocationItemCard

@Composable
fun AccountsScreen(
    onNavigateToGoalDetail: (String) -> Unit = {},
    onNavigateToBudgeting: () -> Unit = {},
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToView by remember { mutableStateOf<Account?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Header ──────────────────────────────────────────────────────────
            AccountsHeader(
                accountCount = uiState.accounts.size,
                onAddClick = { showAddAccountDialog = true }
            )

            // ── Content ─────────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = LocalAccent.current,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                uiState.accounts.isEmpty() -> {
                    EmptyAccountsState(
                        onAddAccountClick = { showAddAccountDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    AccountsList(
                        accounts = uiState.accounts,
                        onAccountClick = { account -> accountToView = account },
                        onEditAccount = { account -> accountToEdit = account },
                        onDeleteAccount = { viewModel.onEvent(AccountsEvent.DeleteAccount(it.id, it.name)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Add Account Dialog
        AddAccountDialog(
            isOpen = showAddAccountDialog,
            onDismiss = { showAddAccountDialog = false },
            onAccountCreated = { showAddAccountDialog = false }
        )

        // Edit Account Dialog
        accountToEdit?.let { account ->
            EditAccountDialog(
                account = account,
                isOpen = true,
                onDismiss = { accountToEdit = null },
                onAccountUpdated = { accountToEdit = null }
            )
        }

        // Account Detail Bottom Sheet
        accountToView?.let { account ->
            AccountDetailSheet(
                account = account,
                allocation = uiState.getAllocation(account.id),
                onDismiss = { accountToView = null },
                onEdit = {
                    accountToView = null
                    accountToEdit = account
                },
                onDelete = {
                    viewModel.onEvent(AccountsEvent.DeleteAccount(account.id, account.name))
                    accountToView = null
                },
                onGoalClick = onNavigateToGoalDetail,
                onBudgetClick = onNavigateToBudgeting
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun AccountsHeader(
    accountCount: Int,
    onAddClick: () -> Unit
) {
    val accent = LocalAccent.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppPalette.background,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenHorizontalPadding)
                .padding(top = 20.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.accounts_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    if (accountCount > 0) {
                        Text(
                            text = pluralStringResource(R.plurals.accounts_count, accountCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppPalette.textMuted
                        )
                    }
                }

                // Add button in header
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onAddClick() },
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = stringResource(R.string.accounts_add),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

// ── Accounts List ───────────────────────────────────────────────────────────────

@Composable
private fun AccountsList(
    accounts: List<Account>,
    onAccountClick: (Account) -> Unit,
    onEditAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedAccounts = remember(accounts) { accounts.groupBy { it.type } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 80.dp
        )
    ) {
        AccountType.entries.forEach { accountType ->
            val accountsOfType = groupedAccounts[accountType]
            if (!accountsOfType.isNullOrEmpty()) {
                item(key = "header_${accountType.name}") {
                    SectionHeaderWithCount(
                        title = accountType.displayName,
                        count = accountsOfType.size
                    )
                }

                items(
                    items = accountsOfType,
                    key = { it.id }
                ) { account ->
                    AccountRow(
                        account = account,
                        onClick = { onAccountClick(account) },
                        onEdit = { onEditAccount(account) },
                        onDelete = { onDeleteAccount(account) }
                    )
                }
            }
        }
    }
}


// ── Account Row ─────────────────────────────────────────────────────────────────

@Composable
private fun AccountRow(
    account: Account,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val icon = when (account.type) {
        AccountType.CASH -> Icons.Filled.Savings
        AccountType.BANK_ACCOUNT -> Icons.Filled.AccountBalance
        AccountType.E_WALLET -> Icons.Filled.Wallet
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
    }

    val accountColor = try {
        Color(account.color.toColorInt())
    } catch (_: Exception) {
        LocalAccent.current
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = 5.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accountColor.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = account.type.displayName,
                        tint = accountColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Account info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppPalette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (account.isDefault) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = LocalAccent.current.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = stringResource(R.string.account_default_badge),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LocalAccent.current
                                )
                            }
                        }
                    }
                    if (account.notes.isNotBlank()) {
                        Text(
                            text = account.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = account.type.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted
                        )
                    }
                }

                // Balance
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatAmount(account.balance, "IDR"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (account.isLiability) ExpenseRed else AppPalette.textPrimary
                    )
                    if (account.isLiability) {
                        Text(
                            text = stringResource(R.string.account_liability),
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseRed.copy(alpha = 0.7f)
                        )
                    }
                }

                // More menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = AppPalette.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .background(
                                AppPalette.card,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        // Edit
                        DropdownMenuItem(
                            text = {
                    Text(
                        text = stringResource(R.string.edit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppPalette.textPrimary
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LocalAccent.current.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = LocalAccent.current,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = {
                        showMenu = false
                        onEdit()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                // ── Danger zone separator ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            HorizontalDivider(
                                color = ExpenseRed.copy(alpha = 0.15f),
                                thickness = 1.dp
                            )
                        }

                        // Delete (destructive)
                        DropdownMenuItem(
                            text = {
                    Text(
                        text = stringResource(R.string.delete),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ExpenseRed
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ExpenseRed.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = {
                        showMenu = false
                        showDeleteConfirm = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        DeleteAccountConfirmDialog(
            accountName = account.name,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────
// Uses shared EmptyStateSection from core/ui/components/SharedComponents.kt
@Composable
private fun EmptyAccountsState(
    onAddAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.example.insightku.core.ui.components.EmptyStateSection(
        icon = Icons.Outlined.Wallet,
        title = stringResource(R.string.accounts_empty_title),
        description = stringResource(R.string.accounts_empty_desc),
        modifier = modifier,
        actionLabel = stringResource(R.string.accounts_add_first),
        onAction = onAddAccountClick,
        trailingContent = {
            Text(
                text = stringResource(R.string.accounts_add_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted.copy(alpha = 0.7f)
            )
        }
    )
}

// ── Account Detail Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailSheet(
    account: Account,
    allocation: AccountAllocation?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onGoalClick: (String) -> Unit = {},
    onBudgetClick: () -> Unit = {}
) {
    val accent = LocalAccent.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val icon = when (account.type) {
        AccountType.CASH -> Icons.Filled.Savings
        AccountType.BANK_ACCOUNT -> Icons.Filled.AccountBalance
        AccountType.E_WALLET -> Icons.Filled.Wallet
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
    }

    val accountColor = try {
        Color(account.color.toColorInt())
    } catch (_: Exception) {
        accent
    }

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(AppPalette.cardBorder)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // ── Hero Section ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(accountColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = account.type.displayName,
                        tint = accountColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary,
                    textAlign = TextAlign.Center
                )

                // Type
                Text(
                    text = account.type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.textMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Balance
                Text(
                    text = CurrencyUtils.formatAmount(account.balance, "IDR"),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (account.isLiability) ExpenseRed else AppPalette.textPrimary
                )

                if (account.isLiability) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ExpenseRed.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = stringResource(R.string.account_outstanding_liability),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppPalette.cardBorder)
            )

            // ── Scrollable body (bounded so flings settle; wraps when short) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
            // ── Allocation Breakdown Section ─────────────────────────────────
            if (allocation != null && allocation.hasAllocations) {
                AllocationBreakdownSection(
                    allocation = allocation,
                    accountColor = accountColor,
                    onGoalClick = onGoalClick,
                    onBudgetClick = onBudgetClick
                )

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppPalette.cardBorder)
                )
            }

            // ── Details Section ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.account_detail_details),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = AppPalette.textMuted
                )

                // Created
                DetailRow(
                    label = stringResource(R.string.account_detail_created),
                    value = formatDate(account.createdAt)
                )

                // Last Activity
                DetailRow(
                    label = stringResource(R.string.account_detail_last_activity),
                    value = formatDate(account.updatedAt)
                )

                // Account Type
                DetailRow(
                    label = stringResource(R.string.account_detail_account_type),
                    value = account.type.displayName
                )

                // Notes (if present)
                if (account.notes.isNotBlank()) {
                    DetailRow(
                        label = stringResource(R.string.account_detail_notes),
                        value = account.notes
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppPalette.cardBorder)
            )

            // ── Actions ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Edit
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.ButtonRadiusSmall))
                        .clickable { onEdit() },
                    shape = RoundedCornerShape(Dimens.ButtonRadiusSmall),
                    color = accent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.account_edit),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Delete
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.ButtonRadiusSmall))
                        .clickable { showDeleteConfirm = true },
                    shape = RoundedCornerShape(Dimens.ButtonRadiusSmall),
                    color = AppPalette.cardElevated,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.account_delete),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
                    }
                }
            }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        DeleteAccountConfirmDialog(
            accountName = account.name,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

// Allocation Breakdown Section - Detailed view of all allocations
@Composable
private fun AllocationBreakdownSection(
    allocation: AccountAllocation,
    accountColor: Color,
    onGoalClick: (String) -> Unit = {},
    onBudgetClick: () -> Unit = {}
) {
    val successGreen = com.example.insightku.core.ui.theme.SuccessColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.account_allocation_breakdown),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            color = AppPalette.textMuted
        )

        // Current Balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.account_current_balance),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted
            )
            Text(
                text = CurrencyUtils.formatAmount(allocation.account.balance, "IDR"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
        }

        // Goal Allocations with progress bars
        if (allocation.goalAllocations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.account_goal_allocations),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted
                )

                allocation.goalAllocations.forEach { goal ->
                    val goalColor = try {
                        Color(goal.goalColor.toColorInt())
                    } catch (_: Exception) {
                        accountColor
                    }

                    AllocationItemCard(
                        name = goal.goalName,
                        amount = goal.allocatedAmount,
                        progressPercent = goal.progressPercent,
                        color = goalColor,
                        icon = goal.goalIcon,
                        targetAmount = goal.targetAmount,
                        statusLabel = if (goal.goalStatus == "paused") stringResource(R.string.goal_status_paused) else null,
                        onClick = { onGoalClick(goal.goalId) }
                    )
                }
            }
        }

        // Budget Allocations with progress bars
        if (allocation.budgetAllocations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.account_budget_allocations),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted
                )

                allocation.budgetAllocations.forEach { budget ->
                    val budgetColor = try {
                        budget.budgetColor?.let { Color(it.toColorInt()) }
                    } catch (_: Exception) {
                        null
                    } ?: accountColor

                    AllocationItemCard(
                        name = budget.budgetName,
                        amount = budget.allocatedAmount,
                        progressPercent = budget.usagePercent,
                        color = budgetColor,
                        icon = budget.budgetIcon,
                        targetAmount = budget.budgetLimit,
                        isOverBudget = budget.isOverBudget,
                        onClick = { onBudgetClick() }
                    )
                }
            }
        }

        // Available Cash
        if (allocation.availableCash > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(successGreen)
                    )
                    Text(
                        text = stringResource(R.string.account_available_cash),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppPalette.textPrimary
                    )
                }
                Text(
                    text = CurrencyUtils.formatAmount(allocation.availableCash, "IDR"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = successGreen
                )
            }
        }
    }
}


// ── Date Formatter ──────────────────────────────────────────────────────────────

private fun formatDate(timestamp: Long): String {
    return com.example.insightku.core.i18n.DateFormatter.formatShortDate(timestamp)
}
