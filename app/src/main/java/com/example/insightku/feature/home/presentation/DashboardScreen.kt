package com.example.insightku.feature.home.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalHideAmounts
import com.example.insightku.feature.home.presentation.dashboard.AiForecastCard
import com.example.insightku.feature.home.presentation.dashboard.BudgetPreviewSection
import com.example.insightku.feature.home.presentation.dashboard.DailyStreakCard
import com.example.insightku.feature.home.presentation.dashboard.DashboardHeader
import com.example.insightku.feature.home.presentation.dashboard.DraftInboxSection
import com.example.insightku.feature.home.presentation.dashboard.GoalsPreviewSection
import com.example.insightku.feature.home.presentation.dashboard.InsightsSection
import com.example.insightku.feature.home.presentation.dashboard.RecentTransactionsPreview
import com.example.insightku.feature.home.presentation.dashboard.StickyFinanceStatusBar
import com.example.insightku.feature.home.presentation.dashboard.StreakCelebrationDialog
import com.example.insightku.feature.home.presentation.dashboard.StreakDetailSheet
import com.example.insightku.feature.home.presentation.dashboard.UpcomingPaymentsSection

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
    onNavigateToNotifications: (() -> Unit)? = null,
    onNavigateToGoals: () -> Unit = {},
    onNavigateToBudgeting: () -> Unit = {},
    onCreateGoal: () -> Unit = {},
    onCreateBudget: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-derive when the applied language changes. The dashboard state bakes localized text
    // (txDisplayTitle, relative times) resolved once per rebuild, and this ViewModel survives
    // the Activity recreation that setApplicationLocales triggers — see DashboardViewModel.
    // onLocaleChanged. Keyed on the configuration's language tag (snapshot state, so it
    // re-runs); the ViewModel does its own comparison against what it built with.
    val languageTag = LocalConfiguration.current.locales.toLanguageTags()
    LaunchedEffect(languageTag) { viewModel.onLocaleChanged() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.recentTransactions.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.error != null && uiState.recentTransactions.isEmpty() -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                ) {
                    // Calm empty/error glyph — replaces the raw "??" placeholder.
                    Text(
                        text = "😮‍💨",
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
                    onNavigateToNotifications = onNavigateToNotifications,
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
    onNavigateToNotifications: (() -> Unit)? = null,
    onNavigateToGoals: () -> Unit = {},
    onNavigateToBudgeting: () -> Unit = {},
    onCreateGoal: () -> Unit = {},
    onCreateBudget: () -> Unit = {}
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
                    isBalanceVisible   = !LocalHideAmounts.current,
                    isLoading          = uiState.isLoading,
                    onToggleVisibility = { onEvent(DashboardEvent.ToggleBalanceVisibility) },
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToNotifications = onNavigateToNotifications
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
            // Actionable activity follows the balance so recent and pending items are quick to find.
            item {
                RecentTransactionsPreview(
                    transactions   = uiState.recentTransactions,
                    onViewAllClick = onNavigateToTransactionDetails,
                    onAddTransaction = onAddTransaction,
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
            item {
                GoalsPreviewSection(
                    goals = uiState.previewGoals,
                    totalCount = uiState.totalGoalCount,
                    isBalanceVisible = !LocalHideAmounts.current,
                    onClickGoal = { onNavigateToGoals() },
                    onClickViewAll = onNavigateToGoals,
                    onCreateGoal = onCreateGoal,
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = Dimens.CardSpacing
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
            item {
                DailyStreakCard(
                    currentStreak    = uiState.currentStreak,
                    hasTrackedToday  = uiState.hasTrackedToday,
                    trackedDayKeys   = uiState.trackedDayKeys,
                    repairAvailable  = uiState.repairAvailable,
                    onCardClick      = { showStreakDetail = true },
                    onAddTransaction = onAddTransactionForStreak,
                    onUseRepair      = { onEvent(DashboardEvent.UseStreakRepair) },
                    modifier         = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            item {
                AiForecastCard(
                    forecast       = uiState.forecast,
                    selectedPeriod = uiState.forecastPeriod,
                    onPeriodChange = { onEvent(DashboardEvent.ToggleForecastPeriod(it)) },
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
            trackedDayKeys  = uiState.trackedDayKeys,
            onDismiss       = { showStreakDetail = false }
        )
    } // Scaffold
}
