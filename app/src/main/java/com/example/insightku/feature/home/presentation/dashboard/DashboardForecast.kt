package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.feature.home.domain.forecast.ForecastBar
import com.example.insightku.feature.home.domain.forecast.ForecastNotice
import com.example.insightku.feature.home.domain.forecast.ForecastUi
import com.example.insightku.feature.home.presentation.ForecastPeriod

/**
 * Spending-forecast card.
 *
 * Renders exactly what the model returned — see [ForecastUi] for why the two
 * views are 7 daily bars and 4 weekly buckets of one 28-day profile, and why
 * there is no 12-month view. Every bar carries its own date-derived label, so
 * no label here is hardcoded to a weekday or month that may not match the data.
 *
 * When there is nothing honest to draw ([ForecastUi.notice] is set, or the bars
 * are empty) an explanatory state is shown instead. A refusal is rendered as a
 * calm "needs more history" note, never as an error and never as zero bars.
 */
@Composable
fun AiForecastCard(
    forecast: ForecastUi,
    selectedPeriod: ForecastPeriod,
    onPeriodChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isWeekly = selectedPeriod == ForecastPeriod.WEEKLY
    val bars = if (isWeekly) forecast.weeklyBars else forecast.monthlyBars
    val barColor = NavPurple
    val barBg = AppPalette.cardBorder

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(Dimens.CardRadiusLarge),
        color = AppPalette.card,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(Modifier.padding(Dimens.CardInnerPaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(stringResource(R.string.dashboard_spending_forecast), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                    Text(stringResource(R.string.dashboard_ai_analysis), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                }
                SegmentedControl(listOf(stringResource(R.string.dashboard_weekly), stringResource(R.string.dashboard_monthly)), if (isWeekly) 0 else 1) {
                    onPeriodChange(if (it == 0) "week" else "month")
                }
            }
            Spacer(Modifier.height(16.dp))

            if (bars.isEmpty()) {
                ForecastEmptyState(forecast.notice)
            } else {
                ForecastBars(bars, barColor, barBg)
                // Insight line only when the model actually answered.
                val insight = forecast.insight
                if (insight.isNotBlank()) {
                    ForecastNote(insight, NavPurple.copy(alpha = 0.06f), NavPurple.copy(alpha = 0.12f), "💡")
                }
                // The endpoint reports model_status = research_not_validated_on_idr.
                // Surface that caveat whenever a projection is shown, so the
                // number is never read as validated financial advice.
                if (forecast.modelStatus == RESEARCH_STATUS) {
                    Spacer(Modifier.height(8.dp))
                    ForecastNote(
                        stringResource(R.string.dashboard_forecast_research_note),
                        AppPalette.background, AppPalette.cardBorder, "⚠️"
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastBars(bars: List<ForecastBar>, barColor: Color, barBg: Color) {
    val maxValue = bars.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            val fraction = (bar.value / maxValue).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Canvas(Modifier.fillMaxWidth().height(88.dp)) {
                    val fh = size.height * fraction
                    drawRoundRect(barBg, Offset.Zero, Size(size.width, size.height), CornerRadius(8f))
                    drawRoundRect(barColor, Offset(0f, size.height - fh), Size(size.width, fh), CornerRadius(8f))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = AppPalette.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Explains why there is no chart. A model refusal gets a "needs more history"
 * message; offline and transport errors get their own, each without implying a
 * zero projection.
 */
@Composable
private fun ForecastEmptyState(notice: ForecastNotice?) {
    val (title, body) = when (notice) {
        is ForecastNotice.Refused ->
            stringResource(R.string.dashboard_no_data) to
                stringResource(R.string.dashboard_forecast_need_more_history)
        is ForecastNotice.Offline ->
            stringResource(R.string.dashboard_no_data) to
                stringResource(R.string.dashboard_forecast_offline)
        is ForecastNotice.Error -> {
            val detail = stringResource(R.string.dashboard_forecast_refused_detail, notice.message)
            stringResource(R.string.dashboard_no_data) to
                stringResource(R.string.dashboard_forecast_unavailable, detail)
        }
        null ->
            stringResource(R.string.dashboard_no_data) to
                stringResource(R.string.dashboard_start_adding)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = AppPalette.background,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
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
                Icon(Icons.Default.BarChart, null, tint = NavPurple.copy(alpha = 0.45f), modifier = Modifier.size(26.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text(body, style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ForecastNote(text: String, bg: Color, border: Color, emoji: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(NavPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 10.sp, color = NavPurple)
            }
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textPrimary,
                modifier = Modifier.weight(1f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
internal fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = AppPalette.cardElevated,
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(Modifier.padding(3.dp)) {
            options.forEachIndexed { index, label ->
                val sel = index == selectedIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (sel) NavPurple else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel) Color.White else AppPalette.textMuted,
                        // Never wrap: with a narrow leftover width a soft-wrapping label
                        // stacked one character per line (observed on device: "Monthly" as a
                        // 37px-wide, 285px-tall column). Ellipsis keeps the failure visible
                        // instead of destroying the card's height.
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private const val RESEARCH_STATUS = "research_not_validated_on_idr"
