package com.example.insightku.feature.home.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.ui.theme.InsightKuTheme

@Preview(name = "Dashboard Screen Light", showBackground = true)
@Preview(name = "Dashboard Screen Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DashboardScreenPreview() {
    InsightKuTheme {
        val dummyState = DashboardUiState(
            isLoading = false,
            userName = "Andi",
            totalBalance = 4256.80,
            monthlyIncome = 3200.0,
            monthlyExpenses = 2650.0,
            monthlySavings = 550.0,
            isBalanceVisible = true,
            aiInsightMessage = "💡 You're on track this month. Spending is 8% lower than average.",
            insightMessages = listOf(
                "You're saving 17% of your income this month. Keep the momentum.",
                "Recurring payments are under control — spending ratio looks healthy.",
                "5-day tracking streak. Your habit is becoming automatic."
            ),
            recentTransactions = listOf(
                TransactionItem(
                    id = "1", title = "Grocery Shopping", category = "Food & Dining",
                    amount = 85.50, time = "2h", isIncome = false,
                    iconName = "shopping", colorHex = "#FF6B6B"
                ),
                TransactionItem(
                    id = "2", title = "Salary Payment", category = "Income",
                    amount = 3500.00, time = "1d", isIncome = true,
                    iconName = "salary", colorHex = "#4ECDC4"
                )
            ),
            currentStreak = 5,
            hasTrackedToday = false
        )
        DashboardScreenContent(
            uiState = dummyState,
            onEvent = {},
            onNavigateToTransactionDetails = {},
            onAddTransaction = {}
        )
    }
}

@Preview(name = "Dashboard Header Light", showBackground = true)
@Composable
fun DashboardHeaderPreview() {
    InsightKuTheme {
        DashboardHeader(
            userName = "Andi",
            monthlySavings = 550.0,
            currentStreak = 5,
            isBalanceVisible = true,
            isLoading = false,
            onToggleVisibility = {}
        )
    }
}

@Preview(name = "Hero Balance Card Light", showBackground = true)
@Composable
fun HeroBalanceCardPreview() {
    InsightKuTheme {
        Surface {
            HeroBalanceCard(
                totalBalance = 4256.80,
                monthlyIncome = 3200.0,
                monthlyExpenses = 2650.0,
                monthlySavings = 550.0,
                isBalanceVisible = true
            )
        }
    }
}

@Preview(name = "Daily Streak Card Active Light", showBackground = true)
@Composable
fun DailyStreakCardActivePreview() {
    InsightKuTheme {
        Surface {
            DailyStreakCard(
                currentStreak = 5,
                hasTrackedToday = false,
                onAddTransaction = {}
            )
        }
    }
}

@Preview(name = "Insights Section Light", showBackground = true)
@Composable
fun InsightsSectionPreview() {
    InsightKuTheme {
        Surface {
            InsightsSection(
                insightMessages = listOf(
                    "You're saving 17% of your income this month. Keep the momentum.",
                    "Recurring payments are under control.",
                    "5-day tracking streak. Your habit is becoming automatic."
                )
            )
        }
    }
}

@Preview(name = "Flame Tier Debug - All Levels", showBackground = true, backgroundColor = 0xFFFAF9FE, heightDp = 900)
@Composable
fun PremiumFlameDebugPreview() {
    InsightKuTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAF9FE))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Flame Tier Debug",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
            Text(
                "FLAME_DEBUG_MODE = ${"true"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9E9E9E)
            )

            listOf(0, 1, 3, 7, 14, 30, 100).forEach { streak ->
                val config = flameConfig(streak)
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECE7F6)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flame
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PremiumFlameIcon(
                                active = streak > 0,
                                size   = 72.dp,
                                streak = streak
                            )
                        }

                        // Info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "$streak days",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (streak > 0) config.flamePrimary else Color(0xFF9E9E9E)
                                )
                                androidx.compose.material3.Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                                    color = if (streak > 0) config.flamePrimary.copy(alpha = 0.10f)
                                            else Color(0xFFECE7F6)
                                ) {
                                    Text(
                                        config.statusCopy,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (streak > 0) config.flamePrimary else Color(0xFF9E9E9E)
                                    )
                                }
                            }
                            // Scale + speed
                            Text(
                                "scale=${config.flameScale}x  speed=${config.animationSpeed}x  particles=${config.particleIntensity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9E9E9E),
                                fontSize = 9.sp
                            )
                            // Color hex
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(config.flamePrimary)
                                )
                                Text(
                                    "#${Integer.toHexString(config.flamePrimary.hashCode()).uppercase().takeLast(6)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 9.sp
                                )
                                // Particle dots indicator
                                if (config.particleIntensity > 0) {
                                    repeat(config.particleIntensity) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(config.flamePrimary.copy(alpha = 0.6f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Flame Debug - Streak Card 0", showBackground = true, backgroundColor = 0xFFFAF9FE)
@Composable
fun FlameCardStreak0Preview() {
    InsightKuTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            DailyStreakCard(currentStreak = 0, hasTrackedToday = false, onAddTransaction = {})
        }
    }
}

@Preview(name = "Flame Debug - Streak Card 7", showBackground = true, backgroundColor = 0xFFFAF9FE)
@Composable
fun FlameCardStreak7Preview() {
    InsightKuTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            DailyStreakCard(currentStreak = 7, hasTrackedToday = true, onAddTransaction = {})
        }
    }
}

@Preview(name = "Flame Debug - Streak Card 30", showBackground = true, backgroundColor = 0xFFFAF9FE)
@Composable
fun FlameCardStreak30Preview() {
    InsightKuTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            DailyStreakCard(currentStreak = 30, hasTrackedToday = true, onAddTransaction = {})
        }
    }
}

@Preview(name = "Recent Transactions Preview Light", showBackground = true)
@Composable
fun RecentTransactionsPreviewPreview() {
    InsightKuTheme {
        Surface {
            RecentTransactionsPreview(
                transactions = listOf(
                    TransactionItem(
                        id = "1", title = "Grocery Shopping", category = "Food & Dining",
                        amount = 85.50, time = "2h", isIncome = false,
                        iconName = "shopping", colorHex = "#FF6B6B"
                    )
                ),
                onViewAllClick = {}
            )
        }
    }
}

