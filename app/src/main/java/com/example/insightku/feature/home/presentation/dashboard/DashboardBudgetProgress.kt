package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver
import com.example.insightku.core.ui.theme.*
import com.example.insightku.feature.home.presentation.BudgetSpendingItem
import com.example.insightku.feature.home.presentation.formatCurrencyShort

@Composable
fun BudgetPreviewSection(
    budgets: List<BudgetSpendingItem>,
    totalCount: Int,
    isBalanceVisible: Boolean,
    onClickViewAll: () -> Unit,
    onNavigateToBudgetDetail: (String) -> Unit = {},
    onNavigateToBudgeting: () -> Unit = {},
    onCreateBudget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                val subtitle = when {
                    totalCount == 0 -> "No active budgets yet"
                    totalCount <= 3  -> "$totalCount active budget${if (totalCount != 1) "s" else ""}"
                    else            -> "${budgets.size} of $totalCount budgets"
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            if (totalCount > 3) {
                Surface(
                    modifier = Modifier.clickable(onClick = onClickViewAll),
                    shape    = RoundedCornerShape(50.dp),
                    color    = AppPalette.card,
                    border   = BorderStroke(1.dp, NavPurple)
                ) {
                    Text(
                        "View All",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NavPurple
                    )
                }
            }
        }

        if (budgets.isEmpty()) {
            // Empty state
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(Dimens.CardRadiusLarge),
                color    = AppPalette.card,
                border   = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(NavPurple.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            tint = NavPurple.copy(alpha = 0.45f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        "No active budgets yet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textPrimary
                    )
                    Surface(
                        modifier = Modifier.clickable(onClick = onCreateBudget),
                        shape = RoundedCornerShape(Dimens.ButtonRadius),
                        color = NavPurple
                    ) {
                        Text(
                            "Create Budget",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Budget items
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
                color           = AppPalette.card,
                tonalElevation  = 0.dp,
                shadowElevation = Dimens.ElevationSmall,
                border          = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    budgets.forEachIndexed { index, budget ->
                        BudgetPreviewItem(
                            budget = budget,
                            isBalanceVisible = isBalanceVisible,
                            onClick = {
                                if (totalCount == 1 && budget.id.isNotBlank()) {
                                    onNavigateToBudgetDetail(budget.id)
                                } else {
                                    onNavigateToBudgeting()
                                }
                            }
                        )
                        if (index < budgets.lastIndex) {
                            HorizontalDivider(
                                color = AppPalette.cardBorder,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = Dimens.CardInnerPadding)
                            )
                        }
                    }
                }
            }

            if (totalCount > 3) {
                Text(
                    "+${totalCount - 3} more budgets",
                    style = MaterialTheme.typography.labelMedium,
                    color = NavPurple,
                    modifier = Modifier
                        .clickable(onClick = onClickViewAll)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BudgetPreviewItem(
    budget: BudgetSpendingItem,
    isBalanceVisible: Boolean,
    onClick: () -> Unit = {}
) {
    val resolved = CategoryIconResolver.resolve(budget.iconName)
    val icon = resolved.icon
    val iconColor = resolved.color
    val limit = budget.limit ?: 0.0
    val utilization = if (limit > 0) (budget.spent / limit) * 100 else 0.0
    val progressColor = when {
        utilization >= 100.0 -> ExpenseRed
        utilization >= 80.0 -> WarningYellow
        else -> NavPurple
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }

        // Name + progress bar
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = budget.categoryName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((utilization / 100.0).coerceIn(0.0, 1.0).toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressColor)
                )
            }
            // Amount
            Text(
                text = if (isBalanceVisible) "${formatCurrencyShort(budget.spent)} / ${formatCurrencyShort(limit)}" else "·····",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }

        // Percentage badge
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = progressColor.copy(alpha = 0.10f)
        ) {
            Text(
                text = "${utilization.toInt()}%",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }
    }
}
