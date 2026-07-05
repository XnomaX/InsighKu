package com.example.insightku.feature.budgeting.presentation.screen

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.feature.budgeting.domain.model.Goal
import com.example.insightku.feature.budgeting.presentation.event.ContributionEvent
import com.example.insightku.feature.budgeting.presentation.state.ContributionUiState
import com.example.insightku.feature.budgeting.presentation.state.QuickAmount
import com.example.insightku.feature.budgeting.presentation.components.getGoalIcon
import com.example.insightku.feature.budgeting.presentation.viewmodel.ContributionViewModel

/**
 * Premium Contribution Screen - redesigned to focus on progress and motivation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionScreen(
    goalId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    prefillAmount: Double? = null,
    viewModel: ContributionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(goalId) {
        viewModel.onEvent(ContributionEvent.Initialize(goalId, prefillAmount))
    }

    LaunchedEffect(uiState.contributionSaved) {
        if (uiState.contributionSaved) {
            kotlinx.coroutines.delay(1500)
            onSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(ContributionEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(ContributionEvent.ClearSnackbar)
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(AppPalette.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = LocalAccent.current)
        }
        return
    }

    val goal = uiState.goal
    val goalColor = goal?.let { parseGoalColor(it.color) } ?: LocalAccent.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppPalette.background
    ) { paddingValues ->
        ContributionContent(
            uiState = uiState,
            goal = goal,
            goalColor = goalColor,
            onBack = onBack,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(paddingValues)
        )

        if (uiState.showAccountPicker) {
            AccountPickerDialog(
                accounts = uiState.accounts,
                selectedAccountId = uiState.selectedAccountId,
                onAccountSelected = { accountId ->
                    viewModel.onEvent(ContributionEvent.SelectAccount(accountId))
                    viewModel.onEvent(ContributionEvent.HideAccountPicker)
                },
                onDismiss = { viewModel.onEvent(ContributionEvent.HideAccountPicker) },
                goalColor = goalColor
            )
        }

        if (uiState.isSuccess) {
            com.example.insightku.core.ui.components.dialogs.PremiumSuccessOverlay(
                message = "${com.example.insightku.core.utils.CurrencyUtils.formatAmountCompact(uiState.parsedAmount)} added to ${goal?.name ?: ""}",
                subtitle = null
            )
        }
    }
}

@Composable
private fun ContributionContent(
    uiState: ContributionUiState,
    goal: Goal?,
    goalColor: Color,
    onBack: () -> Unit,
    onEvent: (ContributionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenHorizontalPadding)
                .padding(top = Dimens.ScreenHorizontalPadding)
        ) {
            ContributionHeader(onBack = onBack)

            Spacer(Modifier.height(Dimens.CardSpacing))

            goal?.let {
                GoalSummaryPreview(goal = it, goalColor = goalColor)
            }

            Spacer(Modifier.height(Dimens.CardSpacing))

            AmountInputCard(
                rawAmount = uiState.rawAmount,
                onAmountChange = { onEvent(ContributionEvent.UpdateAmount(it)) },
                goalColor = goalColor
            )

            Spacer(Modifier.height(12.dp))

            QuickAmountChipsRow(
                selectedAmount = uiState.parsedAmount,
                onSelectAmount = { onEvent(ContributionEvent.SelectQuickAmount(it)) },
                goalColor = goalColor
            )

            Spacer(Modifier.height(Dimens.CardSpacing))

            AccountSelectorCard(
                account = uiState.selectedAccount,
                availableCash = uiState.availableCash,
                goalColor = goalColor,
                onClick = { onEvent(ContributionEvent.ShowAccountPicker) }
            )

            if (uiState.isInsufficientFunds) {
                Spacer(Modifier.height(Dimens.CardSpacing))
                InsufficientFundsCard(shortfall = uiState.shortfall)
            }

            Spacer(Modifier.height(Dimens.CardSpacing))

            goal?.let {
                AllocationPreviewCard(
                    goal = it,
                    parsedAmount = uiState.parsedAmount,
                    newAmount = uiState.newGoalAmount,
                    newPercent = uiState.newProgressPercent,
                    goalColor = goalColor,
                    availableCash = uiState.availableCash,
                    newAvailableCash = uiState.newAvailableCash
                )
            }

            Spacer(Modifier.height(Dimens.CardSpacing))

            NotesCard(
                notes = uiState.notes,
                onNotesChange = { onEvent(ContributionEvent.UpdateNotes(it)) }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onEvent(ContributionEvent.SubmitContribution) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeightPrimary),
                enabled = uiState.isValid && !uiState.isSubmitting,
                shape = RoundedCornerShape(Dimens.ButtonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goalColor,
                    disabledContainerColor = goalColor.copy(alpha = 0.3f)
                )
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Outlined.Savings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Save to Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContributionHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = AppPalette.textPrimary
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Save to Goal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )
    }
}

@Composable
private fun GoalSummaryPreview(goal: Goal, goalColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CardRadius))
            .background(AppPalette.card)
            .padding(Dimens.CardInnerPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(goalColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getGoalIcon(goal.iconName),
                contentDescription = null,
                tint = goalColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Saving to",
                style = MaterialTheme.typography.labelMedium,
                color = AppPalette.textMuted
            )
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = goalColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = "${goal.progressPercent.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = goalColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AmountInputCard(
    rawAmount: String,
    onAmountChange: (String) -> Unit,
    goalColor: Color
) {
    OutlinedTextField(
        value = rawAmount,
        onValueChange = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "0",
                style = MaterialTheme.typography.displaySmall,
                color = AppPalette.textMuted.copy(alpha = 0.5f)
            )
        },
        prefix = {
            Text(
                text = "Rp ",
                style = MaterialTheme.typography.displaySmall,
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
}

@Composable
private fun QuickAmountChipsRow(
    selectedAmount: Double,
    onSelectAmount: (Long) -> Unit,
    goalColor: Color
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(QuickAmount.entries) { quickAmount ->
            val isSelected = selectedAmount == quickAmount.rawValue.toDouble()
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) goalColor else AppPalette.cardElevated,
                animationSpec = tween(200),
                label = "chip_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else AppPalette.textPrimary,
                animationSpec = tween(200),
                label = "chip_text"
            )
            Surface(
                onClick = { onSelectAmount(quickAmount.rawValue) },
                shape = RoundedCornerShape(12.dp),
                color = backgroundColor
            ) {
                Text(
                    text = quickAmount.displayText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelectorCard(
    account: Account?,
    availableCash: Double,
    goalColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CardRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = AppPalette.card,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (account == null) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = goalColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Select Account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.textMuted
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(goalColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAccountIcon(account.type),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        text = "Available: ${CurrencyUtils.formatAmount(availableCash)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessColor
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppPalette.textMuted
            )
        }
    }
}

@Composable
private fun InsufficientFundsCard(shortfall: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Insufficient Funds",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "Need ${CurrencyUtils.formatAmountCompact(shortfall)} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF795548)
                )
            }
        }
    }
}

@Composable
private fun AllocationPreviewCard(
    goal: Goal,
    parsedAmount: Double,
    newAmount: Double,
    newPercent: Double,
    goalColor: Color,
    availableCash: Double,
    newAvailableCash: Double
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
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        shape = RoundedCornerShape(Dimens.CardRadius)
    ) {
        Column(modifier = Modifier.padding(Dimens.CardInnerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppPalette.textMuted
                )
                Text(
                    text = "After Saving",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppPalette.textMuted
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = CurrencyUtils.formatAmountCompact(goal.currentAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = "${goal.progressPercent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatAmountCompact(animatedNewAmount.toDouble()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                    Text(
                        text = "${animatedNewPercent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                }
            }

            if (parsedAmount > 0) {
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${goal.progressPercent.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.textMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppPalette.cardBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((goal.progressPercent / 100).toFloat().coerceIn(0f, 1f))
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${animatedNewPercent.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = goalColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppPalette.cardBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((newPercent / 100).toFloat().coerceIn(0f, 1f))
                                    .background(goalColor)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = getMotivationText(parsedAmount, goal.progressPercent, newPercent),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (newPercent >= 100) SuccessColor else AppPalette.textMuted
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = AppPalette.cardBorder)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Available Cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.textMuted
                        )
                        Text(
                            text = "${CurrencyUtils.formatAmountCompact(availableCash)} → ${CurrencyUtils.formatAmountCompact(newAvailableCash)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            if (newPercent >= 100) {
                Spacer(Modifier.height(8.dp))
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
                    Spacer(Modifier.width(8.dp))
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

@Composable
private fun NotesCard(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Add notes (optional)",
                color = AppPalette.textMuted.copy(alpha = 0.5f)
            )
        },
        label = { Text("Notes") },
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LocalAccent.current,
            unfocusedBorderColor = AppPalette.cardBorder,
            focusedContainerColor = AppPalette.cardElevated,
            unfocusedContainerColor = AppPalette.cardElevated
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerDialog(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    goalColor: Color
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 4.dp).fillMaxWidth(),
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )

            Spacer(Modifier.height(16.dp))

            accounts.forEach { account ->
                val isSelected = account.id == selectedAccountId

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onAccountSelected(account.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) goalColor.copy(alpha = 0.1f) else AppPalette.cardElevated,
                    border = if (isSelected) BorderStroke(1.dp, goalColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) goalColor.copy(alpha = 0.12f)
                                    else AppPalette.cardBorder
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getAccountIcon(account.type),
                                contentDescription = null,
                                tint = if (isSelected) goalColor else AppPalette.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = AppPalette.textPrimary
                            )
                            Text(
                                text = "Balance: ${CurrencyUtils.formatAmount(account.balance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.textMuted
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Selected",
                                tint = goalColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}



private fun getAccountIcon(accountType: AccountType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (accountType) {
        AccountType.CASH -> Icons.Outlined.Money
        AccountType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance
        AccountType.E_WALLET -> Icons.Outlined.Wallet
        AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
        else -> Icons.Outlined.AccountBalance
    }
}

@Composable
private fun parseGoalColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        LocalAccent.current
    }
}

private fun getMotivationText(parsedAmount: Double, currentPercent: Double, newPercent: Double): String = when {
    parsedAmount <= 0 -> ""
    newPercent >= 100 -> "Goal will be completed!"
    newPercent >= 75 -> "Almost there! Keep going!"
    newPercent >= 50 -> "Halfway there!"
    newPercent >= 25 -> "Building momentum!"
    else -> "Great start! Every contribution counts!"
}
