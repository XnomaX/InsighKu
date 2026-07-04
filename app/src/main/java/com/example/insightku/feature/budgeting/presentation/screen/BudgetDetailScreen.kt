package com.example.insightku.feature.budgeting.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.CurrencyUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Budget Detail Screen showing spending progress for a budget.
 *
 * Features:
 * - Progress visualization with percentage
 * - Spent vs remaining amounts
 * - Transaction list for the budget period
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: String,
    budgetName: String,
    budgetLimit: Double,
    spentAmount: Double,
    colorHex: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = try {
        colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
    } catch (e: Exception) {
        null
    } ?: LocalAccent.current

    val usagePercent = if (budgetLimit > 0) {
        (spentAmount / budgetLimit * 100).coerceIn(0.0, 100.0)
    } else 0.0
    val remaining = (budgetLimit - spentAmount).coerceAtLeast(0.0)
    val isOverBudget = spentAmount > budgetLimit

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppPalette.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = AppPalette.textPrimary
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = budgetName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )

                    if (isOverBudget) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ExpenseRed.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Over Budget!",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Progress Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${usagePercent.toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) ExpenseRed else categoryColor
                    )

                    Text(
                        text = if (isOverBudget) "Over Budget" else "of limit used",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(AppPalette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((usagePercent / 100.0).toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            if (isOverBudget) ExpenseRed.copy(alpha = 0.8f) else categoryColor.copy(alpha = 0.8f),
                                            if (isOverBudget) ExpenseRed else categoryColor
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Amount row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppPalette.textMuted
                            )
                            Text(
                                text = CurrencyUtils.formatAmount(spentAmount, "IDR"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) ExpenseRed else AppPalette.textPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isOverBudget) "Over by" else "Remaining",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppPalette.textMuted
                            )
                            Text(
                                text = CurrencyUtils.formatAmount(
                                    if (isOverBudget) spentAmount - budgetLimit else remaining,
                                    "IDR"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) ExpenseRed else SuccessColor
                            )
                        }
                    }
                }
            }
        }

        // Budget Info
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Budget Limit",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(budgetLimit, "IDR"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                }
            }
        }

        // Empty transaction list placeholder
        item {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Transaction history for this budget will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }
        }
    }
}
