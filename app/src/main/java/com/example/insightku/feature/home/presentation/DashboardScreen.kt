package com.example.insightku.feature.home.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalHideAmounts
import com.example.insightku.feature.home.presentation.dashboard.*

// --- Root Screen -------------------------------------------------------------

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTransactionDetails: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddTransactionForStreak: () -> Unit = onAddTransaction,
    onOpenDraft: (com.example.insightku.core.data.model.DraftTransaction) -> Unit = {},
    onDraftDismissed: (com.example.insightku.core.data.model.DraftTransaction) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToBudgeting: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.recentTransactions.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null && uiState.recentTransactions.isEmpty() && !uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "??",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = "Tidak dapat memuat data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiState.error ?: "Periksa koneksi internet dan coba lagi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.onEvent(DashboardEvent.RefreshData) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Coba Lagi", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        else -> {
            DashboardScreenContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onNavigateToTransactionDetails = onNavigateToTransactionDetails,
                onAddTransaction = onAddTransaction,
                onAddTransactionForStreak = onAddTransactionForStreak,
                onOpenDraft = onOpenDraft,
                onDraftDismissed = onDraftDismissed,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToGoals = onNavigateToGoals,
                onNavigateToBudgeting = onNavigateToBudgeting
            )
        }
    }
}

// --- Screen Content -----------------------------------------------------------

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    onNavigateToTransactionDetails: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddTransactionForStreak: () -> Unit = onAddTransaction,
    onOpenDraft: (com.example.insightku.core.data.model.DraftTransaction) -> Unit = {},
    onDraftDismissed: (com.example.insightku.core.data.model.DraftTransaction) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToBudgeting: () -> Unit = {}
) {
    val density = LocalDensity.current
    var showStreakPopup by remember { mutableStateOf(false) }
    var showStreakDetail by remember { mutableStateOf(false) }

    val stickyThresholdPx = with(density) { 260.dp.toPx() }
    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }
    val showStickyBar by remember { derivedStateOf { scrollOffsetPx < -stickyThresholdPx } }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                scrollOffsetPx = (scrollOffsetPx + available.y).coerceAtMost(0f)
                return Offset.Zero
            }
        }
    }

    val prevTracked = remember { mutableStateOf(uiState.hasTrackedToday) }
    LaunchedEffect(uiState.hasTrackedToday) {
        if (uiState.hasTrackedToday && !prevTracked.value) showStreakPopup = true
        prevTracked.value = uiState.hasTrackedToday
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(bottom = Dimens.ContentBottomPadding)
        ) {
            item {
                DashboardHeader(
                    userName           = uiState.userName,
                    monthlySavings     = uiState.monthlySavings,
                    currentStreak      = uiState.currentStreak,
                    isBalanceVisible   = !LocalHideAmounts.current,
                    isLoading          = uiState.isLoading,
                    onToggleVisibility = { onEvent(DashboardEvent.ToggleBalanceVisibility) },
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            item {
                HeroBalanceCard(
                    totalBalance      = uiState.totalBalance,
                    accountBalance    = uiState.totalAccountBalance,
                    accountCount      = uiState.accountCount,
                    monthlyIncome     = uiState.monthlyIncome,
                    monthlyExpenses   = uiState.monthlyExpenses,
                    monthlySavings    = uiState.monthlySavings,
                    isBalanceVisible  = !LocalHideAmounts.current,
                    modifier          = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = 4.dp
                    )
                )
            }
            item {
                DraftInboxSection(
                    drafts         = uiState.pendingDrafts,
                    onOpenDraft    = onOpenDraft,
                    onDismissDraft = { draft ->
                        onEvent(DashboardEvent.DismissDraft(draft.id))
                        onDraftDismissed(draft)
                    },
                    modifier       = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item {
                DailyStreakCard(
                    currentStreak    = uiState.currentStreak,
                    hasTrackedToday  = uiState.hasTrackedToday,
                    onCardClick      = { showStreakDetail = true },
                    onAddTransaction = onAddTransactionForStreak,
                    modifier         = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item {
                InsightsSection(
                    insightMessages = uiState.insightMessages,
                    modifier        = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            // Goals preview list (always shown — handles empty state)
            item {
                GoalsPreviewSection(
                    goals = uiState.previewGoals,
                    totalCount = uiState.totalGoalCount,
                    isBalanceVisible = !LocalHideAmounts.current,
                    onClickGoal = { /* TODO: navigate to goal detail */ },
                    onClickViewAll = onNavigateToGoals,
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = Dimens.CardSpacing
                    )
                )
            }
            // Budget preview list (always shown — handles empty state)
            item {
                BudgetPreviewSection(
                    budgets = uiState.previewBudgets,
                    totalCount = uiState.totalBudgetCount,
                    isBalanceVisible = !LocalHideAmounts.current,
                    onClickViewAll = onNavigateToBudgeting,
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = Dimens.CardSpacing
                    )
                )
            }
            item {
                AiForecastCard(
                    weeklyData     = uiState.weeklyForecastData,
                    monthlyData    = uiState.monthlyForecastData,
                    aiInsight      = uiState.aiInsightMessage,
                    selectedPeriod = uiState.forecastPeriod,
                    onPeriodChange = { onEvent(DashboardEvent.ToggleForecastPeriod(it)) },
                    modifier       = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item {
                UpcomingPaymentsSection(
                    recurringBudgets      = uiState.recurringBudgets,
                    installments          = uiState.installments,
                    onMarkRecurringPaid   = { onEvent(DashboardEvent.MarkRecurringPaid(it)) },
                    onMarkInstallmentPaid = { onEvent(DashboardEvent.MarkInstallmentPaid(it)) },
                    modifier              = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item {
                RecentTransactionsPreview(
                    transactions   = uiState.recentTransactions,
                    onViewAllClick = onNavigateToTransactionDetails,
                    modifier       = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
        }

        AnimatedVisibility(
            visible  = showStickyBar,
            enter    = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it },
            exit     = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            StickyFinanceStatusBar(
                monthlyIncome   = uiState.monthlyIncome,
                monthlyExpenses = uiState.monthlyExpenses,
                currentStreak   = uiState.currentStreak,
                hasTrackedToday = uiState.hasTrackedToday,
                freezeCount     = uiState.freezeCount
            )
        }
    }

    if (showStreakPopup) {
        StreakCelebrationDialog(streak = uiState.currentStreak, onDismiss = { showStreakPopup = false })
    }
    if (showStreakDetail) {
        StreakDetailSheet(
            currentStreak   = uiState.currentStreak,
            bestStreak      = uiState.bestStreak,
            hasTrackedToday = uiState.hasTrackedToday,
            onDismiss       = { showStreakDetail = false }
        )
    }
}
