package com.example.insightku.ui.components.analytics

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.ui.theme.Dimens
import com.example.insightku.ui.theme.LocalCurrencyCode
import com.example.insightku.ui.theme.LocalResponsiveDimens
import com.example.insightku.utils.CurrencyUtils
import com.example.insightku.viewmodel.AnalyticsViewModel

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
    val formatAmount: (Double) -> String = { CurrencyUtils.formatAmount(it, currencyCode) }

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
                Text(
                    "A calm look at your habits 💜",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AnalyticsPalette.textMuted
                )
            }
        }

        var order = 0
        staggered(order++) {
            SpendingPersonalityCard(
                personality = uiState.personality,
                patterns = uiState.patterns,
                expanded = uiState.patternsExpanded,
                onToggle = { onEvent(AnalyticsEvent.TogglePatterns) }
            )
        }

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

        staggered(order++) { ConsistencySection(uiState.streak) }

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
            staggered(order++) { BigDecisionsSection(uiState.bigDecisions, formatAmount = formatAmount) }
        }

        staggered(order++) { SpotlightSection(uiState.spotlight, formatAmount = formatAmount) }

        uiState.mood?.let { mood -> staggered(order++) { SpendingMoodSection(mood) } }
    }
}

/** LazyListScope helper: wraps a section item in a staggered fade-in + slight rise on first compose. */
private fun androidx.compose.foundation.lazy.LazyListScope.staggered(
    index: Int,
    content: @Composable () -> Unit
) {
    item {
        var visible by remember { mutableStateOf(false) }
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(durationMillis = 320, delayMillis = index * 60),
            label = "stagger_alpha_$index"
        )
        val translate by animateFloatAsState(
            targetValue = if (visible) 0f else 24f,
            animationSpec = tween(durationMillis = 320, delayMillis = index * 60),
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
