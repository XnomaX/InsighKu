package com.example.insightku.core.navigation

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = Route.HOME,
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Analysis : BottomNavItem(
        route = Route.ANALYSIS,
        title = "Analisis",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics
    )

    object Budgeting : BottomNavItem(
        route = Route.BUDGETING,
        title = "Budgeting",
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance
    )

    object Accounts : BottomNavItem(
        route = Route.ACCOUNTS,
        title = "Accounts",
        selectedIcon = Icons.Filled.Wallet,
        unselectedIcon = Icons.Outlined.Wallet
    )
}

// List untuk Bottom Navigation (4 items, FAB di tengah)
val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Analysis,
    BottomNavItem.Budgeting,
    BottomNavItem.Accounts
)
