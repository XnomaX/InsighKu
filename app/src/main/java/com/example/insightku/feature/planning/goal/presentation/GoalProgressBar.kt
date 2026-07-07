package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.SuccessColor

@Composable
fun GoalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Unspecified,
    showPercentage: Boolean = true,
    animated: Boolean = true
) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 100f) / 100f, animationSpec = tween(durationMillis = 800), label = "progress")
    val displayProgress = if (animated) animatedProgress else progress.coerceIn(0f, 100f) / 100f
    val isComplete = progress >= 100f
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (backgroundColor != Color.Unspecified) backgroundColor else Color.LightGray)) {
            Box(modifier = Modifier.fillMaxWidth(displayProgress).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if (isComplete) SuccessColor else if (progressColor != Color.Unspecified) progressColor else Color.Unspecified))
        }
        if (showPercentage) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${progress.toInt()}%", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = if (isComplete) SuccessColor else Color.Gray, fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
