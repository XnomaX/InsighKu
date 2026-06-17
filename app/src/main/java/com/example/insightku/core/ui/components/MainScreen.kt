package com.example.insightku.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.insightku.core.navigation.Route
import com.example.insightku.feature.analytics.presentation.AnalyticsScreen
import com.example.insightku.feature.budgeting.presentation.BudgetingEvent
import com.example.insightku.feature.budgeting.presentation.BudgetingScreen
import com.example.insightku.feature.home.presentation.DashboardScreen
import com.example.insightku.feature.settings.presentation.SettingsScreen
import com.example.insightku.feature.home.presentation.AddTransactionDialog
import com.example.insightku.feature.budgeting.presentation.DialogState
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.home.presentation.AddTransactionViewModel
import com.example.insightku.feature.budgeting.presentation.BudgetingViewModel
import com.example.insightku.feature.home.presentation.DashboardViewModel
import com.example.insightku.feature.home.presentation.TransactionDetailsViewModel
import com.example.insightku.feature.home.presentation.TransactionDetailsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.insightku.core.notification.NotificationTransactionData
import com.example.insightku.core.data.model.DraftTransaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.home.presentation.DashboardEvent

// ─── Design tokens ────────────────────────────────────────────────────────────

private val NavPurple   = Color(0xFF7C4DFF)
private val NavBorder: Color  @Composable get() = AppPalette.cardBorder
private val NavBg: Color      @Composable get() = AppPalette.card
private val NavInactive = Color(0xFFB0AABF)

/** Konversi draft → NotificationTransactionData agar bisa pakai ulang AddTransactionDialog prefilled. */
private fun DraftTransaction.toNotificationData(): NotificationTransactionData =
    NotificationTransactionData(
        amount      = amountGuess,
        title       = merchantGuess,
        bankName    = bankName,
        typeHint    = if (typeGuess == TransactionType.INCOME) "INCOME" else "EXPENSE",
        timestamp   = detectedAt,
        description = rawContent,
        draftId     = id
    )

// ─── Nav item data ────────────────────────────────────────────────────────────

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem(Route.HOME,      "Home",      Icons.Filled.Home),
    NavItem(Route.ANALYSIS,  "Analytics", Icons.Filled.BarChart),
    NavItem(Route.BUDGETING, "Budget",    Icons.Filled.AccountBalanceWallet),
    NavItem(Route.SETTINGS,  "Settings",  Icons.Filled.Settings)
)

// ─── MainScreen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    rootNavController: NavHostController,
    notificationData: NotificationTransactionData? = null
) {
    val navController                = rememberNavController()
    val dashboardViewModel: DashboardViewModel           = hiltViewModel()
    val addTransactionViewModel: AddTransactionViewModel = hiltViewModel()
    val budgetingViewModel: BudgetingViewModel           = hiltViewModel()
    val expenseCategories          by addTransactionViewModel.expenseCategories.collectAsState()
    val incomeCategories           by addTransactionViewModel.incomeCategories.collectAsState()
    val addTxUiState               by addTransactionViewModel.uiState.collectAsState()
    val budgetingUiState           by budgetingViewModel.uiState.collectAsState()
    var showAddTransactionDialog   by remember { mutableStateOf(false) }
    var pendingStreakPopup          by remember { mutableStateOf(false) }
    // Prefill source for AddTransaction: either the launch deep link, or a tapped draft.
    var activeDraftData            by remember { mutableStateOf<NotificationTransactionData?>(null) }

    // Auto-open AddTransaction when app is launched from bank notification deep link
    LaunchedEffect(notificationData) {
        if (notificationData != null) {
            activeDraftData = notificationData
            showAddTransactionDialog = true
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Derived — nav hides whenever ANY overlay is open.
    // TRANSACTION_DETAILS is a regular nav destination — navbar stays visible so
    // user can tap Home to return. Overlays (dialogs/sheets) hide it.
    val anyDialogOpen by remember {
        derivedStateOf {
            showAddTransactionDialog ||
            budgetingUiState.dialogState !is DialogState.None
        }
    }

    LaunchedEffect(addTxUiState.savedSuccessfully) {
        if (addTxUiState.savedSuccessfully) {
            showAddTransactionDialog = false
            pendingStreakPopup        = false
            addTransactionViewModel.clearSavedState()
        }
    }

    var backPressedOnce      by remember { mutableStateOf(false) }
    val snackbarHostState    = remember { SnackbarHostState() }
    val density              = LocalDensity.current
    var navBarHeightDp       by remember { mutableStateOf(0.dp) }
    val context              = LocalContext.current
    val scope                = rememberCoroutineScope()

    // Handle back: dismiss budgeting dialogs first, then add-transaction, then double-tap exit
    BackHandler(enabled = budgetingUiState.dialogState !is DialogState.None) {
        budgetingViewModel.onEvent(
            when (budgetingUiState.dialogState) {
                is DialogState.AddBudget            -> BudgetingEvent.HideAddBudgetDialog
                is DialogState.EditBudget           -> BudgetingEvent.HideEditBudgetDialog
                is DialogState.DeleteConfirm        -> BudgetingEvent.HideDeleteConfirmDialog
                is DialogState.ManageRecurring      -> BudgetingEvent.HideRecurringDialog
                is DialogState.AddRecurringPayment  -> BudgetingEvent.HideRecurringDialog
                is DialogState.EditRecurringPayment -> BudgetingEvent.HideRecurringDialog
                is DialogState.AddInstallment       -> BudgetingEvent.HideInstallmentDialog
                is DialogState.EditInstallment      -> BudgetingEvent.HideInstallmentDialog
                else                                -> BudgetingEvent.ClearError
            }
        )
    }

    BackHandler(enabled = showAddTransactionDialog) {
        showAddTransactionDialog = false
        pendingStreakPopup        = false
    }

    BackHandler(enabled = !anyDialogOpen) {
        if (backPressedOnce) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedOnce = true
        }
    }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            snackbarHostState.showSnackbar(
                message  = "Tekan back sekali lagi untuk keluar",
                duration = SnackbarDuration.Short
            )
            delay(2000)
            backPressedOnce = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main content — full screen, nav bar floats on top
        Scaffold(
            modifier            = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost        = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier  = Modifier.padding(bottom = navBarHeightDp)
                )
            },
            containerColor      = Color.Transparent,
            bottomBar           = {}
        ) { innerPadding ->
            MainNavHost(
                navController                 = navController,
                rootNavController             = rootNavController,
                dashboardViewModel            = dashboardViewModel,
                budgetingViewModel            = budgetingViewModel,
                onShowAddTransaction          = { showAddTransactionDialog = true },
                onShowAddTransactionForStreak = {
                    showAddTransactionDialog = true
                    pendingStreakPopup        = true
                },
                onOpenDraft = { draft ->
                    activeDraftData = draft.toNotificationData()
                    showAddTransactionDialog = true
                },
                onDraftDismissed = { draft ->
                    // Draft sudah ditandai DISMISSED oleh ViewModel (hilang dari Inbox).
                    // Tawarkan undo 5 detik; jika tidak di-undo, hard-delete.
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message     = "Draft dihapus",
                            actionLabel = "Urungkan",
                            duration    = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            dashboardViewModel.onEvent(DashboardEvent.UndoDismissDraft(draft.id))
                        } else {
                            dashboardViewModel.onEvent(DashboardEvent.CommitDismissDraft(draft.id))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(AppPalette.background)
            )
        }

        // Floating premium bottom nav — hides when any dialog is open
        AnimatedVisibility(
            visible  = !anyDialogOpen,
            enter    = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
            exit     = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Transparent)
        ) {
            PremiumBottomNav(
                navController = navController,
                onAddClick    = { showAddTransactionDialog = true },
                modifier      = Modifier
                    .onSizeChanged { size ->
                        navBarHeightDp = with(density) { size.height.toDp() }
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        // Add Transaction overlay — above everything including nav
        AddTransactionDialog(
            isOpen    = showAddTransactionDialog,
            onDismiss = {
                showAddTransactionDialog = false
                pendingStreakPopup        = false
                activeDraftData           = null
            },
            onTransactionAdded = { transaction ->
                addTransactionViewModel.addTransaction(transaction, activeDraftData?.draftId)
            },
            onOpenScanner      = {},
            expenseCategories  = expenseCategories,
            incomeCategories   = incomeCategories,
            notificationData   = activeDraftData,
            onCreateCategory   = {
                navController.navigate(Route.BUDGETING) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState    = true
                }
                showAddTransactionDialog = false
            }
        )
    }
}

// ─── Premium Bottom Nav ───────────────────────────────────────────────────────

@Composable
fun PremiumBottomNav(
    navController: NavHostController,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                // Don't save HOME state — always return to clean root
                saveState = route != Route.HOME
            }
            launchSingleTop = true
            restoreState    = route != Route.HOME
        }
    }

    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(32.dp),
        color           = NavBg,
        tonalElevation  = 0.dp,
        shadowElevation = 4.dp,
        border          = androidx.compose.foundation.BorderStroke(
            1.dp, NavBorder
        )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
                // Home
                BottomNavItem(
                    item       = navItems[0],
                    isSelected = currentRoute == navItems[0].route,
                    onClick    = { navigateToTab(navItems[0].route) }
                )

                // Analytics
                BottomNavItem(
                    item       = navItems[1],
                    isSelected = currentRoute == navItems[1].route,
                    onClick    = { navigateToTab(navItems[1].route) }
                )

                // Center Add button
                CenterAddButton(onClick = onAddClick)

                // Budgeting
                BottomNavItem(
                    item       = navItems[2],
                    isSelected = currentRoute == navItems[2].route,
                    onClick    = { navigateToTab(navItems[2].route) }
                )

                // Settings
                BottomNavItem(
                    item       = navItems[3],
                    isSelected = currentRoute == navItems[3].route,
                    onClick    = { navigateToTab(navItems[3].route) }
                )
        }
    }
}

// ─── Nav Item ─────────────────────────────────────────────────────────────────

@Composable
private fun BottomNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val NavPurple = LocalAccent.current // global accent — recolors the nav live
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val iconScale by animateFloatAsState(
        targetValue   = when {
            isPressed  -> 0.88f
            isSelected -> 1.08f
            else       -> 1f
        },
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label         = "icon_scale_${item.label}"
    )

    val pillWidth by animateDpAsState(
        targetValue   = if (isSelected) 56.dp else 40.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label         = "pill_width_${item.label}"
    )

    val pillAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label         = "pill_alpha_${item.label}"
    )

    Column(
        modifier              = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(4.dp)
    ) {
        // Icon with animated pill background
        Box(
            modifier          = Modifier
                .width(pillWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(NavPurple.copy(alpha = pillAlpha * 0.10f)),
            contentAlignment  = Alignment.Center
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = item.label,
                modifier           = Modifier.size(20.dp).scale(iconScale),
                tint               = if (isSelected) NavPurple else NavInactive
            )
        }

        // Label
        Text(
            text       = item.label,
            style      = MaterialTheme.typography.labelSmall,
            fontSize   = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = if (isSelected) NavPurple else NavInactive
        )
    }
}

// ─── Center Add Button ────────────────────────────────────────────────────────

@Composable
fun CenterAddButton(onClick: () -> Unit) {
    val NavPurple = LocalAccent.current // global accent — recolors the add button live
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label         = "add_scale"
    )

    Column(
        modifier            = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(scale)
                .shadow(
                    elevation    = 4.dp,
                    shape        = CircleShape,
                    ambientColor = NavPurple.copy(alpha = 0.15f),
                    spotColor    = NavPurple.copy(alpha = 0.20f)
                )
                .clip(CircleShape)
                .background(NavPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Add Transaction",
                tint               = Color.White,
                modifier           = Modifier.size(22.dp)
            )
        }

        Text(
            text       = "Add",
            style      = MaterialTheme.typography.labelSmall,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color      = NavPurple
        )
    }
}

// ─── NavHost ──────────────────────────────────────────────────────────────────

@Composable
private fun MainNavHost(
    navController: NavHostController,
    rootNavController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    budgetingViewModel: BudgetingViewModel,
    onShowAddTransaction: () -> Unit = {},
    onShowAddTransactionForStreak: () -> Unit = {},
    onOpenDraft: (DraftTransaction) -> Unit = {},
    onDraftDismissed: (DraftTransaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = Route.HOME,
        modifier         = modifier
    ) {
        composable(Route.HOME) {
            DashboardScreen(
                viewModel                      = dashboardViewModel,
                onNavigateToTransactionDetails = {
                    navController.navigate(Route.TRANSACTION_DETAILS) {
                        // Keep HOME in back stack so Back returns to it
                        launchSingleTop = true
                    }
                },
                onAddTransaction               = onShowAddTransaction,
                onAddTransactionForStreak      = onShowAddTransactionForStreak,
                onOpenDraft                    = onOpenDraft,
                onDraftDismissed               = onDraftDismissed
            )
        }
        composable(Route.TRANSACTION_DETAILS) {
            val txDetailsViewModel: TransactionDetailsViewModel = hiltViewModel()
            TransactionDetailsScreen(
                viewModel           = txDetailsViewModel,
                initialTransactions = emptyList(),
                onBack              = { navController.popBackStack() },
                onEditTransaction   = { txDetailsViewModel.updateTransaction(it) },
                onDeleteTransaction = { id ->
                    txDetailsViewModel.deleteTransaction(id)
                }
            )
        }
        composable(Route.ANALYSIS) { AnalyticsScreen() }
        composable(Route.BUDGETING) { BudgetingScreen(viewModel = budgetingViewModel) }
        composable(Route.SETTINGS) {
            SettingsScreen(
                onLogout = {
                    rootNavController.navigate(Route.AUTH_GRAPH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToNotificationDebug = { navController.navigate(Route.NOTIFICATION_DEBUG) },
                onNavigateToBankWhitelist = { navController.navigate(Route.BANK_WHITELIST) },
                onNavigateToAutoDetectionOnboarding = { navController.navigate(Route.AUTO_DETECTION_ONBOARDING) }
            )
        }
        composable(Route.NOTIFICATION_DEBUG) {
            com.example.insightku.core.notification.NotificationDebugScreen()
        }
        composable(Route.BANK_WHITELIST) {
            com.example.insightku.feature.settings.presentation.BankWhitelistScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.AUTO_DETECTION_ONBOARDING) {
            com.example.insightku.feature.settings.presentation.AutoDetectionOnboardingScreen(
                onFinish = { navController.popBackStack() }
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFFAF9FE)
@Composable
private fun PremiumBottomNavPreview() {
    MaterialTheme {
        Box(
            modifier          = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAF9FE))
                .padding(16.dp),
            contentAlignment  = Alignment.BottomCenter
        ) {
            PremiumBottomNav(
                navController = rememberNavController(),
                onAddClick    = {}
            )
        }
    }
}




