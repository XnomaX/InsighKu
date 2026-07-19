package com.example.insightku.feature.home.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.insightku.core.data.model.TransactionType
import androidx.compose.ui.graphics.Color
import com.example.insightku.R
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
    @StringRes val labelRes: Int,
    val showAmountPrefix: Boolean = false,
    val allowsEdit: Boolean = false,
    val allowsDelete: Boolean = false,
    val showCategory: Boolean = false
) {
    INCOME(
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        color = AppPalette.success,
        labelRes = R.string.type_income,
        showAmountPrefix = true,
        allowsEdit = true,
        allowsDelete = true,
        showCategory = true
    ),
    EXPENSE(
        icon = Icons.AutoMirrored.Filled.TrendingDown,
        color = AppPalette.error,
        labelRes = R.string.type_expense,
        showAmountPrefix = true,
        allowsEdit = true,
        allowsDelete = true,
        showCategory = true
    ),
    TRANSFER_OUT(
        icon = Icons.Default.SwapHoriz,
        color = AppPalette.defaultBlue,
        labelRes = R.string.type_transfer_out
    ),
    TRANSFER_IN(
        icon = Icons.Default.SwapHoriz,
        color = AppPalette.defaultBlue,
        labelRes = R.string.type_transfer_in
    ),
    GOAL_CONTRIBUTION(
        icon = Icons.Default.Savings,
        color = AppPalette.notesPurple,
        labelRes = R.string.type_goal_contribution
    ),
    GOAL_WITHDRAWAL(
        icon = Icons.Default.Savings,
        color = AppPalette.cyan,
        labelRes = R.string.type_goal_withdrawal
    ),
    AUTO_ALLOCATION(
        icon = Icons.Default.AutoAwesome,
        color = AppPalette.indigo,
        labelRes = R.string.type_auto_allocation
    ),
    BALANCE_ADJUSTMENT(
        icon = Icons.Default.Tune,
        color = AppPalette.warning,
        labelRes = R.string.type_balance_adjustment,
        allowsEdit = true,
        allowsDelete = true
    );

    /**
     * Get the localized display label for this transaction type.
     * Use [label] in Composable contexts for automatic recomposition.
     */
    fun label(context: Context): String = context.getString(labelRes)

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
         * Get the localized display label for a transaction type.
         */
        fun labelForType(type: TransactionType, context: Context): String =
            forType(type).label(context)
    }
}
