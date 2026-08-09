package com.example.insightku.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.ui.theme.AppPalette

// --- Premium Transaction Detail Overlay --------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailOverlay(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: ((Transaction) -> Unit)? = null,
    categoryMap: Map<String, Category> = emptyMap(),
    accountMap: Map<String, Account> = emptyMap()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val txType      = transaction.type
    val amountColor = txTypeColor(txType)
    val typeLabel   = txTypeLabel(txType)
    val prefix      = txAmountPrefix(txType)
    val showCat     = txShowCategory(txType)
    val canEdit     = txAllowsEdit(txType)

    // Resolve category color for badge (icon is handled by shared TransactionCategoryIcon)
    val resolved = resolveCategoryIcon(transaction.category, transaction.type, categoryMap)
    val catColor = resolved.color

    val accent = TxAccent
    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = TxCard,
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(TxCardBorder)
                )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
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
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
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

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TxCardBorder)
            )

            // -- Info grid -------------------------------------------------
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumInfoTile(Icons.Default.CalendarMonth, stringResource(R.string.tx_detail_date), formatFullDate(transaction.date), accent, Modifier.weight(1f))
                    PremiumInfoTile(Icons.Default.AccessTime, stringResource(R.string.tx_detail_time), transaction.time, accent, Modifier.weight(1f))
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
                    val accountColor = account?.let {
                        runCatching { Color(it.color.toColorInt()) }.getOrDefault(AppPalette.defaultBlue)
                    } ?: AppPalette.defaultBlue
                    PremiumInfoTile(accountIcon, stringResource(R.string.tx_detail_account), account?.name ?: stringResource(R.string.tx_detail_no_account), accountColor, Modifier.weight(1f))
                    // Sync status
                    PremiumInfoTile(
                        if (transaction.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        stringResource(R.string.tx_detail_status), if (transaction.isSynced) stringResource(R.string.tx_detail_synced) else stringResource(R.string.tx_detail_syncing),
                        if (transaction.isSynced) TxIncomeGreen else TxAdjustOrange, Modifier.weight(1f)
                    )
                }
                // Transfer info: show related account
                if (txType == TransactionType.TRANSFER_OUT || txType == TransactionType.TRANSFER_IN) {
                    transaction.relatedAccountId?.let { relatedId ->
                        val relatedAccount = accountMap[relatedId]
                        val label = if (txType == TransactionType.TRANSFER_OUT) stringResource(R.string.tx_detail_to_account) else stringResource(R.string.tx_detail_from_account)
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
                        PremiumInfoTile(goalIcon, stringResource(R.string.tx_detail_goal), goalName, goalColor, Modifier.fillMaxWidth())
                    }
                }
                // Contribution type info for Goal Contribution
                if (txType == TransactionType.GOAL_CONTRIBUTION) {
                    val contribType = if (transaction.isAuto) stringResource(R.string.tx_detail_auto) else stringResource(R.string.tx_detail_manual)
                    PremiumInfoTile(
                            if (transaction.isAuto) Icons.Default.AutoAwesome else Icons.Default.TouchApp,
                            stringResource(R.string.tx_detail_contribution_type), contribType,
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
                if (!transaction.description.isNullOrBlank()) PremiumInfoTileWide(Icons.AutoMirrored.Filled.Notes, stringResource(R.string.tx_detail_notes), transaction.description, AppPalette.notesPurple)
                if (!transaction.location.isNullOrBlank()) PremiumInfoTileWide(Icons.Default.LocationOn, stringResource(R.string.tx_detail_location), transaction.location, AppPalette.locationPink)
            }

            // -- Action buttons (type-dependent) --------------------------------
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Edit + Delete for editable types (Income, Expense, Balance Adjustment)
                if (canEdit) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { onEdit(transaction) },
                            RoundedCornerShape(16.dp),
                            TxTint,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                accent.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Edit, null, tint = accent, modifier = Modifier.size(16.dp))
                                    Text(stringResource(R.string.edit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = accent)
                                }
                            }
                        }
                        Surface(
                            Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { showDeleteDialog = true },
                            RoundedCornerShape(16.dp),
                            TxExpenseRed.copy(alpha = 0.10f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = TxExpenseRed, modifier = Modifier.size(16.dp))
                                    Text(stringResource(R.string.delete), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TxExpenseRed)
                                }
                            }
                        }
                    }
                }
                // Goal Contribution/Withdrawal: View Goal button
                if (txType == TransactionType.GOAL_CONTRIBUTION || txType == TransactionType.GOAL_WITHDRAWAL || txType == TransactionType.AUTO_ALLOCATION) {
                    Surface(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable { onDismiss() },
                        RoundedCornerShape(16.dp),
                        TxTint,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            TxGoalPurple.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Flag, null, tint = TxGoalPurple, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.transaction_view_goal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TxGoalPurple)
                            }
                        }
                    }
                }
                // Transfer: View Transfer Details button
                if (txType == TransactionType.TRANSFER_OUT || txType == TransactionType.TRANSFER_IN) {
                    Surface(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable { onDismiss() },
                        RoundedCornerShape(16.dp),
                        TxTint,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            TxTransferBlue.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.SwapHoriz, null, tint = TxTransferBlue, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.transaction_view_transfer), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TxTransferBlue)
                            }
                        }
                    }
                }
                // Duplicate only for editable types
                if (canEdit && onDuplicate != null) {
                    Surface(
                        Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clickable {
                                onDuplicate(
                                    transaction.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        date = System.currentTimeMillis()
                                    )
                                )
                                onDismiss()
                            }, RoundedCornerShape(16.dp), TxTint,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TxCardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ContentCopy, null, tint = accent, modifier = Modifier.size(15.dp))
                                Text(stringResource(R.string.transaction_duplicate), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = accent)
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
            title = stringResource(R.string.transaction_delete),
            message = "\"${transaction.title}\" ${stringResource(R.string.transaction_delete_desc)}",
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
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
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(color.copy(alpha = 0.14f)),
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
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.14f)),
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
