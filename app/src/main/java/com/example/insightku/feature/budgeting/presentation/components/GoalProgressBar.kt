package com.example.insightku.feature.budgeting.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.SuccessColor

/**
 * Animated progress bar for goal completion.
 */
@Composable
fun GoalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showPercentage: Boolean = true,
    animated: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 100f) / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    val displayProgress = if (animated) animatedProgress else progress.coerceIn(0f, 100f) / 100f
    val isComplete = progress >= 100f

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isComplete) SuccessColor else progressColor)
            )
        }

        if (showPercentage) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${progress.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (isComplete) SuccessColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Compact progress indicator for use in cards.
 */
@Composable
fun CompactProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 100f) / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "compact_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(animatedProgress)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(progressColor)
    )
}

/**
 * Segmented progress bar showing multiple goals.
 */
@Composable
fun SegmentedProgressBar(
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        segments.forEach { (progress, color) ->
            if (progress > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(progress.coerceIn(0f, 100f) / 100f)
                        .background(color)
                )
            }
        }
    }
}
