package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor

/**
 * Confirmation sheet shown when completing or deleting a goal that still holds
 * allocated funds. The user either keeps/returns the funds (envelope release —
 * no balance movement) or transfers them to another account (physical move).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFundsBottomSheet(
    goalName: String,
    goalIcon: String,
    goalColor: String,
    totalFunds: Double,
    fundsByAccount: Map<String, Double>,
    accounts: List<Account>,
    isDelete: Boolean,
    onDismiss: () -> Unit,
    onKeepOrReturn: () -> Unit,
    onTransfer: (targetAccountId: String) -> Unit
) {
    val accent = LocalAccent.current
    val parsedGoalColor = remember(goalColor) { try { Color(android.graphics.Color.parseColor(goalColor)) } catch (e: Exception) { accent } }
    var transferMode by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    // Destination options: active accounts that are not a source of this goal's funds
    val destinationAccounts = remember(accounts, fundsByAccount) { accounts.filter { it.id !in fundsByAccount.keys } }

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle = { Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(AppPalette.cardBorder)) } }
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 32.dp)) {
            // ── Goal header ─────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(parsedGoalColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(imageVector = getGoalIcon(goalIcon), contentDescription = null, tint = parsedGoalColor, modifier = Modifier.size(26.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(if (isDelete) R.string.goal_delete_funds_title else R.string.goal_complete_funds_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                    Text(text = goalName, style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Funds summary ───────────────────────────────────────────
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated, border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.goal_funds_total), style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
                    Text(NumberFormatter.formatCurrency(totalFunds), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = parsedGoalColor)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(if (isDelete) R.string.goal_delete_funds_desc else R.string.goal_complete_funds_desc), style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)

            Spacer(Modifier.height(20.dp))

            if (!transferMode) {
                // ── Choice mode ─────────────────────────────────────────
                Button(onClick = onKeepOrReturn, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isDelete) AppPalette.error else parsedGoalColor)) {
                    Icon(imageVector = if (isDelete) Icons.AutoMirrored.Outlined.Undo else Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(if (isDelete) R.string.goal_funds_return else R.string.goal_funds_keep), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { transferMode = true }, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = destinationAccounts.isNotEmpty(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, AppPalette.cardBorder), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textPrimary)) {
                    Icon(imageVector = Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.goal_funds_transfer), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (destinationAccounts.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.goal_funds_no_other_account), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            } else {
                // ── Transfer mode: destination picker ───────────────────
                Text(stringResource(R.string.goal_funds_select_account), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                Spacer(Modifier.height(8.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated, border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                    Column(modifier = Modifier.heightIn(max = 280.dp)) {
                        destinationAccounts.forEachIndexed { index, account ->
                            val isSelected = account.id == selectedAccountId
                            val acctColor = remember(account.color) { try { Color(android.graphics.Color.parseColor(account.color)) } catch (e: Exception) { accent } }
                            val acctIcon = when (account.type) { AccountType.CASH -> Icons.Outlined.Payments; AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance; AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet; AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard }
                            Row(modifier = Modifier.fillMaxWidth().clickable { selectedAccountId = account.id }.background(if (isSelected) acctColor.copy(alpha = 0.08f) else Color.Transparent).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isSelected) acctColor.copy(alpha = 0.15f) else AppPalette.cardBorder.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { Icon(acctIcon, contentDescription = null, tint = if (isSelected) acctColor else AppPalette.textMuted, modifier = Modifier.size(20.dp)) }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(com.example.insightku.core.utils.CurrencyUtils.formatAmountCompact(account.balance), style = MaterialTheme.typography.bodySmall, color = if (account.balance > 0) SuccessColor else ExpenseRed)
                                }
                                if (isSelected) { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = acctColor, modifier = Modifier.size(20.dp)) }
                            }
                            if (index < destinationAccounts.lastIndex) { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder) }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { selectedAccountId?.let { onTransfer(it) } }, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = selectedAccountId != null, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = parsedGoalColor)) {
                    Icon(imageVector = Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.goal_funds_confirm_transfer), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { transferMode = false }, modifier = Modifier.fillMaxWidth().height(44.dp)) { Text(stringResource(R.string.back), style = MaterialTheme.typography.titleMedium, color = AppPalette.textMuted) }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(stringResource(R.string.cancel), style = MaterialTheme.typography.titleMedium, color = AppPalette.textMuted) }
        }
    }
}
