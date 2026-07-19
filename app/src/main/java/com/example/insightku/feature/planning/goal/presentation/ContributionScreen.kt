package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.feature.planning.goal.domain.model.Goal
import kotlinx.coroutines.delay

// ─── Quick Amount ─────────────────────────────────────────────────────────────

enum class QuickAmount(val rawValue: Long, val displayText: String) {
    K_10(10_000, "10K"),
    K_25(25_000, "25K"),
    K_50(50_000, "50K"),
    K_100(100_000, "100K"),
    K_250(250_000, "250K"),
    K_500(500_000, "500K"),
    K_1000(1_000_000, "1M")
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(goalId) { viewModel.onEvent(ContributionEvent.Initialize(goalId, prefillAmount)) }
    LaunchedEffect(uiState.contributionSaved) {
        if (uiState.contributionSaved) { delay(1500); onSuccess() }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.onEvent(ContributionEvent.ClearError) }
    }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.onEvent(ContributionEvent.ClearSnackbar) }
    }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize().background(AppPalette.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LocalAccent.current)
        }
        return
    }

    val goal = uiState.goal
    val goalColor = goal?.let {
        try { Color(android.graphics.Color.parseColor(it.color)) } catch (_: Exception) { LocalAccent.current }
    } ?: LocalAccent.current

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
                message = "${CurrencyUtils.formatAmountCompact(uiState.parsedAmount)} added to ${goal?.name ?: ""}",
                subtitle = null
            )
        }
    }
}

// ─── Content ──────────────────────────────────────────────────────────────────

@Composable
private fun ContributionContent(
    uiState: ContributionUiState,
    goal: Goal?,
    goalColor: Color,
    onBack: () -> Unit,
    onEvent: (ContributionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
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

            goal?.let { GoalSummaryPreview(goal = it, goalColor = goalColor) }
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

            androidx.compose.material3.Button(
                onClick = { onEvent(ContributionEvent.SubmitContribution) },
                modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeightPrimary),
                enabled = uiState.isValid && !uiState.isSubmitting,
                shape = RoundedCornerShape(Dimens.ButtonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goalColor,
                    disabledContainerColor = goalColor.copy(alpha = 0.3f)
                )
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Outlined.Savings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.contribution_screen_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun ContributionHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppPalette.textPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.contribution_screen_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )
    }
}

// ─── Goal Summary ─────────────────────────────────────────────────────────────

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
            Text(text = stringResource(R.string.contribution_screen_saving_to), style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted)
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
        }
        Surface(shape = RoundedCornerShape(8.dp), color = goalColor.copy(alpha = 0.12f)) {
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

// ─── Amount Input ─────────────────────────────────────────────────────────────

@Composable
private fun AmountInputCard(rawAmount: String, onAmountChange: (String) -> Unit, goalColor: Color) {
    OutlinedTextField(
        value = rawAmount,
        onValueChange = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(text = "0", style = MaterialTheme.typography.displaySmall, color = AppPalette.textMuted.copy(alpha = 0.5f))
        },
        prefix = {
            Text(text = "${NumberFormatter.getCurrencySymbol()} ", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = goalColor)
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

// ─── Quick Amount Chips ───────────────────────────────────────────────────────

@Composable
private fun QuickAmountChipsRow(selectedAmount: Double, onSelectAmount: (Long) -> Unit, goalColor: Color) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(QuickAmount.entries) { quickAmount ->
            val isSelected = selectedAmount == quickAmount.rawValue.toDouble()
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) goalColor else AppPalette.cardElevated,
                animationSpec = tween(200), label = "chip_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else AppPalette.textPrimary,
                animationSpec = tween(200), label = "chip_text"
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

// ─── Account Selector ─────────────────────────────────────────────────────────

@Composable
private fun AccountSelectorCard(account: Account?, availableCash: Double, goalColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.CardRadius)).clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = AppPalette.card,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (account == null) {
                Icon(imageVector = Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = goalColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(text = stringResource(R.string.contribution_screen_select_account), style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
            } else {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = getAccountIcon(account.type), contentDescription = null, tint = goalColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
                    Text(text = stringResource(R.string.contribution_screen_available, CurrencyUtils.formatAmount(availableCash)), style = MaterialTheme.typography.labelSmall, color = SuccessColor)
                }
            }
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = AppPalette.textMuted)
        }
    }
}

// ─── Insufficient Funds ───────────────────────────────────────────────────────

@Composable
private fun InsufficientFundsCard(shortfall: Double) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = AppPalette.insufficientFundsBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = AppPalette.insufficientFundsText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.contribution_screen_insufficient), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.insufficientFundsText)
                Text(text = stringResource(R.string.contribution_screen_need_more, CurrencyUtils.formatAmountCompact(shortfall)), style = MaterialTheme.typography.bodySmall, color = AppPalette.insufficientFundsDetail)
            }
        }
    }
}

// ─── Allocation Preview ───────────────────────────────────────────────────────

@Composable
private fun AllocationPreviewCard(goal: Goal, parsedAmount: Double, newAmount: Double, newPercent: Double, goalColor: Color, availableCash: Double, newAvailableCash: Double) {
    val animatedNewAmount by androidx.compose.animation.core.animateFloatAsState(
        targetValue = newAmount.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "amount"
    )
    val animatedNewPercent by androidx.compose.animation.core.animateFloatAsState(
        targetValue = newPercent.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "percent"
    )
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = AppPalette.card),
        shape = RoundedCornerShape(Dimens.CardRadius)
    ) {
        Column(modifier = Modifier.padding(Dimens.CardInnerPadding)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.contribution_screen_current), style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted)
                Text(text = stringResource(R.string.contribution_screen_after_saving), style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = CurrencyUtils.formatAmountCompact(goal.currentAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textMuted
                    )
                    Text(text = "${goal.progressPercent.toInt()}%", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
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
        }
    }
}

// ─── Notes ────────────────────────────────────────────────────────────────────

@Composable
private fun NotesCard(notes: String, onNotesChange: (String) -> Unit) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = stringResource(R.string.contribution_screen_add_notes), color = AppPalette.textMuted.copy(alpha = 0.5f)) },
        label = { Text(stringResource(R.string.contribution_screen_notes)) },
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

// ─── Account Picker Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerDialog(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    goalColor: Color
) {
    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {             Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(AppPalette.cardBorder))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(text = stringResource(R.string.contribution_screen_select_account), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
            Spacer(Modifier.height(16.dp))
            accounts.forEach { account ->
                val isSelected = account.id == selectedAccountId
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onAccountSelected(account.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) goalColor.copy(alpha = 0.1f) else AppPalette.cardElevated,
                    border = if (isSelected) BorderStroke(1.dp, goalColor) else null
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isSelected) goalColor.copy(alpha = 0.12f) else AppPalette.cardBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = getAccountIcon(account.type), contentDescription = null, tint = if (isSelected) goalColor else AppPalette.textMuted, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = AppPalette.textPrimary)
                            Text(text = stringResource(R.string.contribution_screen_balance, CurrencyUtils.formatAmount(account.balance)), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.content_selected), tint = goalColor, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun getAccountIcon(accountType: AccountType) = when (accountType) {
    AccountType.CASH -> Icons.Outlined.Money
    AccountType.BANK_ACCOUNT -> Icons.Filled.AccountBalance
    AccountType.E_WALLET -> Icons.Outlined.Wallet
    AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
}
