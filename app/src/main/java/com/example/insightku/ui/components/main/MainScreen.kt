package com.example.insightku.ui.components.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.insightku.ui.dialogs.AddTransactionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Route.HOME

    val purpleColor = Color(0xFF5A2A82) // Sama seperti di contoh

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Bottom Navigation seperti di contoh React
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp), // 16 * 4 = 64dp (h-16)
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 1. Home
                    BottomNavButton(
                        icon = Icons.Default.Home,
                        label = "Home",
                        isActive = currentRoute == Route.HOME,
                        purpleColor = purpleColor,
                        isActionButton = false
                    ) {
                        if (currentRoute != Route.HOME) {
                            navController.navigate(Route.HOME) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }

                    // 2. Analytics
                    BottomNavButton(
                        icon = Icons.Default.BarChart,
                        label = "Analytics",
                        isActive = currentRoute == Route.ANALYSIS,
                        purpleColor = purpleColor,
                        isActionButton = false
                    ) {
                        if (currentRoute != Route.ANALYSIS) {
                            navController.navigate(Route.ANALYSIS) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }

                    // 3. Add (Action Button) - Tengah
                    BottomNavButton(
                        icon = Icons.Default.Add,
                        label = "Add",
                        isActive = false,
                        purpleColor = purpleColor,
                        isActionButton = true
                    ) {
                        showAddTransactionDialog = true
                    }

                    // 4. Budgeting
                    BottomNavButton(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Budgeting",
                        isActive = currentRoute == Route.BUDGETING,
                        purpleColor = purpleColor,
                        isActionButton = false
                    ) {
                        if (currentRoute != Route.BUDGETING) {
                            navController.navigate(Route.BUDGETING) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }

                    // 5. Settings
                    BottomNavButton(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        isActive = currentRoute == Route.SETTINGS,
                        purpleColor = purpleColor,
                        isActionButton = false
                    ) {
                        if (currentRoute != Route.SETTINGS) {
                            navController.navigate(Route.SETTINGS) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        MainNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            onLogout = onLogout
        )
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            isOpen = showAddTransactionDialog,
            onDismiss = { showAddTransactionDialog = false },
            onTransactionAdded = {
                showAddTransactionDialog = false
            },
            onOpenScanner = {
                showAddTransactionDialog = false
            }
        )
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Route.HOME,
        modifier = modifier
    ) {
        composable(Route.HOME) {
            DashboardScreen(
                onNavigateToTransactionDetails = {
                    navController.navigate(Route.TRANSACTION_DETAILS)
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
            SettingsScreen(onLogout = onLogout)
        }

        composable(Route.TRANSACTION_DETAILS) {
            // TODO: Create TransactionDetailsScreen
            // TransactionDetailsScreen(
            //     onNavigateBack = { navController.popBackStack() }
            // )

            // For now, showing a placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Transaction Details",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "This screen will show all transaction details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.popBackStack() }
                    ) {
                        Text("Go Back")
                    }
                }
            }
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
        MainScreen()
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
