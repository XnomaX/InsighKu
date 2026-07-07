package com.example.insightku.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver

// ─── Resolved Icon Data ──────────────────────────────────────────────────────

data class ResolvedCategoryIcon(
    val icon: ImageVector,
    val color: Color
)

// ─── Resolution Logic ────────────────────────────────────────────────────────

/**
 * Resolves the icon and color for a transaction category.
 * Handles both category-based resolution (Income/Expense) and
 * type-based resolution (Transfers, Goals, etc.).
 *
 * This is the single source of truth for transaction icon resolution,
 * used by Home → Recent Transactions, All Transactions, and Transaction Detail.
 *
 * @param categoryName The category name or icon key
 * @param transactionType Optional transaction type for system types (transfers, goals, etc.)
 * @param categoryMap Optional map of category name → Category for more accurate resolution
 */
fun resolveCategoryIcon(
    categoryName: String,
    transactionType: TransactionType? = null,
    categoryMap: Map<String, Category> = emptyMap()
): ResolvedCategoryIcon {
    val isSystemType = transactionType != null &&
        transactionType !in setOf(TransactionType.INCOME, TransactionType.EXPENSE)

    if (isSystemType) {
        val presentation = TransactionTypePresentation.forType(transactionType)
        return ResolvedCategoryIcon(presentation.icon, presentation.color)
    }

    val matchedCat = categoryMap[categoryName.trim().lowercase()]
    val resolved = CategoryIconResolver.resolve(
        matchedCat?.icon?.ifBlank { categoryName } ?: categoryName
    )
    val color = if (!matchedCat?.color.isNullOrBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(matchedCat!!.color)) }
            .getOrDefault(resolved.color)
    } else resolved.color

    return ResolvedCategoryIcon(resolved.icon, color)
}

// ─── Shared Composable ───────────────────────────────────────────────────────

/**
 * Shared composable for rendering a transaction category icon.
 * Used across Home → Recent Transactions, All Transactions, and Transaction Detail
 * to ensure consistent icon rendering throughout the application.
 *
 * @param categoryName The category name or icon key to resolve
 * @param transactionType Optional transaction type for system types (transfers, goals, etc.)
 * @param categoryMap Optional map of category name → Category for more accurate icon resolution
 * @param containerSize Size of the icon container (default 44.dp)
 * @param iconSize Size of the icon itself (default 20.dp)
 * @param cornerRadius Corner radius of the container (default 14.dp)
 * @param alpha Alpha value for the container background (default 0.12f)
 * @param borderColor Optional border color (none by default)
 * @param borderWidth Border width (only applied when borderColor is set)
 */
@Composable
fun TransactionCategoryIcon(
    categoryName: String,
    transactionType: TransactionType? = null,
    categoryMap: Map<String, Category> = emptyMap(),
    modifier: Modifier = Modifier,
    containerSize: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 14.dp,
    alpha: Float = 0.12f,
    borderColor: Color? = null,
    borderWidth: Dp = 0.dp
) {
    val resolved = resolveCategoryIcon(categoryName, transactionType, categoryMap)

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (borderColor != null && borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
                } else Modifier
            )
            .background(resolved.color.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(resolved.icon, null, tint = resolved.color, modifier = Modifier.size(iconSize))
    }
}
