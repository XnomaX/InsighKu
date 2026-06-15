package com.example.insightku.feature.analytics.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalComfortMode
import com.example.insightku.core.ui.theme.LocalHideAmounts
import com.example.insightku.core.ui.theme.LocalCurrencyCode
import com.example.insightku.core.ui.theme.LocalInsightTone
import com.example.insightku.core.ui.theme.LocalResponsiveDimens
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.feature.analytics.presentation.AnalyticsViewModel

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AnalyticsPalette.background)
    ) {
        // TEMPORARY debug banner — shows which synthetic scenario is active.
        if (uiState.debugLabel != null) {
            DebugBanner(
                label = uiState.debugLabel!!,
                onCycle = { viewModel.onEvent(AnalyticsEvent.CycleDebugScenario) },
                onExit = { viewModel.onEvent(AnalyticsEvent.ExitDebug) }
            )
        }
        when {
            uiState.isLoading -> AnalyticsLoading(Modifier.fillMaxSize())
            uiState.error != null -> AnalyticsErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.onEvent(AnalyticsEvent.RefreshData) },
                modifier = Modifier.fillMaxSize()
            )
            uiState.isEmpty -> AnalyticsEmpty(Modifier.fillMaxSize())
            else -> AnalyticsContent(uiState = uiState, onEvent = viewModel::onEvent)
        }
    }
}

/**
 * Stateless behavioral content. Sections are ordered by emotional payoff first, detail later:
 * personality → rhythm heatmap → consistency → where it goes → biggest moves → spotlight → mood.
 * Each section fades+rises in with a staggered delay as the screen settles.
 */
@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onEvent: (AnalyticsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalResponsiveDimens.current
    val currencyCode = LocalCurrencyCode.current
    val hideAmounts = LocalHideAmounts.current
    // Respect global privacy: mask every Analytics figure when hide-amounts is on.
    val formatAmount: (Double) -> String = {
        if (hideAmounts) "����"
        else CurrencyUtils.formatAmount(it, currencyCode)
    }
    // A covered section is "revealed" once the user uncovers it; in debug mode everything is shown
    // so all states are inspectable.
    val isRevealed: (String) -> Boolean = { id -> uiState.debugLabel != null || id in uiState.uncoveredSections }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AnalyticsPalette.background),
        contentPadding = PaddingValues(
            start = dimens.screenHorizontalPadding,
            end = dimens.screenHorizontalPadding,
            top = Dimens.PaddingExtraLarge,
            bottom = Dimens.ContentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)
    ) {
        // Header — long-press reveals the temporary debug mode.
        item {
            Column(
                Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onEvent(AnalyticsEvent.CycleDebugScenario) })
                }
            ) {
                Text(
                    "Here's how your month felt",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnalyticsPalette.textPrimary
                )
                Spacer(Modifier.height(Dimens.PaddingSmall))
                val toneSubtitle = when (LocalInsightTone.current) {
                    com.example.insightku.core.ui.theme.InsightTone.GENTLE -> "A soft, no-pressure look at your habits 💜"
                    com.example.insightku.core.ui.theme.InsightTone.DIRECT -> "Your habits, straight up"
                    com.example.insightku.core.ui.theme.InsightTone.WARM -> "A calm look at your habits 💜"
                }
                Text(
                    toneSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AnalyticsPalette.textMuted
                )
                // TEMPORARY — visible button to enter the preview/debug scenarios on a real device.
                // Remove this (and the debug system) before final polish.
                Spacer(Modifier.height(Dimens.PaddingMedium))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AnalyticsPalette.Purple.copy(alpha = 0.12f))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onEvent(AnalyticsEvent.CycleDebugScenario) })
                        }
                        .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingSmall)
                ) {
                    Text(
                        "🐞 Preview Two Yous",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AnalyticsPalette.Purple
                    )
                }
            }
        }

        var order = 0
        staggered(order++) {
            TwoYousCard(
                twoYous = uiState.twoYous,
                personality = uiState.personality,
                patterns = uiState.patterns,
                patternsExpanded = uiState.patternsExpanded,
                onTogglePatterns = { onEvent(AnalyticsEvent.TogglePatterns) }
            )
        }

        uiState.noticing?.let { n -> staggered(order++) { DidYouNoticeCard(n) } }

        if (uiState.heatmapCells.isNotEmpty()) {
            staggered(order++) {
                HabitHeatmapSection(
                    cells = uiState.heatmapCells,
                    rhythm = uiState.rhythm,
                    expandedDay = uiState.expandedDay,
                    onDayTap = { onEvent(AnalyticsEvent.ExpandDay(it)) },
                    formatAmount = formatAmount
                )
            }
        }

        staggered(order++) {
            Uncoverable(
                revealed = isRevealed("consistency"),
                emoji = "🌱",
                invitation = "Are you more consistent than last month?",
                onReveal = { onEvent(AnalyticsEvent.Uncover("consistency")) }
            ) { ConsistencySection(uiState.streak) }
        }

        if (uiState.categoryBubbles.isNotEmpty()) {
            staggered(order++) {
                CategoryBubblesSection(
                    bubbles = uiState.categoryBubbles,
                    expanded = uiState.expandedBubble,
                    onExpand = { onEvent(AnalyticsEvent.ExpandBubble(it)) },
                    formatAmount = formatAmount
                )
            }
        }

        if (uiState.bigDecisions.isNotEmpty()) {
            staggered(order++) {
                Uncoverable(
                    revealed = isRevealed("bigDecisions"),
                    emoji = "💸",
                    invitation = "Ready to see the few choices that shaped your month?",
                    onReveal = { onEvent(AnalyticsEvent.Uncover("bigDecisions")) }
                ) { BigDecisionsSection(uiState.bigDecisions, formatAmount = formatAmount) }
            }
        }

        staggered(order++) {
            Uncoverable(
                revealed = isRevealed("spotlight"),
                emoji = "🔦",
                invitation = "Who got the most of your money this month?",
                onReveal = { onEvent(AnalyticsEvent.Uncover("spotlight")) }
            ) { SpotlightSection(uiState.spotlight, formatAmount = formatAmount) }
        }

        uiState.mood?.let { mood ->
            staggered(order++) {
                Uncoverable(
                    revealed = isRevealed("mood"),
                    emoji = "🌤️",
                    invitation = "What's your spending mood been lately?",
                    onReveal = { onEvent(AnalyticsEvent.Uncover("mood")) }
                ) { SpendingMoodSection(mood) }
            }
        }
    }
}

/** LazyListScope helper: wraps a section item in a staggered fade-in + slight rise on first compose. */
private fun androidx.compose.foundation.lazy.LazyListScope.staggered(
    index: Int,
    content: @Composable () -> Unit
) {
    item {
        // Comfort mode softens motion app-wide: gentler fade, no rise, no stagger delay.
        val comfort = LocalComfortMode.current
        var visible by remember { mutableStateOf(false) }
        val duration = if (comfort) 220 else 320
        val delay = if (comfort) 0 else index * 60
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(durationMillis = duration, delayMillis = delay),
            label = "stagger_alpha_$index"
        )
        val translate by animateFloatAsState(
            targetValue = if (visible) 0f else if (comfort) 0f else 24f,
            animationSpec = tween(durationMillis = duration, delayMillis = delay),
            label = "stagger_y_$index"
        )
        androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
        Box(
            Modifier
                .alpha(alpha)
                .graphicsLayer { translationY = translate }
        ) { content() }
    }
}

@Composable
private fun DebugBanner(label: String, onCycle: () -> Unit, onExit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AnalyticsPalette.Purple)
            .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("DEBUG · $label", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
            DebugChip("Next", onCycle)
            DebugChip("Exit", onExit)
        }
    }
}

@Composable
private fun DebugChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = Dimens.PaddingMedium, vertical = 4.dp)
    ) {
        Text(label, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium)
    }
}




