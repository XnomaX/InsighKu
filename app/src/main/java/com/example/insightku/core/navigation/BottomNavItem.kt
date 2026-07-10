package com.example.insightku.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.insightku.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = Route.HOME,
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Analysis : BottomNavItem(
        route = Route.ANALYSIS,
        titleRes = R.string.nav_analysis,
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics
    )

    object Budgeting : BottomNavItem(
        route = Route.BUDGETING,
        titleRes = R.string.nav_budgeting,
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance
    )

    object Accounts : BottomNavItem(
        route = Route.ACCOUNTS,
        titleRes = R.string.nav_accounts,
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
