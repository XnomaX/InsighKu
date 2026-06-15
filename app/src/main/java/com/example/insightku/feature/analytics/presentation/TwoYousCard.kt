package com.example.insightku.feature.analytics.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.insightku.feature.analytics.domain.BehavioralPattern
import com.example.insightku.feature.analytics.domain.SpendingPersonality
import com.example.insightku.core.ui.theme.Dimens
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * "Two Yous" — the Analytics centerpiece. A draggable mirror: the user drags a thumb between Weekday
 * You (left) and Weekend You (right) and the card morphs continuously between the two selves. The
 * insight is the GAP between them, felt under the thumb.
 *
 * Two hard rules (see the `analytics_two_yous` memory) are load-bearing here:
 *  1. The bold headline always narrates the *relationship + swing*, never the current pole. It stays
 *     constant as you drag — the morph below is the evidence you explore to verify it.
 *  2. Consistency of meaning: left is always the quieter self; the magnitude tint uses purple
 *     intensity, never red (red would read as "weekend = bad").
 *
 * When [TwoYousUi.isConfident] is false (sparse data) the card shows a calm greeting instead of a
 * faked comparison, using [personality] — keeping the top-of-screen 3-second greeting intact.
 */
@Composable
fun TwoYousCard(
    twoYous: TwoYousUi,
    personality: SpendingPersonality,
    patterns: List<BehavioralPattern>,
    patternsExpanded: Boolean,
    onTogglePatterns: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!twoYous.isConfident) {
        GreetingFallbackCard(personality, patterns, patternsExpanded, onTogglePatterns, modifier)
        return
    }

    // Drag fraction 0f (Weekday You) .. 1f (Weekend You). Ephemeral view state — like scroll
    // position, it never round-trips through the ViewModel. Starts mid-gap so the morph is the
    // first thing the user reaches out and moves.
    val t = remember { Animatable(0.5f) }
    val scope = rememberCoroutineScope()

    AnalyticsCard(modifier) {
        // ── Hard Rule 1: the constant gap headline ────────────────────────────
        Text(
            twoYous.gapHeadline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AnalyticsPalette.textPrimary
        )
        Spacer(Modifier.height(Dimens.PaddingLarge))

        // ── The morph stage ───────────────────────────────────────────────────
        MorphStage(twoYous, t.value)

        Spacer(Modifier.height(Dimens.PaddingLarge))

        // ── The instrument: track + draggable thumb ───────────────────────────
        PoleTrack(
            t = t.value,
            leftLabel = twoYous.left.label,
            rightLabel = twoYous.right.label,
            onDrag = { deltaPx, usableWidthPx ->
                if (usableWidthPx > 0f) {
                    val next = (t.value + deltaPx / usableWidthPx).coerceIn(0f, 1f)
                    scope.launch { t.snapTo(next) }
                }
            },
            onDragEnd = {
                // Settle to the nearest of 3 meaningful rest points: Weekday · gap · Weekend.
                val target = listOf(0f, 0.5f, 1f).minByOrNull { abs(it - t.value) } ?: 0.5f
                scope.launch { t.animateTo(target, spring()) }
            }
        )

        // ── Absorbed deeper patterns (progressive disclosure) ──────────────────
        if (patterns.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.PaddingLarge))
            PatternsDisclosure(patterns, patternsExpanded, onTogglePatterns)
        }
    }
}

/** Emoji crossfade + morphing category dots, interpolated by [t] between the two selves. */
@Composable
private fun MorphStage(twoYous: TwoYousUi, t: Float) {
    val left = twoYous.left
    val right = twoYous.right
    // Magnitude tint: purple whose alpha tracks the per-day intensity of the side we're nearest.
    val intensity = lerp(left.intensity, right.intensity, t)
    val tintAlpha = 0.06f + 0.16f * intensity

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
            .background(AnalyticsPalette.Purple.copy(alpha = tintAlpha))
            .padding(Dimens.CardInnerPaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Crossfading faces — both drawn, alpha-blended by t (no popping between states).
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(56.dp)) {
            Text(left.emoji, style = MaterialTheme.typography.displaySmall, modifier = Modifier.alpha(1f - t))
            Text(right.emoji, style = MaterialTheme.typography.displaySmall, modifier = Modifier.alpha(t))
        }
        Spacer(Modifier.height(Dimens.PaddingSmall))
        // Orientation label — which self you're looking at (fades across the midpoint).
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(20.dp)) {
            Text(
                left.label,
                style = MaterialTheme.typography.labelLarge,
                color = AnalyticsPalette.Purple,
                modifier = Modifier.alpha(1f - t)
            )
            Text(
                right.label,
                style = MaterialTheme.typography.labelLarge,
                color = AnalyticsPalette.Purple,
                modifier = Modifier.alpha(t)
            )
        }
        Spacer(Modifier.height(Dimens.PaddingLarge))
        MorphingDots(left.categories, right.categories, t)
    }
}

/**
 * The category dots that grow/shrink as you drag. Each dot is keyed by category name so the same
 * category keeps its identity (color + position) across the morph; its size lerps between its weight
 * in the left self and its weight in the right self.
 */
@Composable
private fun MorphingDots(
    leftCats: List<SelfCategoryUi>,
    rightCats: List<SelfCategoryUi>,
    t: Float
) {
    // Union of categories, ordered by combined weight so the loudest sit first.
    val names = remember(leftCats, rightCats) {
        (leftCats + rightCats)
            .groupBy { it.name }
            .entries
            .sortedByDescending { (_, v) -> v.maxOf { it.proportion } }
            .map { it.key }
            .take(5)
    }
    val leftByName = remember(leftCats) { leftCats.associateBy { it.name } }
    val rightByName = remember(rightCats) { rightCats.associateBy { it.name } }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
        verticalAlignment = Alignment.Bottom
    ) {
        names.forEach { name ->
            val l = leftByName[name]
            val r = rightByName[name]
            val color = (l ?: r)?.color ?: AnalyticsPalette.Purple
            val lp = l?.proportion ?: 0f
            val rp = r?.proportion ?: 0f
            val p = lerp(lp, rp, t)
            val dotSize = (16 + 40 * p).dp
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.30f + 0.45f * p))
                )
                Spacer(Modifier.height(Dimens.PaddingSmall))
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = AnalyticsPalette.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** The track + draggable thumb. The thumb position reflects [t]; dragging reports pixel deltas up. */
@Composable
private fun PoleTrack(
    t: Float,
    leftLabel: String,
    rightLabel: String,
    onDrag: (deltaPx: Float, usableWidthPx: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val thumb = 28.dp
    val thumbPx = with(density) { thumb.toPx() }
    var usableWidthPx by remember { mutableFloatStateOf(1f) }

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(thumb)
                .onSizeChanged { usableWidthPx = (it.width - thumbPx).coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount, usableWidthPx)
                    }
                }
        ) {
            // Rail
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AnalyticsPalette.Purple.copy(alpha = 0.15f))
            )
            // Thumb — positioned by t along the usable track (full width minus the thumb's own width).
            val offsetX = with(density) { (usableWidthPx * t).toDp() }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = offsetX)
                    .size(thumb)
                    .clip(CircleShape)
                    .background(AnalyticsPalette.Purple)
            )
        }
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftLabel, style = MaterialTheme.typography.labelSmall, color = AnalyticsPalette.textMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.UnfoldMore, null, tint = AnalyticsPalette.textMuted, modifier = Modifier.size(12.dp).rotate(90f))
                Spacer(Modifier.width(2.dp))
                Text("drag", style = MaterialTheme.typography.labelSmall, color = AnalyticsPalette.textMuted)
            }
            Text(rightLabel, style = MaterialTheme.typography.labelSmall, color = AnalyticsPalette.textMuted)
        }
    }
}

/** The "there's a little more to your story" expand, absorbed from the old personality card. */
@Composable
private fun PatternsDisclosure(
    patterns: List<BehavioralPattern>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggle() }) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (expanded) "Less" else "There's a little more to your story",
                style = MaterialTheme.typography.labelLarge,
                color = AnalyticsPalette.Purple
            )
            val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
            Icon(
                Icons.Filled.ExpandMore, null,
                tint = AnalyticsPalette.Purple,
                modifier = Modifier.size(20.dp).rotate(rotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                Modifier.padding(top = Dimens.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                patterns.forEach { p ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(p.emoji, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(Dimens.PaddingMedium))
                        Text(p.text, style = MaterialTheme.typography.bodyMedium, color = AnalyticsPalette.textPrimary)
                    }
                }
            }
        }
    }
}

/** Sparse-data fallback — a calm single greeting, never a faked comparison. */
@Composable
private fun GreetingFallbackCard(
    personality: SpendingPersonality,
    patterns: List<BehavioralPattern>,
    patternsExpanded: Boolean,
    onTogglePatterns: () -> Unit,
    modifier: Modifier
) {
    AnalyticsCard(modifier) {
        Text(personality.emoji, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(Dimens.PaddingMedium))
        Text(
            personality.sentence,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AnalyticsPalette.textPrimary
        )
        if (patterns.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.PaddingLarge))
            PatternsDisclosure(patterns, patternsExpanded, onTogglePatterns)
        }
    }
}


