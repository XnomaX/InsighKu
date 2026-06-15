package com.example.insightku.feature.analytics.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.feature.analytics.domain.BehavioralPattern
import com.example.insightku.feature.analytics.domain.BigDecision
import com.example.insightku.feature.analytics.domain.ConsistencyDirection
import com.example.insightku.feature.analytics.domain.DayComparison
import com.example.insightku.feature.analytics.domain.HeatmapCell
import com.example.insightku.feature.analytics.domain.MoodData
import com.example.insightku.feature.analytics.domain.Noticing
import com.example.insightku.feature.analytics.domain.RhythmCaption
import com.example.insightku.feature.analytics.domain.SpotlightData
import com.example.insightku.feature.analytics.domain.StreakData
import com.example.insightku.core.ui.theme.Dimens
import kotlin.math.roundToInt

// ── 1. Spending Personality + progressive deeper patterns ─────────────────────
// (Replaced by TwoYousCard — the centerpiece. The old card's deeper-patterns disclosure was
//  absorbed into TwoYousCard; its sparse-data greeting lives in TwoYousCard's fallback.)

// ── "Did you notice?" — a gentle, rotating teaching moment from the user's own data ──────────

@Composable
fun DidYouNoticeCard(noticing: Noticing, modifier: Modifier = Modifier) {
    // Soft tinted card so it reads as a warm aside, distinct from the white reflection cards.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = AnalyticsPalette.tint(AnalyticsPalette.Purple),
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(Dimens.CardInnerPaddingLarge),
            verticalAlignment = Alignment.Top
        ) {
            Text(noticing.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(Dimens.PaddingMedium))
            Column {
                Text(
                    "Did you notice?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnalyticsPalette.Purple
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    noticing.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AnalyticsPalette.textPrimary
                )
            }
        }
    }
}

// ── 2. Habit Heatmap + rhythm + rich day tap-reveal ────────────────────────────

@Composable
fun HabitHeatmapSection(
    cells: List<HeatmapCell>,
    rhythm: RhythmCaption,
    expandedDay: Int?,
    onDayTap: (Int?) -> Unit,
    formatAmount: (Double) -> String,
    modifier: Modifier = Modifier
) {
    if (cells.isEmpty()) return
    val weeks = (cells.maxOf { it.weekIndex } + 1).coerceAtLeast(1)
    val maxAmount = cells.maxOf { it.amount }.coerceAtLeast(1.0)
    val byKey = cells.associateBy { it.dayOfWeek to it.weekIndex }
    val selected = cells.firstOrNull { it.dayOfMonth == expandedDay }
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    AnalyticsCard(modifier) {
        SectionHeader("Your spending rhythm", "Tap any day to see what happened")
        Spacer(Modifier.height(Dimens.PaddingLarge))

        // Day-of-week header row for orientation
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dayLabels.forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = AnalyticsPalette.textMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        for (w in 0 until weeks) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                for (dow in 0..6) {
                    val cell = byKey[dow to w]
                    val amount = cell?.amount ?: 0.0
                    val intensity = (amount / maxAmount).toFloat().coerceIn(0f, 1f)
                    val isSelected = cell != null && cell.dayOfMonth == expandedDay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        AnalyticsPalette.Purple.copy(alpha = if (cell == null) 0.04f else 0.12f + 0.82f * intensity),
                                        AnalyticsPalette.Purple.copy(alpha = if (cell == null) 0.03f else 0.08f + 0.78f * intensity)
                                    )
                                )
                            )
                            .then(if (isSelected) Modifier.border(2.dp, AnalyticsPalette.Purple, RoundedCornerShape(6.dp)) else Modifier)
                            .then(if (cell != null) Modifier.clickable { onDayTap(cell.dayOfMonth) } else Modifier)
                    )
                }
            }
        }

        // Tap-reveal contextual day panel (richer than one sentence — governance amended)
        AnimatedVisibility(
            visible = selected != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selected?.let { DayDetailPanel(it, formatAmount) }
        }

        if (rhythm.text.isNotBlank()) {
            Spacer(Modifier.height(Dimens.PaddingLarge))
            Text(rhythm.text, style = MaterialTheme.typography.bodyMedium, color = AnalyticsPalette.textPrimary)
        }
    }
}

@Composable
private fun DayDetailPanel(cell: HeatmapCell, formatAmount: (Double) -> String) {
    Column(
        Modifier
            .padding(top = Dimens.PaddingLarge)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
            .background(AnalyticsPalette.cardElevated)
            .padding(Dimens.CardInnerPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(cell.dateLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AnalyticsPalette.textPrimary)
        Text(
            "${cell.transactionCount} ${if (cell.transactionCount == 1) "transaction" else "transactions"} · ${formatAmount(cell.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = AnalyticsPalette.textPrimary
        )
        cell.topCategory?.let {
            Text("Mostly $it", style = MaterialTheme.typography.bodySmall, color = AnalyticsPalette.textMuted)
        }
        cell.mostActiveTime?.let {
            Text("Most active in the ${it.lowercase()}", style = MaterialTheme.typography.bodySmall, color = AnalyticsPalette.textMuted)
        }
        val comparisonText = when (cell.comparison) {
            DayComparison.ABOVE -> "A heavier day than usual for you"
            DayComparison.BELOW -> "A lighter day than usual"
            DayComparison.TYPICAL -> "About a typical day for you"
            DayComparison.NONE -> null
        }
        comparisonText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = AnalyticsPalette.Purple)
        }
    }
}

// ── 3. Consistency (this-month presence) ──────────────────────────────────────

@Composable
fun ConsistencySection(streak: StreakData, modifier: Modifier = Modifier) {
    val total = streak.daysElapsedThisMonth.coerceAtLeast(1)
    val target = streak.daysShownUp.toFloat() / total
    val animated by animateFloatAsState(target.coerceIn(0f, 1f), spring(), label = "ringFill")

    // Direction vs the same window last month — frames presence as gentle growth, never pressure.
    val (growthEmoji, growthText) = when (streak.direction) {
        ConsistencyDirection.UP ->
            "🌱" to "You've been more present than this point last month — nice momentum."
        ConsistencyDirection.DOWN ->
            "🌙" to "A little quieter than last month so far — there's plenty of month left."
        ConsistencyDirection.SAME ->
            "🪴" to "You're keeping the same steady presence as last month."
        ConsistencyDirection.STEADY ->
            "🪴" to "You're keeping the same steady presence as last month."
        ConsistencyDirection.NEW ->
            "✨" to "This is your first month here — your rhythm will take shape from here."
    }

    AnalyticsCard(modifier) {
        SectionHeader("Showing up", "How present you've been this month")
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                val ringColor = AnalyticsPalette.Purple // hoist out of Canvas DrawScope (non-composable)
                Canvas(Modifier.size(96.dp)) {
                    val stroke = 12.dp.toPx()
                    drawArc(ringColor.copy(alpha = 0.12f), 0f, 360f, false, style = Stroke(stroke))
                    drawArc(ringColor, -90f, 360f * animated, false, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text(
                    "${streak.daysShownUp}/$total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnalyticsPalette.textPrimary
                )
            }
            Spacer(Modifier.width(Dimens.PaddingLarge))
            Column {
                Text(
                    "You've shown up on ${streak.daysShownUp} ${if (streak.daysShownUp == 1) "day" else "days"} this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AnalyticsPalette.textPrimary
                )
                Spacer(Modifier.height(Dimens.PaddingSmall))
                Text(
                    "$growthEmoji $growthText",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnalyticsPalette.textMuted
                )
            }
        }
    }
}

// ── 4. Where Does It Go? (category bubbles) ────────────────────────────────────

@Composable
fun CategoryBubblesSection(
    bubbles: List<CategoryBubble>,
    expanded: String?,
    onExpand: (String?) -> Unit,
    formatAmount: (Double) -> String,
    modifier: Modifier = Modifier
) {
    if (bubbles.isEmpty()) return
    AnalyticsCard(modifier) {
        SectionHeader("Where it goes", "Tap a bubble to peek inside")
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
            bubbles.forEach { bubble ->
                CategoryBubbleRow(bubble, expanded == bubble.name, { onExpand(bubble.name) }, formatAmount)
            }
        }
    }
}

@Composable
private fun CategoryBubbleRow(
    bubble: CategoryBubble,
    isExpanded: Boolean,
    onClick: () -> Unit,
    formatAmount: (Double) -> String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.PaddingSmall)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val sizeDp = (28 + (28 * bubble.proportion)).dp
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(sizeDp).clip(CircleShape).background(bubble.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(bubble.icon, null, tint = bubble.color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(Dimens.PaddingMedium))
            Text(bubble.name, style = MaterialTheme.typography.bodyLarge, color = AnalyticsPalette.textPrimary, modifier = Modifier.weight(1f))
            Text(formatAmount(bubble.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AnalyticsPalette.textPrimary)
        }
        AnimatedVisibility(isExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(
                Modifier.fillMaxWidth().padding(start = 56.dp + Dimens.PaddingMedium, top = Dimens.PaddingSmall),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                bubble.topTransactions.forEach { tx ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tx.title.ifBlank { bubble.name }, style = MaterialTheme.typography.bodySmall, color = AnalyticsPalette.textMuted)
                        Text(formatAmount(tx.amount), style = MaterialTheme.typography.bodySmall, color = AnalyticsPalette.textMuted)
                    }
                }
            }
        }
    }
}

// ── 5. Big Decisions ───────────────────────────────────────────────────────────

@Composable
fun BigDecisionsSection(
    decisions: List<BigDecision>,
    formatAmount: (Double) -> String,
    modifier: Modifier = Modifier
) {
    if (decisions.isEmpty()) return
    AnalyticsCard(modifier) {
        SectionHeader("Your biggest moves", "The few choices that shaped the month")
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
            decisions.forEach { d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            d.transaction.title.ifBlank { d.transaction.category.ifBlank { "Expense" } },
                            style = MaterialTheme.typography.bodyLarge,
                            color = AnalyticsPalette.textPrimary
                        )
                        Text(formatAmount(d.transaction.amount), style = MaterialTheme.typography.bodySmall, color = AnalyticsPalette.textMuted)
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(Dimens.ChipRadius))
                            .background(AnalyticsPalette.tint(AnalyticsPalette.ExpenseRed))
                            .padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall)
                    ) {
                        Text(
                            "${d.percentOfMonthlySpend.roundToInt()}% of your month",
                            style = MaterialTheme.typography.labelMedium,
                            color = AnalyticsPalette.ExpenseRed
                        )
                    }
                }
            }
        }
        // Layer 3 — a gentle reflective close, derived from how concentrated the big moves are.
        val topShare = decisions.maxOf { it.percentOfMonthlySpend }
        val combinedShare = decisions.sumOf { it.percentOfMonthlySpend }
        val reflection = when {
            topShare >= 40.0 -> "One choice carried most of your month — the big ones really move the needle for you."
            combinedShare >= 60.0 -> "Just a few decisions shaped most of your month — the small stuff matters less than it feels."
            else -> "Your spending's spread across many choices rather than a few big ones."
        }
        ReflectionLine(reflection)
    }
}

/** Layer-3 reflective close — a calm "what it says about you" line beneath a revealed section. */
@Composable
private fun ReflectionLine(text: String) {
    Spacer(Modifier.height(Dimens.PaddingLarge))
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(AnalyticsPalette.Purple)
        )
        Spacer(Modifier.width(Dimens.PaddingMedium))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = AnalyticsPalette.Purple
        )
    }
}

// ── 6. Spotlight ─────────────────────────────────────────────────────────────────

@Composable
fun SpotlightSection(
    spotlight: SpotlightData,
    formatAmount: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val hasExpense = spotlight.topExpenseName != null
    val hasIncome = spotlight.topIncomeName != null
    if (!hasExpense && !hasIncome) return
    AnalyticsCard(modifier) {
        SectionHeader("In the spotlight", "Who got the most of your money")
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
            if (hasExpense) SpotlightTile("Most spent on", spotlight.topExpenseName!!, formatAmount(spotlight.topExpenseAmount), spotlight.expenseDeltaPct, AnalyticsPalette.ExpenseRed, Modifier.weight(1f))
            if (hasIncome) SpotlightTile("Most earned from", spotlight.topIncomeName!!, formatAmount(spotlight.topIncomeAmount ?: 0.0), spotlight.incomeDeltaPct, AnalyticsPalette.IncomeGreen, Modifier.weight(1f))
        }
        // Layer 3 — reflective close, derived from how this month's top expense moved vs last.
        val d = spotlight.expenseDeltaPct
        val name = spotlight.topExpenseName
        val reflection = when {
            name != null && d != null && d >= 25.0 -> "$name pulled noticeably more of your money than last month."
            name != null && d != null && d <= -25.0 -> "$name eased off a lot compared to last month."
            name != null && d != null -> "$name held about steady with last month."
            name != null -> "$name led your spending — there's no last-month read to compare it to yet."
            else -> "Your earning had a clear lead this month."
        }
        ReflectionLine(reflection)
    }
}

@Composable
private fun SpotlightTile(
    label: String, name: String, amount: String, delta: Double?, accent: Color, modifier: Modifier = Modifier
) {
    Column(
        modifier.clip(RoundedCornerShape(Dimens.CornerRadiusMedium)).background(AnalyticsPalette.tint(accent)).padding(Dimens.CardInnerPadding)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AnalyticsPalette.textMuted)
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AnalyticsPalette.textPrimary)
        Text(amount, style = MaterialTheme.typography.bodyMedium, color = accent)
        if (delta != null) {
            val sign = if (delta >= 0) "+" else ""
            Text("$sign${delta.roundToInt()}% vs last month", style = MaterialTheme.typography.labelSmall, color = AnalyticsPalette.textMuted)
        }
    }
}

// ── 7. Spending Mood (renamed from Climate) ─────────────────────────────────────

@Composable
fun SpendingMoodSection(mood: MoodData, modifier: Modifier = Modifier) {
    AnalyticsCard(modifier) {
        SectionHeader("Spending mood", "How you've been spending lately")
        Spacer(Modifier.height(Dimens.PaddingMedium))
        Text(mood.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AnalyticsPalette.Purple)
        Spacer(Modifier.height(Dimens.PaddingLarge))
        val points = mood.points
        if (points.count { it.amount > 0.0 } >= 2) {
            val maxAmount = points.maxOf { it.amount }.coerceAtLeast(1.0)
            val lineColor = AnalyticsPalette.Purple // hoist out of Canvas DrawScope (non-composable)
            Canvas(Modifier.fillMaxWidth().height(64.dp)) {
                val n = points.size
                if (n >= 2) {
                    val stepX = size.width / (n - 1)
                    val path = Path()
                    points.forEachIndexed { i, p ->
                        val x = stepX * i
                        val y = size.height - (p.amount / maxAmount).toFloat() * size.height
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Spacer(Modifier.height(Dimens.PaddingMedium))
        }
        Text(mood.summaryLine, style = MaterialTheme.typography.bodyMedium, color = AnalyticsPalette.textPrimary)
    }
}


