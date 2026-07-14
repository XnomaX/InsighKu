package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.utils.AppConstants
import com.example.insightku.core.ui.theme.*
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.*

// ─── Data Models ──────────────────────────────────────────────────────────────────

private data class ChartPoint(
    val timestamp: Instant,
    val balance: Double,
    val isWithdrawal: Boolean
)

private data class ChartConfig(
    val chartHeight: Float = 200f,
    val markerRadius: Float = 7.5f,
    val markerGlowRadius: Float = 16f,
    val verticalPadding: Float = 40f,
    val horizontalPadding: Float = 30f,
    val yAxisLabelWidth: Float = 68f,
    val lineStrokeGlow: Float = 14f,
    val lineStrokeMain: Float = 9f,
    val minZoom: Float = 1f,
    val maxZoom: Float = 20f
)

private enum class ZoomTimeScale {
    MINUTES, HOURS, DAYS, WEEKS, MONTHS
}

// ─── Main Chart Composable ────────────────────────────────────────────────────────

@Composable
fun SavingsActivityChart(
    contributions: List<Contribution>,
    goalColor: Color,
    goalStartDate: Instant,
    goalDeadline: LocalDate?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.goal_savings_activity_chart),
            subtitle = stringResource(R.string.goal_savings_activity_chart_desc)
        )
        Spacer(Modifier.height(12.dp))
        if (contributions.isEmpty()) {
            EmptyChartCard(goalColor)
        } else {
            ChartCard(contributions = contributions, goalColor = goalColor, goalStartDate = goalStartDate, goalDeadline = goalDeadline)
        }
    }
}

@Composable
private fun ChartCard(
    contributions: List<Contribution>,
    goalColor: Color,
    goalStartDate: Instant,
    goalDeadline: LocalDate?
) {
    val config = remember { ChartConfig() }
    val chartData = remember(contributions, goalStartDate, goalDeadline) {
        prepareChartData(contributions, config, goalStartDate, goalDeadline)
    }

    // ── Zoom & Pan State ─────────────────────────────────────────────────────
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableFloatStateOf(0f) } // px offset from left


    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, goalColor.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)) {
            ChartSummaryHeader(chartData = chartData, goalColor = goalColor)
            Spacer(Modifier.height(12.dp))

            // ── Zoom Controls ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { zoomLevel = (zoomLevel / 1.5f).coerceIn(config.minZoom, config.maxZoom) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom out", tint = AppPalette.textMuted, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${zoomLevel.toInt()}×",
                    style = TextStyle(fontSize = 10.sp, color = AppPalette.textMuted),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = { zoomLevel = (zoomLevel * 1.5f).coerceIn(config.minZoom, config.maxZoom) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom in", tint = AppPalette.textMuted, modifier = Modifier.size(16.dp))
                }
            }

            ChartContent(chartData = chartData, config = config, goalColor = goalColor,
                zoomLevel = zoomLevel, panOffset = panOffset,
                onZoom = { zoom, newPan ->
                    zoomLevel = zoom.coerceIn(config.minZoom, config.maxZoom)
                    panOffset = newPan.coerceIn(0f, maxOf(0f, chartData.totalWidthPx - chartData.viewportWidthPx(zoom)))
                },
                onPan = { delta ->
                    val maxPan = maxOf(0f, chartData.totalWidthPx - chartData.viewportWidthPx(zoomLevel))
                    panOffset = (panOffset + delta).coerceIn(0f, maxPan)
                },
                onDoubleTap = {
                    zoomLevel = 1f
                    panOffset = 0f
                }
            )
        }
    }
}

// ─── Chart Summary Header ─────────────────────────────────────────────────────────

@Composable
private fun ChartSummaryHeader(chartData: PreparedChartData, goalColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(stringResource(R.string.chart_cumulative_balance), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
            Text(
                NumberFormatter.formatCurrency(chartData.points.lastOrNull()?.balance ?: 0.0),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = goalColor
            )
        }
        if (chartData.points.size >= 2) {
            val change = chartData.points.last().balance - chartData.points.first().balance
            val isPositive = change >= 0
            val chipColor = if (isPositive) SuccessColor else ExpenseRed
            Surface(shape = RoundedCornerShape(10.dp), color = chipColor.copy(alpha = 0.10f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.chart_net_change).uppercase(),
                        style = TextStyle(fontSize = 9.sp, letterSpacing = 0.5.sp, color = chipColor),
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

// ─── Chart Content ────────────────────────────────────────────────────────────────

@Composable
private fun ChartContent(
    chartData: PreparedChartData,
    config: ChartConfig,
    goalColor: Color,
    zoomLevel: Float,
    panOffset: Float,
    onZoom: (Float, Float) -> Unit,
    onPan: (Float) -> Unit,
    onDoubleTap: () -> Unit
) {
    val density = LocalDensity.current
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(chartData.points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    val greenColor = SuccessColor
    val redColor = ExpenseRed
    val mutedColor = AppPalette.textMuted
    val gridColor = AppPalette.cardBorder

    // Compute nice axis once
    val niceAxis = remember(chartData.minBalance, chartData.maxBalance) {
        computeNiceAxis(chartData.minBalance, chartData.maxBalance, maxTicks = 5)
    }

    var selectedPointIndex by remember { mutableIntStateOf(-1) }
    val tooltipData = remember(selectedPointIndex, chartData) {
        if (selectedPointIndex >= 0 && selectedPointIndex < chartData.points.size) {
            chartData.points[selectedPointIndex]
        } else null
    }

    val viewportWidthPx = chartData.viewportWidthPx(zoomLevel)
    val totalWidthPx = chartData.totalWidthPx
    val yAxisLabelWidthPx = config.yAxisLabelWidth
    val yAxisLabelWidthDp = with(density) { yAxisLabelWidthPx.toDp() }
    val viewportDp = with(density) { viewportWidthPx.toDp() }

    // Determine visible time range
    val visibleStartMs = chartData.timelineStartMs + (chartData.timelineMsRange * panOffset / totalWidthPx.coerceAtLeast(1f)).toLong()
    val visibleEndMs = visibleStartMs + (chartData.timelineMsRange * viewportWidthPx / totalWidthPx.coerceAtLeast(1f)).toLong()

    // Time scale adapts to the VISIBLE range, changing dynamically as user zooms/pans.
    // This ensures labels transition from monthly → weekly → daily → hourly when zooming in,
    // while still showing the right granularity for the current viewport.
    val zoomTimeScale = remember(visibleStartMs, visibleEndMs) {
        val visibleMs = (visibleEndMs - visibleStartMs).coerceAtLeast(1L)
        when {
            visibleMs <= 3_600_000L -> ZoomTimeScale.MINUTES   // < 1 hour
            visibleMs <= 86_400_000L -> ZoomTimeScale.HOURS     // < 24 hours
            visibleMs <= 604_800_000L -> ZoomTimeScale.DAYS     // ≤ 7 days
            visibleMs <= 2_592_000_000L -> ZoomTimeScale.WEEKS  // ≤ 30 days
            else -> ZoomTimeScale.MONTHS
        }
    }

    // Position helper
    fun timeToX(instant: Instant): Float {
        val ms = instant.toEpochMilli()
        val timelineRangeMs = chartData.timelineMsRange.coerceAtLeast(1L)
        val fraction = (ms - chartData.timelineStartMs).toFloat() / timelineRangeMs.toFloat()
        return config.horizontalPadding + fraction * (totalWidthPx - config.horizontalPadding * 2) - panOffset
    }

    // ── Vertical grid positions (canvas-space, aligned with X-axis labels) ──
    val verticalGridPositions = remember(zoomLevel, panOffset, visibleStartMs, visibleEndMs, zoomTimeScale, totalWidthPx, viewportWidthPx, chartData.timelineStartMs, chartData.timelineMsRange) {
        if (chartData.points.isEmpty()) return@remember emptyList<Float>()
        computeLabelGrid(zoomTimeScale, visibleStartMs, visibleEndMs, viewportWidthPx,
            config.horizontalPadding, totalWidthPx, chartData.timelineStartMs,
            chartData.timelineMsRange, panOffset)
            .map { (_, canvasX) -> canvasX }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .offset(x = yAxisLabelWidthDp)
                .width(viewportDp)
                .wrapContentHeight()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(config.chartHeight.dp)
                    .pointerInput(chartData.points) {
                        detectTapGestures(
                            onDoubleTap = { onDoubleTap() },
                            onTap = { offset ->
                                val pts = chartData.points
                                if (pts.isEmpty()) return@detectTapGestures
                                val minB = chartData.minBalance
                                val maxB = chartData.maxBalance
                                val bRange = (maxB - minB).coerceAtLeast(1.0)
                                var closestIndex = 0
                                var closestDist = Float.MAX_VALUE
                                for (i in pts.indices) {
                                    val x = chartData.pointDisplayX(i, config, panOffset)
                                    val y = config.verticalPadding + (config.chartHeight - config.verticalPadding * 2) -
                                        ((pts[i].balance - minB) / bRange * (config.chartHeight - config.verticalPadding * 2)).toFloat()
                                    val dist = (offset.x - x) * (offset.x - x) + (offset.y - y) * (offset.y - y)
                                    if (dist < closestDist) {
                                        closestDist = dist
                                        closestIndex = i
                                    }
                                }
                                val tapThresholdPx = with(density) { 30.dp.toPx() }
                                selectedPointIndex = if (closestDist <= tapThresholdPx * tapThresholdPx) {
                                    if (selectedPointIndex == closestIndex) -1 else closestIndex
                                } else -1
                            }
                        )
                    }
                    .pointerInput(chartData.points) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (zoom != 1f) {
                                val newZoom = (zoomLevel * zoom).coerceIn(config.minZoom, config.maxZoom)
                                val newViewportWidth = chartData.viewportWidthPx(newZoom)
                                val centroidFraction = (centroid.x + panOffset) / totalWidthPx.coerceAtLeast(1f)
                                val newPan = (centroidFraction * totalWidthPx) - (newViewportWidth * centroid.x / viewportWidthPx.coerceAtLeast(1f))
                                onZoom(newZoom, newPan.coerceIn(0f, maxOf(0f, totalWidthPx - newViewportWidth)))
                            } else if (pan.x != 0f) {
                                onPan(-pan.x)
                            }
                        }
                    }
            ) {
                if (chartData.points.isEmpty()) return@Canvas

                val pH = config.horizontalPadding
                val pV = config.verticalPadding
                val dH = size.height - pV * 2
                val pts = chartData.points
                val minB = chartData.minBalance
                val maxB = chartData.maxBalance
                val bRange = (maxB - minB).coerceAtLeast(1.0)

                fun p2o(i: Int): Offset {
                    val x = chartData.pointDisplayX(i, config, panOffset)
                    val y = pV + dH - ((pts[i].balance - minB) / bRange * dH).toFloat()
                    return Offset(x, y)
                }

                // ── Horizontal grid lines ─────────────────────────────────
                for (tick in niceAxis.ticks) {
                    val yFrac = if (niceAxis.max == niceAxis.min) 0.5f else ((niceAxis.max - tick) / (niceAxis.max - niceAxis.min)).toFloat()
                    val y = pV + dH * yFrac
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 0.5f)
                }

                // ── Vertical grid lines (aligned with X-axis labels) ───────
                for (vx in verticalGridPositions) {
                    if (vx >= 0f && vx <= size.width) {
                        drawLine(gridColor, Offset(vx, pV), Offset(vx, pV + dH), 0.5f)
                    }
                }

                // ── Empty Future Timeline ───────────────────────────────────
                val todayMs = System.currentTimeMillis()
                val chartEndMs = chartData.timelineEndMs
                if (chartEndMs > todayMs) {
                    val timelineRangeMs = chartData.timelineMsRange.coerceAtLeast(1L)
                    val futureFraction = ((chartEndMs - todayMs).toFloat() / timelineRangeMs.toFloat())
                    val futureLeft = (todayMs - chartData.timelineStartMs).toFloat() / timelineRangeMs.toFloat()
                    val futureXStart = pH + futureLeft * (totalWidthPx - pH * 2) - panOffset
                    val futureXEnd = pH + (futureLeft + futureFraction) * (totalWidthPx - pH * 2) - panOffset
                    drawRect(
                        color = mutedColor.copy(alpha = 0.04f),
                        topLeft = Offset(futureXStart, 0f),
                        size = androidx.compose.ui.geometry.Size(futureXEnd - futureXStart, size.height)
                    )
                    // Future vertical divider line
                    drawLine(
                        color = mutedColor.copy(alpha = 0.15f),
                        start = Offset(futureXStart, pV),
                        end = Offset(futureXStart, pV + dH),
                        strokeWidth = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                    )
                }

                // ── Area fill ───────────────────────────────────────────────
                for (i in 1 until pts.size) {
                    val prev = p2o(i - 1); val curr = p2o(i)
                    // Skip segments outside viewport
                    if (curr.x < -100f || prev.x > size.width + 100f) continue
                    val cOff = (curr.x - prev.x) * 0.3f
                    val segPath = Path().apply {
                        moveTo(prev.x, pV + dH); lineTo(prev.x, prev.y)
                        cubicTo(prev.x + cOff, prev.y, curr.x - cOff, curr.y, curr.x, curr.y)
                        lineTo(curr.x, pV + dH); close()
                    }
                    val segColor = if (pts[i].isWithdrawal) redColor else greenColor
                    val segProg = (animationProgress.value * pts.size - (i - 1)).coerceIn(0f, 1f)
                    drawPath(segPath, Brush.verticalGradient(listOf(segColor.copy(alpha = segProg * 0.15f), segColor.copy(alpha = 0f)), pV, pV + dH))
                }

                // ── Curve line ──────────────────────────────────────────────
                for (i in 1 until pts.size) {
                    val segProg = (animationProgress.value * pts.size - (i - 1)).coerceIn(0f, 1f)
                    if (segProg <= 0f) break
                    val prev = p2o(i - 1); val curr = p2o(i)
                    if (curr.x < -100f || prev.x > size.width + 100f) continue
                    val cOff = (curr.x - prev.x) * 0.3f
                    val segColor = if (pts[i].isWithdrawal) redColor else greenColor
                    val segPath = Path().apply { moveTo(prev.x, prev.y); cubicTo(prev.x + cOff, prev.y, curr.x - cOff, curr.y, curr.x, curr.y) }
                    val drawP = if (segProg < 1f) {
                        val pp = Path(); val pm = PathMeasure(); pm.setPath(segPath, false)
                        pm.getSegment(0f, pm.length * segProg, pp); pp
                    } else segPath
                    drawPath(drawP, segColor.copy(alpha = 0.15f), style = Stroke(width = config.lineStrokeGlow, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(drawP, segColor.copy(alpha = 0.9f), style = Stroke(width = config.lineStrokeMain, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // ── Markers ─────────────────────────────────────────────────
                if (pts.size <= 30) {
                    for (i in pts.indices) {
                        val p = p2o(i)
                        if (p.x < -50f || p.x > size.width + 50f) continue
                        val mProg = (animationProgress.value * pts.size - i).coerceIn(0f, 1f)
                        if (mProg <= 0f) continue
                        val c = if (pts[i].isWithdrawal) redColor else greenColor
                        val isSelected = i == selectedPointIndex
                        drawCircle(c.copy(alpha = 0.15f * mProg), config.markerGlowRadius, p)
                        drawCircle(c.copy(alpha = 0.9f * mProg), config.markerRadius, p)
                        drawCircle(Color.White, config.markerRadius * 0.5f, p)
                        if (isSelected) {
                            drawCircle(c.copy(alpha = 0.2f), config.markerGlowRadius * 2f, p)
                            drawCircle(c, config.markerRadius * 1.5f, p)
                            drawCircle(Color.White, config.markerRadius * 0.6f, p)
                        }
                    }
                }
            }
        }

        // ── Y-axis labels ───────────────────────────────────────────────────
        if (chartData.points.isNotEmpty()) {
            val yLabels = remember(niceAxis, density, config) {
                niceAxis.ticks.map { tick ->
                    val yFrac = if (niceAxis.max == niceAxis.min) 0.5f else ((niceAxis.max - tick) / (niceAxis.max - niceAxis.min)).toFloat()
                    val yDp = (config.verticalPadding + (config.chartHeight - config.verticalPadding * 2) * yFrac) - 6f
                    AxisLabelInfo(
                        text = NumberFormatter.formatCurrencyCompact(tick),
                        xDp = 10f,
                        yDp = yDp
                    )
                }
            }
            for (info in yLabels) {
                YAxisLabel(text = info.text, xDp = info.xDp, yDp = info.yDp, mutedColor = mutedColor)
            }
        }

        // ── X-axis labels ────────────────────────────────────────────────────
        if (chartData.points.isNotEmpty()) {
            // Include zoomLevel + panOffset directly as keys to force recomputation on zoom/pan
            val xLabels = remember(zoomLevel, panOffset, visibleStartMs, visibleEndMs, zoomTimeScale, yAxisLabelWidthPx, config, totalWidthPx, viewportWidthPx, chartData.timelineStartMs, chartData.timelineMsRange, mutedColor) {
                if (chartData.points.isEmpty()) return@remember emptyList<AxisLabelInfo>()
                computeLabelGrid(zoomTimeScale, visibleStartMs, visibleEndMs, viewportWidthPx,
                    config.horizontalPadding, totalWidthPx, chartData.timelineStartMs,
                    chartData.timelineMsRange, panOffset)
                    .map { (labelMs, canvasX) ->
                        val xPos = yAxisLabelWidthPx + canvasX
                        AxisLabelInfo(
                            text = formatTimeLabel(Instant.ofEpochMilli(labelMs), zoomTimeScale),
                            xDp = xPos,
                            yDp = config.chartHeight - 10f
                        )
                    }
            }
            for (info in xLabels) {
                XAxisLabel(text = info.text, xDp = info.xDp, yDp = info.yDp, mutedColor = mutedColor, density = density)
            }
        }

        // ── Tooltip overlay ─────────────────────────────────────────────────
        val showTooltip = tooltipData != null
        AnimatedVisibility(
            visible = showTooltip,
            enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { -it / 4 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it / 4 }
        ) {
            if (tooltipData != null) {
                val isWithdrawal = tooltipData.isWithdrawal
                val tooltipColor = if (isWithdrawal) ExpenseRed else SuccessColor
                val tooltipDate = formatTimeLabel(tooltipData.timestamp, zoomTimeScale)
                val tooltipBalance = NumberFormatter.formatCurrency(tooltipData.balance)
                val tooltipType = if (isWithdrawal) stringResource(R.string.chart_legend_withdrawal) else stringResource(R.string.chart_legend_deposit)

                val xPosPx = if (selectedPointIndex >= 0 && selectedPointIndex < chartData.spreadPositions.size) {
                    chartData.pointDisplayX(selectedPointIndex, config, panOffset)
                } else {
                    timeToX(tooltipData.timestamp)
                }
                val xPosDp = with(density) { (yAxisLabelWidthPx + xPosPx).toDp() }

                // Marker Y position
                val markerYDp = with(density) {
                    val pV = config.verticalPadding
                    val dH = config.chartHeight - config.verticalPadding * 2
                    val bRange = (chartData.maxBalance - chartData.minBalance).coerceAtLeast(1.0)
                    (pV + dH - ((tooltipData.balance - chartData.minBalance) / bRange * dH)).dp
                }

                // Vertical line
                val lineHeightPx = config.chartHeight - markerYDp.value
                Box(
                    modifier = Modifier
                        .offset(x = xPosDp - 0.5.dp, y = markerYDp)
                        .width(1.dp)
                        .height(with(density) { lineHeightPx.toDp() }.coerceAtLeast(0.dp))
                        .background(tooltipColor.copy(alpha = 0.3f))
                )

                // Tooltip card
                val tooltipWidth = 120.dp
                val tooltipHeight = 64.dp
                val chartWidthDp = with(density) { (yAxisLabelWidthPx + viewportWidthPx).toDp() }
                val tooltipXDp = (xPosDp - tooltipWidth / 2).coerceIn(0.dp, chartWidthDp - tooltipWidth)
                val tooltipYDp = (markerYDp - tooltipHeight - 8.dp).coerceAtLeast(0.dp)
                Card(
                    modifier = Modifier
                        .offset(x = tooltipXDp, y = tooltipYDp)
                        .width(tooltipWidth)
                        .shadow(4.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                    border = BorderStroke(1.dp, tooltipColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = tooltipDate, style = TextStyle(fontSize = 10.sp, color = mutedColor, fontWeight = FontWeight.Medium))
                        Spacer(Modifier.height(2.dp))
                        Text(text = tooltipBalance, style = TextStyle(fontSize = 13.sp, color = tooltipColor, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(2.dp))
                        Text(text = tooltipType, style = TextStyle(fontSize = 9.sp, color = mutedColor))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChartCard(goalColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, goalColor.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Savings, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.chart_empty_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, textAlign = TextAlign.Center)
            Text(stringResource(R.string.chart_empty_desc), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted, textAlign = TextAlign.Center)
        }
    }
}

// ─── Data Processing ──────────────────────────────────────────────────────────────

private data class PreparedChartData(
    val points: List<ChartPoint>,
    val minBalance: Double,
    val maxBalance: Double,
    val timelineStartMs: Long,
    val timelineEndMs: Long,
    val timelineMsRange: Long,
    val totalWidthPx: Float,
    val spreadPositions: List<Float> = emptyList()
) {
    fun viewportWidthPx(zoom: Float): Float = totalWidthPx / zoom

    /**
     * Returns the display x-position (chart-space, before panOffset) for point at [index].
     * Falls back to the linear timeline position if spread positions are not available.
     */
    fun pointDisplayX(index: Int, config: ChartConfig, panOffset: Float): Float {
        val x = if (index in spreadPositions.indices) {
            spreadPositions[index]
        } else if (index in points.indices) {
            val ms = points[index].timestamp.toEpochMilli()
            val fraction = (ms - timelineStartMs).toFloat() / timelineMsRange.coerceAtLeast(1L).toFloat()
            config.horizontalPadding + fraction * (totalWidthPx - config.horizontalPadding * 2)
        } else 0f
        return x - panOffset
    }
}

private fun prepareChartData(
    contributions: List<Contribution>,
    config: ChartConfig,
    goalStartDate: Instant,
    goalDeadline: LocalDate?
): PreparedChartData {
    if (contributions.isEmpty()) {
        val now = System.currentTimeMillis()
        val deadlineMs = goalDeadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: (now + 30L * 24 * 60 * 60 * 1000)
        val startMs = goalStartDate.toEpochMilli()
        val range = (deadlineMs - startMs).coerceAtLeast(86_400_000L)
        val widthPx = (range / 86_400_000f) * 200f + config.horizontalPadding * 2 // ~200px per day base
        return PreparedChartData(emptyList(), 0.0, 100.0, startMs, deadlineMs, range, widthPx.coerceAtLeast(360f))
    }

    val sorted = contributions.sortedBy { it.createdAt }
    val pointCount = sorted.size

    // ── Timeline bounds ────────────────────────────────────────────────────
    val startMs = goalStartDate.toEpochMilli()
    val deadlineMs = goalDeadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        ?: maxOf(sorted.last().createdAt.toEpochMilli() + 30L * 24 * 60 * 60 * 1000, System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
    val timelineMsRange = (deadlineMs - startMs).coerceAtLeast(86_400_000L) // at least 1 day

    // ── Calculate balance series ────────────────────────────────────────────
    var bal = 0.0
    var rawPoints = sorted.map { c ->
        bal += c.amount
        ChartPoint(c.createdAt, bal, c.isWithdrawal)
    }

    // Add zero-start point if needed
    if (rawPoints.isNotEmpty() && abs(0.0 - rawPoints.first().balance) > 0.01) {
        rawPoints = listOf(ChartPoint(goalStartDate, 0.0, rawPoints.first().isWithdrawal)) + rawPoints
    }

    // ── Total width: proportional to timeline, minimum 360px ───────────────
    val pxPerMs = 360f / minOf(timelineMsRange.toFloat(), 86_400_000f * 90) // 360px for up to 90 days
    val timelineWidthPx = maxOf(360f, timelineMsRange.toFloat() * pxPerMs + config.horizontalPadding * 2)

    // ── Spread positions: enforce minimum horizontal gap between adjacent points ──
    //    Spread always scales to fit within timelineWidthPx, so no width adjustment needed.
    val spreadPositions = computeSpreadPositions(
        rawPoints, timelineWidthPx, config, startMs, timelineMsRange
    )
    val totalWidthPx = timelineWidthPx

    // ── Y-axis with adaptive scaling (Section 8) ───────────────────────────
    val balances = rawPoints.map { it.balance }
    val dataMin = balances.minOrNull() ?: 0.0
    val dataMax = balances.maxOrNull() ?: 100.0
    val dataRange = (dataMax - dataMin).coerceAtLeast(1.0)

    // Adaptive padding: more padding for fewer data points
    val smallDataMultiplier = when {
        pointCount <= 2 -> 0.60f    // 60% extra headroom for 1-2 points
        pointCount <= 5 -> 0.40f    // 40% for 3-5 points
        pointCount <= 10 -> 0.30f   // 30% for 6-10 points
        pointCount <= 20 -> 0.20f   // 20% for 11-20 points
        else -> 0.15f                // standard 15% for larger datasets
    }
    val pad = (dataRange * smallDataMultiplier).coerceAtLeast(1.0)
    val adjustedMin = maxOf(0.0, dataMin - pad)
    val adjustedMax = dataMax + pad

    return PreparedChartData(rawPoints, adjustedMin, adjustedMax, startMs, deadlineMs, timelineMsRange, totalWidthPx, spreadPositions)
}

// ─── Spread Position Computation ────────────────────────────────────────────────

/**
 * Computes x-positions for each data point that enforce a minimum horizontal gap
 * to prevent clustered points from collapsing into a near-vertical line.
 *
 * Strategy:
 * - A forward pass establishes ideal minimum-gap positions.
 * - If the resulting span exceeds the available chart width, positions are
 *   proportionally scaled to fit, so the axis labels remain visually aligned.
 * - Small datasets (≤3 pts) get full spread for readability.
 * - As dataset size grows, spread intensity gradually decreases.
 * - Large datasets (>20 pts) use pure timeline-based positions (no spread).
 * - Chronological order is preserved throughout.
 */
private fun computeSpreadPositions(
    points: List<ChartPoint>,
    timelineWidthPx: Float,
    config: ChartConfig,
    timelineStartMs: Long,
    timelineMsRange: Long
): List<Float> {
    val n = points.size
    if (n < 2) return emptyList()

    val hPad = config.horizontalPadding
    val availWidth = timelineWidthPx - hPad * 2

    // Responsive min gap: proportion of available chart width (14%), clamped for sanity
    val minGapPx = (availWidth * 0.14f).coerceIn(36f, 60f)

    // Step 1: Compute linear (timeline-based) positions
    val linearPos = FloatArray(n)
    for (i in 0 until n) {
        val fraction = (points[i].timestamp.toEpochMilli() - timelineStartMs).toFloat() /
            timelineMsRange.coerceAtLeast(1L).toFloat()
        linearPos[i] = hPad + fraction * availWidth
    }

    // Step 2: Determine spread intensity — fewer points = more aggressive spread.
    // Tiers use tighter granularity for smoother visual transitions as data grows.
    val spreadFactor = when {
        n <= 3 -> 1.0f
        n <= 5 -> 0.85f
        n <= 8 -> 0.70f
        n <= 12 -> 0.55f
        n <= 16 -> 0.40f
        n <= 20 -> 0.25f
        else -> 0.0f
    }
    if (spreadFactor <= 0f) return linearPos.toList()

    val targetGapPx = minGapPx * spreadFactor

    // Step 3: Forward pass — compute ideal spread positions with minimum gaps
    val spreadPos = FloatArray(n)
    spreadPos[0] = linearPos[0]
    for (i in 1 until n) {
        val linearGap = linearPos[i] - linearPos[i - 1]
        spreadPos[i] = spreadPos[i - 1] + maxOf(linearGap, targetGapPx)
    }

    // Step 4: Scale spread positions to fit within available width if they overflow.
    // This keeps axis labels (linear) and data points roughly aligned.
    val spreadSpan = spreadPos[n - 1] - spreadPos[0]
    if (spreadSpan > availWidth) {
        val scale = availWidth / spreadSpan
        for (i in 0 until n) {
            spreadPos[i] = hPad + (spreadPos[i] - hPad) * scale
        }
    }

    return spreadPos.toList()
}

// ─── Label Grid Computation ───────────────────────────────────────────────────

/**
 * Computes timestamp and canvas-space x-position pairs for each X-axis label
 * within the current viewport. Used by both vertical grid lines and X-axis labels
 * to keep them aligned without duplicating the position logic.
 */
private fun computeLabelGrid(
    zoomTimeScale: ZoomTimeScale,
    visibleStartMs: Long,
    visibleEndMs: Long,
    viewportWidthPx: Float,
    hPad: Float,
    totalWidthPx: Float,
    timelineStartMs: Long,
    timelineMsRange: Long,
    panOffset: Float
): List<Pair<Long, Float>> {
    val labelCount = when (zoomTimeScale) {
        ZoomTimeScale.MINUTES -> maxOf(4, (viewportWidthPx / 90f).toInt())
        ZoomTimeScale.HOURS   -> maxOf(4, (viewportWidthPx / 100f).toInt())
        ZoomTimeScale.DAYS    -> maxOf(3, (viewportWidthPx / 110f).toInt())
        ZoomTimeScale.WEEKS   -> maxOf(3, (viewportWidthPx / 120f).toInt())
        ZoomTimeScale.MONTHS  -> maxOf(3, (viewportWidthPx / 130f).toInt())
    }
    val totalVisibleMs = (visibleEndMs - visibleStartMs).coerceAtLeast(1L)
    val minStepMs = when (zoomTimeScale) {
        ZoomTimeScale.MINUTES -> 60_000L
        ZoomTimeScale.HOURS   -> 1_800_000L
        ZoomTimeScale.DAYS    -> 3_600_000L
        ZoomTimeScale.WEEKS   -> 21_600_000L
        ZoomTimeScale.MONTHS  -> 86_400_000L
    }
    val stepMs = (totalVisibleMs / labelCount).coerceAtLeast(minStepMs)
    val firstLabelMs = visibleStartMs - (visibleStartMs % stepMs)
    val labelCountTotal = ((visibleEndMs - firstLabelMs) / stepMs).toInt().coerceAtMost(50)
    val availW = totalWidthPx - hPad * 2
    return buildList {
        val tlRange = timelineMsRange.coerceAtLeast(1L)
        for (i in 0..labelCountTotal) {
            val labelMs = firstLabelMs + i * stepMs
            if (labelMs > visibleEndMs) break
            if (labelMs >= timelineStartMs) {
                val fraction = (labelMs - timelineStartMs).toFloat() / tlRange.toFloat()
                val x = hPad + fraction * availW - panOffset
                if (x >= 0f && x <= viewportWidthPx) {
                    add(labelMs to x)
                }
            }
        }
    }
}

// ─── Formatting Helpers ──────────────────────────────────────────────────────────

private fun formatTimeLabel(instant: Instant, timeScale: ZoomTimeScale): String {
    if (instant.toEpochMilli() < AppConstants.EPOCH_CUTOFF_MS) return "—"
    val dt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return when (timeScale) {
        // Each scale produces a visually distinct label format
        ZoomTimeScale.MINUTES -> dt.format(DateTimeFormatter.ofPattern("HH:mm"))   // "14:30" — precise clock
        ZoomTimeScale.HOURS   -> dt.format(DateTimeFormatter.ofPattern("HH'h'"))    // "14h"   — hour with suffix
        ZoomTimeScale.DAYS    -> dt.format(DateTimeFormatter.ofPattern("EEE d"))    // "Mon 10" — day-of-week + date
        ZoomTimeScale.WEEKS   -> dt.format(DateTimeFormatter.ofPattern("d MMM"))    // "10 Mar" — date (reversed)
        ZoomTimeScale.MONTHS  -> dt.format(DateTimeFormatter.ofPattern("MMM"))      // "Mar"    — month only
    }
}

// ─── Nice Axis Algorithm ──────────────────────────────────────────────────────────

private data class NiceAxis(val min: Double, val max: Double, val ticks: List<Double>)

private fun computeNiceAxis(dataMin: Double, dataMax: Double,   maxTicks: Int = 5): NiceAxis {
    val range = niceNum(dataMax - dataMin, false)
    val tickSpacing = niceNum(range / (maxTicks - 1), true)
    val niceFloor = floor(dataMin / tickSpacing) * tickSpacing
    val niceCeil = ceil(dataMax / tickSpacing) * tickSpacing
    val ticks = mutableListOf<Double>()
    var tick = niceFloor
    while (tick <= niceCeil + 0.5 * tickSpacing) {
        ticks.add(tick)
        tick += tickSpacing
    }
    return NiceAxis(niceFloor, niceCeil, ticks)
}

private fun niceNum(x: Double, round: Boolean): Double {
    if (x <= 0) return 1.0
    val exp = floor(log10(x)).toInt()
    val frac = x / 10.0.pow(exp.toDouble())
    val nice = when {
        round && frac < 1.5 -> 1.0
        round && frac < 3.0 -> 2.0
        round && frac < 7.0 -> 5.0
        round -> 10.0
        frac <= 1.0 -> 1.0
        frac <= 2.0 -> 2.0
        frac <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * 10.0.pow(exp.toDouble())
}

// ─── Labels Data & Composables ─────────────────────────────────────────────────────

private data class AxisLabelInfo(
    val text: String,
    val xDp: Float,
    val yDp: Float
)

@Composable
private fun YAxisLabel(text: String, xDp: Float, yDp: Float, mutedColor: Color) {
    Text(
        text = text,
        style = TextStyle(fontSize = 9.sp, color = mutedColor.copy(alpha = 0.7f)),
        modifier = Modifier
            .offset(x = xDp.dp, y = yDp.dp)
            .width(50.dp)
    )
}

@Composable
private fun XAxisLabel(text: String, xDp: Float, yDp: Float, mutedColor: Color, density: Density) {
    val textWidthPx = with(density) { text.length * 6f.sp.toPx() }
    val offsetXDp = with(density) { (xDp - textWidthPx / 2f).toDp().coerceAtLeast(0.dp) }
    Text(
        text = text,
        style = TextStyle(fontSize = 9.sp, color = mutedColor.copy(alpha = 0.7f)),
        modifier = Modifier
            .offset(x = offsetXDp, y = yDp.dp)
            .width(60.dp)
    )
}
