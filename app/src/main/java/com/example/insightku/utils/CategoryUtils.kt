
package com.example.insightku.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryUtils {

    fun getIconForCategoryName(categoryName: String): ImageVector {
        return when (categoryName.lowercase()) {
            "food & drinks", "food" -> Icons.Default.Restaurant
            "transportation", "transport" -> Icons.Default.DirectionsCar
            "shopping" -> Icons.Default.ShoppingCart
            "entertainment" -> Icons.Default.Movie
            "healthcare", "health" -> Icons.Default.LocalHospital
            "utilities" -> Icons.Default.Lightbulb
            "housing" -> Icons.Default.Home
            "salary", "income" -> Icons.Default.AttachMoney
            "freelance" -> Icons.Default.Work
            "investment" -> Icons.Default.TrendingUp
            "gift" -> Icons.Default.CardGiftcard
            "bills & utilities" -> Icons.Default.Receipt
            "coffee" -> Icons.Default.LocalCafe
            else -> Icons.Default.Category
        }
    }

    fun getColorForCategoryName(categoryName: String): Color {
        return when (categoryName.lowercase()) {
            "food & drinks", "food" -> Color(0xFFF59E0B) // Amber
            "transportation", "transport" -> Color(0xFF3B82F6) // Blue
            "shopping" -> Color(0xFFEC4899) // Pink
            "entertainment" -> Color(0xFF8B5CF6) // Purple
            "healthcare", "health" -> Color(0xFFEF4444) // Red
            "utilities" -> Color(0xFF10B981) // Green
            "housing" -> Color(0xFF6B7280) // Gray
            "salary", "income" -> Color(0xFF10B981) // Green
            "freelance" -> Color(0xFF3B82F6) // Blue
            "investment" -> Color(0xFF8B5CF6) // Purple
            "gift" -> Color(0xFFF59E0B) // Amber
            "bills & utilities" -> Color(0xFFEF4444) // Red
            "coffee" -> Color(0xFFF59E0B) // Amber
            else -> Color.Gray // Default color
        }
    }
}
