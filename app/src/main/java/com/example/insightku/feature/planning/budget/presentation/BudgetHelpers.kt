package com.example.insightku.feature.planning.budget.presentation

import com.example.insightku.core.i18n.LocaleHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import kotlin.math.abs

// ─── Budget Status Helpers ────────────────────────────────────────────────────

@Composable
internal fun budgetStatusColor(health: BudgetHealth): Color = when (health) {
    BudgetHealth.Unlimited -> LocalAccent.current
    BudgetHealth.Good -> AppPalette.success
    BudgetHealth.Warning -> AppPalette.warning
    BudgetHealth.Over -> AppPalette.deleteRed
}

/**
 * Section subtitle summarizing budget health.
 *
 * `@Composable` so the copy resolves through [stringResource] and follows the
 * in-app language switch; its single caller (`BudgetingScreen:233`) is already a
 * composable. Same shape as `TxGroup.label()`.
 */
@Composable
internal fun categoryListSubtitle(uiState: BudgetingUiState): String {
    val total = uiState.budgetCategories.size
    val over = uiState.overBudgetCategories.size
    val warning = uiState.budgetCategories.count { it.health == BudgetHealth.Warning }
    return when {
        !uiState.hasExpenseCategories -> ""
        over > 0 -> pluralStringResource(R.plurals.budget_subtitle_over, over, over, total)
        warning > 0 -> pluralStringResource(R.plurals.budget_subtitle_warning, warning, warning, total)
        else -> pluralStringResource(R.plurals.budget_subtitle_ok, total, total)
    }
}

@Composable
internal fun budgetSpentText(category: BudgetCategory): String =
    stringResource(R.string.budget_spent_label, formatCurrencyPlain(category.spentAmount))

/**
 * Pill text for a budget's health. `@Composable` returning `String` rather than
 * a `@StringRes Int` because the `Warning` branch is a computed percentage, not a
 * resource.
 */
@Composable
internal fun categoryStatusText(category: BudgetCategory): String {
    return when (category.health) {
        BudgetHealth.Unlimited -> stringResource(R.string.budget_status_no_limit)
        BudgetHealth.Good -> stringResource(R.string.budget_status_safe)
        BudgetHealth.Warning -> "${category.utilizationPercentage.formatPercent()}%"
        BudgetHealth.Over -> stringResource(R.string.budget_status_over)
    }
}

// ─── Currency Formatting ──────────────────────────────────────────────────────

internal fun formatCurrencyPlain(amount: Double): String {
    val absAmount = abs(amount)
    val sign = if (amount < 0) "-" else ""
    val formatted = if (absAmount >= 1_000) NumberFormatter.formatCurrencyCompact(absAmount)
        else "${NumberFormatter.getCurrencySymbol()}${absAmount.toInt()}"
    return "$sign$formatted"
}

// ─── Category Helpers ─────────────────────────────────────────────────────────

internal fun parseCategoryColor(value: String): Color {
    return com.example.insightku.core.ui.components.parseCategoryColor(value)
}

internal fun categoryIcon(category: BudgetCategory): ImageVector =
    CategoryIconResolver.resolveIcon(category.icon.ifBlank { category.name })

// ─── Percentage Formatting ────────────────────────────────────────────────────

internal fun Double.formatPercent(): String {
    return if (abs(this - toInt()) < 0.05) {
        toInt().toString()
    } else {
        String.format(LocaleHelper.currentLocale(), "%.1f", this)
    }
}

// ─── Insight Builder ──────────────────────────────────────────────────────────

internal data class InsightData(
    val text: String,
    val bgColor: Color,
    val iconTint: Color,
    val icon: ImageVector
)

/**
 * Budget insight cards. `@Composable` so the copy resolves through
 * [stringResource]; the single caller (`BudgetComponents:522`) is a composable.
 */
@Composable
internal fun buildInsights(categories: List<BudgetCategory>): List<InsightData> {
    val result = mutableListOf<InsightData>()
    val overBudget = categories.filter { it.health == BudgetHealth.Over }
    val warning = categories.filter { it.health == BudgetHealth.Warning }
    val safe = categories.filter { it.health == BudgetHealth.Good }

    overBudget.firstOrNull()?.let {
        result.add(
            InsightData(
                text = stringResource(R.string.budget_insight_over, it.name),
                bgColor = AppPalette.deleteBg,
                iconTint = AppPalette.deleteRed,
                icon = Icons.Default.Warning
            )
        )
    }
    warning.firstOrNull()?.let {
        result.add(
            InsightData(
                text = stringResource(R.string.budget_insight_warning, it.name),
                bgColor = AppPalette.warningBg,
                iconTint = AppPalette.warning,
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )
        )
    }
    if (safe.size >= 2) {
        result.add(
            InsightData(
                text = pluralStringResource(R.plurals.budget_insight_safe, safe.size, safe.size),
                bgColor = AppPalette.success.copy(alpha = 0.08f),
                iconTint = AppPalette.success,
                icon = Icons.AutoMirrored.Filled.TrendingDown
            )
        )
    }
    return result.take(3)
}
