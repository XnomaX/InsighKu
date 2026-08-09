package com.example.insightku.feature.analytics.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.i18n.AnalyticsStrings
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.utils.CategoryUtils
import com.example.insightku.feature.analytics.domain.AnalyticsInsights
import com.example.insightku.feature.analytics.domain.AnalyticsPeriodType
import com.example.insightku.feature.analytics.domain.DaySummary
import com.example.insightku.feature.analytics.domain.SpendingMood
import com.example.insightku.feature.analytics.domain.TrendPoint
import com.example.insightku.feature.analytics.presentation.components.AnalyticsEmptyState
import com.example.insightku.feature.analytics.presentation.components.AnalyticsSectionHeader
import com.example.insightku.feature.analytics.presentation.components.AnalyticsSkeleton
import com.example.insightku.feature.analytics.presentation.components.CategoryBar
import com.example.insightku.feature.analytics.presentation.components.HeroInsightCard
import com.example.insightku.feature.analytics.presentation.components.InsightChip
import com.example.insightku.feature.analytics.presentation.components.NarrativeCard

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Dimens.ScreenHorizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Period Selector
            PeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = { period ->
                    viewModel.onEvent(AnalyticsEvent.SelectPeriod(period))
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            AnimatedContent(
                targetState = uiState.isLoading to uiState.selectedPeriod,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(200))
                },
                label = "analytics_content"
            ) { (isLoading, period) ->
                when {
                    isLoading -> AnalyticsSkeleton()
                    uiState.error != null -> AnalyticsEmptyState(
                        emoji = "⚠️",
                        title = stringResource(R.string.analytics_error_title),
                        subtitle = uiState.error ?: stringResource(R.string.analytics_unknown_error)
                    )
                    uiState.insights == null || uiState.insights?.heroInsight?.headline?.contains(AnalyticsStrings.heroWeekStartLogging()) == true -> {
                        AnalyticsEmptyState(
                            emoji = "📊",
                            title = stringResource(R.string.analytics_empty_title),
                            subtitle = stringResource(R.string.analytics_empty_subtitle)
                        )
                    }
                    else -> {
                        when (period) {
                            AnalyticsPeriodType.WEEKLY -> WeeklyContent(insights = uiState.insights!!)
                            AnalyticsPeriodType.MONTHLY -> MonthlyContent(insights = uiState.insights!!)
                            AnalyticsPeriodType.ANNUAL -> AnnualContent(insights = uiState.insights!!)
                        }
                    }
                }
            }
        }
    }
}

// ─── Weekly Content ───────────────────────────────────────────────────────────

@Composable
private fun WeeklyContent(insights: AnalyticsInsights) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Hero Insight
        item {
            HeroInsightCard(hero = insights.heroInsight)
        }

        // Week Comparison
        insights.weekComparison?.let { wc ->
            item {
                NarrativeCard(
                    emoji = if (wc.deltaPercent < 0) "📉" else "📈",
                    title = stringResource(R.string.analytics_week_vs_last),
                    body = AnalyticsStrings.weekComparisonBody(
                        deltaPercent = wc.deltaPercent,
                        thisWeekTotal = NumberFormatter.formatCurrency(wc.thisWeekTotal),
                        lastWeekTotal = NumberFormatter.formatCurrency(wc.lastWeekTotal),
                        firstWeek = wc.lastWeekTotal <= 0,
                        thisWeekAmount = NumberFormatter.formatCurrency(wc.thisWeekTotal)
                    )
                )
            }
        }

        // Daily Spending Bar Chart
        if (insights.dailySummaries.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = stringResource(R.string.analytics_week_this_spending))
            }
            item {
                val dailyChartDesc = stringResource(R.string.analytics_chart_daily_accessibility)
                DailyBarChart(summaries = insights.dailySummaries, modifier = Modifier.semantics { contentDescription = dailyChartDesc })
            }
        }

        // Weekly Habit
        insights.weeklyHabit?.let { habit ->
            item {
                NarrativeCard(
                    emoji = "🎯",
                    title = stringResource(R.string.analytics_weekly_rhythm),
                    body = AnalyticsStrings.weeklyHabitBody(
                        loudestDay = habit.loudestDay,
                        loudestAmount = NumberFormatter.formatCurrency(habit.loudestAmount),
                        quietestDay = habit.quietestDay,
                        quietestAmount = NumberFormatter.formatCurrency(habit.quietestAmount),
                        avgDailySpend = NumberFormatter.formatCurrency(habit.avgDailySpend)
                    )
                )
            }
        }

        // Category Breakdown
        if (insights.categorySlices.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = stringResource(R.string.analytics_top_categories))
            }
            items(insights.categorySlices, key = { it.name }) { slice ->
                CategoryBar(
                    name = slice.name.ifBlank { stringResource(R.string.analytics_uncategorized) },
                    percentage = slice.proportion,
                    amount = "${(slice.proportion * 100).toInt()}%",
                    color = CategoryUtils.getColorForCategoryName(slice.name)
                )
            }
        }

        // Patterns
        if (insights.patterns.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = stringResource(R.string.analytics_what_we_noticed))
            }
            items(insights.patterns) { pattern ->
                InsightChip(emoji = pattern.emoji, text = pattern.description)
            }
        }

        // Big Decisions
        if (insights.bigDecisions.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = stringResource(R.string.analytics_biggest_transactions))
            }
            items(insights.bigDecisions, key = { it.transaction.id }) { decision ->
                NarrativeCard(
                    emoji = "💰",
                    title = decision.transaction.title.ifBlank { decision.transaction.category },
                    body = stringResource(R.string.analytics_pct_of_total, NumberFormatter.formatCurrency(decision.transaction.amount), decision.percentOfMonthlySpend.toInt())
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Monthly Content ──────────────────────────────────────────────────────────

@Composable
private fun MonthlyContent(insights: AnalyticsInsights) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Hero Insight
        item {
            HeroInsightCard(hero = insights.heroInsight)
        }

        // Financial Health
        insights.monthlyHealth?.let { health ->
            // Cashflow Balance
            item {
                NarrativeCard(
                    emoji = if (health.balance.isPositive) "✅" else "⚠️",
                    title = stringResource(R.string.analytics_income_vs_expenses),
                    body = AnalyticsStrings.cashflowBody(
                        earned = NumberFormatter.formatCurrency(health.balance.totalIncome),
                        spent = NumberFormatter.formatCurrency(health.balance.totalExpenses),
                        isPositive = health.balance.isPositive,
                        savingsAmount = NumberFormatter.formatCurrency(kotlin.math.abs(health.balance.savingsAmount)),
                        savingsRate = (health.balance.savingsRate * 100).toInt()
                    )
                )
            }

            // Health Score
            item {
                HealthScoreCard(score = health.healthScore)
            }

            // Category Ranking
            if (health.topCategories.isNotEmpty()) {
                item {
                    AnalyticsSectionHeader(title = stringResource(R.string.analytics_where_money_went))
                }
                items(health.topCategories, key = { it.name }) { cat ->
                    CategoryBar(
                        name = cat.name.ifBlank { stringResource(R.string.analytics_uncategorized) },
                        percentage = cat.percentage.toFloat(),
                        amount = NumberFormatter.formatCurrency(cat.amount),
                        color = CategoryUtils.getColorForCategoryName(cat.name)
                    )
                }
            }

            // Trend
            if (health.trend.isNotEmpty()) {
                item {
                    AnalyticsSectionHeader(title = stringResource(R.string.analytics_6month_trend))
                }
                item {
                    val trendChartDesc = stringResource(R.string.analytics_chart_trend_accessibility)
                    MiniTrendChart(points = health.trend, modifier = Modifier.semantics { contentDescription = trendChartDesc })
                }
            }

            // Recurring
            if (health.recurringTotal > 0) {
                item {
                    NarrativeCard(
                        emoji = "🔄",
                    title = stringResource(R.string.analytics_recurring_payments),
                    body = stringResource(R.string.analytics_recurring_body, NumberFormatter.formatCurrency(health.recurringTotal))
                    )
                }
            }

            // Trend Narrative
            if (health.trend.size >= 2) {
                val currentMonthExpenses = health.trend.last().expense
                val prevMonthExpenses = health.trend[health.trend.size - 2].expense
                if (prevMonthExpenses > 0) {
                    val delta = ((currentMonthExpenses - prevMonthExpenses) / prevMonthExpenses * 100).toInt()
                    item {
                        val trendText = when {
                            delta < -5 -> stringResource(R.string.analytics_trend_decreased, kotlin.math.abs(delta))
                            delta > 5 -> stringResource(R.string.analytics_trend_increased, delta)
                            else -> stringResource(R.string.analytics_trend_stable)
                        }
                        NarrativeCard(
                            emoji = when {
                                delta < -5 -> "📉"
                                delta > 5 -> "📈"
                                else -> "➡️"
                            },
                            title = stringResource(R.string.analytics_month_over_month),
                            body = trendText
                        )
                    }
                }
            }
        }

        // Mood
        insights.mood?.let { mood ->
            item {
                NarrativeCard(
                    emoji = when (mood.mood) {
                        SpendingMood.CALM -> "🌤️"
                        SpendingMood.STEADY -> "🌊"
                        SpendingMood.RESTLESS -> "🌧️"
                    },
                    title = stringResource(R.string.analytics_spending_mood),
                    body = mood.summaryLine.ifBlank { mood.headline }
                )
            }
        }

        // Patterns
        if (insights.patterns.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = stringResource(R.string.analytics_patterns_found))
            }
            items(insights.patterns) { pattern ->
                InsightChip(emoji = pattern.emoji, text = pattern.description)
            }
        }

        // Big Decisions
        if (insights.bigDecisions.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = stringResource(R.string.analytics_notable_transactions))
            }
            items(insights.bigDecisions, key = { it.transaction.id }) { decision ->
                NarrativeCard(
                    emoji = "💡",
                    title = decision.transaction.title.ifBlank { decision.transaction.category },
                    body = stringResource(R.string.analytics_pct_of_month, NumberFormatter.formatCurrency(decision.transaction.amount), decision.percentOfMonthlySpend.toInt())
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Annual Content ───────────────────────────────────────────────────────────

@Composable
private fun AnnualContent(insights: AnalyticsInsights) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Hero Insight
        item {
            HeroInsightCard(hero = insights.heroInsight)
        }

        // Annual Growth
        insights.annualGrowth?.let { growth ->
            // Year Summary
            item {
                NarrativeCard(
                    emoji = "📊",
                    title = stringResource(R.string.analytics_your_financial_year),
                    body = stringResource(R.string.analytics_year_totals, NumberFormatter.formatCurrency(growth.totalIncome), NumberFormatter.formatCurrency(growth.totalExpenses), NumberFormatter.formatCurrency(growth.avgMonthlyExpense))
                )
            }

            // Savings Rate
            item {
                NarrativeCard(
                    emoji = if (growth.savingsRate > 0.15) "🎉" else "💡",
                    title = stringResource(R.string.analytics_savings_rate),
                    body = if (growth.savingsRate > 0) {
                        stringResource(R.string.analytics_saved_pct_income, (growth.savingsRate * 100).toInt(), NumberFormatter.formatCurrency(growth.totalSaved))
                    } else {
                        stringResource(R.string.analytics_no_savings_yet)
                    }
                )
            }

            // Monthly Trend
            if (growth.monthlyPoints.isNotEmpty()) {
                item {
                    AnalyticsSectionHeader(title = stringResource(R.string.analytics_monthly_journey))
                }
                item {
                    val annualChartDesc = stringResource(R.string.analytics_chart_annual_accessibility)
                    AnnualBarChart(points = growth.monthlyPoints, modifier = Modifier.semantics { contentDescription = annualChartDesc })
                }
            }

            // Best / Worst Month
            growth.bestMonth?.let { best ->
                if (best.savings > 0) {
                    item {
                        NarrativeCard(
                            emoji = "⭐",
                            title = stringResource(R.string.analytics_best_month, best.monthLabel),
                            body = stringResource(R.string.analytics_best_month_body, NumberFormatter.formatCurrency(best.savings), best.monthLabel)
                        )
                    }
                }
            }
            growth.worstMonth?.let { worst ->
                item {                        NarrativeCard(
                            emoji = "💪",
                            title = stringResource(R.string.analytics_toughest_month, worst.monthLabel),
                            body = stringResource(R.string.analytics_toughest_month_body, NumberFormatter.formatCurrency(kotlin.math.abs(worst.savings)), worst.monthLabel)
                    )
                }
            }

            // Year Comparison
            growth.yearComparison?.let { yc ->
                if (yc.lastYearTotal > 0) {
                    item {
                        NarrativeCard(
                            emoji = if (yc.deltaPercent < 0) "🏆" else "📈",
                            title = stringResource(R.string.analytics_year_vs_last),
                            body = if (yc.deltaPercent < 0) {
                                stringResource(R.string.analytics_expenses_dropped, kotlin.math.abs(yc.deltaPercent).toInt())
                            } else {
                                stringResource(R.string.analytics_expenses_rose, yc.deltaPercent.toInt())
                            }
                        )
                    }
                }
            }

            // Category Evolution
            if (growth.categoryEvolution.isNotEmpty()) {
                item {
                    AnalyticsSectionHeader(title = stringResource(R.string.analytics_top_categories_year))
                }
                items(growth.categoryEvolution, key = { it.first }) { (name, amount) ->
                    NarrativeCard(
                        emoji = "📂",
                        title = name.ifBlank { stringResource(R.string.analytics_uncategorized) },
                        body = NumberFormatter.formatCurrency(amount)
                    )
                }
            }
        }

        // Personality
        item {
            NarrativeCard(
                emoji = insights.personality.emoji,
                title = stringResource(R.string.analytics_money_personality),
                body = insights.personality.headline
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Daily Bar Chart ──────────────────────────────────────────────────────────

@Composable
private fun DailyBarChart(summaries: List<DaySummary>, modifier: Modifier = Modifier) {
    val maxAmount = summaries.maxOfOrNull { it.totalSpent }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppPalette.cardBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            summaries.forEach { day ->
                val barHeight = if (day.totalSpent > 0) {
                    (day.totalSpent / maxAmount * 120).dp.coerceAtLeast(4.dp)
                } else 4.dp

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Amount label
                    if (day.totalSpent > 0) {
                        Text(
                            text = NumberFormatter.formatCompact(day.totalSpent),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Bar
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(barHeight)
                            .background(
                                color = if (day.isToday) AppPalette.accent else AppPalette.accent.copy(
                                    alpha = 0.4f
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Day label
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) AppPalette.accent else AppPalette.textMuted
                    )
                }
            }
        }
    }
}

// ─── Health Score Card ────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(score: Int) {
    val scoreColor = when {
        score >= 80 -> AppPalette.success
        score >= 50 -> AppPalette.accent
        else -> AppPalette.warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppPalette.cardBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(                    text = stringResource(R.string.analytics_financial_health),
                style = MaterialTheme.typography.titleSmall,
                color = AppPalette.textMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = scoreColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    score >= 80 -> stringResource(R.string.analytics_health_excellent)
                    score >= 60 -> stringResource(R.string.analytics_health_good)
                    score >= 40 -> stringResource(R.string.analytics_health_fair)
                    else -> stringResource(R.string.analytics_health_needs_attention)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted
            )
        }
    }
}

// ─── Mini Trend Chart ─────────────────────────────────────────────────────────

@Composable
private fun MiniTrendChart(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    val maxVal = points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppPalette.cardBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InsightChip(emoji = "💚", text = stringResource(R.string.analytics_income_label))
                InsightChip(emoji = "🔴", text = stringResource(R.string.analytics_expenses_label))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(100.dp)
                        ) {
                            // Income bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height((point.income / maxVal * 100).dp.coerceAtLeast(2.dp))
                                    .background(
                                        AppPalette.success,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            topStart = 2.dp,
                                            topEnd = 2.dp
                                        )
                                    )
                            )
                            // Expense bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height((point.expense / maxVal * 100).dp.coerceAtLeast(2.dp))
                                    .background(
                                        AppPalette.error,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            topStart = 2.dp,
                                            topEnd = 2.dp
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.textMuted
                        )
                    }
                }
            }
        }
    }
}

// ─── Annual Bar Chart ─────────────────────────────────────────────────────────

@Composable
private fun AnnualBarChart(points: List<com.example.insightku.feature.analytics.domain.AnnualMonthPoint>, modifier: Modifier = Modifier) {
    val maxVal = points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppPalette.cardBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InsightChip(emoji = "💚", text = stringResource(R.string.analytics_income_label))
                InsightChip(emoji = "🔴", text = stringResource(R.string.analytics_expenses_label))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(100.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height((point.income / maxVal * 100).dp.coerceAtLeast(2.dp))
                                    .background(
                                        AppPalette.success,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            topStart = 2.dp,
                                            topEnd = 2.dp
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height((point.expense / maxVal * 100).dp.coerceAtLeast(2.dp))
                                    .background(
                                        AppPalette.error,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            topStart = 2.dp,
                                            topEnd = 2.dp
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = point.monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.textMuted
                        )
                    }
                }
            }
        }
    }
}
