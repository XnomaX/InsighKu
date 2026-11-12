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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.example.insightku.core.ui.components.FitText
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
                        FitText(
                            text = period.displayName,
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Indicator row — mirrors the tab row's weight(1f) segments so the marker is
            // always centred under its tab, at any screen width. The previous version
            // computed `offset(x = (indicatorOffset * 360).dp)`, which assumed a 360dp-wide
            // screen: on any other width the indicator drifted away from the selected tab.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            ) {
                periods.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (index == selectedIndex) {
                            Box(
                                modifier = Modifier
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
        }
    }
}