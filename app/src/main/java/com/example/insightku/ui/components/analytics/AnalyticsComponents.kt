package com.example.insightku.ui.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.ui.components.analytics.model.AnalyticsUtils
import java.text.NumberFormat
import java.util.*

@Composable
fun SavingsRateCard(
    savingsRate: Double,
    selectedMonth: String,
    modifier: Modifier = Modifier
) {
    val monthName = AnalyticsUtils.getMonthDisplayName(selectedMonth)
    val targetRate = 20.0
    val normalizedRate = kotlin.math.abs(savingsRate) / 100.0
    val progressValue = normalizedRate.coerceIn(0.0, 1.0).toFloat()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Savings Rate",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Monthly savings performance for $monthName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target: ${targetRate.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${"%.1f".format(kotlin.math.abs(savingsRate))}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5A2A82)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progressValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    savingsRate >= targetRate -> Color(0xFF10B981)
                    savingsRate >= 0 -> Color(0xFF5A2A82)
                    else -> Color(0xFFEF4444)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status message
            Text(
                text = when {
                    savingsRate >= targetRate -> "🎉 Congratulations! Target achieved"
                    savingsRate >= 0 -> "💪 Keep saving to reach your target"
                    else -> "⚠️ Expenses exceeded income this month"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    savingsRate >= targetRate -> Color(0xFF10B981)
                    savingsRate >= 0 -> MaterialTheme.colorScheme.onSurface
                    else -> Color(0xFFEF4444)
                }
            )
        }
    }
}

@Composable
fun StatisticsCardsSection(
    totalIncome: Double,
    totalExpenses: Double,
    savings: Double,
    savingsRate: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Income Card
        StatisticCard(
            title = "Income",
            amount = totalIncome,
            icon = Icons.Default.TrendingUp,
            iconColor = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )

        // Expenses Card
        StatisticCard(
            title = "Expenses",
            amount = totalExpenses,
            icon = Icons.Default.TrendingDown,
            iconColor = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )

        // Savings Card
        StatisticCard(
            title = "Savings",
            amount = kotlin.math.abs(savings),
            icon = Icons.Default.Savings,
            iconColor = if (savings >= 0) Color(0xFF5A2A82) else Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatisticCard(
    title: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        iconColor.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = AnalyticsUtils.formatCurrencyShort(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}

private fun Double.format(digits: Int): String {
    return "%.${digits}f".format(this)
}

fun getMonthName(monthKey: String): String {
    return AnalyticsUtils.getMonthDisplayName(monthKey)
}

fun formatCurrency(amount: Double): String {
    return AnalyticsUtils.formatCurrency(amount)
}
