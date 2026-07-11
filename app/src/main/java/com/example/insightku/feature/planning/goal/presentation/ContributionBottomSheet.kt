package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.feature.planning.goal.domain.model.Goal
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionBottomSheet(goal: Goal, accounts: List<Account>, accountAllocations: Map<String, AccountAllocation> = emptyMap(), initialAccountId: String? = null, onDismiss: () -> Unit, onContribute: (accountId: String, amount: Double) -> Unit) {
    val goalColor = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { LocalAccent.current }
    var selectedAccountId by remember { mutableStateOf(initialAccountId ?: accounts.firstOrNull()?.id) }; var amount by remember { mutableStateOf("") }; var showAccountPicker by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }; val selectedAllocation = selectedAccountId?.let { accountAllocations[it] }; val availableCash = selectedAllocation?.availableCash ?: selectedAccount?.balance ?: 0.0; val parsedAmount = amount.toDoubleOrNull() ?: 0.0; val isValid = selectedAccountId != null && parsedAmount > 0 && parsedAmount <= availableCash
    val newTotalAmount = goal.currentAmount + parsedAmount; val newProgressPercent = if (goal.targetAmount > 0) ((newTotalAmount / goal.targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0; val newRemaining = (goal.targetAmount - newTotalAmount).coerceAtLeast(0.0)
    val quickAmounts = listOf(50_000L, 100_000L, 250_000L, 500_000L, 1_000_000L)
    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle = { Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(AppPalette.cardBorder)) } }
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 40.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(goalColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(imageVector = getGoalIcon(goal.iconName), contentDescription = null, tint = goalColor, modifier = Modifier.size(26.dp)) }; Column(modifier = Modifier.weight(1f)) { Text(text = stringResource(R.string.contribution_saving_to), style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted); Text(text = goal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Surface(shape = RoundedCornerShape(10.dp), color = goalColor.copy(alpha = 0.12f)) { Text(text = "${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = goalColor, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) } }
            Text(text = stringResource(R.string.contribution_amount), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
            OutlinedTextField(value = if (amount.isEmpty()) "" else NumberFormatter.formatCurrency(amount.toDoubleOrNull() ?: 0.0), onValueChange = { amount = it.filter { c -> c.isDigit() }.take(12) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("${NumberFormatter.getCurrencySymbol()} 0", color = AppPalette.textMuted.copy(alpha = 0.5f)) }, prefix = { Text("${NumberFormatter.getCurrencySymbol()} ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = goalColor) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = goalColor, unfocusedBorderColor = AppPalette.cardBorder, focusedContainerColor = AppPalette.cardElevated, unfocusedContainerColor = AppPalette.cardElevated))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) { items(quickAmounts) { quickAmount -> Surface(onClick = { amount = quickAmount.toString() }, shape = RoundedCornerShape(12.dp), color = if (parsedAmount == quickAmount.toDouble()) goalColor else AppPalette.cardElevated, border = BorderStroke(1.dp, if (parsedAmount == quickAmount.toDouble()) goalColor else AppPalette.cardBorder)) { Text(text = "+${when { quickAmount >= 1_000_000 -> "${quickAmount / 1_000_000}M"; quickAmount >= 1_000 -> "${quickAmount / 1_000}K"; else -> quickAmount.toString() }}", style = MaterialTheme.typography.labelLarge, fontWeight = if (parsedAmount == quickAmount.toDouble()) FontWeight.Bold else FontWeight.Medium, color = if (parsedAmount == quickAmount.toDouble()) Color.White else AppPalette.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) } } }
            if (parsedAmount > availableCash && parsedAmount > 0) { val shortfall = parsedAmount - availableCash; Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = AppPalette.insufficientFundsBg), border = BorderStroke(1.dp, AppPalette.warning)) { Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = AppPalette.insufficientFundsText, modifier = Modifier.size(20.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = stringResource(R.string.contribution_insufficient_cash), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.insufficientFundsText);                Text(text = stringResource(R.string.contribution_insufficient_cash_detail, NumberFormatter.formatCurrency(shortfall)), style = MaterialTheme.typography.bodySmall, color = AppPalette.insufficientFundsDetail) } } } }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { selectedAccountId?.let { accountId -> onContribute(accountId, parsedAmount) } }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = isValid, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = goalColor, disabledContainerColor = goalColor.copy(alpha = 0.3f))) { Icon(imageVector = Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(10.dp)); Text(text = stringResource(R.string.contribution_save_to_goal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WithdrawalBottomSheet(goal: Goal, accounts: List<Account>, accountAllocations: Map<String, AccountAllocation> = emptyMap(), onDismiss: () -> Unit, onWithdraw: (accountId: String, amount: Double) -> Unit) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }; var amount by remember { mutableStateOf("") }; var showAccountPicker by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }; val selectedAllocation = selectedAccountId?.let { accountAllocations[it] }; val parsedAmount = amount.toDoubleOrNull() ?: 0.0; val isValid = selectedAccountId != null && parsedAmount > 0 && parsedAmount <= goal.currentAmount
    val expenseRed = AppPalette.error
    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle = { Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(AppPalette.cardBorder)) } }
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 40.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(expenseRed.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Outlined.ArrowUpward, contentDescription = null, tint = expenseRed, modifier = Modifier.size(26.dp)) }; Column(modifier = Modifier.weight(1f)) { Text(text = stringResource(R.string.contribution_withdrawing_from), style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted); Text(text = goal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Surface(shape = RoundedCornerShape(10.dp), color = expenseRed.copy(alpha = 0.12f)) { Text(text = NumberFormatter.formatCurrency(goal.currentAmount), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = expenseRed, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) } }
            Text(text = "AMOUNT", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
            OutlinedTextField(value = if (amount.isEmpty()) "" else NumberFormatter.formatCurrency(amount.toDoubleOrNull() ?: 0.0), onValueChange = { amount = it.filter { c -> c.isDigit() }.take(12) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("${NumberFormatter.getCurrencySymbol()} 0", color = AppPalette.textMuted.copy(alpha = 0.5f)) }, prefix = { Text("${NumberFormatter.getCurrencySymbol()} ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = expenseRed) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = expenseRed, unfocusedBorderColor = AppPalette.cardBorder, focusedContainerColor = AppPalette.cardElevated, unfocusedContainerColor = AppPalette.cardElevated), isError = parsedAmount > goal.currentAmount)
            if (parsedAmount > goal.currentAmount) { Text(text = stringResource(R.string.contribution_exceeds_balance), style = MaterialTheme.typography.labelSmall, color = expenseRed, modifier = Modifier.padding(start = 8.dp)) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) { item { Surface(onClick = { amount = goal.currentAmount.toLong().toString() }, shape = RoundedCornerShape(12.dp), color = if (parsedAmount == goal.currentAmount) expenseRed else AppPalette.cardElevated, border = BorderStroke(1.dp, if (parsedAmount == goal.currentAmount) expenseRed else AppPalette.cardBorder)) { Text(text = "+${NumberFormatter.formatCurrency(goal.currentAmount)}", style = MaterialTheme.typography.labelLarge, fontWeight = if (parsedAmount == goal.currentAmount) FontWeight.Bold else FontWeight.Medium, color = if (parsedAmount == goal.currentAmount) Color.White else AppPalette.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) } }; item { Surface(onClick = { amount = (goal.currentAmount / 2).toLong().toString() }, shape = RoundedCornerShape(12.dp), color = if (parsedAmount == goal.currentAmount / 2) expenseRed else AppPalette.cardElevated, border = BorderStroke(1.dp, if (parsedAmount == goal.currentAmount / 2) expenseRed else AppPalette.cardBorder)) { Text(text = "+${NumberFormatter.formatCurrency(goal.currentAmount / 2)}", style = MaterialTheme.typography.labelLarge, fontWeight = if (parsedAmount == goal.currentAmount / 2) FontWeight.Bold else FontWeight.Medium, color = if (parsedAmount == goal.currentAmount / 2) Color.White else AppPalette.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) } } }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { selectedAccountId?.let { accountId -> onWithdraw(accountId, parsedAmount) } }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = isValid, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = expenseRed, disabledContainerColor = expenseRed.copy(alpha = 0.3f))) { Icon(imageVector = Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(10.dp)); Text(text = stringResource(R.string.withdraw), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        }
    }
}


