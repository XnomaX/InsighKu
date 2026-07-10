package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.*
import com.example.insightku.feature.home.presentation.ForecastPeriod

@Composable
fun AiForecastCard(
    weeklyData: List<Float>,
    monthlyData: List<Float>,
    aiInsight: String,
    selectedPeriod: ForecastPeriod,
    onPeriodChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isWeekly = selectedPeriod == ForecastPeriod.WEEKLY
    val data = if (isWeekly) weeklyData else monthlyData
    val labels = if (isWeekly) listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    else listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val barColor = NavPurple
    val barBg = AppPalette.cardBorder

    Surface(
        modifier        = modifier.fillMaxWidth().animateContentSize(),
        shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
        color           = AppPalette.card,
        tonalElevation  = 0.dp,
        shadowElevation = 4.dp,
        border          = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(Modifier.padding(Dimens.CardInnerPaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.dashboard_spending_forecast), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                    Text(stringResource(R.string.dashboard_ai_analysis), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                }
                SegmentedControl(listOf(stringResource(R.string.dashboard_weekly), stringResource(R.string.dashboard_monthly)), if (isWeekly) 0 else 1) {
                    onPeriodChange(if (it == 0) "week" else "month")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (data.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    color = AppPalette.background,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(NavPurple.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BarChart, null, tint = NavPurple.copy(alpha = 0.45f), modifier = Modifier.size(26.dp))
                        }
                        Text(stringResource(R.string.dashboard_no_data), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
                        Text(stringResource(R.string.dashboard_start_adding), style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted, textAlign = TextAlign.Center)
                    }
                }
            } else {
                val maxValue = data.maxOrNull()?.takeIf { it > 0f } ?: 1f
                Row(
                    modifier = Modifier.fillMaxWidth().height(108.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { i, value ->
                        val fraction = (value / maxValue).coerceIn(0f, 1f)
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
                                labels.getOrNull(i) ?: "",
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
            if (aiInsight.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    color = NavPurple.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, NavPurple.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(NavPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 10.sp, color = NavPurple)
                        }
                        Text(
                            aiInsight,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textPrimary,
                            modifier = Modifier.weight(1f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
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
                        color = if (sel) Color.White else AppPalette.textMuted
                    )
                }
            }
        }
    }
}
