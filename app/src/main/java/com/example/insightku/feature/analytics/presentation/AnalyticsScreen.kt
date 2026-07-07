package com.example.insightku.feature.analytics.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.formatCurrency
import com.example.insightku.core.utils.CategoryUtils
import com.example.insightku.feature.analytics.domain.*
import com.example.insightku.feature.analytics.presentation.components.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                        title = "Something went wrong",
                        subtitle = uiState.error ?: "Unknown error"
                    )
                    uiState.insights == null || uiState.insights?.heroInsight?.headline?.contains("Start logging") == true -> {
                        AnalyticsEmptyState(
                            emoji = "📊",
                            title = "Your financial story awaits",
                            subtitle = "Complete a few transactions to unlock insights about your spending habits"
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
                    title = "Week vs Last Week",
                    body = if (wc.lastWeekTotal > 0) {
                        val change = kotlin.math.abs(wc.deltaPercent).toInt()
                        if (wc.deltaPercent < 0) {
                            "You spent $change% less than last week (${formatCurrency(wc.thisWeekTotal)} vs ${formatCurrency(wc.lastWeekTotal)})"
                        } else {
                            "You spent $change% more than last week (${formatCurrency(wc.thisWeekTotal)} vs ${formatCurrency(wc.lastWeekTotal)})"
                        }
                    } else {
                        "First week of tracking — ${formatCurrency(wc.thisWeekTotal)} total spent"
                    }
                )
            }
        }

        // Daily Spending Bar Chart
        if (insights.dailySummaries.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = "This Week's Spending")
            }
            item {
                DailyBarChart(summaries = insights.dailySummaries)
            }
        }

        // Weekly Habit
        insights.weeklyHabit?.let { habit ->
            item {
                NarrativeCard(
                    emoji = "🎯",
                    title = "Your Weekly Rhythm",
                    body = "${habit.loudestDay}s are your biggest spending days at ${formatCurrency(habit.loudestAmount)}, while ${habit.quietestDay}s stay quiet at ${formatCurrency(habit.quietestAmount)}. Average daily spend: ${formatCurrency(habit.avgDailySpend)}."
                )
            }
        }

        // Category Breakdown
        if (insights.categorySlices.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = "Top Categories")
            }
            items(insights.categorySlices) { slice ->
                CategoryBar(
                    name = slice.name.ifBlank { "Uncategorized" },
                    percentage = slice.proportion,
                    amount = "${(slice.proportion * 100).toInt()}%",
                    color = CategoryUtils.getColorForCategoryName(slice.name)
                )
            }
        }

        // Patterns
        if (insights.patterns.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = "What We Noticed")
            }
            items(insights.patterns) { pattern ->
                InsightChip(emoji = pattern.emoji, text = pattern.description)
            }
        }

        // Big Decisions
        if (insights.bigDecisions.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = "Biggest Transactions")
            }
            items(insights.bigDecisions) { decision ->
                NarrativeCard(
                    emoji = "💰",
                    title = decision.transaction.title.ifBlank { decision.transaction.category },
                    body = "${formatCurrency(decision.transaction.amount)} — ${decision.percentOfMonthlySpend.toInt()}% of total spending"
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
                    title = "Income vs Expenses",
                    body = buildString {
                        append("Earned ${formatCurrency(health.balance.totalIncome)}, spent ${formatCurrency(health.balance.totalExpenses)}. ")
                        if (health.balance.isPositive) {
                            append("You saved ${formatCurrency(health.balance.savingsAmount)} (${(health.balance.savingsRate * 100).toInt()}% savings rate).")
                        } else {
                            append("You spent ${formatCurrency(kotlin.math.abs(health.balance.savingsAmount))} more than you earned.")
                        }
                    }
                )
            }

            // Health Score
            item {
                HealthScoreCard(score = health.healthScore)
            }

            // Category Ranking
            if (health.topCategories.isNotEmpty()) {
                item {
                    AnalyticsSectionHeader(title = "Where Your Money Went")
                }
                items(health.topCategories) { cat ->
                    CategoryBar(
                        name = cat.name.ifBlank { "Uncategorized" },
                        percentage = cat.percentage.toFloat(),
                        amount = formatCurrency(cat.amount),
                        color = CategoryUtils.getColorForCategoryName(cat.name)
                    )
                }
            }

            // Trend
            if (health.trend.isNotEmpty()) {
                item {
                    AnalyticsSectionHeader(title = "6-Month Trend")
                }
                item {
                    MiniTrendChart(points = health.trend)
                }
            }

            // Recurring
            if (health.recurringTotal > 0) {
                item {
                    NarrativeCard(
                        emoji = "🔄",
                        title = "Recurring Payments",
                        body = "About ${formatCurrency(health.recurringTotal)} of your spending appears to be recurring (subscriptions, bills, regular purchases)."
                    )
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
                    title = "Spending Mood",
                    body = mood.summaryLine.ifBlank { mood.headline }
                )
            }
        }

        // Patterns
        if (insights.patterns.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = "Patterns We Found")
            }
            items(insights.patterns) { pattern ->
                InsightChip(emoji = pattern.emoji, text = pattern.description)
            }
        }

        // Big Decisions
        if (insights.bigDecisions.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(title = "Notable Transactions")
            }
            items(insights.bigDecisions) { decision ->
                NarrativeCard(
                    emoji = "💡",
                    title = decision.transaction.title.ifBlank { decision.transaction.category },
                    body = "${formatCurrency(decision.transaction.amount)} — ${decision.percentOfMonthlySpend.toInt()}% of this month's spending"
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
                    title = "Your Financial Year",
                    body = "Total income: ${formatCurrency(growth.totalIncome)}. Total expenses: ${formatCurrency(growth.totalExpenses)}. Average monthly spending: ${formatCurrency(growth.avgMonthlyExpense)}."
                )
            }

            // Savings Rate
            item {
                NarrativeCard(
                    emoji = if (growth.savingsRate > 0.15) "🎉" else "💡",
                    title = "Savings Rate",
                    body = if (growth.savingsRate > 0) {
                        "You saved ${(growth.savingsRate * 100).toInt()}% of your total income this year — ${formatCurrency(growth.totalSaved)} saved."
                    } else {
                        "No savings recorded yet. Track income to see your savings rate."
                    }
                )
            }

            // Monthly Trend
            if (growth.monthlyPoints.isNotEmpty()) {
                item {
                    AnalyticsSectionHeader(title = "Monthly Journey")
                }
                item {
                    AnnualBarChart(points = growth.monthlyPoints)
                }
            }

            // Best / Worst Month
            growth.bestMonth?.let { best ->
                if (best.savings > 0) {
                    item {
                        NarrativeCard(
                            emoji = "⭐",
                            title = "Best Month: ${best.monthLabel}",
                            body = "You saved ${formatCurrency(best.savings)} in ${best.monthLabel} — your strongest financial month."
                        )
                    }
                }
            }
            growth.worstMonth?.let { worst ->
                item {
                    NarrativeCard(
                        emoji = "💪",
                        title = "Toughest Month: ${worst.monthLabel}",
                        body = "You overspent by ${formatCurrency(kotlin.math.abs(worst.savings))} in ${worst.monthLabel}. Every month is a new opportunity."
                    )
                }
            }

            // Year Comparison
            growth.yearComparison?.let { yc ->
                if (yc.lastYearTotal > 0) {
                    item {
                        NarrativeCard(
                            emoji = if (yc.deltaPercent < 0) "🏆" else "📈",
                            title = "Year vs Last Year",
                            body = if (yc.deltaPercent < 0) {
                                "Expenses dropped ${kotlin.math.abs(yc.deltaPercent).toInt()}% from last year. Great progress!"
                            } else {
                                "Expenses rose ${yc.deltaPercent.toInt()}% from last year. Review your spending patterns."
                            }
                        )
                    }
                }
            }

            // Category Evolution
            if (growth.categoryEvolution.isNotEmpty()) {
                item {
                    AnalyticsSectionHeader(title = "Top Categories This Year")
                }
                items(growth.categoryEvolution) { (name, amount) ->
                    NarrativeCard(
                        emoji = "📂",
                        title = name.ifBlank { "Uncategorized" },
                        body = formatCurrency(amount)
                    )
                }
            }
        }

        // Personality
        item {
            NarrativeCard(
                emoji = insights.personality.emoji,
                title = "Your Money Personality",
                body = insights.personality.headline
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Daily Bar Chart ──────────────────────────────────────────────────────────

@Composable
private fun DailyBarChart(summaries: List<DaySummary>) {
    val maxAmount = summaries.maxOfOrNull { it.totalSpent }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                            text = "${(day.totalSpent / 1000).toInt()}K",
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
                                color = if (day.isToday) AppPalette.accent else AppPalette.accent.copy(alpha = 0.4f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
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
            Text(
                text = "Financial Health",
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
                    score >= 80 -> "Excellent"
                    score >= 60 -> "Good"
                    score >= 40 -> "Fair"
                    else -> "Needs Attention"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted
            )
        }
    }
}

// ─── Mini Trend Chart ─────────────────────────────────────────────────────────

@Composable
private fun MiniTrendChart(points: List<TrendPoint>) {
    val maxVal = points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                InsightChip(emoji = "💚", text = "Income")
                InsightChip(emoji = "🔴", text = "Expenses")
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
                                    .background(AppPalette.success, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            )
                            // Expense bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height((point.expense / maxVal * 100).dp.coerceAtLeast(2.dp))
                                    .background(AppPalette.error, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
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
private fun AnnualBarChart(points: List<com.example.insightku.feature.analytics.domain.AnnualMonthPoint>) {
    val maxVal = points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                InsightChip(emoji = "💚", text = "Income")
                InsightChip(emoji = "🔴", text = "Expenses")
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
                                    .background(AppPalette.success, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height((point.expense / maxVal * 100).dp.coerceAtLeast(2.dp))
                                    .background(AppPalette.error, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
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
