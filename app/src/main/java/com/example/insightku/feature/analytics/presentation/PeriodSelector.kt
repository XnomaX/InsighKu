package com.example.insightku.feature.analytics.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.feature.analytics.domain.AnalyticsPeriodType

/**
 * Analytics Period Selector — Premium navigation between Weekly, Monthly, and Annual views.
 *
 * Custom implementation with no ripple effect.
 * Uses Box + clickable for clean tap interaction.
 */
@Composable
fun PeriodSelector(
    selectedPeriod: AnalyticsPeriodType,
    onPeriodSelected: (AnalyticsPeriodType) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = AnalyticsPeriodType.entries
    val selectedIndex = periods.indexOf(selectedPeriod)
    val indicatorWidth = 48.dp

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab row with labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                periods.forEachIndexed { index, period ->
                    val isSelected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(durationMillis = 200),
                        label = "tabTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onPeriodSelected(period) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.displayName,
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Indicator row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                // Calculate indicator offset: each tab is 1/3 of width, center it in selected segment
                val indicatorOffset = when (selectedIndex) {
                    0 -> 0.166f
                    1 -> 0.5f
                    2 -> 0.833f
                    else -> 0.166f
                }

                Box(
                    modifier = Modifier
                        .offset(x = (indicatorOffset * 360).dp - indicatorWidth / 2)
                        .width(indicatorWidth)
                        .height(2.5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }
        }
    }
}