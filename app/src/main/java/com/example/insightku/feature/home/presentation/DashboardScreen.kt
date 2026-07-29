package com.example.insightku.feature.home.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
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
    onNavigateToBudgeting: () -> Unit = {},
    onCreateGoal: () -> Unit = {},
    onCreateBudget: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(DashboardEvent.ClearSnackbar)
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(DashboardEvent.ClearError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
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
                        text = stringResource(R.string.cannot_load_data),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiState.error ?: stringResource(R.string.check_connection),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.onEvent(DashboardEvent.RefreshData) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.try_again), fontWeight = FontWeight.SemiBold)
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
                onNavigateToBudgeting = onNavigateToBudgeting,
                onCreateGoal = onCreateGoal,
                onCreateBudget = onCreateBudget
            )
        }
    }
    }
}

// --- Screen Content -----------------------------------------------------------

private const val RECOMP_TAG = "DashboardRecomp"

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
    onNavigateToBudgeting: () -> Unit = {},
    onCreateGoal: () -> Unit = {},
    onCreateBudget: () -> Unit = {}
) {
    val density = LocalDensity.current
    var showStreakPopup by remember { mutableStateOf(false) }
    var showStreakDetail by remember { mutableStateOf(false) }

    // PERF FIX: Use LazyListState instead of tracking scrollOffsetPx manually
    // This avoids recomposing the entire screen on every scroll event
    val lazyListState = rememberLazyListState()
    val stickyThresholdPx = with(density) { 260.dp.toPx() }

    // Stable lambdas to avoid recreation on recomposition
    val onToggleBalanceVisibility = remember { { onEvent(DashboardEvent.ToggleBalanceVisibility) } }
    val onShowStreakDetail = remember { { showStreakDetail = true } }
    val onUseStreakRepair = remember { { onEvent(DashboardEvent.UseStreakRepair) } }
    val onClickGoal = remember { { _: String -> onNavigateToGoals() } }
    val onCreateGoalLambda = remember { { onCreateGoal() } }
    val onToggleForecastPeriod = remember { { period: String -> onEvent(DashboardEvent.ToggleForecastPeriod(period)) } }
    val onMarkRecurringPaid = remember { { budget: com.example.insightku.core.data.model.RecurringBudget -> onEvent(DashboardEvent.MarkRecurringPaid(budget)) } }
    val onMarkInstallmentPaid = remember { { installment: com.example.insightku.core.data.model.Installment -> onEvent(DashboardEvent.MarkInstallmentPaid(installment)) } }
    val onDismissDraftLambda = remember { { draft: com.example.insightku.core.data.model.DraftTransaction ->
        onEvent(DashboardEvent.DismissDraft(draft.id))
        onDraftDismissed(draft)
    } }

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
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimens.ContentBottomPadding)
        ) {
            item(key = "header") {
                DashboardHeader(
                    userName           = uiState.userName,
                    monthlySavings     = uiState.monthlySavings,
                    currentStreak      = uiState.currentStreak,
                    isBalanceVisible   = !LocalHideAmounts.current,
                    isLoading          = uiState.isLoading,
                    onToggleVisibility = onToggleBalanceVisibility,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            item(key = "hero_balance") {
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
            item(key = "draft_inbox") {
                DraftInboxSection(
                    drafts         = uiState.pendingDrafts,
                    onOpenDraft    = onOpenDraft,
                    onDismissDraft = onDismissDraftLambda,
                    modifier       = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item(key = "daily_streak") {
                val isScrolling by remember {
                    derivedStateOf { lazyListState.isScrollInProgress }
                }
                DailyStreakCard(
                    currentStreak    = uiState.currentStreak,
                    hasTrackedToday  = uiState.hasTrackedToday,
                    repairAvailable  = uiState.repairAvailable,
                    freezeCount      = uiState.freezeCount,
                    onCardClick      = onShowStreakDetail,
                    onAddTransaction = onAddTransactionForStreak,
                    onUseRepair      = onUseStreakRepair,
                    isScrolling      = isScrolling,
                    modifier         = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item(key = "insights") {
                InsightsSection(
                    insightMessages = uiState.insightMessages,
                    modifier        = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item(key = "goals_preview") {
                GoalsPreviewSection(
                    goals = uiState.previewGoals,
                    totalCount = uiState.totalGoalCount,
                    isBalanceVisible = !LocalHideAmounts.current,
                    onClickGoal = onClickGoal,
                    onClickViewAll = onNavigateToGoals,
                    onCreateGoal = onCreateGoalLambda,
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = Dimens.CardSpacing
                    )
                )
            }
            item(key = "budget_preview") {
                BudgetPreviewSection(
                    budgets = uiState.previewBudgets,
                    totalCount = uiState.totalBudgetCount,
                    isBalanceVisible = !LocalHideAmounts.current,
                    onClickViewAll = onNavigateToBudgeting,
                    onNavigateToBudgeting = onNavigateToBudgeting,
                    onCreateBudget = onCreateBudget,
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = Dimens.CardSpacing
                    )
                )
            }
            item(key = "ai_forecast") {
                AiForecastCard(
                    weeklyData     = uiState.weeklyForecastData,
                    monthlyData    = uiState.monthlyForecastData,
                    aiInsight      = uiState.aiInsightMessage,
                    selectedPeriod = uiState.forecastPeriod,
                    onPeriodChange = onToggleForecastPeriod,
                    modifier       = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item(key = "upcoming_payments") {
                UpcomingPaymentsSection(
                    recurringBudgets      = uiState.recurringBudgets,
                    installments          = uiState.installments,
                    onMarkRecurringPaid   = onMarkRecurringPaid,
                    onMarkInstallmentPaid = onMarkInstallmentPaid,
                    modifier              = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item(key = "recent_transactions") {
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

        // PERF FIX: Extract sticky bar into separate composable to isolate scroll state reading
        StickyBar(
            lazyListState = lazyListState,
            stickyThresholdPx = stickyThresholdPx,
            monthlyIncome = uiState.monthlyIncome,
            monthlyExpenses = uiState.monthlyExpenses,
            currentStreak = uiState.currentStreak,
            hasTrackedToday = uiState.hasTrackedToday,
            freezeCount = uiState.freezeCount,
            modifier = Modifier.align(Alignment.TopCenter)
        )
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
    } // Scaffold
}

@Composable
private fun StickyBar(
    lazyListState: LazyListState,
    stickyThresholdPx: Float,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    currentStreak: Int,
    hasTrackedToday: Boolean,
    freezeCount: Int,
    modifier: Modifier = Modifier
) {
    // Only this composable reads scroll state, isolating recomposition
    val showStickyBar by derivedStateOf {
        lazyListState.firstVisibleItemIndex > 0 ||
        lazyListState.firstVisibleItemScrollOffset > stickyThresholdPx
    }

    AnimatedVisibility(
        visible = showStickyBar,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
        modifier = modifier
    ) {
        StickyFinanceStatusBar(
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            currentStreak = currentStreak,
            hasTrackedToday = hasTrackedToday,
            freezeCount = freezeCount
        )
    }
}
