package com.example.insightku.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.insightku.ui.components.analytics.model.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.hypot

@Composable
fun IncomeExpensesChart(
    timePeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentData = if (timePeriod == TimePeriod.WEEKLY) {
        getWeeklyIncomeExpenseData()
    } else {
        getMonthlyIncomeExpenseData()
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    var chartWidthPx by remember { mutableStateOf(0f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with title and toggle buttons - sama seperti BudgetPerformanceCard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = Color(0xFF5A2A82),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Income vs Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${if (timePeriod == TimePeriod.WEEKLY) "Weekly" else "Monthly"} comparison",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Toggle buttons dengan styling yang sama seperti BudgetPerformanceCard
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Weekly Button
                    Button(
                        onClick = { onPeriodChange(TimePeriod.WEEKLY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timePeriod == TimePeriod.WEEKLY) Color(0xFF5A2A82) else Color.Transparent,
                            contentColor = if (timePeriod == TimePeriod.WEEKLY) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (timePeriod != TimePeriod.WEEKLY) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
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

                    // Monthly Button
                    Button(
                        onClick = { onPeriodChange(TimePeriod.MONTHLY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timePeriod == TimePeriod.MONTHLY) Color(0xFF5A2A82) else Color.Transparent,
                            contentColor = if (timePeriod == TimePeriod.MONTHLY) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (timePeriod != TimePeriod.MONTHLY) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
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

            // Chart area: Line chart income vs expenses
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp) // bottom padding kecil
                    .onGloballyPositioned { coordinates ->
                        chartWidthPx = coordinates.size.width.toFloat()
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                val bottomMargin = with(density) { 32.dp.toPx() } // tetap ada bottom margin di mapY
                val topMargin = with(density) { 8.dp.toPx() }
                val maxValue = currentData.maxOfOrNull { maxOf(it.income, it.expenses) } ?: 1.0
                val minValue = currentData.minOfOrNull { minOf(it.income, it.expenses) } ?: 0.0
                val yRange = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
                val pointCount = currentData.size
                androidx.compose.foundation.Canvas(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentData) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val position = event.changes.firstOrNull()?.position ?: continue
                                val w = size.width
                                val h = size.height
                                val xStep = if (pointCount > 1) w / (pointCount - 1) else w
                                fun mapY(value: Double): Float =
                                    (h - bottomMargin - ((value - minValue) / yRange * (h - topMargin - bottomMargin)).toFloat()) + topMargin
                                val tapRadius = 36f
                                var found = false
                                for (i in currentData.indices) {
                                    val x = xStep * i
                                    val yIncome = mapY(currentData[i].income)
                                    val yExpenses = mapY(currentData[i].expenses)
                                    if (hypot(position.x - x, position.y - yIncome) < tapRadius ||
                                        hypot(position.x - x, position.y - yExpenses) < tapRadius) {
                                        selectedIndex = i
                                        found = true
                                        break
                                    }
                                }
                                if (!found) selectedIndex = null
                            }
                        }
                    }
                ) {
                    val w = size.width
                    val h = size.height
                    val xStep = if (pointCount > 1) w / (pointCount - 1) else w
                    fun mapY(value: Double): Float =
                        (h - bottomMargin - ((value - minValue) / yRange * (h - topMargin - bottomMargin)).toFloat()) + topMargin
                    // Draw smooth curve for income
                    if (pointCount > 1) {
                        val pathIncome = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, mapY(currentData[0].income))
                            for (i in 1 until pointCount) {
                                val prevX = xStep * (i - 1)
                                val prevY = mapY(currentData[i - 1].income)
                                val currX = xStep * i
                                val currY = mapY(currentData[i].income)
                                val c1x = prevX + (currX - prevX) / 2f
                                val c1y = prevY
                                val c2x = prevX + (currX - prevX) / 2f
                                val c2y = currY
                                cubicTo(c1x, c1y, c2x, c2y, currX, currY)
                            }
                        }
                        drawPath(
                            path = pathIncome,
                            color = Color(0xFF10B981),
                            style = Stroke(width = 7f, cap = StrokeCap.Round)
                        )
                        // Tidak perlu area fill/gradient biru di bawah garis income
                    }
                    // Draw smooth curve for expenses
                    if (pointCount > 1) {
                        val pathExpenses = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, mapY(currentData[0].expenses))
                            for (i in 1 until pointCount) {
                                val prevX = xStep * (i - 1)
                                val prevY = mapY(currentData[i - 1].expenses)
                                val currX = xStep * i
                                val currY = mapY(currentData[i].expenses)
                                val c1x = prevX + (currX - prevX) / 2f
                                val c1y = prevY
                                val c2x = prevX + (currX - prevX) / 2f
                                val c2y = currY
                                cubicTo(c1x, c1y, c2x, c2y, currX, currY)
                            }
                        }
                        drawPath(
                            path = pathExpenses,
                            color = Color(0xFF5A2A82),
                            style = Stroke(width = 7f, cap = StrokeCap.Round)
                        )
                        // Tidak perlu area fill/gradient ungu di bawah garis expenses
                    }
                    // Draw points (dot) income & expenses
                    currentData.forEachIndexed { i, data ->
                        val x = xStep * i
                        // Income point
                        drawCircle(
                            color = Color.White,
                            radius = 16f,
                            center = Offset(x, mapY(data.income))
                        )
                        drawCircle(
                            color = Color(0xFF10B981),
                            radius = 12f,
                            center = Offset(x, mapY(data.income))
                        )
                        // Expenses point
                        drawCircle(
                            color = Color.White,
                            radius = 16f,
                            center = Offset(x, mapY(data.expenses))
                        )
                        drawCircle(
                            color = Color(0xFF5A2A82),
                            radius = 12f,
                            center = Offset(x, mapY(data.expenses))
                        )
                    }
                }
                // Tooltip
                selectedIndex?.let { idx ->
                    val data = currentData[idx]
                    val xStep = if (pointCount > 1) chartWidthPx / (pointCount - 1) else chartWidthPx
                    val x = xStep * idx + with(density) { 16.dp.toPx() }
                    androidx.compose.ui.window.Popup(alignment = Alignment.TopStart, offset = IntOffset(x.toInt() - 100, 0)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.widthIn(min = 140.dp).padding(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = data.period,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Income: " + AnalyticsUtils.formatCurrencyShort(data.income),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Expenses: " + AnalyticsUtils.formatCurrencyShort(data.expenses),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF5A2A82)
                                )
                            }
                        }
                    }
                }
                // Spacer agar label X axis tidak tertimpa chart
                Spacer(modifier = Modifier.height(8.dp)) // Spacer kecil
                // Label X axis (periode)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    currentData.forEach { data ->
                        Text(
                            text = data.period,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 24.dp),
                            maxLines = 1
                        )
                    }
                }
            }
            // Legend
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF10B981), RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF10B981)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF5A2A82), RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF5A2A82)
                    )
                }
            }
        }
    }
}