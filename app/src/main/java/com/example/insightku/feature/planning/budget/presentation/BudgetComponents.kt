package com.example.insightku.feature.planning.budget.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.IncomeGreen
import com.example.insightku.core.ui.theme.InsightTone
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.LocalComfortMode
import com.example.insightku.core.ui.theme.LocalInsightTone
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.WarningYellow
import com.example.insightku.core.i18n.NumberFormatter

// ─── Budget Health Card ───────────────────────────────────────────────────────

@Composable
fun BudgetHealthCard(
    percentage: Double,
    totalBudget: Double,
    limitedSpent: Double,
    remaining: Double,
    riskyCount: Int,
    overBudgetCount: Int,
    safeCount: Int,
    modifier: Modifier = Modifier
) {
    val tone = LocalInsightTone.current
    val soften = LocalComfortMode.current || tone == InsightTone.GENTLE
    val overColor = if (soften) AppPalette.deleteRed else ExpenseRed
    val warnColor = if (soften) AppPalette.warning.copy(alpha = 0.6f) else WarningYellow
    val accent = LocalAccent.current
    val progressColor by animateColorAsState(
        targetValue = when {
            percentage >= 100.0 -> overColor
            percentage >= 70.0 -> warnColor
            else -> accent
        },
        animationSpec = tween(400),
        label = "healthColor"
    )

    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) (percentage / 100.0).coerceIn(0.0, 1.0).toFloat() else 0f,
        animationSpec = tween(900, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "barProgress"
    )

    val healthLabel = when {
        totalBudget <= 0.0 -> stringResource(R.string.health_no_limits)
        percentage >= 100.0 -> if (soften) stringResource(R.string.health_over_budget_gentle) else stringResource(R.string.health_over_budget)
        percentage >= 70.0 -> if (soften) stringResource(R.string.health_getting_close) else stringResource(R.string.health_watch_spending)
        else -> if (tone == InsightTone.DIRECT) stringResource(R.string.health_within_budget) else stringResource(R.string.health_on_track)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppPalette.card,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.monthly_overview),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = progressColor.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = stringResource(R.string.percent_used, percentage.toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressColor)
                )
            }

            Text(
                text = healthLabel,
                style = MaterialTheme.typography.bodySmall,
                color = progressColor.copy(alpha = 0.8f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetHeroStat(
                    label = stringResource(R.string.budget_stat_label),
                    value = formatCurrencyPlain(totalBudget),
                    color = AppPalette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(AppPalette.cardBorder)
                        .align(Alignment.CenterVertically)
                )
                BudgetHeroStat(
                    label = stringResource(R.string.spent_stat_label),
                    value = formatCurrencyPlain(limitedSpent),
                    color = progressColor,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(AppPalette.cardBorder)
                        .align(Alignment.CenterVertically)
                )
                BudgetHeroStat(
                    label = stringResource(R.string.remaining_stat_label),
                    value = if (totalBudget > 0) formatCurrencyPlain(remaining.coerceAtLeast(0.0)) else "—",
                    color = if (remaining < 0) ExpenseRed else IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            if (totalBudget > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (safeCount > 0) {
                        CompactStatDot(label = stringResource(R.string.budget_safe, safeCount), color = IncomeGreen)
                    }
                    if (riskyCount > 0) {
                        CompactStatDot(label = stringResource(R.string.budget_warning, riskyCount), color = WarningYellow)
                    }
                    if (overBudgetCount > 0) {
                        CompactStatDot(label = stringResource(R.string.budget_over, overBudgetCount), color = ExpenseRed)
                    }
                }
            }
        }
    }
}

@Composable
internal fun BudgetHeroStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.textMuted
        )
    }
}

@Composable
internal fun CompactStatDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.textMuted
        )
    }
}

// ─── Budget Category Card ─────────────────────────────────────────────────────

@Composable
fun BudgetCategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = budgetStatusColor(category.health),
        animationSpec = tween(300),
        label = "catStatusColor"
    )
    val categoryColor = parseCategoryColor(category.color)

    var progressAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(category.id) { progressAnimated = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (progressAnimated) category.progressFraction else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "catProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = categoryColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = budgetSpentText(category),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (category.hasLimit) stringResource(R.string.remaining_stat_label) else stringResource(R.string.category_tracked),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = if (category.hasLimit) NumberFormatter.formatCurrency(category.remainingAmount) else stringResource(R.string.category_no_limit),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (category.hasLimit) statusColor else LocalAccent.current,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_item, category.name),
                        modifier = Modifier.size(16.dp),
                        tint = AppPalette.accent
                    )
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_item, category.name),
                            modifier = Modifier.size(16.dp),
                            tint = AppPalette.deleteRed.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = statusColor,
                    trackColor = AppPalette.cardBorder
                )
                if (!category.recurringPeriod.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = LocalAccent.current.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = category.recurringPeriod,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LocalAccent.current
                        )
                    }
                }
                BudgetStatePill(
                    text = categoryStatusText(category),
                    color = statusColor
                )
            }
        }
    }
}

@Composable
internal fun BudgetStatePill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// ─── Income Category Card ─────────────────────────────────────────────────────

@Composable
internal fun IncomeCategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = parseCategoryColor(category.color)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = categoryColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (category.spentAmount > 0)
                        stringResource(R.string.income_this_month, NumberFormatter.formatCurrency(category.spentAmount))
                    else stringResource(R.string.no_income_recorded),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            if (category.spentAmount > 0) {
                Text(
                    text = NumberFormatter.formatCurrency(category.spentAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.success
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.cd_edit),
                    modifier = Modifier.size(16.dp),
                    tint = AppPalette.accent
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete),
                        modifier = Modifier.size(16.dp),
                        tint = AppPalette.deleteRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ─── Budget Error Card ────────────────────────────────────────────────────────

@Composable
internal fun BudgetErrorCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppPalette.deleteBg,
            contentColor = AppPalette.deleteRed
        ),
        border = BorderStroke(1.dp, AppPalette.deleteRed.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingLarge),
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppPalette.error)
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.dismiss),
                modifier = Modifier.clickable(onClick = onDismiss),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AppPalette.error
            )
        }
    }
}

// ─── Empty Budget State ───────────────────────────────────────────────────────

@Composable
internal fun EmptyBudgetState(
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(LocalAccent.current.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = LocalAccent.current.copy(alpha = 0.5f)
                )
            }
            Text(
                text = stringResource(R.string.empty_budget_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.empty_budget_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(50.dp),
                color = AppPalette.card,
                border = BorderStroke(1.dp, LocalAccent.current)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = LocalAccent.current
                    )
                    Text(
                        stringResource(R.string.add_category),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalAccent.current
                    )
                }
            }
        }
    }
}

// ─── Empty Income State ───────────────────────────────────────────────────────

@Composable
internal fun EmptyIncomeState(
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(AppPalette.success.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = AppPalette.success.copy(alpha = 0.5f)
                )
            }
            Text(
                text = stringResource(R.string.empty_income_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.empty_income_desc),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(50.dp),
                color = AppPalette.card,
                border = BorderStroke(1.dp, AppPalette.success)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppPalette.success
                    )
                    Text(
                        stringResource(R.string.add_income_source),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.success
                    )
                }
            }
        }
    }
}

// ─── Budget Insight Chip ──────────────────────────────────────────────────────

@Composable
fun BudgetInsightChip(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Budget Insights Section ──────────────────────────────────────────────────

@Composable
internal fun BudgetInsightsSection(
    categories: List<BudgetCategory>,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val insights = buildInsights(categories)
    if (insights.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.budget_insights_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )
        insights.forEach { insight ->
            InsightCard(
                text = insight.text,
                iconTint = insight.iconTint,
                icon = insight.icon
            )
        }
    }
}

@Composable
internal fun InsightCard(
    text: String,
    iconTint: Color,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppPalette.card,
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconTint
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
