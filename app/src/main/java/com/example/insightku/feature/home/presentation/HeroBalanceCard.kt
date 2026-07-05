package com.example.insightku.feature.home.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.IncomeGreen
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.formatCurrency
import java.util.Locale
import kotlin.math.abs

// --- Hero Balance Card --------------------------------------------------------

@Composable
fun HeroBalanceCard(
    totalBalance: Double,
    accountBalance: Double,
    accountCount: Int,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    monthlySavings: Double,
    isBalanceVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
        color           = AppPalette.card,
        tonalElevation  = 0.dp,
        shadowElevation = 4.dp,
        border          = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardInnerPaddingLarge),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance label + amount
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Total Balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.textMuted,
                    letterSpacing = 0.5.sp
                )
                AnimatedContent(
                    targetState = isBalanceVisible,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "balance_visibility"
                ) { visible ->
                    Text(
                        text = if (visible) formatCurrency(accountBalance) else "\u2022\u2022\u2022\u2022\u2022",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                }
                // Account count + ledger balance subtitle
                if (isBalanceVisible) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (accountCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = AppPalette.cardBorder
                            ) {
                                Text(
                                    text = "$accountCount account${if (accountCount != 1) "s" else ""}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppPalette.textMuted
                                )
                            }
                        }
                        if (totalBalance != accountBalance) {
                            Text(
                                "\u2022\u2022",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.cardBorder
                            )
                            Text(
                                text = "Ledger: ${formatCurrencyShort(totalBalance)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.textMuted
                            )
                        }
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppPalette.cardBorder)
            )

            // 3-stat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroStatItem(
                    label  = "Income",
                    value  = if (isBalanceVisible) formatCurrencyShort(monthlyIncome) else "\u2022\u2022\u2022",
                    icon   = Icons.Default.ArrowUpward,
                    tint   = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(AppPalette.cardBorder)
                        .align(Alignment.CenterVertically)
                )
                HeroStatItem(
                    label  = "Expenses",
                    value  = if (isBalanceVisible) formatCurrencyShort(monthlyExpenses) else "\u2022\u2022\u2022",
                    icon   = Icons.Default.ArrowDownward,
                    tint   = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(AppPalette.cardBorder)
                        .align(Alignment.CenterVertically)
                )
                HeroStatItem(
                    label  = "Savings",
                    value  = if (isBalanceVisible) formatCurrencyShort(abs(monthlySavings)) else "\u2022\u2022\u2022",
                    icon   = if (monthlySavings >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    tint   = if (monthlySavings >= 0) IncomeGreen else ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun HeroStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(11.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted,
                fontSize = 10.sp
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textPrimary,
            maxLines = 1
        )
    }
}

internal fun formatCurrencyShort(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> "Rp${String.format(Locale.getDefault(), "%.1f", amount / 1_000_000_000)}B"
        amount >= 1_000_000     -> "Rp${String.format(Locale.getDefault(), "%.1f", amount / 1_000_000)}M"
        amount >= 1_000         -> "Rp${String.format(Locale.getDefault(), "%.0f", amount / 1_000)}K"
        else                    -> "Rp${amount.toInt()}"
    }
}
