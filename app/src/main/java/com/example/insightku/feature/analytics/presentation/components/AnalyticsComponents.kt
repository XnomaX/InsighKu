package com.example.insightku.feature.analytics.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.feature.analytics.domain.HeroInsight
import com.example.insightku.feature.analytics.presentation.AnalyticsPalette

// ─── Hero Insight Card ────────────────────────────────────────────────────────

@Composable
fun HeroInsightCard(
    hero: HeroInsight,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = AppPalette.card
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppPalette.accent.copy(alpha = 0.15f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji
            Text(
                text = hero.emoji,
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Headline
            Text(
                text = hero.headline,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                ),
                color = AppPalette.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtext
            Text(
                text = hero.subtext,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Narrative Card ───────────────────────────────────────────────────────────

@Composable
fun NarrativeCard(
    emoji: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    accentColor: Color = AppPalette.accent
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppPalette.cardBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppPalette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.textMuted,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ─── Category Bar ─────────────────────────────────────────────────────────────

@Composable
fun CategoryBar(
    name: String,
    percentage: Float,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppPalette.cardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// ─── Insight Chip ─────────────────────────────────────────────────────────────

@Composable
fun InsightChip(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.ChipRadius),
        color = AppPalette.cardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textPrimary
            )
        }
    }
}

// ─── Stat Row ─────────────────────────────────────────────────────────────────

@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AppPalette.textPrimary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun AnalyticsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = AppPalette.textPrimary,
        modifier = modifier.padding(bottom = 12.dp)
    )
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun AnalyticsEmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Layered circles
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AppPalette.accent.copy(alpha = 0.06f))
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AppPalette.accent.copy(alpha = 0.1f))
            )
            Text(text = emoji, fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Skeleton Loader ──────────────────────────────────────────────────────────

@Composable
fun AnalyticsSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(Dimens.CardRadiusLarge))
                .background(AppPalette.cardBorder.copy(alpha = alpha))
        )

        // Cards skeleton
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(Dimens.CardRadius))
                    .background(AppPalette.cardBorder.copy(alpha = alpha))
            )
        }

        // Bar skeleton
        repeat(4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppPalette.cardBorder.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppPalette.cardBorder.copy(alpha = alpha))
                )
            }
        }
    }
}
