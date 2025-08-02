/*
package com.example.insightku.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(
            title = "Dashboard",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            route = "dashboard"
        ),
        BottomNavItem(
            title = "Analytics",
            selectedIcon = Icons.Filled.Analytics,
            unselectedIcon = Icons.Outlined.Analytics,
            route = "analytics"
        ),
        BottomNavItem(
            title = "Budget",
            selectedIcon = Icons.Filled.AccountBalanceWallet,
            unselectedIcon = Icons.Outlined.AccountBalanceWallet,
            route = "budget"
        ),
        BottomNavItem(
            title = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        )
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                BottomNavItemContent(
                    item = item,
                    isSelected = selectedRoute == item.route,
                    onClick = { onRouteSelected(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemContent(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF5A2A82).copy(alpha = 0.1f)
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        NavigationBarItem(
            icon = {
                Box {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        tint = if (isSelected) Color(0xFF5A2A82) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    // Notification badge
                    if (item.hasNotification) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    Color(0xFFEF4444),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            },
            label = {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    color = if (isSelected) Color(0xFF5A2A82) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            selected = isSelected,
            onClick = onClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF5A2A82),
                selectedTextColor = Color(0xFF5A2A82),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Alternative simplified version if NavigationBarItem doesn't work as expected
@Composable
private fun SimpleBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF5A2A82).copy(alpha = 0.1f)
                else Color.Transparent
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .then(
                if (onClick != {}) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = if (isSelected) Color(0xFF5A2A82) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            if (item.hasNotification) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color(0xFFEF4444),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .align(Alignment.TopEnd)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isSelected) Color(0xFF5A2A82) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}*/
