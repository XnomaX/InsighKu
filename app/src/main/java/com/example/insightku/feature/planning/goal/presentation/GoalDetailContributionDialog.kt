package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.PurpleViolet
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.core.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalDetailContributionDialog(uiState: GoalDetailUiState, onEvent: (GoalDetailEvent) -> Unit) {
    val isWithdraw = uiState.showWithdrawDialog
    val goal = uiState.goal ?: return
    val goalColor = remember(goal.color) { try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { PurpleViolet } }
    val amount = uiState.contributionAmount.toDoubleOrNull() ?: 0.0
    val currentSaved = goal.currentAmount
    val target = goal.targetAmount
    val newTotal = if (isWithdraw) (currentSaved - amount).coerceAtLeast(0.0) else currentSaved + amount
    val remainingTarget = (target - currentSaved).coerceAtLeast(0.0)
    val currentPercent = if (target > 0) (currentSaved / target * 100).coerceIn(0.0, 100.0) else 0.0
    val newPercent = if (target > 0) (newTotal / target * 100).coerceIn(0.0, 100.0) else 0.0
    val isAmountValid = amount > 0 && (!isWithdraw || amount <= currentSaved)
    val exceedsTarget = !isWithdraw && newTotal > target
    val showPreview = isAmountValid && amount > 0
    val displayPercent = if (showPreview) newPercent else currentPercent
    val animatedDisplayPercent by animateFloatAsState(targetValue = displayPercent.toFloat() / 100f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f), label = "previewProgress")

    // Highest priority validation message
    val validationMessage: Pair<String, Color>? = when {
        uiState.isInsufficientFunds -> stringResource(R.string.goal_contribution_insufficient_funds) to ExpenseRed
        exceedsTarget -> stringResource(R.string.goal_amount_exceeds) to WarningYellow
        else -> null
    }

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = { onEvent(GoalDetailEvent.DismissDialog) },
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle = { Box(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(AppPalette.cardBorder)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 32.dp)) {
            // ── 1. Goal Header ──────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(goalColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(getGoalIcon(goal.iconName), contentDescription = null, tint = goalColor, modifier = Modifier.size(24.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isWithdraw) stringResource(R.string.goal_contribution_withdraw) else stringResource(R.string.goal_contribution_add), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                    Text(goal.name, style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
                }
            }
            if (uiState.hasUnsyncedChanges) { Spacer(Modifier.height(8.dp)); Surface(shape = RoundedCornerShape(10.dp), color = WarningYellow.copy(alpha = 0.10f), border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.25f))) { Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(16.dp)); Text(stringResource(R.string.goal_pending_sync), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = WarningYellow) } } }

            Spacer(Modifier.height(16.dp))

            // ── 2. Goal Progress (enhanced) ────────────────────────────
            Surface(shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Current → Preview row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(R.string.goal_contribution_current_progress), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                            Text("${currentPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                        }
                        if (showPreview) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AppPalette.textMuted.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${stringResource(R.string.goal_dialog_after)} ${if (isWithdraw) stringResource(R.string.goal_dialog_after_withdrawal) else stringResource(R.string.goal_dialog_after_contribution)}", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                Text("${newPercent.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = goalColor)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Progress bar
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(AppPalette.cardBorder)) {
                        Box(modifier = Modifier.fillMaxWidth(animatedDisplayPercent).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(goalColor.copy(alpha = 0.7f), goalColor))))
                    }
                    // Remaining target
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.goal_summary_remaining), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                        Text(formatCurrencyFull(remainingTarget), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 3. Amount ──────────────────────────────────────────────
            Column {
                Text(stringResource(R.string.goal_amount_label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                Spacer(Modifier.height(8.dp))
                val formattedAmount = if (uiState.contributionAmount.isEmpty()) "" else NumberFormatter.formatNumber(uiState.contributionAmount.toDoubleOrNull() ?: 0.0)
OutlinedTextField(value = formattedAmount, onValueChange = { val filtered = it.filter { c -> c.isDigit() }; onEvent(GoalDetailEvent.UpdateAmount(filtered)) }, placeholder = { Text("0", color = AppPalette.textMuted.copy(alpha = 0.5f)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = goalColor, unfocusedBorderColor = AppPalette.cardBorder, focusedContainerColor = goalColor.copy(alpha = 0.04f), unfocusedContainerColor = AppPalette.cardElevated), textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = AppPalette.textPrimary))
            }

            Spacer(Modifier.height(12.dp))

            // ── 4. Quick Amount Chips ──────────────────────────────────
            if (!isWithdraw) {
                val quickAmounts = listOf(50_000.0, 100_000.0, 250_000.0, 500_000.0, 1_000_000.0)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(quickAmounts.size) { index -> val chipAmount = quickAmounts[index]; val isSelected = uiState.contributionAmount.toDoubleOrNull() == chipAmount; Surface(onClick = { onEvent(GoalDetailEvent.UpdateAmount(chipAmount.toLong().toString())) }, shape = RoundedCornerShape(12.dp), color = if (isSelected) goalColor.copy(alpha = 0.15f) else AppPalette.cardElevated, border = BorderStroke(1.dp, if (isSelected) goalColor.copy(alpha = 0.4f) else AppPalette.cardBorder)) { Text("+${NumberFormatter.formatCompact(chipAmount)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = if (isSelected) goalColor else AppPalette.textMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) } } }
                Spacer(Modifier.height(12.dp))
            }

            // ── 5. From Account ────────────────────────────────────────
            val displayAccounts = uiState.linkedAccounts.ifEmpty { uiState.accountMap.values.toList() }
            if (displayAccounts.isNotEmpty()) {
                val selectedAccount = displayAccounts.find { it.id == uiState.selectedAccountId }
                var accountExpanded by remember { mutableStateOf(false) }
                Text(stringResource(R.string.goal_dialog_from_account), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                Spacer(Modifier.height(8.dp))
                AccountDropdownSelector(account = selectedAccount, isWithdraw = isWithdraw, isExpanded = accountExpanded, onClick = { accountExpanded = !accountExpanded })
                Spacer(Modifier.height(4.dp))
                AnimatedVisibility(visible = accountExpanded, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(200))) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppPalette.cardElevated,
                        border = BorderStroke(1.dp, goalColor.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                            displayAccounts.forEachIndexed { index, account ->
                                val isSelected = account.id == uiState.selectedAccountId
                                val accountColor = remember(account.color) { try { Color(android.graphics.Color.parseColor(account.color)) } catch (e: Exception) { PurpleViolet } }
                                val accountIcon = when (account.type) { AccountType.CASH -> Icons.Outlined.Payments; AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance; AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet; AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onEvent(GoalDetailEvent.SelectAccount(account.id))
                                            accountExpanded = false
                                        }
                                        .background(if (isSelected) accountColor.copy(alpha = 0.08f) else Color.Transparent)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isSelected) accountColor.copy(alpha = 0.15f) else AppPalette.cardBorder.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) { Icon(accountIcon, contentDescription = null, tint = if (isSelected) accountColor else AppPalette.textMuted, modifier = Modifier.size(20.dp)) }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(com.example.insightku.core.utils.CurrencyUtils.formatAmountCompact(account.balance), style = MaterialTheme.typography.bodySmall, color = if (account.balance > 0) SuccessColor else ExpenseRed)
                                    }
                                    if (isSelected) { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = accountColor, modifier = Modifier.size(20.dp)) }
                                }
                                if (index < displayAccounts.lastIndex) { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder) }
                            }
                        }
                    }
                }
            }

            // ── 6. Validation Message (highest priority only) ──────────
            if (validationMessage != null) {
                Spacer(Modifier.height(16.dp))
                val (msg, color) = validationMessage
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(msg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
                            if (uiState.isInsufficientFunds) {
                                Text(stringResource(R.string.goal_dialog_insufficient_funds_detail, CurrencyUtils.formatAmountCompact(uiState.shortfall)), style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            // ── 7. Footer Actions (always visible) ─────────────────────
            Spacer(Modifier.height(20.dp))
            val isSubmitting = uiState.isSubmitting
            val canSubmit = isAmountValid && !exceedsTarget && !isSubmitting
            Button(onClick = { if (isWithdraw) onEvent(GoalDetailEvent.SubmitWithdrawal) else onEvent(GoalDetailEvent.SubmitContribution) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = canSubmit, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isWithdraw) WarningYellow else goalColor, disabledContainerColor = AppPalette.cardBorder), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 4.dp)) {
                if (isSubmitting) { CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp) }
                else { Text(if (isWithdraw) stringResource(R.string.goal_dialog_withdraw) else stringResource(R.string.goal_dialog_save_contribution), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (canSubmit) Color.White else AppPalette.textMuted) }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onEvent(GoalDetailEvent.DismissDialog) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(stringResource(R.string.cancel), style = MaterialTheme.typography.titleMedium, color = AppPalette.textMuted) }
        }
    }
}

@Composable
internal fun AccountDropdownSelector(account: Account?, isWithdraw: Boolean, isExpanded: Boolean, onClick: () -> Unit) {
    val accountColor = remember(account?.color) { try { Color(android.graphics.Color.parseColor(account?.color ?: "#9C27B0")) } catch (e: Exception) { PurpleViolet } }
    val accountIcon = when (account?.type) { AccountType.CASH -> Icons.Outlined.Payments; AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance; AccountType.E_WALLET -> Icons.Outlined.AccountBalanceWallet; AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard; else -> Icons.Outlined.AccountBalance }
    val borderColor by animateColorAsState(targetValue = if (isExpanded) accountColor.copy(alpha = 0.4f) else AppPalette.cardBorder, animationSpec = tween(200), label = "border")
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AppPalette.cardElevated, border = BorderStroke(1.dp, borderColor)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(accountColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(accountIcon, contentDescription = null, tint = accountColor, modifier = Modifier.size(22.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                if (account != null) { Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(com.example.insightku.core.utils.CurrencyUtils.formatAmountCompact(account.balance), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = if (account.balance > 0) SuccessColor else ExpenseRed) }
                else { Text(if (isWithdraw) stringResource(R.string.goal_dialog_select_destination) else stringResource(R.string.goal_dialog_select_source), style = MaterialTheme.typography.bodyLarge, color = AppPalette.textMuted) }
            }
            Icon(imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(22.dp))
        }
    }
}
