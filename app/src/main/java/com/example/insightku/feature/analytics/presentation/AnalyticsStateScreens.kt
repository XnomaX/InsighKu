package com.example.insightku.feature.analytics.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.Dimens

/**
 * Shared card shell for every Analytics section: white (or dark) surface, 1dp brand border,
 * flat tonal elevation, generous inner padding. Optionally clickable (for tap-to-expand cards),
 * with a shared [interaction] source so callers can layer a press micro-interaction.
 */
@Composable
fun AnalyticsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    interaction: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction ?: remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier
    Surface(
        modifier = modifier.fillMaxWidth().then(clickModifier),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = AnalyticsPalette.card,
        border = BorderStroke(1.dp, AnalyticsPalette.cardBorder),
        tonalElevation = 0.dp,
        shadowElevation = Dimens.ElevationSmall
    ) {
        Column(Modifier.padding(Dimens.CardInnerPaddingLarge)) { content() }
    }
}

/** Small section title + optional subtitle, following the design-system header pattern. */
@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = AnalyticsPalette.textPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = AnalyticsPalette.textMuted
            )
        }
    }
}

@Composable
fun AnalyticsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = AnalyticsPalette.Purple)
    }
}

@Composable
fun AnalyticsEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.PaddingExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "✨", style = androidx.compose.material3.MaterialTheme.typography.displaySmall)
        Text(
            text = "No habits to show yet — log a few transactions and watch your story appear.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = AnalyticsPalette.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.PaddingLarge)
        )
    }
}

@Composable
fun AnalyticsErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.PaddingExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = AnalyticsPalette.textPrimary,
            textAlign = TextAlign.Center
        )
        Surface(
            modifier = Modifier
                .padding(top = Dimens.PaddingLarge)
                .clickable(onClick = onRetry),
            shape = RoundedCornerShape(50.dp),
            color = AnalyticsPalette.card,
            border = BorderStroke(1.dp, AnalyticsPalette.Purple)
        ) {
            Text(
                text = "Try again",
                color = AnalyticsPalette.Purple,
                modifier = Modifier.padding(horizontal = Dimens.PaddingExtraLarge, vertical = Dimens.PaddingMedium)
            )
        }
    }
}

/**
 * "Covered until curious" — the core interaction loop. Until [revealed], shows a calm invitation
 * (emoji + question + soft tap affordance). On tap it fires [onReveal] and cross-fades to [content].
 * One-way: an uncovered section stays uncovered for the session. Quiet Reveal texture — no quiz,
 * no guessing, just an invitation that unfolds in place.
 */
@Composable
fun Uncoverable(
    revealed: Boolean,
    emoji: String,
    invitation: String,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = revealed,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "uncover"
    ) { isRevealed ->
        if (isRevealed) {
            content()
        } else {
            val interaction = remember { MutableInteractionSource() }
            Surface(
                modifier = modifier.fillMaxWidth().clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onReveal
                ),
                shape = RoundedCornerShape(Dimens.CardRadius),
                color = AnalyticsPalette.card,
                border = BorderStroke(1.dp, AnalyticsPalette.cardBorder),
                tonalElevation = 0.dp,
                shadowElevation = Dimens.ElevationSmall
            ) {
                Row(
                    Modifier.padding(Dimens.CardInnerPaddingLarge),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(Dimens.PaddingLarge))
                    Column(Modifier.weight(1f)) {
                        Text(
                            invitation,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AnalyticsPalette.textPrimary
                        )
                        Spacer(Modifier.size(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.TouchApp, null,
                                tint = AnalyticsPalette.Purple,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Tap to reveal",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = AnalyticsPalette.Purple
                            )
                        }
                    }
                }
            }
        }
    }
}


