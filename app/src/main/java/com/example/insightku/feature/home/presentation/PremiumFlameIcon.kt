package com.example.insightku.feature.home.presentation

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*
import com.example.insightku.R

// ─── Flame Config ─────────────────────────────────────────────────────────────

data class FlameConfig(
    val flamePrimary: Color,
    val flameSecondary: Color,
    val flameScale: Float,
    val animationSpeed: Float,
    val particleIntensity: Int, // 0–5
    val glowAlpha: Float,
    val statusCopy: String
)

fun flameConfig(streak: Int): FlameConfig = when {
    streak >= 100 -> FlameConfig(          // Legendary
        flamePrimary      = Color(0xFFC084FC),
        flameSecondary    = Color(0xFFEDE9FE),
        flameScale        = 1.42f,
        animationSpeed    = 1.75f,
        particleIntensity = 5,
        glowAlpha         = 0.58f,
        statusCopy        = "Legendary"
    )
    streak >= 75  -> FlameConfig(          // Mythic
        flamePrimary      = Color(0xFFB76EFA),
        flameSecondary    = Color(0xFFDDD6FE),
        flameScale        = 1.34f,
        animationSpeed    = 1.60f,
        particleIntensity = 4,
        glowAlpha         = 0.50f,
        statusCopy        = "Mythic"
    )
    streak >= 50  -> FlameConfig(          // Elite
        flamePrimary      = Color(0xFFA855F7),
        flameSecondary    = Color(0xFFC4B5FD),
        flameScale        = 1.28f,
        animationSpeed    = 1.48f,
        particleIntensity = 4,
        glowAlpha         = 0.42f,
        statusCopy        = "Elite"
    )
    streak >= 30  -> FlameConfig(          // Blazing
        flamePrimary      = Color(0xFF9333EA),
        flameSecondary    = Color(0xFFA78BFA),
        flameScale        = 1.20f,
        animationSpeed    = 1.38f,
        particleIntensity = 3,
        glowAlpha         = 0.34f,
        statusCopy        = "Blazing"
    )
    streak >= 21  -> FlameConfig(          // Committed
        flamePrimary      = Color(0xFF8B5CF6),
        flameSecondary    = Color(0xFF9333EA),
        flameScale        = 1.14f,
        animationSpeed    = 1.28f,
        particleIntensity = 3,
        glowAlpha         = 0.28f,
        statusCopy        = "Committed"
    )
    streak >= 14  -> FlameConfig(          // Strong
        flamePrimary      = Color(0xFF7C3AED),
        flameSecondary    = Color(0xFF8B5CF6),
        flameScale        = 1.08f,
        animationSpeed    = 1.20f,
        particleIntensity = 2,
        glowAlpha         = 0.22f,
        statusCopy        = "Strong"
    )
    streak >= 10  -> FlameConfig(          // Focused
        flamePrimary      = Color(0xFF6D28D9),
        flameSecondary    = Color(0xFF7C3AED),
        flameScale        = 1.02f,
        animationSpeed    = 1.12f,
        particleIntensity = 2,
        glowAlpha         = 0.17f,
        statusCopy        = "Focused"
    )
    streak >= 7   -> FlameConfig(          // Growing
        flamePrimary      = Color(0xFF6D28D9),
        flameSecondary    = Color(0xFF7C3AED),
        flameScale        = 0.96f,
        animationSpeed    = 1.05f,
        particleIntensity = 1,
        glowAlpha         = 0.13f,
        statusCopy        = "Growing"
    )
    streak >= 5   -> FlameConfig(          // Ignited
        flamePrimary      = Color(0xFF5B21B6),
        flameSecondary    = Color(0xFF6D28D9),
        flameScale        = 0.90f,
        animationSpeed    = 0.98f,
        particleIntensity = 1,
        glowAlpha         = 0.09f,
        statusCopy        = "Ignited"
    )
    streak >= 3   -> FlameConfig(          // Forming
        flamePrimary      = Color(0xFF4C1D95),
        flameSecondary    = Color(0xFF5B21B6),
        flameScale        = 0.84f,
        animationSpeed    = 0.90f,
        particleIntensity = 0,
        glowAlpha         = 0.06f,
        statusCopy        = "Forming"
    )
    streak >= 1   -> FlameConfig(          // Ember
        flamePrimary      = Color(0xFF3B0764),
        flameSecondary    = Color(0xFF4C1D95),
        flameScale        = 0.76f,
        animationSpeed    = 0.80f,
        particleIntensity = 0,
        glowAlpha         = 0.03f,
        statusCopy        = "Ember"
    )
    else          -> FlameConfig(          // Dormant
        flamePrimary      = Color(0xFF6B7280),
        flameSecondary    = Color(0xFF9CA3AF),
        flameScale        = 0.68f,
        animationSpeed    = 0f,
        particleIntensity = 0,
        glowAlpha         = 0f,
        statusCopy        = "Dormant"
    )
}

// ─── Freeze palette ───────────────────────────────────────────────────────────

private val FreezeOuter  = Color(0xFF67E8F9) // icy cyan — outer frozen flame
private val FreezeInner  = Color(0xFFE0F7FA) // near-white cold highlight — inner core
private val FreezeShard1 = Color(0xFFA5F3FC) // light cyan shard
private val FreezeShard2 = Color(0xFF7DD3FC) // pale blue shard
private val FreezeShard3 = Color(0xFFBAE6FD) // crystal white-blue

// ─── Particle color helpers ───────────────────────────────────────────────────

private fun particleColors(config: FlameConfig) = Triple(
    config.flameSecondary,
    config.flamePrimary,
    config.flamePrimary.copy(
        red   = (config.flamePrimary.red   * 0.85f).coerceIn(0f, 1f),
        green = (config.flamePrimary.green * 0.85f).coerceIn(0f, 1f),
        blue  = (config.flamePrimary.blue  * 0.85f).coerceIn(0f, 1f)
    )
)

// ─── Premium Flame Icon ───────────────────────────────────────────────────────

@Composable
fun PremiumFlameIcon(
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    streak: Int = 0,
    frozen: Boolean = false,
    isScrolling: Boolean = false
) {
    val config = remember(streak) { flameConfig(streak) }

    val effectiveSize  = size * config.flameScale
    // PERF FIX: Pause all animations while scrolling to prevent jank.
    // The flame icon has 12+ simultaneous infinite animations that destroy scroll performance.
    val effectiveSpeed = when {
        !active || isScrolling -> 0f
        frozen   -> config.animationSpeed * 0.28f
        else     -> config.animationSpeed
    }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.fire)
    )

    // Frozen flame uses icy cyan palette; normal flame uses tier purple palette.
    // Two-layer split keeps internal contrast: outer body vs bright inner core.
    val outerArgb = remember(streak, frozen) {
        if (frozen) FreezeOuter.toArgb() else config.flamePrimary.toArgb()
    }
    val innerArgb = remember(streak, frozen) {
        if (frozen) {
            FreezeInner.toArgb()
        } else {
            val s = config.flameSecondary
            Color(
                red   = (0.75f + s.red   * 0.25f),
                green = (0.75f + s.green * 0.25f),
                blue  = (0.75f + s.blue  * 0.25f),
                alpha = 1f
            ).toArgb()
        }
    }
    val tierProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value    = PorterDuffColorFilter(outerArgb, PorterDuff.Mode.SRC_ATOP),
            keyPath  = arrayOf("Shape Layer 1", "**")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value    = PorterDuffColorFilter(innerArgb, PorterDuff.Mode.SRC_ATOP),
            keyPath  = arrayOf("Shape Layer 2", "**")
        )
    )

    val freezeAnim = rememberInfiniteTransition(label = "freeze_$streak")
    // Crystal core glow — slow deep breath
    val crystalPulse by freezeAnim.animateFloat(
        initialValue = 0.20f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "crystal_$streak"
    )
    // Frost shard drift — slow downward fall
    val f1 by freezeAnim.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), "f1_$streak")
    val f2 by freezeAnim.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), "f2_$streak")
    val f3 by freezeAnim.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, easing = LinearEasing), RepeatMode.Restart), "f3_$streak")
    val f4 by freezeAnim.animateFloat(0f, 1f, infiniteRepeatable(tween(3100, easing = LinearEasing), RepeatMode.Restart), "f4_$streak")
    val f5 by freezeAnim.animateFloat(0f, 1f, infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), "f5_$streak")

    val anim = rememberInfiniteTransition(label = "embers_$streak")

    // Each ember has its own independent rise timeline with different durations and delays
    // so they never move in sync. Progress goes 0→1 linearly then restarts.
    val e1 by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), "e1_$streak")
    val e2 by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(850,  easing = LinearEasing), RepeatMode.Restart), "e2_$streak")
    val e3 by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1350, easing = LinearEasing), RepeatMode.Restart), "e3_$streak")
    val e4 by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(950,  easing = LinearEasing), RepeatMode.Restart), "e4_$streak")
    val e5 by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), "e5_$streak")
    val e6 by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(700,  easing = LinearEasing), RepeatMode.Restart), "e6_$streak")
    val e7 by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), "e7_$streak")

    Box(
        modifier = modifier.size(effectiveSize),
        contentAlignment = Alignment.Center
    ) {
        // Crystal core glow — behind the flame, only in frozen state
        if (frozen) {
            Box(
                modifier = Modifier
                    .size(effectiveSize * 0.72f)
                    .clip(RoundedCornerShape(50))
                    .background(FreezeOuter.copy(alpha = crystalPulse * 0.30f))
            )
        }

        LottieAnimation(
            composition       = composition,
            iterations        = LottieConstants.IterateForever,
            isPlaying         = active,
            speed             = effectiveSpeed,
            dynamicProperties = tierProperties,
            modifier          = Modifier.fillMaxSize()
        )

        if (frozen && !isScrolling) {
            // PERF FIX: Skip frost shards while scrolling to prevent particle animation jank
            // Frost shards — fall downward slowly, drift outward, fade at bottom
            val flameH = effectiveSize.value
            FrostShard(progress = f1, spawnX = -flameH * 0.12f, spawnY = -flameH * 0.36f,
                driftX = -3f, fallRange = flameH * 0.32f,
                width = 2.dp, height = 4.dp, color = FreezeShard1)
            FrostShard(progress = f2, spawnX =  flameH * 0.16f, spawnY = -flameH * 0.28f,
                driftX =  4f, fallRange = flameH * 0.28f,
                width = 3.dp, height = 2.dp, color = FreezeShard2)
            FrostShard(progress = f3, spawnX =  flameH * 0.04f, spawnY = -flameH * 0.40f,
                driftX = -2f, fallRange = flameH * 0.36f,
                width = 2.dp, height = 3.dp, color = FreezeShard3)
            FrostShard(progress = f4, spawnX = -flameH * 0.20f, spawnY = -flameH * 0.20f,
                driftX = -5f, fallRange = flameH * 0.24f,
                width = 2.dp, height = 2.dp, color = FreezeShard1)
            FrostShard(progress = f5, spawnX =  flameH * 0.22f, spawnY = -flameH * 0.32f,
                driftX =  6f, fallRange = flameH * 0.30f,
                width = 2.dp, height = 2.dp, color = FreezeShard2)

            // Outer crystal ring — breathes slowly
            Box(
                modifier = Modifier
                    .size(effectiveSize * 1.12f)
                    .clip(RoundedCornerShape(50))
                    .background(FreezeOuter.copy(alpha = crystalPulse * 0.12f))
            )
        } else if (active && !isScrolling && config.particleIntensity >= 1) {
            // PERF FIX: Skip ember particles while scrolling to prevent animation jank
            val flameH = effectiveSize.value
            val (pColor1, pColor2, pColor3) = particleColors(config)

            Ember(progress = e1, spawnX = -flameH * 0.18f, spawnY = -flameH * 0.30f,
                driftX = -4f, riseRange = flameH * 0.28f,
                width = 3.dp, height = 2.dp, color = pColor1)
            Ember(progress = e2, spawnX =  flameH * 0.14f, spawnY = -flameH * 0.32f,
                driftX =  5f, riseRange = flameH * 0.24f,
                width = 2.dp, height = 3.dp, color = pColor2)

            if (config.particleIntensity >= 2) {
                Ember(progress = e3, spawnX =  flameH * 0.04f, spawnY = -flameH * 0.38f,
                    driftX = -2f, riseRange = flameH * 0.32f,
                    width = 2.dp, height = 2.dp, color = pColor3)
                Ember(progress = e4, spawnX = -flameH * 0.22f, spawnY = -flameH * 0.22f,
                    driftX = -6f, riseRange = flameH * 0.20f,
                    width = 2.dp, height = 1.dp, color = pColor1)
            }

            if (config.particleIntensity >= 3) {
                Ember(progress = e5, spawnX =  flameH * 0.20f, spawnY = -flameH * 0.26f,
                    driftX =  7f, riseRange = flameH * 0.36f,
                    width = 3.dp, height = 2.dp, color = pColor2)
                Ember(progress = e6, spawnX = -flameH * 0.08f, spawnY = -flameH * 0.34f,
                    driftX =  3f, riseRange = flameH * 0.30f,
                    width = 2.dp, height = 2.dp, color = pColor3)
            }

            if (config.particleIntensity >= 4) {
                Ember(progress = e7, spawnX =  flameH * 0.02f, spawnY = -flameH * 0.44f,
                    driftX = -3f, riseRange = flameH * 0.42f,
                    width = 2.dp, height = 4.dp, color = pColor1, bright = true)
                Ember(progress = e1, spawnX =  flameH * 0.26f, spawnY = -flameH * 0.18f,
                    driftX =  9f, riseRange = flameH * 0.22f,
                    width = 2.dp, height = 2.dp, color = pColor2)
                Ember(progress = e3, spawnX = -flameH * 0.24f, spawnY = -flameH * 0.20f,
                    driftX = -8f, riseRange = flameH * 0.26f,
                    width = 2.dp, height = 2.dp, color = pColor3)
            }

            if (config.particleIntensity >= 5) {
                Ember(progress = e2, spawnX = -flameH * 0.06f, spawnY = -flameH * 0.46f,
                    driftX = -5f, riseRange = flameH * 0.50f,
                    width = 2.dp, height = 5.dp, color = pColor1, bright = true)
                Ember(progress = e4, spawnX =  flameH * 0.30f, spawnY = -flameH * 0.14f,
                    driftX = 11f, riseRange = flameH * 0.28f,
                    width = 2.dp, height = 2.dp, color = pColor2)
                Ember(progress = e6, spawnX = -flameH * 0.28f, spawnY = -flameH * 0.16f,
                    driftX = -10f, riseRange = flameH * 0.30f,
                    width = 2.dp, height = 2.dp, color = pColor3)
                Ember(progress = e5, spawnX =  flameH * 0.10f, spawnY = -flameH * 0.40f,
                    driftX =  4f, riseRange = flameH * 0.46f,
                    width = 3.dp, height = 2.dp, color = pColor1)
            }
        }
    }
}

// ─── Ember composable ─────────────────────────────────────────────────────────

@Composable
private fun Ember(
    progress: Float,
    spawnX: Float,
    spawnY: Float,
    driftX: Float,
    riseRange: Float,
    width: Dp,
    height: Dp,
    color: Color,
    bright: Boolean = false
) {
    val alpha = when {
        progress < 0.20f -> progress / 0.20f
        progress < 0.60f -> 1f
        else             -> 1f - (progress - 0.60f) / 0.40f
    }.coerceIn(0f, 1f) * if (bright) 1.0f else 0.85f

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .offset(x = (spawnX + driftX * progress).dp, y = (spawnY - riseRange * progress).dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = alpha))
    )
}

// ─── FrostShard composable ────────────────────────────────────────────────────
// Like Ember but falls downward and drifts outward — cold crystal fragments
// that descend from the frozen flame instead of rising from heat.

@Composable
private fun FrostShard(
    progress: Float,
    spawnX: Float,
    spawnY: Float,
    driftX: Float,
    fallRange: Float,
    width: Dp,
    height: Dp,
    color: Color
) {
    // Fade in quickly, hold, fade out in bottom third
    val alpha = when {
        progress < 0.15f -> progress / 0.15f
        progress < 0.65f -> 1f
        else             -> 1f - (progress - 0.65f) / 0.35f
    }.coerceIn(0f, 0.90f)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .offset(
                x = (spawnX + driftX * progress).dp,
                y = (spawnY + fallRange * progress).dp  // positive = downward
            )
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = alpha))
    )
}

