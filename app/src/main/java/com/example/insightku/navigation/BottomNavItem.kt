package com.example.insightku.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.*

data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasNews: Boolean = false,
    val badgeCount: Int? = null
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, Icons.Default.Dashboard, Icons.Default.Dashboard),
    BottomNavItem(Screen.Analytics, Icons.Default.Analytics, Icons.Default.Analytics),
    BottomNavItem(Screen.Budgeting, Icons.Default.AccountBalance, Icons.Default.AccountBalance),
    BottomNavItem(Screen.Settings, Icons.Default.Settings, Icons.Default.Settings),
    BottomNavItem(Screen.AddTransaction, Icons.Default.Add, Icons.Default.Add)
)
