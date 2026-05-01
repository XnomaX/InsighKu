package com.example.insightku.ui.components.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.insightku.navigation.Route
import com.example.insightku.ui.components.analytics.AnalyticsScreen
import com.example.insightku.ui.components.budgeting.BudgetingScreen
import com.example.insightku.ui.components.dashboard.DashboardScreen
import com.example.insightku.ui.components.settings.SettingsScreen
import com.example.insightku.ui.components.addtransaction.AddTransactionDialog
import com.example.insightku.ui.components.dashboard.DashboardEvent
import com.example.insightku.viewmodel.DashboardViewModel
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.viewmodel.TransactionDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var pendingStreakPopup by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomNavBar(navController = navController, onAddClick = { showAddTransactionDialog = true }) }
    ) { paddingValues ->
        MainNavHost(
            navController = navController,
            rootNavController = rootNavController,
            dashboardViewModel = dashboardViewModel,
            onShowAddTransaction = { showAddTransactionDialog = true },
            onShowAddTransactionForStreak = {
                showAddTransactionDialog = true
                pendingStreakPopup = true
            },
            modifier = Modifier
                .padding(paddingValues)
                .statusBarsPadding()
        )
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            isOpen = showAddTransactionDialog,
            onDismiss = {
                showAddTransactionDialog = false
                pendingStreakPopup = false // cancelled, no popup
            },
            onTransactionAdded = { transaction ->
                dashboardViewModel.onEvent(DashboardEvent.AddTransaction(transaction))
                showAddTransactionDialog = false
                // pendingStreakPopup stays true → DashboardScreen will show it
            },
            onOpenScanner = {}
        )
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    rootNavController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    onShowAddTransaction: () -> Unit = {},
    onShowAddTransactionForStreak: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.HOME,
        modifier = modifier
    ) {
        composable(Route.HOME) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToTransactionDetails = {
                    navController.navigate(Route.TRANSACTION_DETAILS)
                },
                onAddTransaction = onShowAddTransaction,
                onAddTransactionForStreak = onShowAddTransactionForStreak
            )
        }
        composable(Route.TRANSACTION_DETAILS) {
            // BUG8 FIX: Ambil TransactionDetailsViewModel dan hubungkan
            // onEditTransaction/onDeleteTransaction ke database via ViewModel.
            // Sebelumnya kedua callback ini selalu {} (lambda kosong) → Edit/Delete tidak bekerja.
            val txDetailsViewModel: TransactionDetailsViewModel = hiltViewModel()
            com.example.insightku.ui.components.details.TransactionDetailsScreen(
                viewModel = txDetailsViewModel,
                initialTransactions = emptyList(),
                onBack = { navController.popBackStack() },
                onEditTransaction = { updatedTransaction ->
                    txDetailsViewModel.updateTransaction(updatedTransaction)
                },
                onDeleteTransaction = { transactionId ->
                    txDetailsViewModel.deleteTransaction(transactionId)
                    navController.popBackStack()
                }
            )
        }
        composable(Route.ANALYSIS) {
            AnalyticsScreen()
        }
        composable(Route.BUDGETING) {
            BudgetingScreen()
        }
        composable(Route.SETTINGS) {
            SettingsScreen(onLogout = {
                // BUG12 FIX: popUpTo(0) membersihkan seluruh back stack hingga root
                // agar user tidak bisa kembali ke Main setelah logout dengan tombol back.
                rootNavController.navigate(Route.AUTH_GRAPH) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
    }
}


@Composable
fun BottomNavBar(
    navController: NavHostController,
    onAddClick: () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val purpleColor = Color(0xFF5A2A82)

    // BUG13 FIX: Helper untuk navigasi tab yang benar — tidak menumpuk back stack
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            // Kembali ke start destination agar back stack tidak menumpuk
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Tidak buat instance baru jika sudah ada
            launchSingleTop = true
            // Restore state saat kembali ke tab yang pernah dibuka
            restoreState = true
        }
    }

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavButton(
                icon = Icons.Default.Home,
                label = "Home",
                isActive = currentRoute == Route.HOME,
                purpleColor = purpleColor,
                onClick = { navigateToTab(Route.HOME) }
            )
            BottomNavButton(
                icon = Icons.Default.BarChart,
                label = "Analytics",
                isActive = currentRoute == Route.ANALYSIS,
                purpleColor = purpleColor,
                onClick = { navigateToTab(Route.ANALYSIS) }
            )
            BottomNavButton(
                icon = Icons.Default.Add,
                label = "Add",
                isActive = false,
                purpleColor = purpleColor,
                isActionButton = true,
                onClick = onAddClick
            )
            BottomNavButton(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Budgeting",
                isActive = currentRoute == Route.BUDGETING,
                purpleColor = purpleColor,
                onClick = { navigateToTab(Route.BUDGETING) }
            )
            BottomNavButton(
                icon = Icons.Default.Settings,
                label = "Settings",
                isActive = currentRoute == Route.SETTINGS,
                purpleColor = purpleColor,
                onClick = { navigateToTab(Route.SETTINGS) }
            )
        }
    }
}


@Composable
fun BottomNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    purpleColor: Color,
    isActionButton: Boolean = false,
    onClick: () -> Unit
) {
    if (isActionButton) {
        // Action Button (Add) - Circular purple button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .clickable { onClick() }
                .padding(4.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp), // w-12 h-12
                shape = CircleShape,
                color = purpleColor,
                shadowElevation = 4.dp // shadow-lg
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp) // w-6 h-6
                    )
                }
            }
        }
    } else {
        // Regular Navigation Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .clickable { onClick() }
                .padding(4.dp)
        ) {
            // Icon Container
            Surface(
                modifier = Modifier.size(32.dp), // p-2
                shape = RoundedCornerShape(8.dp), // rounded-lg
                color = if (isActive) purpleColor else Color.Transparent
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp) // w-5 h-5
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp)) // gap-1

            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) purpleColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                fontSize = 12.sp // text-xs
            )
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreen(rootNavController = rememberNavController())
    }
}

@Preview
@Composable
fun BottomNavBarPreview() {
    MaterialTheme {
        val purpleColor = Color(0xFF6200EE)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                BottomNavButton(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isActive = true,
                    purpleColor = purpleColor,
                    isActionButton = false
                ) { }

                // 2. Analytics
                BottomNavButton(
                    icon = Icons.Default.BarChart,
                    label = "Analytics",
                    isActive = false,
                    purpleColor = purpleColor,
                    isActionButton = false
                ) { }

                // 3. Add (Plus) - Tengah
                BottomNavButton(
                    icon = Icons.Default.Add,
                    label = "Add",
                    isActive = false,
                    purpleColor = purpleColor,
                    isActionButton = true
                ) { }

                // 4. Budgeting
                BottomNavButton(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Budgeting",
                    isActive = false,
                    purpleColor = purpleColor,
                    isActionButton = false
                ) { }

                // 5. Settings
                BottomNavButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    isActive = false,
                    purpleColor = purpleColor,
                    isActionButton = false
                ) { }
            }
        }
    }
}

@Preview
@Composable
fun NavButtonPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BottomNavButton(
                icon = Icons.Default.Home,
                label = "Home",
                isActive = true,
                purpleColor = Color(0xFF6200EE)
            ) { }

            BottomNavButton(
                icon = Icons.Default.Add,
                label = "Add",
                isActive = false,
                purpleColor = Color(0xFF6200EE),
                isActionButton = true
            ) { }

            BottomNavButton(
                icon = Icons.Default.Settings,
                label = "Settings",
                isActive = false,
                purpleColor = Color(0xFF6200EE)
            ) { }
        }
    }
}
