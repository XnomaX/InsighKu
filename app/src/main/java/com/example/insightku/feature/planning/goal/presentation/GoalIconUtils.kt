package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

internal fun getGoalIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "savings", "piggy bank" -> Icons.Outlined.Savings
        "wallet", "account balance wallet" -> Icons.Outlined.AccountBalanceWallet
        "cash", "money", "paid" -> Icons.Outlined.Paid
        "flight", "airplane" -> Icons.Outlined.Flight
        "car", "directions car" -> Icons.Outlined.DirectionsCar
        "home", "house" -> Icons.Outlined.Home
        "school", "education", "graduation" -> Icons.Outlined.School
        "health", "health and safety" -> Icons.Outlined.HealthAndSafety
        "warning", "emergency" -> Icons.Outlined.Warning
        "trending up", "investment", "stocks" -> Icons.Outlined.TrendingUp
        "card giftcard", "gift" -> Icons.Outlined.CardGiftcard
        "celebration" -> Icons.Outlined.Celebration
        "star" -> Icons.Outlined.Star
        "flag", "target", "gps fixed" -> Icons.Outlined.Flag
        "beach", "travel" -> Icons.Outlined.BeachAccess
        "hotel", "suitcase" -> Icons.Outlined.Luggage
        "laptop", "technology" -> Icons.Outlined.Laptop
        "phone", "smartphone" -> Icons.Outlined.Smartphone
        "diamond", "gold", "investment" -> Icons.Outlined.Diamond
        else -> Icons.Outlined.Savings
    }
}
