package com.example.insightku.feature.accounts.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.CurrencyUtils

@Composable
fun AccountsScreen(
    onNavigateToAddAccount: () -> Unit = {},
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
                isOpen = true,
                onDismiss = { accountToView = null },
                onEdit = {
                    accountToView = null
                    accountToEdit = account
                },
                onDelete = {
                    viewModel.onEvent(AccountsEvent.DeleteAccount(account.id, account.name))
                    accountToView = null
                }
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
                        text = "Accounts",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    if (accountCount > 0) {
                        Text(
                            text = "$accountCount account${if (accountCount > 1) "s" else ""}",
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
                            text = "Add",
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
    val groupedAccounts = accounts.groupBy { it.type }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = 80.dp
        )
    ) {
        AccountType.entries.forEach { accountType ->
            val accountsOfType = groupedAccounts[accountType]
            if (!accountsOfType.isNullOrEmpty()) {
                item(key = "header_${accountType.name}") {
                    SectionHeader(
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

// ── Section Header ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = 24.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            color = AppPalette.textMuted
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(AppPalette.cardBorder)
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textMuted
            )
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
        Color(android.graphics.Color.parseColor(account.color))
    } catch (e: Exception) {
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
                                    text = "Default",
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
                            text = "Liability",
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
                            contentDescription = "More options",
                            tint = AppPalette.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = AppPalette.textMuted
                                    )
                                    Text("Edit")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = ExpenseRed
                                    )
                                    Text(
                                        text = "Delete",
                                        color = ExpenseRed
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
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

@Composable
private fun EmptyAccountsState(
    onAddAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Layered circles for depth
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.06f))
            )
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.10f))
            )
            Icon(
                imageVector = Icons.Outlined.Wallet,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "No accounts yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Add your bank accounts, e-wallets, credit cards, and cash to keep everything organized in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.ButtonRadiusSmall))
                .clickable { onAddAccountClick() },
            shape = RoundedCornerShape(Dimens.ButtonRadiusSmall),
            color = accent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Add your first account",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "You can add as many accounts as you need",
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted.copy(alpha = 0.7f)
        )
    }
}

// ── Account Detail Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailSheet(
    account: Account,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (!isOpen) return

    val accent = LocalAccent.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val icon = when (account.type) {
        AccountType.CASH -> Icons.Filled.Savings
        AccountType.BANK_ACCOUNT -> Icons.Filled.AccountBalance
        AccountType.E_WALLET -> Icons.Filled.Wallet
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
    }

    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.color))
    } catch (e: Exception) {
        accent
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppPalette.card,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
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
                .navigationBarsPadding()
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
                            text = "Outstanding Liability",
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

            // ── Details Section ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "DETAILS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = AppPalette.textMuted
                )

                // Created
                DetailRow(
                    label = "Created",
                    value = formatDate(account.createdAt)
                )

                // Last Activity
                DetailRow(
                    label = "Last activity",
                    value = formatDate(account.updatedAt)
                )

                // Account Type
                DetailRow(
                    label = "Account type",
                    value = account.type.displayName
                )

                // Notes (if present)
                if (account.notes.isNotBlank()) {
                    DetailRow(
                        label = "Notes",
                        value = account.notes
                    )
                }

                // Latest Transaction
                // NOTE: Transaction model has no accountId field. Once that field is added,
                // wire up a latestTransaction parameter here to display real transaction data.
                DetailRow(
                    label = "Latest transaction",
                    value = "No transactions yet",
                    isPlaceholder = true
                )
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
                            text = "Edit Account",
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
                            text = "Delete Account",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
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

// ── Detail Row ─────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(
    label: String,
    value: String,
    hint: String? = null,
    isPlaceholder: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.textMuted.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isPlaceholder) AppPalette.textMuted.copy(alpha = 0.5f) else AppPalette.textPrimary
        )
    }
}

// ── Date Formatter ──────────────────────────────────────────────────────────────

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
