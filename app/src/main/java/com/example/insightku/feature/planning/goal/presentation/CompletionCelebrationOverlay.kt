package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.SuccessColor

/**
 * Elegant celebration overlay shown when a goal is completed.
 * Displays a trophy icon with a subtle pulse animation and a congratulations message.
 */
@Composable
fun CompletionCelebrationOverlay(
    goalName: String,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    // Auto-dismiss after 3 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        visible = false
        onDismiss()
    }

    // Pulse animation for the trophy
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophy_scale"
    )

    // Fade-in particles
    val particleAlpha by animateFloatAsState(
        targetValue = if (visible) 0.6f else 0f,
        animationSpec = tween(600),
        label = "particle_alpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(400))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            // ── Decorative dots (minimal particles) ─────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                listOf(
                    Alignment.TopStart to 60.dp,
                    Alignment.TopEnd to 80.dp,
                    Alignment.BottomStart to 70.dp,
                    Alignment.BottomEnd to 50.dp,
                    Alignment.CenterStart to 90.dp,
                    Alignment.CenterEnd to 65.dp
                ).forEach { (alignment, offset) ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SuccessColor.copy(alpha = particleAlpha * 0.5f))
                            .align(alignment)
                            .padding(start = if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart || alignment == Alignment.CenterStart) offset else 0.dp)
                            .padding(end = if (alignment == Alignment.TopEnd || alignment == Alignment.BottomEnd || alignment == Alignment.CenterEnd) offset else 0.dp)
                            .padding(top = if (alignment == Alignment.TopStart || alignment == Alignment.TopEnd) offset else 0.dp)
                            .padding(bottom = if (alignment == Alignment.BottomStart || alignment == Alignment.BottomEnd) offset else 0.dp)
                    )
                }
            }

            // ── Main celebration card ──────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(32.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Trophy with pulse
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(SuccessColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = SuccessColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.goal_celebration_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.goal_celebration_desc, goalName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { visible = false; onDismiss() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                    ) {
                        Text(
                            stringResource(R.string.goal_celebration_dismiss),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
