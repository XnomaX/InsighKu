package com.example.insightku.feature.planning.budget.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.components.FitText
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent

// ─── Budget Category Card ─────────────────────────────────────────────────────

@Composable
fun BudgetCategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
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
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "catProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale),
        shape = RoundedCornerShape(Dimens.CardRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1 — identity: icon, name, actions. The name gets the full
            // remaining width so it never clips against the figures.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = categoryColor
                    )
                }

                Text(
                    text = category.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_item, category.name),
                        modifier = Modifier.size(18.dp),
                        tint = AppPalette.textMuted
                    )
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_item, category.name),
                            modifier = Modifier.size(18.dp),
                            tint = AppPalette.textMuted
                        )
                    }
                }
            }

            // Row 2 — figures: spent (of the limit) left, remaining right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = budgetSpentText(category),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (category.hasLimit) formatCurrencyPlain(category.remainingAmount)
                    else stringResource(R.string.category_no_limit),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (category.hasLimit) statusColor else AppPalette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3 — progress and status.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = statusColor,
                    trackColor = AppPalette.cardBorder
                )
                if (!category.recurringPeriod.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppPalette.cardElevated
                    ) {
                        Text(
                            text = category.recurringPeriod,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.textMuted
                        )
                    }
                }
                BudgetStateText(
                    text = categoryStatusText(category),
                    color = statusColor
                )
            }
        }
    }
}

/**
 * Quiet budget status: a small status dot plus the label, no pill background.
 * The dot keeps the status readable without a colored container.
 */
@Composable
internal fun BudgetStateText(text: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        FitText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1
        )
    }
}

// ─── Income Category Card ─────────────────────────────────────────────────────

@Composable
internal fun IncomeCategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val categoryColor = parseCategoryColor(category.color)

    // Same card system as [BudgetCategoryCard]: no border, no elevation,
    // 40dp icon box, muted 18dp actions, figures on their own row.
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = categoryColor
                    )
                }

                Text(
                    text = category.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit),
                        modifier = Modifier.size(18.dp),
                        tint = AppPalette.textMuted
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete),
                            modifier = Modifier.size(18.dp),
                            tint = AppPalette.textMuted
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (category.spentAmount > 0)
                        stringResource(R.string.income_this_month, formatCurrencyPlain(category.spentAmount))
                    else stringResource(R.string.no_income_recorded),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (category.spentAmount > 0) {
                    Text(
                        text = formatCurrencyPlain(category.spentAmount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.success,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        shape = RoundedCornerShape(Dimens.CardRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FitText(
                text = stringResource(R.string.empty_budget_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.empty_budget_desc),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = LocalAccent.current
                    )
                    FitText(
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
        shape = RoundedCornerShape(Dimens.CardRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_income_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.empty_income_desc),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppPalette.success
                    )
                    FitText(
                        stringResource(R.string.add_income_source),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.success
                    )
                }
            }
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.budget_insights_title),
            style = MaterialTheme.typography.titleMedium,
            color = AppPalette.textPrimary,
            modifier = Modifier.padding(top = 4.dp)
        )
        insights.forEach { insight ->
            InsightCard(
                text = insight.text,
                iconTint = insight.iconTint,
                icon = insight.icon,
                bgColor = insight.bgColor
            )
        }
    }
}

@Composable
internal fun InsightCard(
    text: String,
    iconTint: Color,
    icon: ImageVector,
    bgColor: Color
) {
    // Quiet insight row: a soft tint of the status color, no border, no icon
    // box. The icon carries the status color; the text stays primary.
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconTint
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
