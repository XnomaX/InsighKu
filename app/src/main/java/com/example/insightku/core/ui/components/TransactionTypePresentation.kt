package com.example.insightku.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.insightku.core.data.model.TransactionType
import androidx.compose.ui.graphics.Color
import com.example.insightku.core.ui.theme.AppPalette

/**
 * Semantic presentation for each [TransactionType].
 *
 * Every system-generated transaction gets a unique icon, color, and display title
 * so users can immediately distinguish transaction types at a glance.
 */
enum class TransactionTypePresentation(
    val icon: ImageVector,
    val color: Color,
    val label: String,
    val showAmountPrefix: Boolean = false,
    val allowsEdit: Boolean = false,
    val allowsDelete: Boolean = false,
    val showCategory: Boolean = false
) {
    INCOME(
        icon = Icons.Default.TrendingUp,
        color = AppPalette.success,
        label = "Income",
        showAmountPrefix = true,
        allowsEdit = true,
        allowsDelete = true,
        showCategory = true
    ),
    EXPENSE(
        icon = Icons.Default.TrendingDown,
        color = AppPalette.error,
        label = "Expense",
        showAmountPrefix = true,
        allowsEdit = true,
        allowsDelete = true,
        showCategory = true
    ),
    TRANSFER_OUT(
        icon = Icons.Default.SwapHoriz,
        color = AppPalette.defaultBlue,
        label = "Transfer Out"
    ),
    TRANSFER_IN(
        icon = Icons.Default.SwapHoriz,
        color = AppPalette.defaultBlue,
        label = "Transfer In"
    ),
    GOAL_CONTRIBUTION(
        icon = Icons.Default.Savings,
        color = AppPalette.notesPurple,
        label = "Goal Contribution"
    ),
    GOAL_WITHDRAWAL(
        icon = Icons.Default.Savings,
        color = AppPalette.cyan,
        label = "Goal Withdrawal"
    ),
    AUTO_ALLOCATION(
        icon = Icons.Default.AutoAwesome,
        color = AppPalette.indigo,
        label = "Auto Allocation"
    ),
    BALANCE_ADJUSTMENT(
        icon = Icons.Default.Tune,
        color = AppPalette.warning,
        label = "Balance Adjustment",
        allowsEdit = true,
        allowsDelete = true
    );

    companion object {
        /**
         * Get the presentation for a given [TransactionType].
         */
        fun forType(type: TransactionType): TransactionTypePresentation = when (type) {
            TransactionType.INCOME -> INCOME
            TransactionType.EXPENSE -> EXPENSE
            TransactionType.TRANSFER_OUT -> TRANSFER_OUT
            TransactionType.TRANSFER_IN -> TRANSFER_IN
            TransactionType.GOAL_CONTRIBUTION -> GOAL_CONTRIBUTION
            TransactionType.GOAL_WITHDRAWAL -> GOAL_WITHDRAWAL
            TransactionType.AUTO_ALLOCATION -> AUTO_ALLOCATION
            TransactionType.BALANCE_ADJUSTMENT -> BALANCE_ADJUSTMENT
        }

        /**
         * Get the display icon for a transaction type.
         * For INCOME/EXPENSE, falls back to category-based icon resolution.
         * For system transactions, always returns the semantic icon.
         */
        fun iconForType(type: TransactionType): ImageVector = forType(type).icon

        /**
         * Get the display color for a transaction type.
         */
        fun colorForType(type: TransactionType): Color = forType(type).color

        /**
         * Get the display label for a transaction type.
         */
        fun labelForType(type: TransactionType): String = forType(type).label
    }
}
