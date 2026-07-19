package com.example.insightku.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.feature.home.presentation.TransactionTypePresentation
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import java.util.Calendar
import kotlin.math.abs

// --- Design tokens ------------------------------------------------------------
// Semantic colors delegated to TransactionTypePresentation for single source of truth.
internal val TxIncomeGreen @Composable get() = TransactionTypePresentation.INCOME.color
internal val TxExpenseRed @Composable get() = TransactionTypePresentation.EXPENSE.color
internal val TxTransferBlue @Composable get() = TransactionTypePresentation.TRANSFER_OUT.color
internal val TxGoalPurple @Composable get() = TransactionTypePresentation.GOAL_CONTRIBUTION.color
internal val TxWithdrawalTeal @Composable get() = TransactionTypePresentation.GOAL_WITHDRAWAL.color
internal val TxAutoAllocIndigo @Composable get() = TransactionTypePresentation.AUTO_ALLOCATION.color
internal val TxAdjustOrange @Composable get() = TransactionTypePresentation.BALANCE_ADJUSTMENT.color

// All surface/text/border tokens are resolved at runtime from AppPalette or
// LocalAccent so they adapt to dark mode and the user's chosen accent.
internal val TxAccent:      Color @Composable get() = LocalAccent.current
internal val TxBackground:  Color @Composable get() = AppPalette.background
internal val TxCard:        Color @Composable get() = AppPalette.card
internal val TxCardBorder:  Color @Composable get() = AppPalette.cardBorder
internal val TxTextPrimary: Color @Composable get() = AppPalette.textPrimary
internal val TxTextMuted:   Color @Composable get() = AppPalette.textMuted
internal val TxTint:        Color @Composable get() = AppPalette.cardElevated

// --- Type-based helpers -------------------------------------------------------

/** Get the accent color for a transaction type. */
internal fun txTypeColor(type: TransactionType): Color = TransactionTypePresentation.colorForType(type)

/** Get the display label for a transaction type (e.g. "Transfer", "Goal Contribution"). */
@Composable
internal fun txTypeLabel(type: TransactionType): String = stringResource(TransactionTypePresentation.forType(type).labelRes)

/** Get the amount prefix/sign for display. */
internal fun txAmountPrefix(type: TransactionType): String = when (type) {
    TransactionType.INCOME -> "+"
    TransactionType.EXPENSE -> "-"
    else -> ""
}

/** Whether this type should show category info. */
internal fun txShowCategory(type: TransactionType): Boolean = TransactionTypePresentation.forType(type).showCategory

/** Whether this type allows editing. */
internal fun txAllowsEdit(type: TransactionType): Boolean = TransactionTypePresentation.forType(type).allowsEdit

/** Whether this type allows deletion. */
internal fun txAllowsDelete(type: TransactionType): Boolean = TransactionTypePresentation.forType(type).allowsDelete

// --- Enums --------------------------------------------------------------------
enum class FilterType { ALL, INCOME, EXPENSE, TRANSFER, GOAL, AUTO_ALLOC, TODAY, WEEK, MONTH }
enum class SortType   { NEWEST, OLDEST, HIGHEST, LOWEST, CATEGORY }

// --- Transaction group label --------------------------------------------------
internal enum class TxGroup { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, OLDER }

internal fun getTxGroup(dateMillis: Long): TxGroup {
    val now   = Calendar.getInstance()
    val txCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val diffDays = ((now.timeInMillis - dateMillis) / 86_400_000L).toInt()
    return when {
        now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) -> TxGroup.TODAY
        diffDays == 1 -> TxGroup.YESTERDAY
        diffDays <= 7 -> TxGroup.THIS_WEEK
        now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        now.get(Calendar.MONTH) == txCal.get(Calendar.MONTH) -> TxGroup.THIS_MONTH
        else -> TxGroup.OLDER
    }
}

@Composable
internal fun TxGroup.label(): String = when (this) {
    TxGroup.TODAY      -> stringResource(R.string.tx_group_today)
    TxGroup.YESTERDAY  -> stringResource(R.string.tx_group_yesterday)
    TxGroup.THIS_WEEK  -> stringResource(R.string.tx_group_this_week)
    TxGroup.THIS_MONTH -> stringResource(R.string.tx_group_this_month)
    TxGroup.OLDER      -> stringResource(R.string.tx_group_earlier)
}

// --- Formatting helpers -------------------------------------------------------

internal fun formatFullDate(dateMillis: Long): String =
    DateFormatter.formatDateTime(dateMillis)

internal fun formatCurrencyRp(amount: Double): String =
    NumberFormatter.formatCurrency(abs(amount))
