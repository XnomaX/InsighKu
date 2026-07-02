package com.example.insightku.feature.budgeting.presentation.components

import androidx.compose.animation.animateContentSize
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
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.budgeting.domain.model.Goal
import java.text.NumberFormat
import java.util.Locale

/**
 * Premium Contribution Bottom Sheet with live preview and smooth animations.
 *
 * Features:
 * - Goal summary header with icon and progress
 * - Quick amount chips
 * - Live preview showing before/after contribution
 * - Beautiful source account selector
 * - Smooth animations
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
    val goalColor = try {
        Color(android.graphics.Color.parseColor(goal.color))
    } catch (e: Exception) {
        LocalAccent.current
    }

    var selectedAccountId by remember { mutableStateOf(initialAccountId ?: accounts.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    var showAccountPicker by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isValid = selectedAccountId != null && parsedAmount > 0

    // Calculate preview values
    val newTotalAmount = goal.currentAmount + parsedAmount
    val newProgressPercent = if (goal.targetAmount > 0) {
        ((newTotalAmount / goal.targetAmount) * 100).coerceIn(0.0, 100.0)
    } else 0.0
    val newRemaining = (goal.targetAmount - newTotalAmount).coerceAtLeast(0.0)

    // Quick amount chips in IDR
    val quickAmounts = listOf(
        50_000L, 100_000L, 250_000L, 500_000L, 1_000_000L
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Header: Goal Summary ────────────────────────────────────────────
            GoalContributionHeader(
                goal = goal,
                goalColor = goalColor
            )

            // ── Live Preview Card ───────────────────────────────────────────────
            LivePreviewCard(
                currentAmount = goal.currentAmount,
                newAmount = newTotalAmount,
                currentPercent = goal.progressPercent,
                newPercent = newProgressPercent,
                targetAmount = goal.targetAmount,
                remaining = newRemaining,
                goalColor = goalColor,
                parsedAmount = parsedAmount
            )

            // ── Source Account Selector ─────────────────────────────────────────
            Text(
                text = "FROM ACCOUNT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textMuted
            )

            if (showAccountPicker) {
                AccountPickerPremium(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = {
                        selectedAccountId = it.id
                        showAccountPicker = false
                    },
                    onDismiss = { showAccountPicker = false }
                )
            } else {
                AccountSelectorPremium(
                    account = selectedAccount,
                    onClick = { showAccountPicker = true },
                    goalColor = goalColor
                )
            }

            // ── Amount Input ────────────────────────────────────────────────────
            Text(
                text = "AMOUNT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textMuted
            )

            OutlinedTextField(
                value = if (amount.isEmpty()) "" else formatCurrencyIDR(amount.toLongOrNull() ?: 0L),
                onValueChange = {
                    amount = it.filter { c -> c.isDigit() }.take(12)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Rp 0",
                        color = AppPalette.textMuted.copy(alpha = 0.5f)
                    )
                },
                prefix = {
                    Text(
                        "Rp ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = goalColor,
                    unfocusedBorderColor = AppPalette.cardBorder,
                    focusedContainerColor = AppPalette.cardElevated,
                    unfocusedContainerColor = AppPalette.cardElevated
                )
            )

            // ── Quick Amount Chips ──────────────────────────────────────────────
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(quickAmounts) { quickAmount ->
                    QuickAmountChip(
                        amount = quickAmount,
                        isSelected = parsedAmount == quickAmount.toDouble(),
                        onClick = {
                            amount = quickAmount.toString()
                        },
                        goalColor = goalColor
                    )
                }
            }

            // ── Primary CTA Button ──────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    selectedAccountId?.let { accountId ->
                        onContribute(accountId, parsedAmount)
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
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Save to Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Goal Contribution Header ───────────────────────────────────────────────────

@Composable
private fun GoalContributionHeader(
    goal: Goal,
    goalColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Goal icon
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(goalColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getGoalIconVector(goal.iconName),
                contentDescription = null,
                tint = goalColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Saving to",
                style = MaterialTheme.typography.labelMedium,
                color = AppPalette.textMuted
            )
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Progress badge
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = goalColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = "${goal.progressPercent.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = goalColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// ─── Live Preview Card ─────────────────────────────────────────────────────────

@Composable
private fun LivePreviewCard(
    currentAmount: Double,
    newAmount: Double,
    currentPercent: Double,
    newPercent: Double,
    targetAmount: Double,
    remaining: Double,
    goalColor: Color,
    parsedAmount: Double
) {
    val animatedNewAmount by animateFloatAsState(
        targetValue = newAmount.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "amount"
    )

    val animatedNewPercent by animateFloatAsState(
        targetValue = newPercent.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "percent"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.cardElevated),
        border = BorderStroke(1.dp, goalColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = formatCurrencyIDR(currentAmount.toLong()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "After Saving",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = formatCurrencyIDR(animatedNewAmount.toLong()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Before
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${currentPercent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppPalette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(currentPercent.toFloat() / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppPalette.textMuted.copy(alpha = 0.4f))
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = goalColor,
                    modifier = Modifier.size(18.dp)
                )

                // After
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${animatedNewPercent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = goalColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppPalette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedNewPercent.toFloat() / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(goalColor)
                        )
                    }
                }
            }

            if (parsedAmount > 0 && remaining > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${formatCurrencyIDR(remaining.toLong())} more to reach your goal",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (newPercent >= 100) SuccessColor else AppPalette.textMuted
                    )
                }
            }

            if (newPercent >= 100) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Celebration,
                        contentDescription = null,
                        tint = SuccessColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Goal will be completed!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessColor
                    )
                }
            }
        }
    }
}

// ─── Premium Account Selector ──────────────────────────────────────────────────

@Composable
private fun AccountSelectorPremium(
    account: Account?,
    onClick: () -> Unit,
    goalColor: Color
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppPalette.cardElevated,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(goalColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
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
                    tint = goalColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account?.name ?: "Select Account",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary
                )
                if (account != null) {
                    Text(
                        text = "Balance: ${formatCurrencyIDR(account.balance.toLong())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppPalette.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─── Account Picker ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerPremium(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = AppPalette.textMuted
                    )
                }
            }

            HorizontalDivider(color = AppPalette.cardBorder)

            accounts.forEachIndexed { index, account ->
                val isSelected = account.id == selectedAccountId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAccountSelected(account) }
                        .background(if (isSelected) AppPalette.cardElevated else Color.Transparent)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) LocalAccent.current.copy(alpha = 0.12f)
                                else AppPalette.cardElevated
                            ),
                        contentAlignment = Alignment.Center
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
                            tint = if (isSelected) LocalAccent.current else AppPalette.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = AppPalette.textPrimary
                        )
                        Text(
                            text = "Balance: ${formatCurrencyIDR(account.balance.toLong())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Selected",
                            tint = LocalAccent.current,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (index < accounts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = AppPalette.cardBorder
                    )
                }
            }
        }
    }
}

// ─── Quick Amount Chip ─────────────────────────────────────────────────────────

@Composable
private fun QuickAmountChip(
    amount: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    goalColor: Color
) {
    val displayAmount = when {
        amount >= 1_000_000 -> "${amount / 1_000_000}M"
        amount >= 1_000 -> "${amount / 1_000}K"
        else -> amount.toString()
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) goalColor else AppPalette.cardElevated,
        border = BorderStroke(
            1.dp,
            if (isSelected) goalColor else AppPalette.cardBorder
        )
    ) {
        Text(
            text = "+$displayAmount",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else AppPalette.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// ─── Helper Functions ──────────────────────────────────────────────────────────

private fun getGoalIconVector(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName.lowercase()) {
        "savings", "piggy bank" -> Icons.Outlined.Savings
        "wallet", "account balance wallet" -> Icons.Outlined.AccountBalanceWallet
        "cash", "money", "paid" -> Icons.Outlined.Paid
        "flight", "airplane" -> Icons.Outlined.Flight
        "car", "directions car" -> Icons.Outlined.DirectionsCar
        "home", "house" -> Icons.Outlined.Home
        "school", "education", "graduation" -> Icons.Outlined.School
        "health", "health and safety" -> Icons.Outlined.HealthAndSafety
        "warning", "emergency" -> Icons.Outlined.Warning
        "trending up", "investment", "stocks" -> Icons.Outlined.TrendingUp
        "card giftcard", "gift" -> Icons.Outlined.CardGiftcard
        "celebration" -> Icons.Outlined.Celebration
        "star" -> Icons.Outlined.Star
        "flag", "target", "gps fixed" -> Icons.Outlined.Flag
        "beach", "travel" -> Icons.Outlined.BeachAccess
        "hotel", "suitcase" -> Icons.Outlined.Luggage
        "laptop", "technology" -> Icons.Outlined.Laptop
        "phone", "smartphone" -> Icons.Outlined.Smartphone
        "diamond", "gold", "investment" -> Icons.Outlined.Diamond
        else -> Icons.Outlined.Savings
    }
}

private fun formatCurrencyIDR(amount: Long): String {
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)
}

// ─── Withdrawal Bottom Sheet ─────────────────────────────────────────────────────

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

    val expenseRed = Color(0xFFEF4444)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(expenseRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = expenseRed,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Withdrawing from",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = expenseRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = formatCurrencyIDR(goal.currentAmount.toLong()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = expenseRed,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.cardElevated),
                border = BorderStroke(1.dp, expenseRed.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available to withdraw",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = formatCurrencyIDR(goal.currentAmount.toLong()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = expenseRed
                    )
                }
            }

            // Source Account Selector
            Text(
                text = "TO ACCOUNT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textMuted
            )

            if (showAccountPicker) {
                AccountPickerPremium(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = {
                        selectedAccountId = it.id
                        showAccountPicker = false
                    },
                    onDismiss = { showAccountPicker = false }
                )
            } else {
                AccountSelectorPremium(
                    account = selectedAccount,
                    onClick = { showAccountPicker = true },
                    goalColor = expenseRed
                )
            }

            // Amount Input
            Text(
                text = "AMOUNT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textMuted
            )

            OutlinedTextField(
                value = if (amount.isEmpty()) "" else formatCurrencyIDR(amount.toLongOrNull() ?: 0L),
                onValueChange = {
                    amount = it.filter { c -> c.isDigit() }.take(12)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Rp 0",
                        color = AppPalette.textMuted.copy(alpha = 0.5f)
                    )
                },
                prefix = {
                    Text(
                        "Rp ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = expenseRed
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = expenseRed,
                    unfocusedBorderColor = AppPalette.cardBorder,
                    focusedContainerColor = AppPalette.cardElevated,
                    unfocusedContainerColor = AppPalette.cardElevated
                ),
                isError = parsedAmount > goal.currentAmount
            )

            if (parsedAmount > goal.currentAmount) {
                Text(
                    text = "Amount exceeds available balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = expenseRed,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Quick Amount Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    QuickAmountChip(
                        amount = goal.currentAmount.toLong(),
                        isSelected = parsedAmount == goal.currentAmount,
                        onClick = { amount = goal.currentAmount.toLong().toString() },
                        goalColor = expenseRed
                    )
                }
                item {
                    QuickAmountChip(
                        amount = (goal.currentAmount / 2).toLong(),
                        isSelected = parsedAmount == goal.currentAmount / 2,
                        onClick = { amount = (goal.currentAmount / 2).toLong().toString() },
                        goalColor = expenseRed
                    )
                }
            }

            // Withdraw Button
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    selectedAccountId?.let { accountId ->
                        onWithdraw(accountId, parsedAmount)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = expenseRed,
                    disabledContainerColor = expenseRed.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Withdraw",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
