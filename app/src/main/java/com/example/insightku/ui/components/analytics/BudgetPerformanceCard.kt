package com.example.insightku.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.ui.components.analytics.model.*

@Composable
fun BudgetPerformanceCard(
    budgetPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use data directly from AnalyticsDataSource
    val currentData = if (budgetPeriod == TimePeriod.WEEKLY) {
        AnalyticsDataSource.weeklyBudgetData
    } else {
        AnalyticsDataSource.monthlyBudgetData
    }

    val averageBudget = currentData.map { it.budget }.average()
    val averageSpending = currentData.map { it.actual }.average()
    val budgetTimeLabel = if (budgetPeriod == TimePeriod.WEEKLY) "Weekly" else "Monthly"

    val periodOptions = listOf(TimePeriod.WEEKLY, TimePeriod.MONTHLY)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with title and toggle buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp), // Add more horizontal padding
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Budget Performance",
                        style = MaterialTheme.typography.titleSmall, // Make title smaller
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp)) // Reduce space
                    Text(
                        text = "How well you stick to your budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Toggle buttons remain, but add minWidth and spacing
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPeriodChange(TimePeriod.WEEKLY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (budgetPeriod == TimePeriod.WEEKLY) Color(0xFF5A2A82) else Color.Transparent,
                            contentColor = if (budgetPeriod == TimePeriod.WEEKLY) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (budgetPeriod != TimePeriod.WEEKLY) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Weekly",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = { onPeriodChange(TimePeriod.MONTHLY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (budgetPeriod == TimePeriod.MONTHLY) Color(0xFF5A2A82) else Color.Transparent,
                            contentColor = if (budgetPeriod == TimePeriod.MONTHLY) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (budgetPeriod != TimePeriod.MONTHLY) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Monthly",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart Area - Mengikuti desain TypeScript dengan height 256dp (h-64) dan styling yang lebih baik
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp) // Margin dalam chart seperti TypeScript
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Chart bars dengan styling seperti ResponsiveContainer BarChart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 10.dp), // Margin seperti TypeScript
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                if (budgetPeriod == TimePeriod.WEEKLY) 16.dp else 12.dp
                            ), // barCategoryGap: "10%"
                            verticalAlignment = Alignment.Bottom
                        ) {
                            currentData.take(if (budgetPeriod == TimePeriod.WEEKLY) 4 else 6).forEach { data ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Container untuk bars dengan spacing yang tepat
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp), // Tinggi chart yang seimbang
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        // Background untuk menentukan skala maksimum
                                        val maxValue = currentData.maxOfOrNull { maxOf(it.budget, it.actual) } ?: 1.0
                                        val budgetHeight = (data.budget / maxValue * 0.9).coerceIn(0.1, 0.9).toFloat()
                                        val actualHeight = (data.actual / maxValue * 0.9).coerceIn(0.1, 0.9).toFloat()

                                        // Side-by-side bars dengan spacing 4dp
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            // Budget bar (kiri)
                                            Box(
                                                modifier = Modifier
                                                    .width(15.dp) // Sedikit lebih kecil untuk side-by-side
                                                    .fillMaxHeight(budgetHeight)
                                                    .background(
                                                        Color(0xFF5A2A82).copy(alpha = 0.7f), // opacity 0.7
                                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                    )
                                            )

                                            // Actual spending bar (kanan)
                                            Box(
                                                modifier = Modifier
                                                    .width(15.dp) // Sedikit lebih kecil untuk side-by-side
                                                    .fillMaxHeight(actualHeight)
                                                    .background(
                                                        Color(0xFF7C3AED), // Warna lebih kontras
                                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                    )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Period label (seperti XAxis)
                                    Text(
                                        text = data.period,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp, // fontSize: 12 seperti TypeScript
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid dengan 2 kolom untuk Average Budget dan Average Spending
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Average Budget
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Avg. $budgetTimeLabel Budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AnalyticsUtils.formatCurrencyShort(averageBudget),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5A2A82)
                        )
                    }
                }

                // Average Spending
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Avg. $budgetTimeLabel Spending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AnalyticsUtils.formatCurrencyShort(averageSpending),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
