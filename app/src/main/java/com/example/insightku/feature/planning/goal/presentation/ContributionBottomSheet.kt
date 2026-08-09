package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.planning.goal.domain.model.Goal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionBottomSheet(goal: Goal, accounts: List<Account>, accountAllocations: Map<String, AccountAllocation> = emptyMap(), initialAccountId: String? = null, onDismiss: () -> Unit, onContribute: (accountId: String, amount: Double) -> Unit) {
    val goalColor = try {
        Color(goal.color.toColorInt())
    } catch (_: Exception) {
        LocalAccent.current
    }
    var selectedAccountId by remember { mutableStateOf(initialAccountId ?: accounts.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val selectedAllocation = selectedAccountId?.let { accountAllocations[it] }
    val availableCash = selectedAllocation?.availableCash ?: selectedAccount?.balance ?: 0.0
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isValid = selectedAccountId != null && parsedAmount > 0 && parsedAmount <= availableCash
    val currentPercent = if (goal.targetAmount > 0) ((goal.currentAmount / goal.targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0
    val newTotal = goal.currentAmount + parsedAmount
    val newPercent = if (goal.targetAmount > 0) ((newTotal / goal.targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0
    val remainingTarget = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    val showPreview = isValid
    val displayPercent = if (showPreview) newPercent else currentPercent
    val animatedDisplayPercent by animateFloatAsState(targetValue = displayPercent.toFloat() / 100f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f), label = "previewProgress")
    val quickAmounts = listOf(50_000L, 100_000L, 250_000L, 500_000L, 1_000_000L)

    val insufficientFundsStr = stringResource(R.string.contribution_insufficient_cash)
    val exceedsBalanceStr = stringResource(R.string.contribution_exceeds_balance)
    
    // Highest priority validation message
    val validationMessage: Pair<String, Color>? = when {
        parsedAmount > availableCash && availableCash > 0 -> insufficientFundsStr to ExpenseRed
        parsedAmount > availableCash -> exceedsBalanceStr to ExpenseRed
        else -> null
    }

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                        .clip(RoundedCornerShape(50.dp))
                        .background(AppPalette.cardBorder)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // ── 1. Goal Header ──────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGoalIcon(goal.iconName),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) { Text(text = stringResource(R.string.contribution_saving_to), style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted); Text(text = goal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Surface(shape = RoundedCornerShape(10.dp), color = goalColor.copy(alpha = 0.12f)) { Text(text = "${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = goalColor, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) } }

            Spacer(Modifier.height(16.dp))

            // ── 2. Goal Progress (enhanced) ────────────────────────────
            Surface(shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(R.string.goal_contribution_current_progress), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                            Text("${currentPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                        }
                        if (showPreview) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AppPalette.textMuted.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${stringResource(R.string.goal_dialog_after)} ${stringResource(R.string.goal_dialog_after_contribution)}", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                Text("${newPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = goalColor)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AppPalette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedDisplayPercent)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            goalColor.copy(alpha = 0.7f),
                                            goalColor
                                        )
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.goal_summary_remaining), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                        Text(NumberFormatter.formatCurrency(remainingTarget), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 3. Amount ──────────────────────────────────────────────
            Text(text = stringResource(R.string.contribution_amount), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
            OutlinedTextField(value = if (amount.isEmpty()) "" else NumberFormatter.formatNumber(amount.toDoubleOrNull() ?: 0.0), onValueChange = { amount = it.filter { c -> c.isDigit() }.take(12) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0", color = AppPalette.textMuted.copy(alpha = 0.5f)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = goalColor, unfocusedBorderColor = AppPalette.cardBorder, focusedContainerColor = AppPalette.cardElevated, unfocusedContainerColor = AppPalette.cardElevated))

            Spacer(Modifier.height(12.dp))

            // ── 4. Quick Amount Chips ──────────────────────────────────
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) { items(quickAmounts) { quickAmount -> Surface(onClick = { amount = quickAmount.toString() }, shape = RoundedCornerShape(12.dp), color = if (parsedAmount == quickAmount.toDouble()) goalColor else AppPalette.cardElevated, border = BorderStroke(1.dp, if (parsedAmount == quickAmount.toDouble()) goalColor else AppPalette.cardBorder)) { Text(text = "+${when { quickAmount >= 1_000_000 -> "${quickAmount / 1_000_000}M"; quickAmount >= 1_000 -> "${quickAmount / 1_000}K"; else -> quickAmount.toString() }}", style = MaterialTheme.typography.labelLarge, fontWeight = if (parsedAmount == quickAmount.toDouble()) FontWeight.Bold else FontWeight.Medium, color = if (parsedAmount == quickAmount.toDouble()) Color.White else AppPalette.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) } } }

            Spacer(Modifier.height(12.dp))

            // ── 5. From Account ────────────────────────────────────────
            var accountExpanded by remember { mutableStateOf(false) }
            Text(text = stringResource(R.string.goal_dialog_from_account), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
            val fallbackColor = LocalAccent.current
            val accountColor = remember(selectedAccount?.color) {
                try {
                    Color((selectedAccount?.color ?: "#9C27B0").toColorInt())
                } catch (_: Exception) {
                    fallbackColor
                }
            }
            val borderColor by animateColorAsState(targetValue = if (accountExpanded) accountColor.copy(alpha = 0.4f) else AppPalette.cardBorder, animationSpec = tween(200), label = "border")
            Surface(onClick = { accountExpanded = !accountExpanded }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated, border = BorderStroke(1.dp, borderColor)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val accountIcon = when (selectedAccount?.type) { AccountType.CASH -> Icons.Outlined.Payments; AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance; AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet; AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard; else -> Icons.Outlined.AccountBalance }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accountColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            accountIcon,
                            contentDescription = null,
                            tint = accountColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedAccount != null) { Text(selectedAccount.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(com.example.insightku.core.utils.CurrencyUtils.formatAmountCompact(selectedAccount.balance), style = MaterialTheme.typography.bodySmall, color = if (selectedAccount.balance > 0) SuccessColor else ExpenseRed) }
                        else { Text(stringResource(R.string.goal_dialog_select_source), style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted) }
                    }
                    Icon(imageVector = if (accountExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(22.dp))
                }
            }
            AnimatedVisibility(visible = accountExpanded, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(200))) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated, border = BorderStroke(1.dp, accountColor.copy(alpha = 0.4f))) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        accounts.forEachIndexed { index, account ->
                            val isSelected = account.id == selectedAccountId
                            val acctFallback = LocalAccent.current
                            val acctColor = remember(account.color) {
                                try {
                                    Color(account.color.toColorInt())
                                } catch (e: Exception) {
                                    acctFallback
                                }
                            }
                            val acctIcon = when (account.type) { AccountType.CASH -> Icons.Outlined.Payments; AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance; AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet; AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAccountId = account.id; accountExpanded = false
                                    }
                                    .background(if (isSelected) acctColor.copy(alpha = 0.08f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) acctColor.copy(alpha = 0.15f) else AppPalette.cardBorder.copy(
                                                alpha = 0.3f
                                            )
                                        ), contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        acctIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) acctColor else AppPalette.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(com.example.insightku.core.utils.CurrencyUtils.formatAmountCompact(account.balance), style = MaterialTheme.typography.bodySmall, color = if (account.balance > 0) SuccessColor else ExpenseRed)
                                }
                                if (isSelected) { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = acctColor, modifier = Modifier.size(20.dp)) }
                            }
                            if (index < accounts.lastIndex) { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder) }
                        }
                    }
                }
            }

            // ── 6. Validation Message ──────────────────────────────────
            if (validationMessage != null) {
                Spacer(Modifier.height(16.dp))
                val (msg, color) = validationMessage
                val shortfall = parsedAmount - availableCash
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) { Text(text = msg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color); Text(text = stringResource(R.string.contribution_insufficient_cash_detail, NumberFormatter.formatCurrency(shortfall)), style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.8f)) }
                    }
                }
            }

            // ── 7. Footer Actions ──────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    selectedAccountId?.let { accountId ->
                        onContribute(
                            accountId,
                            parsedAmount
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goalColor,
                    disabledContainerColor = goalColor.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Savings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                ); Spacer(modifier = Modifier.width(10.dp)); Text(
                text = stringResource(R.string.contribution_save_to_goal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss, modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    stringResource(R.string.cancel),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalBottomSheet(
    goal: Goal,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onWithdraw: (accountId: String, amount: Double) -> Unit
) {
    val goalColor = AppPalette.error
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isValid = selectedAccountId != null && parsedAmount > 0 && parsedAmount <= goal.currentAmount
    val currentPercent = if (goal.targetAmount > 0) ((goal.currentAmount / goal.targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0
    val newTotal = (goal.currentAmount - parsedAmount).coerceAtLeast(0.0)
    val newPercent = if (goal.targetAmount > 0) ((newTotal / goal.targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0
    val remainingTarget = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    val showPreview = isValid
    val displayPercent = if (showPreview) newPercent else currentPercent
    val animatedDisplayPercent by animateFloatAsState(targetValue = displayPercent.toFloat() / 100f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f), label = "previewProgress")

    val exceedsBalanceStr = stringResource(R.string.contribution_exceeds_balance)
    
    // Highest priority validation
    val validationMessage: Pair<String, Color>? = if (parsedAmount > goal.currentAmount) {
        exceedsBalanceStr to goalColor
    } else null

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                        .clip(RoundedCornerShape(50.dp))
                        .background(AppPalette.cardBorder)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // ── 1. Goal Header ──────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) { Text(text = stringResource(R.string.contribution_withdrawing_from), style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted); Text(text = goal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Surface(shape = RoundedCornerShape(10.dp), color = goalColor.copy(alpha = 0.12f)) { Text(text = NumberFormatter.formatCurrency(goal.currentAmount), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = goalColor, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) } }

            Spacer(Modifier.height(16.dp))

            // ── 2. Goal Progress (enhanced) ────────────────────────────
            Surface(shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(R.string.goal_contribution_current_progress), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                            Text("${currentPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                        }
                        if (showPreview) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AppPalette.textMuted.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${stringResource(R.string.goal_dialog_after)} ${stringResource(R.string.goal_dialog_after_withdrawal)}", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                Text("${newPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = goalColor)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AppPalette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedDisplayPercent)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            goalColor.copy(alpha = 0.7f),
                                            goalColor
                                        )
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.goal_summary_remaining), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                        Text(NumberFormatter.formatCurrency(remainingTarget), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 3. Amount ──────────────────────────────────────────────
            Text(text = stringResource(R.string.contribution_amount), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
            OutlinedTextField(value = if (amount.isEmpty()) "" else NumberFormatter.formatNumber(amount.toDoubleOrNull() ?: 0.0), onValueChange = { amount = it.filter { c -> c.isDigit() }.take(12) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0", color = AppPalette.textMuted.copy(alpha = 0.5f)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = goalColor, unfocusedBorderColor = AppPalette.cardBorder, focusedContainerColor = AppPalette.cardElevated, unfocusedContainerColor = AppPalette.cardElevated), isError = parsedAmount > goal.currentAmount)

            Spacer(Modifier.height(12.dp))

            // ── 4. Quick Amount Chips ──────────────────────────────────
            val quickAmounts = listOf(
                (goal.currentAmount * 0.25).toLong() to "25%",
                (goal.currentAmount * 0.50).toLong() to "50%",
                (goal.currentAmount * 0.75).toLong() to "75%",
                goal.currentAmount.toLong() to "100%"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                items(quickAmounts.size) { index ->
                    val (chipAmount, label) = quickAmounts[index]
                    val isSelected = parsedAmount.toLong() == chipAmount
                    Surface(
                        onClick = { amount = chipAmount.toString() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) goalColor else AppPalette.cardElevated,
                        border = BorderStroke(1.dp, if (isSelected) goalColor else AppPalette.cardBorder)
                    ) {
                        Text(
                            text = "-${label}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else AppPalette.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 5. To Account ──────────────────────────────────────────
            var accountExpanded by remember { mutableStateOf(false) }
            Text(text = stringResource(R.string.goal_dialog_to_account), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
            val fallbackColor = LocalAccent.current
            val accountColor = remember(selectedAccount?.color) {
                try {
                    Color((selectedAccount?.color ?: "#9C27B0").toColorInt())
                } catch (_: Exception) {
                    fallbackColor
                }
            }
            val borderColor by animateColorAsState(targetValue = if (accountExpanded) accountColor.copy(alpha = 0.4f) else AppPalette.cardBorder, animationSpec = tween(200), label = "border")
            Surface(onClick = { accountExpanded = !accountExpanded }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated, border = BorderStroke(1.dp, borderColor)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val accountIcon = when (selectedAccount?.type) { AccountType.CASH -> Icons.Outlined.Payments; AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance; AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet; AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard; else -> Icons.Outlined.AccountBalance }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accountColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            accountIcon,
                            contentDescription = null,
                            tint = accountColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedAccount != null) { Text(selectedAccount.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(com.example.insightku.core.utils.CurrencyUtils.formatAmountCompact(selectedAccount.balance), style = MaterialTheme.typography.bodySmall, color = if (selectedAccount.balance > 0) SuccessColor else ExpenseRed) }
                        else { Text(stringResource(R.string.goal_dialog_select_destination), style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted) }
                    }
                    Icon(imageVector = if (accountExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(22.dp))
                }
            }
            AnimatedVisibility(visible = accountExpanded, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(200))) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated, border = BorderStroke(1.dp, accountColor.copy(alpha = 0.4f))) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        accounts.forEachIndexed { index, account ->
                            val isSelected = account.id == selectedAccountId
                            val acctFallback = LocalAccent.current
                            val acctColor = remember(account.color) {
                                try {
                                    Color(account.color.toColorInt())
                                } catch (_: Exception) {
                                    acctFallback
                                }
                            }
                            val acctIcon = when (account.type) { AccountType.CASH -> Icons.Outlined.Payments; AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance; AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet; AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAccountId = account.id; accountExpanded = false
                                    }
                                    .background(if (isSelected) acctColor.copy(alpha = 0.08f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) acctColor.copy(alpha = 0.15f) else AppPalette.cardBorder.copy(
                                                alpha = 0.3f
                                            )
                                        ), contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        acctIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) acctColor else AppPalette.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(com.example.insightku.core.utils.CurrencyUtils.formatAmountCompact(account.balance), style = MaterialTheme.typography.bodySmall, color = if (account.balance > 0) SuccessColor else ExpenseRed)
                                }
                                if (isSelected) { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = acctColor, modifier = Modifier.size(20.dp)) }
                            }
                            if (index < accounts.lastIndex) { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder) }
                        }
                    }
                }
            }

            // ── 6. Validation Message ──────────────────────────────────
            if (validationMessage != null) {
                Spacer(Modifier.height(16.dp))
                val (msg, color) = validationMessage
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Text(text = msg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── 7. Footer Actions ──────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    selectedAccountId?.let { accountId ->
                        onWithdraw(
                            accountId,
                            parsedAmount
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goalColor,
                    disabledContainerColor = goalColor.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                ); Spacer(modifier = Modifier.width(10.dp)); Text(
                text = stringResource(R.string.withdraw),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss, modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    stringResource(R.string.cancel),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}
