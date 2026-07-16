package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent


import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ─── Data Models ──────────────────────────────────────────────────────────────────

private data class ChartPoint(
    val timestamp: Instant,
    val balance: Double,
    val isWithdrawal: Boolean,
    val label: String,
)

private enum class TimeScale {
    MINUTES, HOURS, DAYS, WEEKS, MONTHS
}

// ExtraStore keys for timestamp mapping
private val timestampKey = ExtraStore.Key<List<Long>>()
private val timeScaleKey = ExtraStore.Key<TimeScale>()

// ─── Main Chart Composable ────────────────────────────────────────────────────────

@Composable
fun SavingsActivityChart(
    contributions: List<Contribution>,
    goalColor: Color,
    goalStartDate: Instant,
    goalDeadline: LocalDate?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.goal_savings_activity_chart),
            subtitle = stringResource(R.string.goal_savings_activity_chart_desc),
        )
        Spacer(Modifier.height(12.dp))
        if (contributions.isEmpty()) {
            EmptyChartCard(goalColor)
        } else {
            ChartCard(
                contributions = contributions,
                goalColor = goalColor,
                goalStartDate = goalStartDate,
                goalDeadline = goalDeadline,
            )
        }
    }
}

// ─── Chart Card ───────────────────────────────────────────────────────────────────

@Composable
private fun ChartCard(
    contributions: List<Contribution>,
    goalColor: Color,
    goalStartDate: Instant,
    goalDeadline: LocalDate?,
) {
    val points = remember(contributions, goalStartDate, goalDeadline) {
        computeChartPoints(contributions, goalStartDate, goalDeadline)
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    val timeScale = remember(points) { determineTimeScale(points) }

    // Timestamp lookup map for marker
    val timestampMap = remember(points) {
        points.associate { it.timestamp.toEpochMilli().toFloat() to it.timestamp }
    }

    LaunchedEffect(points) {
        if (points.isEmpty()) return@LaunchedEffect
        val timestamps = points.map { it.timestamp.toEpochMilli() }
        val balances = points.map { it.balance }

        modelProducer.runTransaction {
            lineModel {
                series(x = timestamps.map { it.toFloat() }, y = balances)
            }
            extras {
                it[timestampKey] = timestamps
                it[timeScaleKey] = timeScale
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, goalColor.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            ChartSummaryHeader(points = points, goalColor = goalColor)
            Spacer(Modifier.height(12.dp))

            val xAxisFormatter = remember(points, timeScale) {
                createXAxisFormatter(points, timeScale)
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(Fill(goalColor)),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill = Fill(
                                        Brush.verticalGradient(
                                            listOf(
                                                goalColor.copy(alpha = 0.25f),
                                                goalColor.copy(alpha = 0.0f),
                                            ),
                                        ),
                                    ),
                                ),
                                interpolator = LineCartesianLayer.Interpolator.cubic(),
                            ),
                        ),
                    ),
                    startAxis = VerticalAxis.rememberStart(
                        valueFormatter = CartesianValueFormatter { _, y, _ ->
                            NumberFormatter.formatCurrencyCompact(y.toDouble())
                        },
                        line = null,
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = xAxisFormatter,
                        line = null,
                    ),
                    marker = rememberSavingsMarker(goalColor, points, timeScale, timestampMap),
                    markerController = CartesianMarkerController.rememberToggleOnTap(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                scrollState = rememberVicoScrollState(),
                zoomState = rememberVicoZoomState(zoomEnabled = true),
            )
        }
    }
}

// ─── Chart Summary Header ─────────────────────────────────────────────────────────

@Composable
private fun ChartSummaryHeader(points: List<ChartPoint>, goalColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                stringResource(R.string.chart_cumulative_balance),
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted,
            )
            Text(
                NumberFormatter.formatCurrency(points.lastOrNull()?.balance ?: 0.0),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = goalColor,
            )
        }
        if (points.size >= 2) {
            val change = points.last().balance - points.first().balance
            val isPositive = change >= 0
            val chipColor = if (isPositive) SuccessColor else ExpenseRed
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = chipColor.copy(alpha = 0.10f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.chart_net_change).uppercase(),
                        style = TextStyle(
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                            color = chipColor,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${NumberFormatter.formatCurrencyCompact(change)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = chipColor,
                    )
                }
            }
        }
    }
}

// ─── Empty Chart Card ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyChartCard(goalColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, goalColor.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(goalColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Savings,
                    null,
                    tint = goalColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.chart_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.chart_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Data Processing ──────────────────────────────────────────────────────────────

private fun computeChartPoints(
    contributions: List<Contribution>,
    goalStartDate: Instant,
    goalDeadline: LocalDate?,
): List<ChartPoint> {
    if (contributions.isEmpty()) return emptyList()

    val sorted = contributions.sortedBy { it.createdAt }
    var balance = 0.0
    val points = sorted.map { c ->
        balance += c.amount
        ChartPoint(
            timestamp = c.createdAt,
            balance = balance,
            isWithdrawal = c.isWithdrawal,
            label = c.notes.ifEmpty { if (c.isWithdrawal) "Withdrawal" else "Deposit" },
        )
    }

    val withStart = if (points.isNotEmpty() && abs(points.first().balance) > 0.01) {
        listOf(
            ChartPoint(
                timestamp = goalStartDate,
                balance = 0.0,
                isWithdrawal = points.first().isWithdrawal,
                label = "Start",
            ),
        ) + points
    } else {
        points
    }

    return withStart
}

// ─── Time Scale Detection ─────────────────────────────────────────────────────────

private fun determineTimeScale(points: List<ChartPoint>): TimeScale {
    if (points.size < 2) return TimeScale.MONTHS
    val totalMs = points.last().timestamp.toEpochMilli() - points.first().timestamp.toEpochMilli()
    return when {
        totalMs <= 3_600_000L -> TimeScale.MINUTES
        totalMs <= 86_400_000L -> TimeScale.HOURS
        totalMs <= 604_800_000L -> TimeScale.DAYS
        totalMs <= 2_592_000_000L -> TimeScale.WEEKS
        else -> TimeScale.MONTHS
    }
}

// ─── Formatters ───────────────────────────────────────────────────────────────────

private fun createXAxisFormatter(
    points: List<ChartPoint>,
    timeScale: TimeScale,
): CartesianValueFormatter {
    return CartesianValueFormatter { _, x, _ ->
        val instant = Instant.ofEpochMilli(x.toLong())
        formatTimeLabel(instant, timeScale)
    }
}

private fun formatTimeLabel(instant: Instant, timeScale: TimeScale): String {
    val dt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return when (timeScale) {
        TimeScale.MINUTES -> dt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
        TimeScale.HOURS -> dt.format(DateTimeFormatter.ofPattern("HH'h'", Locale.getDefault()))
        TimeScale.DAYS -> dt.format(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault()))
        TimeScale.WEEKS -> dt.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
        TimeScale.MONTHS -> dt.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
    }
}

// ─── Marker ───────────────────────────────────────────────────────────────────────

@Composable
private fun rememberSavingsMarker(
    goalColor: Color,
    points: List<ChartPoint>,
    timeScale: TimeScale,
    timestampMap: Map<Float, Instant>,
): DefaultCartesianMarker {
    // Build lookup maps from x-value to balance and transaction type
    val lookupMap = remember(points) {
        points.associate { p ->
            p.timestamp.toEpochMilli().toFloat() to (p.balance to p.isWithdrawal)
        }
    }

    return rememberDefaultCartesianMarker(
        label = rememberTextComponent(),
        valueFormatter = DefaultCartesianMarker.ValueFormatter { _, targets ->
            val target = targets.firstOrNull() ?: return@ValueFormatter ""
            val x = target.x
            val closestTs = timestampMap.keys.minByOrNull { kotlin.math.abs(it - x) }
            val timestamp = timestampMap[closestTs]
            val (balance, isWithdrawal) = lookupMap[closestTs] ?: (0.0 to false)
            val dateStr = if (timestamp != null) formatTimeLabel(timestamp, timeScale) else ""
            val typeStr = if (isWithdrawal) "🔴 Withdrawal" else "🟢 Deposit"
            "$dateStr\n${NumberFormatter.formatCurrency(balance)}\n$typeStr"
        },
        guideline = null,
    )
}
