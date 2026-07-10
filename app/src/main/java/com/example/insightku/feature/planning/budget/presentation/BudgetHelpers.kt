package com.example.insightku.feature.planning.budget.presentation

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import java.util.Locale
import kotlin.math.abs

// ─── Budget Status Helpers ────────────────────────────────────────────────────

@Composable
internal fun budgetStatusColor(health: BudgetHealth): Color = when (health) {
    BudgetHealth.Unlimited -> LocalAccent.current
    BudgetHealth.Good -> AppPalette.success
    BudgetHealth.Warning -> AppPalette.warning
    BudgetHealth.Over -> AppPalette.deleteRed
}

internal fun categoryListSubtitle(uiState: BudgetingUiState): String {
    val total = uiState.budgetCategories.size
    val over = uiState.overBudgetCategories.size
    val warning = uiState.budgetCategories.count { it.health == BudgetHealth.Warning }
    return when {
        !uiState.hasExpenseCategories -> ""
        over > 0 -> "$over over budget, $total total"
        warning > 0 -> "$warning close to limit, $total total"
        else -> "$total categories monitored"
    }
}

internal fun budgetSpentText(category: BudgetCategory): String {
    return if (category.hasLimit) {
        "${formatCurrencyPlain(category.spentAmount)} spent of ${formatCurrencyPlain(category.limitAmount)}"
    } else {
        "${formatCurrencyPlain(category.spentAmount)} spent, unlimited"
    }
}

internal fun categoryStatusText(category: BudgetCategory): String {
    return when (category.health) {
        BudgetHealth.Unlimited -> "No limit"
        BudgetHealth.Good -> "Safe"
        BudgetHealth.Warning -> "${category.utilizationPercentage.formatPercent()}%"
        BudgetHealth.Over -> "Over"
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

internal fun currentMonthLabel(): String {
    return DateFormatter.formatMonthYear(System.currentTimeMillis())
}

// ─── Percentage Formatting ────────────────────────────────────────────────────

internal fun Double.formatPercent(): String {
    return if (abs(this - toInt()) < 0.05) {
        toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", this)
    }
}

// ─── Insight Builder ──────────────────────────────────────────────────────────

internal data class InsightData(
    val text: String,
    val bgColor: Color,
    val iconTint: Color,
    val icon: ImageVector
)

internal fun buildInsights(categories: List<BudgetCategory>): List<InsightData> {
    val result = mutableListOf<InsightData>()
    val overBudget = categories.filter { it.health == BudgetHealth.Over }
    val warning = categories.filter { it.health == BudgetHealth.Warning }
    val safe = categories.filter { it.health == BudgetHealth.Good }

    overBudget.firstOrNull()?.let {
        result.add(
            InsightData(
                text = "${it.name} spending has exceeded the budget",
                bgColor = AppPalette.deleteBg,
                iconTint = AppPalette.deleteRed,
                icon = Icons.Default.Warning
            )
        )
    }
    warning.firstOrNull()?.let {
        result.add(
            InsightData(
                text = "${it.name} budget is nearly reached",
                bgColor = AppPalette.warningBg,
                iconTint = AppPalette.warning,
                icon = Icons.Default.TrendingUp
            )
        )
    }
    if (safe.size >= 2) {
        result.add(
            InsightData(
                text = "${safe.size} categories are well within budget",
                bgColor = AppPalette.success.copy(alpha = 0.08f),
                iconTint = AppPalette.success,
                icon = Icons.Default.TrendingDown
            )
        )
    }
    return result.take(3)
}
