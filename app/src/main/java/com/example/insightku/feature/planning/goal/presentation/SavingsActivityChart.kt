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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
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
    val amount: Double,
    val isWithdrawal: Boolean,
    val label: String,
)

// ExtraStore keys for timestamp mapping
private val timestampKey = ExtraStore.Key<List<Long>>()
private val xRangeStartKey = ExtraStore.Key<Double>()
private val xRangeEndKey = ExtraStore.Key<Double>()


// ─── Main Chart Composable ────────────────────────────────────────────────────────

@Composable
fun SavingsActivityChart(
    contributions: List<Contribution>,
    goalColor: Color,
    goalStartDate: Instant,
    goalDeadline: LocalDate?,
    targetAmount: Double = 0.0,
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
                targetAmount = targetAmount,
            )
        }
    }
}

// ─── Chart Card ───────────────────────────────────────────────────────────────────

@Composable
internal fun ChartCardContent(
    contributions: List<Contribution>,
    goalColor: Color,
    goalStartDate: Instant,
    goalDeadline: LocalDate?,
    targetAmount: Double,
    modifier: Modifier = Modifier,
) {
    val points = remember(contributions, goalStartDate, goalDeadline) {
        computeChartPoints(contributions, goalStartDate, goalDeadline)
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    val deadlineXMillis = remember(goalDeadline, goalStartDate) {
        goalDeadline?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            ?.toFloat()
    }

    val timestampMap = remember(points) {
        points.associate { it.timestamp.toEpochMilli().toFloat() to it.timestamp }
    }

    LaunchedEffect(points, deadlineXMillis) {
        if (points.isEmpty()) return@LaunchedEffect
        val timestamps = points.map { it.timestamp.toEpochMilli() }
        val balances = points.map { it.balance }

        modelProducer.runTransaction {
            lineModel {
                series(x = timestamps.map { it.toFloat() }, y = balances)
            }
            extras {
                it[timestampKey] = timestamps
                it[xRangeStartKey] = timestamps.first().toDouble()
                it[xRangeEndKey] = timestamps.last().toDouble()
            }
        }
    }

    Column(
        modifier = modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        ChartSummaryHeader(points = points, goalColor = goalColor)
        Spacer(Modifier.height(12.dp))

        val zoomState = rememberVicoZoomState(zoomEnabled = true)
        val xAxisFormatter = remember {
            createAdaptiveXAxisFormatter(zoomState)
        }

        val deadlineXFloat = deadlineXMillis
        val firstXFloat = points.firstOrNull()?.timestamp?.toEpochMilli()?.toFloat()
        val deadlineDecoration = remember(goalColor, deadlineXFloat, firstXFloat) {
            if (deadlineXFloat != null && firstXFloat != null) {
                DeadlineVerticalLineDecoration(
                    startX = firstXFloat,
                    endX = deadlineXFloat,
                    deadlineX = deadlineXFloat,
                    color = goalColor,
                )
            } else {
                null
            }
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
                    rangeProvider = remember(deadlineXMillis) {
                        object : CartesianLayerRangeProvider {
                            override fun getMinX(minX: Double, maxX: Double, extraStore: ExtraStore) = minX
                            override fun getMaxX(minX: Double, maxX: Double, extraStore: ExtraStore) =
                                deadlineXMillis?.toDouble() ?: maxX
                            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore) = minY
                            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore) = maxY
                        }
                    },
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = CartesianValueFormatter { _, y, _ ->
                        NumberFormatter.formatCurrencyCompact(y.toDouble())
                    },
                    line = null,
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = xAxisFormatter,
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                        spacing = { maxOf(1, points.size / 6) },
                        addExtremeLabelPadding = true,
                    ),
                    line = null,
                ),
                decorations = if (deadlineDecoration != null) {
                    listOf(deadlineDecoration)
                } else {
                    emptyList()
                },
                marker = rememberSavingsMarker(
                    goalColor = goalColor,
                    points = points,
                    timestampMap = timestampMap,
                    targetAmount = targetAmount,
                    balanceLabel = stringResource(R.string.chart_tooltip_balance),
                    progressLabel = stringResource(R.string.chart_tooltip_progress),
                ),
                markerController = CartesianMarkerController.rememberShowOnPress(),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            scrollState = rememberVicoScrollState(),
            zoomState = zoomState,
        )
    }
}

@Composable
private fun ChartCard(
    contributions: List<Contribution>,
    goalColor: Color,
    goalStartDate: Instant,
    goalDeadline: LocalDate?,
    targetAmount: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, goalColor.copy(alpha = 0.15f)),
    ) {
        ChartCardContent(
            contributions = contributions,
            goalColor = goalColor,
            goalStartDate = goalStartDate,
            goalDeadline = goalDeadline,
            targetAmount = targetAmount,
        )
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
            amount = c.amount,
            isWithdrawal = c.isWithdrawal,
            label = c.notes.ifEmpty { if (c.isWithdrawal) "Withdrawal" else "Deposit" },
        )
    }

    val withStart = if (points.isNotEmpty() && abs(points.first().balance) > 0.01) {
        listOf(
            ChartPoint(
                timestamp = goalStartDate,
                balance = 0.0,
                amount = 0.0,
                isWithdrawal = points.first().isWithdrawal,
                label = "Start",
            ),
        ) + points
    } else {
        points
    }

    return withStart
}

// ─── Time Formatting ─────────────────────────────────────────────────────────────

/**
 * Formats a timestamp label based on the data duration in milliseconds.
 */
private fun formatTimeLabelByDuration(instant: Instant, durationMs: Long): String {
    val dt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val locale = Locale.getDefault()
    return when {
        durationMs <= 3_600_000L ->
            dt.format(DateTimeFormatter.ofPattern("HH:mm", locale))
        durationMs <= 86_400_000L ->
            dt.format(DateTimeFormatter.ofPattern("HH'h'", locale))
        durationMs <= 604_800_000L ->
            dt.format(DateTimeFormatter.ofPattern("EEE d", locale))
        durationMs <= 7_776_000_000L ->
            dt.format(DateTimeFormatter.ofPattern("d MMM", locale))
        else ->
            dt.format(DateTimeFormatter.ofPattern("MMM yyyy", locale))
    }
}

/**
 * Formats a timestamp for display in the tooltip with full date and time.
 * Example: "19 Jul 2026, 14:30"
 */
private fun formatTooltipDateTime(instant: Instant): String {
    val dt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val locale = Locale.getDefault()
    return dt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale))
}

// ─── Formatters ───────────────────────────────────────────────────────────────────

/**
 * Creates an adaptive X-axis formatter that changes label format based on the data range.
 * Reads the X range from [ExtraStore] (set during model transaction) to determine the appropriate
 * time scale: HH:mm → HH'h' → EEE d → d MMM → MMM yyyy.
 */
private fun createAdaptiveXAxisFormatter(
    zoomState: VicoZoomState,
): CartesianValueFormatter {
    return CartesianValueFormatter { context, x, _ ->
        val startMs = context.model.extraStore[xRangeStartKey] ?: 0.0
        val endMs = context.model.extraStore[xRangeEndKey] ?: 0.0
        val totalDurationMs = (endMs - startMs).toLong()
        val zoom = zoomState.value
        val visibleDurationMs = (totalDurationMs / zoom).toLong()
        val instant = Instant.ofEpochMilli(x.toLong())
        formatTimeLabelByDuration(instant, visibleDurationMs)
    }
}

// ─── Deadline Vertical Line Decoration ─────────────────────────────────────────────

/**
 * A custom [Decoration] that draws a vertical dashed line at the goal deadline X position.
 * Follows the same pattern as Vico's built-in [com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine].
 */
private class DeadlineVerticalLineDecoration(
    private val startX: Float,
    private val endX: Float,
    private val deadlineX: Float,
    private val color: Color,
) : Decoration {
    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            val range = endX - startX
            if (range <= 0f) return@with
            val fraction = ((deadlineX - startX) / range).toFloat()
            val canvasX = layerBounds.left + fraction * layerBounds.width

            val d = density.density
            val paint = Paint().apply {
                this.color = this@DeadlineVerticalLineDecoration.color.copy(alpha = 0.7f)
                strokeWidth = 2f * d
                style = PaintingStyle.Stroke
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(12f * d, 6f * d),
                )
            }

            canvas.drawLine(
                Offset(canvasX, layerBounds.top),
                Offset(canvasX, layerBounds.bottom),
                paint,
            )
        }
    }

    override fun drawUnderLayers(context: CartesianDrawingContext) = Unit

    override fun equals(other: Any?): Boolean =
        this === other ||
        other is DeadlineVerticalLineDecoration &&
        startX == other.startX &&
        endX == other.endX &&
        deadlineX == other.deadlineX &&
        color == other.color

    override fun hashCode(): Int =
        31 * (31 * (31 * startX.hashCode() + endX.hashCode()) + deadlineX.hashCode()) + color.hashCode()
}

// ─── Marker ───────────────────────────────────────────────────────────────────────

@Composable
private fun rememberSavingsMarker(
    goalColor: Color,
    points: List<ChartPoint>,
    timestampMap: Map<Float, Instant>,
    targetAmount: Double,
    balanceLabel: String,
    progressLabel: String,
): DefaultCartesianMarker {
    // Build lookup map from x-value to full point info (balance, amount, type)
    val lookupMap = remember(points) {
        points.associate { p ->
            p.timestamp.toEpochMilli().toFloat() to Triple(p.balance, p.amount, p.isWithdrawal)
        }
    }

    val depositColor = SuccessColor
    val withdrawalColor = ExpenseRed
    val textPrimary = AppPalette.textPrimary
    val textMuted = AppPalette.textMuted
    val cardColor = AppPalette.card

    val labelBackground = rememberShapeComponent(
        fill = Fill(cardColor),
        shape = RoundedCornerShape(12.dp),
        strokeFill = Fill(Color.Black.copy(alpha = 0.06f)),
        strokeThickness = 1.dp,
    )
    val indicatorComponent = rememberShapeComponent(
        fill = Fill(goalColor),
        shape = CircleShape,
    )
    val guidelineComponent = rememberLineComponent(
        fill = Fill(goalColor.copy(alpha = 0.35f)),
        thickness = 1.dp,
    )

    return rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = TextStyle(
                color = AppPalette.textPrimary,
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
                lineHeight = 16.sp,
            ),
            padding = Insets(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            background = labelBackground,
        ),
        valueFormatter = DefaultCartesianMarker.ValueFormatter { _, targets ->
            val target = targets.firstOrNull() ?: return@ValueFormatter "—"
            val x = target.x
            val closestTs = timestampMap.keys.minByOrNull { abs(it - x) }
            val timestamp = timestampMap[closestTs]
            val (balance, amount, isWithdrawal) = lookupMap[closestTs]
                ?: Triple(0.0, 0.0, false)

            val dateStr = timestamp?.let { formatTooltipDateTime(it) }.orEmpty()

            // Sign-prefixed transaction amount (primary info)
            val amountStr = NumberFormatter.formatCurrencyWithSign(
                amount = amount,
                isIncome = !isWithdrawal,
            )
            val amountColorLocal = if (isWithdrawal) withdrawalColor else depositColor

            buildAnnotatedString {
                // Line 1: Transaction amount (primary, bold, colored)
                withStyle(
                    SpanStyle(
                        color = amountColorLocal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                ) { append(amountStr) }
                append("\n")

                // Line 2: Full date & time (muted)
                withStyle(
                    SpanStyle(
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                ) { append(dateStr) }
                append("\n")

                // Line 3: Accumulated balance
                withStyle(
                    SpanStyle(
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                ) {
                    append("$balanceLabel: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(NumberFormatter.formatCurrency(balance))
                    }
                }

                // Line 4: Goal progress (only when targetAmount > 0)
                if (targetAmount > 0.0) {
                    append("\n")
                    val progress = (balance / targetAmount * 100).coerceIn(0.0, 100.0)
                    withStyle(
                        SpanStyle(
                            color = textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    ) {
                        append("$progressLabel: ")
                        withStyle(
                            SpanStyle(
                                color = goalColor,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ) { append(String.format(Locale.getDefault(), "%.0f%%", progress)) }
                    }
                }
            }
        },
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
        indicator = { indicatorComponent },
        indicatorSize = 22.dp,
        guideline = guidelineComponent,
    )
}
