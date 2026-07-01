package com.example.insightku.feature.budgeting.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.feature.budgeting.domain.model.Goal

/**
 * Bottom sheet for contributing to a goal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionBottomSheet(
    goal: Goal,
    accounts: List<Account>,
    initialAccountId: String? = null,
    onDismiss: () -> Unit,
    onContribute: (accountId: String, amount: Double) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(initialAccountId ?: accounts.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    var showAccountPicker by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isValid = selectedAccountId != null && parsedAmount > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Save to ${goal.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Remaining: $${"%.2f".format(goal.remainingAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Account Selector
            Text(
                text = "From Account",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (showAccountPicker) {
                AccountPickerList(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = {
                        selectedAccountId = it.id
                        showAccountPicker = false
                    }
                )
            } else {
                AccountSelector(
                    account = selectedAccount,
                    onClick = { showAccountPicker = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Amount Input
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Icon(Icons.Outlined.AttachMoney, contentDescription = null)
                },
                singleLine = true
            )

            // Quick Amount Buttons
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("10", "25", "50", "100").forEach { value ->
                    FilterChip(
                        selected = amount == value,
                        onClick = { amount = value },
                        label = { Text("$$value") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Contribute Button
            Button(
                onClick = {
                    selectedAccountId?.let { accountId ->
                        onContribute(accountId, parsedAmount)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Money")
            }
        }
    }
}

/**
 * Account selector button.
 */
@Composable
fun AccountSelector(
    account: Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (account?.type) {
                    AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
                    AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance
                    AccountType.E_WALLET -> Icons.Outlined.Payment
                    AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
                    else -> Icons.Outlined.AccountBalance
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account?.name ?: "Select Account",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (account != null) {
                    Text(
                        text = "Balance: $${"%.2f".format(account.balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null
            )
        }
    }
}

/**
 * Account picker list.
 */
@Composable
fun AccountPickerList(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (Account) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            accounts.forEachIndexed { index, account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAccountSelected(account) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (account.type) {
                            AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
                            AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance
                            AccountType.E_WALLET -> Icons.Outlined.Payment
                            AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
                            else -> Icons.Outlined.AccountBalance
                        },
                        contentDescription = null,
                        tint = if (account.id == selectedAccountId)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (account.id == selectedAccountId)
                                FontWeight.Bold
                            else
                                FontWeight.Normal
                        )
                        Text(
                            text = "Balance: $${"%.2f".format(account.balance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (account.id == selectedAccountId) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (index < accounts.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

/**
 * Bottom sheet for withdrawing from a goal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalBottomSheet(
    goal: Goal,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onWithdraw: (accountId: String, amount: Double) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    var showAccountPicker by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isValid = selectedAccountId != null && parsedAmount > 0 && parsedAmount <= goal.currentAmount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Withdraw from ${goal.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Available: $${"%.2f".format(goal.currentAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Account Selector
            Text(
                text = "To Account",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (showAccountPicker) {
                AccountPickerList(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = {
                        selectedAccountId = it.id
                        showAccountPicker = false
                    }
                )
            } else {
                AccountSelector(
                    account = selectedAccount,
                    onClick = { showAccountPicker = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Amount Input
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Icon(Icons.Outlined.AttachMoney, contentDescription = null)
                },
                singleLine = true,
                isError = parsedAmount > goal.currentAmount
            )

            if (parsedAmount > goal.currentAmount) {
                Text(
                    text = "Amount exceeds available balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Quick Amount Buttons
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = amount == goal.currentAmount.toString(),
                    onClick = { amount = goal.currentAmount.toString() },
                    label = { Text("All") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = amount == (goal.currentAmount / 2).toString(),
                    onClick = { amount = (goal.currentAmount / 2).toString() },
                    label = { Text("Half") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Withdraw Button
            Button(
                onClick = {
                    selectedAccountId?.let { accountId ->
                        onWithdraw(accountId, parsedAmount)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Withdraw")
            }
        }
    }
}
