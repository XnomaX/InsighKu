package com.example.insightku.feature.analytics.domain

import com.example.insightku.core.data.model.Transaction
import javax.inject.Inject

// ── Personality ───────────────────────────────────────────────────────────────

data class SpendingPersonality(val headline: String, val emoji: String) {
    val summaryLine: String get() = headline
    val sentence: String get() = headline
    companion object {
        val ONBOARDING get() = SpendingPersonality(com.example.insightku.core.i18n.AnalyticsStrings.personalityOnboarding(), "🌱")
    }
}

// ── Two Yous ──────────────────────────────────────────────────────────────────

data class TwoYousData(
    val weekdayDailyAvg: Double,
    val weekendDailyAvg: Double,
    val weekdayTopCategories: List<CategorySlice>,
    val weekendTopCategories: List<CategorySlice>,
    val divergence: Float,
    val gapHeadline: String,
    val isConfident: Boolean
)

data class CategorySlice(val name: String, val proportion: Float)

// ── Behavioral Patterns ───────────────────────────────────────────────────────

data class BehavioralPattern(val emoji: String, val description: String) {
    val text: String get() = description
}

// ── Noticing ──────────────────────────────────────────────────────────────────

data class Noticing(val id: String, val emoji: String, val text: String)

// ── Heatmap ───────────────────────────────────────────────────────────────────

enum class DayComparison { ABOVE, TYPICAL, BELOW, NONE }

data class HeatmapCell(
    val dayOfWeek: Int,
    val weekOfMonth: Int,
    val dayOfMonth: Int,
    val totalAmount: Double,
    val transactionCount: Int,
    val label: String,
    val topCategory: String?,
    val topTimeOfDay: String?,
    val comparison: DayComparison
) {
    val weekIndex: Int get() = weekOfMonth
    val amount: Double get() = totalAmount
    val dateLabel: String get() = label
    val mostActiveTime: String? get() = topTimeOfDay
}

// ── Rhythm ────────────────────────────────────────────────────────────────────

data class RhythmCaption(val text: String)

// ── Big Decisions ─────────────────────────────────────────────────────────────

data class BigDecision(val transaction: Transaction, val percentOfMonthlySpend: Double)

// ── Streak ────────────────────────────────────────────────────────────────────

enum class ConsistencyDirection { UP, DOWN, STEADY, NEW, SAME }

data class StreakData(
    val daysShownUp: Int,
    val daysElapsedThisMonth: Int,
    val quietDays: Int,
    val direction: ConsistencyDirection = ConsistencyDirection.NEW,
    val prevDaysShownUp: Int = 0
)

// ── Spotlight ─────────────────────────────────────────────────────────────────

data class SpotlightData(
    val topExpenseName: String = "",
    val topExpenseAmount: Double = 0.0,
    val expenseDeltaPct: Double? = null,
    val topIncomeName: String? = null,
    val topIncomeAmount: Double? = null,
    val incomeDeltaPct: Double? = null
)

// ── Mood ──────────────────────────────────────────────────────────────────────

enum class SpendingMood { CALM, STEADY, RESTLESS }

data class MonthPoint(val label: String, val amount: Double)

data class MoodData(
    val points: List<MonthPoint>,
    val mood: SpendingMood,
    val caption: String
) {
    val headline: String get() = when (mood) {
        SpendingMood.CALM -> com.example.insightku.core.i18n.AnalyticsStrings.moodCalm()
        SpendingMood.STEADY -> com.example.insightku.core.i18n.AnalyticsStrings.moodSteady()
        SpendingMood.RESTLESS -> com.example.insightku.core.i18n.AnalyticsStrings.moodRestless()
    }
    val summaryLine: String get() = caption.ifBlank { headline }
}

// ── Weekly Insights ───────────────────────────────────────────────────────────

/** A single day's spending summary within the week. */
data class DaySummary(
    val dayOfWeek: Int,       // Calendar.MONDAY..SUNDAY
    val dayLabel: String,     // "Mon", "Tue", etc.
    val totalSpent: Double,
    val transactionCount: Int,
    val topCategory: String?,
    val isToday: Boolean
)

/** Week-over-week comparison data. */
data class WeekComparison(
    val thisWeekTotal: Double,
    val lastWeekTotal: Double,
    val deltaPercent: Double,   // positive = spent more this week
    val thisWeekDays: Int,
    val lastWeekDays: Int
)

/** Weekly habit insight — which day was biggest, which was quietest. */
data class WeeklyHabit(
    val loudestDay: String,
    val loudestAmount: Double,
    val quietestDay: String,
    val quietestAmount: Double,
    val streakDays: Int,        // consecutive days with transactions
    val avgDailySpend: Double,
    val totalCategories: Int
)

// ── Monthly Insights ──────────────────────────────────────────────────────────

/** Income vs Expense balance for a month. */
data class CashflowBalance(
    val totalIncome: Double,
    val totalExpenses: Double,
    val savingsAmount: Double,
    val savingsRate: Double,    // 0.0 .. 1.0
    val isPositive: Boolean
)

/** Top categories with amounts and percentages. */
data class CategoryRanking(
    val name: String,
    val amount: Double,
    val percentage: Double
)

/** Month-over-month trend point. */
data class TrendPoint(
    val label: String,
    val income: Double,
    val expense: Double,
    val savings: Double
)

/** Monthly health summary. */
data class MonthlyHealth(
    val balance: CashflowBalance,
    val topCategories: List<CategoryRanking>,
    val trend: List<TrendPoint>,
    val recurringTotal: Double,
    val healthScore: Int       // 0..100
)

// ── Annual Insights ───────────────────────────────────────────────────────────

/** Monthly data point within a year for trend visualization. */
data class AnnualMonthPoint(
    val monthIndex: Int,       // 0..11
    val monthLabel: String,    // "Jan", "Feb", etc.
    val income: Double,
    val expense: Double,
    val savings: Double
)

/** Year-over-year comparison. */
data class YearComparison(
    val thisYearTotal: Double,
    val lastYearTotal: Double,
    val deltaPercent: Double,
    val thisYearIncome: Double,
    val lastYearIncome: Double
)

/** Annual growth data. */
data class AnnualGrowth(
    val monthlyPoints: List<AnnualMonthPoint>,
    val yearComparison: YearComparison?,
    val bestMonth: AnnualMonthPoint?,
    val worstMonth: AnnualMonthPoint?,
    val totalSaved: Double,
    val totalIncome: Double,
    val totalExpenses: Double,
    val avgMonthlyExpense: Double,
    val savingsRate: Double,
    val categoryEvolution: List<Pair<String, Double>>  // category → total for the year
)

// ── Hero Insight ──────────────────────────────────────────────────────────────

/** The single most important insight at the top of each period view. */
data class HeroInsight(
    val emoji: String,
    val headline: String,
    val subtext: String
)

// ── Analytics Insights (output of InsightEngine) ──────────────────────────────

data class AnalyticsInsights(
    // Core (shared across periods)
    val personality: SpendingPersonality,
    val twoYous: TwoYousData?,
    val patterns: List<BehavioralPattern>,
    val noticing: Noticing?,
    val heatmapCells: List<HeatmapCell>,
    val rhythm: RhythmCaption,
    val categorySlices: List<CategorySlice>,
    val bigDecisions: List<BigDecision>,
    val streak: StreakData,
    val spotlight: SpotlightData,
    val mood: MoodData?,
    val heroInsight: HeroInsight,

    // Weekly-specific
    val weekComparison: WeekComparison? = null,
    val weeklyHabit: WeeklyHabit? = null,
    val dailySummaries: List<DaySummary> = emptyList(),

    // Monthly-specific
    val monthlyHealth: MonthlyHealth? = null,

    // Annual-specific
    val annualGrowth: AnnualGrowth? = null
)

// ── Insight Engine ────────────────────────────────────────────────────────────

class InsightEngine @Inject constructor() {

    fun derive(
        transactions: List<Transaction>,
        nowMillis: Long,
        periodType: AnalyticsPeriodType = AnalyticsPeriodType.MONTHLY
    ): AnalyticsInsights {
        return when (periodType) {
            AnalyticsPeriodType.WEEKLY -> deriveWeekly(transactions, nowMillis)
            AnalyticsPeriodType.MONTHLY -> deriveMonthly(transactions, nowMillis)
            AnalyticsPeriodType.ANNUAL -> deriveAnnual(transactions, nowMillis)
        }
    }

    // ── Weekly Derivation ─────────────────────────────────────────────────────

    private fun deriveWeekly(allTxns: List<Transaction>, nowMillis: Long): AnalyticsInsights {
        val week = AnalyticsPeriod.currentWeek(nowMillis)
        val prevWeek = AnalyticsPeriod.previousWeek(nowMillis)
        val weekTxns = allTxns.filter { week.contains(it.date) }
        val prevWeekTxns = allTxns.filter { prevWeek.contains(it.date) }

        // Localized short day names using DateFormatter.getDayOfWeek and truncating
        val baseCal = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            firstDayOfWeek = java.util.Calendar.MONDAY
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        }
        val dayNames = (0..6).map { dow ->
            val cal = (baseCal.clone() as java.util.Calendar).apply { set(java.util.Calendar.DAY_OF_WEEK, dow + 1) }
            val fullName = com.example.insightku.core.i18n.DateFormatter.getDayOfWeek(cal.timeInMillis)
            fullName.take(3)
        }.toTypedArray()

        // Daily summaries
        val dailySums = weekTxns.groupBy {
            java.util.Calendar.getInstance().apply { timeInMillis = it.date }.get(java.util.Calendar.DAY_OF_WEEK) - 1
        }
        val todayDow = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
            .get(java.util.Calendar.DAY_OF_WEEK) - 1

        val dailySummaries = (0..6).map { dow ->
            val dayTxns = dailySums[dow] ?: emptyList()
            DaySummary(
                dayOfWeek = dow,
                dayLabel = dayNames[dow],
                totalSpent = dayTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount },
                transactionCount = dayTxns.size,
                topCategory = dayTxns.groupBy { it.category }.maxByOrNull { it.value.sumOf { t -> t.amount } }?.key,
                isToday = dow == todayDow
            )
        }

        // Week comparison
        val thisWeekTotal = weekTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
        val lastWeekTotal = prevWeekTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
        val deltaPct = if (lastWeekTotal > 0) ((thisWeekTotal - lastWeekTotal) / lastWeekTotal) * 100 else 0.0

        val weekComparison = WeekComparison(
            thisWeekTotal = thisWeekTotal,
            lastWeekTotal = lastWeekTotal,
            deltaPercent = deltaPct,
            thisWeekDays = weekTxns.map { dayKey(it.date) }.distinct().size,
            lastWeekDays = prevWeekTxns.map { dayKey(it.date) }.distinct().size
        )

        // Weekly habit
        val loudestDay = dailySums.maxByOrNull { it.value.filter { t -> t.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { t -> t.amount } }
        val quietestDay = dailySums.minByOrNull { it.value.filter { t -> t.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { t -> t.amount } }

        val expenseTxns = weekTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        val daysWithTxns = expenseTxns.map { dayKey(it.date) }.distinct().size
        val streakDays = calculateStreakDays(expenseTxns, nowMillis)

        val weeklyHabit = WeeklyHabit(
            loudestDay = loudestDay?.let { dayNames[it.key] } ?: "—",
            loudestAmount = loudestDay?.value?.filter { t -> t.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }?.sumOf { it.amount } ?: 0.0,
            quietestDay = quietestDay?.let { dayNames[it.key] } ?: "—",
            quietestAmount = quietestDay?.value?.filter { t -> t.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }?.sumOf { it.amount } ?: 0.0,
            streakDays = streakDays,
            avgDailySpend = if (daysWithTxns > 0) thisWeekTotal / daysWithTxns else 0.0,
            totalCategories = weekTxns.map { it.category }.distinct().size
        )

        // Hero insight for weekly
        val heroInsight = if (weekTxns.isEmpty()) {
            val (headline, subtext) = com.example.insightku.core.i18n.AnalyticsStrings.heroWeekAtGlance() to com.example.insightku.core.i18n.AnalyticsStrings.heroWeekStartLogging()
            HeroInsight("📊", headline, subtext)
        } else {
            val daysBelowAvg = dailySums.count { (_, txns) ->
                val dayTotal = txns.filter { t -> t.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
                dayTotal < weeklyHabit.avgDailySpend && dayTotal > 0
            }
            if (daysBelowAvg >= 4) {
                val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroStayedUnderAvg(daysBelowAvg)
                HeroInsight("🎯", h, s)
            } else if (deltaPct < -10) {
                val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroSpentLessWeek(kotlin.math.abs(deltaPct).toInt())
                HeroInsight("📉", h, s)
            } else if (deltaPct > 20) {
                val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroSpendingPickedUp(deltaPct.toInt())
                HeroInsight("📈", h, s)
            } else {
                val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroSteadyWeek(expenseTxns.size)
                HeroInsight("🌊", h, s)
            }
        }

        // Existing shared derivations
        val personality = derivePersonality(weekTxns)
        val twoYous = deriveTwoYous(weekTxns)
        val patterns = derivePatterns(weekTxns, allTxns, nowMillis)
        val noticing = deriveNoticing(weekTxns)
        val categorySlices = topCategorySlices(weekTxns)
        val bigDecisions = deriveBigDecisions(weekTxns)
        val streak = deriveStreak(allTxns, nowMillis)
        val spotlight = deriveSpotlight(weekTxns, nowMillis)
        val mood = deriveMood(allTxns, nowMillis)

        return AnalyticsInsights(
            personality = personality,
            twoYous = twoYous,
            patterns = patterns,
            noticing = noticing,
            heatmapCells = emptyList(),
            rhythm = RhythmCaption(com.example.insightku.core.i18n.AnalyticsStrings.rhythmLoudest(weeklyHabit.loudestDay)),
            categorySlices = categorySlices,
            bigDecisions = bigDecisions,
            streak = streak,
            spotlight = spotlight,
            mood = mood,
            heroInsight = heroInsight,
            weekComparison = weekComparison,
            weeklyHabit = weeklyHabit,
            dailySummaries = dailySummaries
        )
    }

    // ── Monthly Derivation ────────────────────────────────────────────────────

    private fun deriveMonthly(allTxns: List<Transaction>, nowMillis: Long): AnalyticsInsights {
        val period = AnalyticsPeriod.currentMonth(nowMillis)
        val prevPeriod = AnalyticsPeriod.previousMonth(nowMillis)
        val monthTxns = allTxns.filter { period.contains(it.date) }
        val prevMonthTxns = allTxns.filter { prevPeriod.contains(it.date) }

        // Cashflow
        val income = monthTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.INCOME }.sumOf { it.amount }
        val expenses = monthTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
        val savings = income - expenses
        val savingsRate = if (income > 0) (savings / income).coerceIn(0.0, 1.0) else 0.0

        val balance = CashflowBalance(
            totalIncome = income,
            totalExpenses = expenses,
            savingsAmount = savings,
            savingsRate = savingsRate,
            isPositive = savings > 0
        )

        // Category ranking
        val expenseTxns = monthTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        val totalExpense = expenseTxns.sumOf { it.amount }.coerceAtLeast(1.0)
        val topCategories = expenseTxns.groupBy { it.category }
            .map { (cat, txns) -> cat to txns.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(6)
            .map { (cat, amt) -> CategoryRanking(cat, amt, amt / totalExpense) }

        // Trend (last 6 months)
        val trend = (5 downTo 0).map { i ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(java.util.Calendar.MONTH, -i)
            }
            val mp = AnalyticsPeriod.currentMonth(cal.timeInMillis)
            val mTxns = allTxns.filter { mp.contains(it.date) }
            TrendPoint(
                label = com.example.insightku.core.i18n.DateFormatter.getShortMonthName(cal.timeInMillis),
                income = mTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.INCOME }.sumOf { it.amount },
                expense = mTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount },
                savings = 0.0
            ).let { it.copy(savings = it.income - it.expense) }
        }

        // Health score (0-100)
        val healthScore = calculateHealthScore(balance, topCategories)

        // Recurring (subscriptions/bills detected by repeated category + similar amounts)
        val recurringTotal = detectRecurring(monthTxns)

        val monthlyHealth = MonthlyHealth(
            balance = balance,
            topCategories = topCategories,
            trend = trend,
            recurringTotal = recurringTotal,
            healthScore = healthScore
        )

        // Hero insight for monthly
        val heroInsight = if (monthTxns.isEmpty()) {
            val (headline, subtext) = com.example.insightku.core.i18n.AnalyticsStrings.heroMonthAtGlance() to com.example.insightku.core.i18n.AnalyticsStrings.heroMonthStartLogging()
            HeroInsight("📊", headline, subtext)
        } else {
            val prevExpenses = prevMonthTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
            val expDelta = if (prevExpenses > 0) ((expenses - prevExpenses) / prevExpenses) * 100 else 0.0
            when {
                savings > 0 && savingsRate > 0.2 -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroSavedPctIncome((savingsRate * 100).toInt()); HeroInsight("🎉", h, s) }
                savings > 0 -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroPositiveBalance(); HeroInsight("✅", h, s) }
                expDelta < -15 -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroExpensesDroppedMonth(kotlin.math.abs(expDelta).toInt()); HeroInsight("📉", h, s) }
                expDelta > 15 -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroSpendingRoseMonth(expDelta.toInt()); HeroInsight("📈", h, s) }
                else -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroSteadyMonth(expenseTxns.size); HeroInsight("🌊", h, s) }
            }
        }

        val personality = derivePersonality(monthTxns)
        val twoYous = deriveTwoYous(monthTxns)
        val patterns = derivePatterns(monthTxns, allTxns, nowMillis)
        val heatmap = deriveHeatmap(monthTxns, nowMillis)
        val rhythm = deriveRhythm(monthTxns)
        val slices = deriveCategorySlices(monthTxns)
        val bigDecisions = deriveBigDecisions(monthTxns)
        val streak = deriveStreak(allTxns, nowMillis)
        val spotlight = deriveSpotlight(allTxns, nowMillis)
        val mood = deriveMood(allTxns, nowMillis)
        val noticing = deriveNoticing(monthTxns)

        return AnalyticsInsights(
            personality = personality,
            twoYous = twoYous,
            patterns = patterns,
            noticing = noticing,
            heatmapCells = heatmap,
            rhythm = rhythm,
            categorySlices = slices,
            bigDecisions = bigDecisions,
            streak = streak,
            spotlight = spotlight,
            mood = mood,
            heroInsight = heroInsight,
            monthlyHealth = monthlyHealth
        )
    }

    // ── Annual Derivation ─────────────────────────────────────────────────────

    private fun deriveAnnual(allTxns: List<Transaction>, nowMillis: Long): AnalyticsInsights {
        val yearPeriod = AnalyticsPeriod.currentYear(nowMillis)
        val yearTxns = allTxns.filter { yearPeriod.contains(it.date) }

        // Localized short month names using DateFormatter
        val monthLabels = (0..11).map { m ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(java.util.Calendar.MONTH, m)
            }
            com.example.insightku.core.i18n.DateFormatter.getShortMonthName(cal.timeInMillis)
        }.toTypedArray()

        // Monthly points
        val monthlyPoints = (0..11).map { m ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(java.util.Calendar.MONTH, m)
            }
            val mp = AnalyticsPeriod.currentMonth(cal.timeInMillis)
            val mTxns = allTxns.filter { mp.contains(it.date) }
            val inc = mTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.INCOME }.sumOf { it.amount }
            val exp = mTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
            AnnualMonthPoint(m, monthLabels[m], inc, exp, inc - exp)
        }

        // Filter to only months that have passed
        val currentMonth = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
            .get(java.util.Calendar.MONTH)
        val activeMonths = monthlyPoints.filter { it.monthIndex <= currentMonth }

        // Year comparison
        val prevYearPeriod = AnalyticsPeriod.previousYear(nowMillis)
        val prevYearTxns = allTxns.filter { prevYearPeriod.contains(it.date) }
        val thisYearTotal = yearTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
        val lastYearTotal = prevYearTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
        val thisYearIncome = yearTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.INCOME }.sumOf { it.amount }
        val lastYearIncome = prevYearTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.INCOME }.sumOf { it.amount }

        val yearComparison = if (lastYearTotal > 0 || lastYearIncome > 0) {
            YearComparison(
                thisYearTotal = thisYearTotal,
                lastYearTotal = lastYearTotal,
                deltaPercent = if (lastYearTotal > 0) ((thisYearTotal - lastYearTotal) / lastYearTotal) * 100 else 0.0,
                thisYearIncome = thisYearIncome,
                lastYearIncome = lastYearIncome
            )
        } else null

        // Best/worst month (by savings)
        val bestMonth = activeMonths.maxByOrNull { it.savings }
        val worstMonth = activeMonths.minByOrNull { it.savings }

        // Totals
        val totalSaved = activeMonths.sumOf { it.savings }
        val totalIncome = activeMonths.sumOf { it.income }
        val totalExpenses = activeMonths.sumOf { it.expense }
        val avgMonthlyExpense = if (activeMonths.isNotEmpty()) totalExpenses / activeMonths.size else 0.0
        val savingsRate = if (totalIncome > 0) (totalSaved / totalIncome).coerceIn(0.0, 1.0) else 0.0

        // Category evolution
        val categoryEvolution = yearTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, txns) -> cat to txns.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(8)

        val annualGrowth = AnnualGrowth(
            monthlyPoints = activeMonths,
            yearComparison = yearComparison,
            bestMonth = bestMonth,
            worstMonth = worstMonth,
            totalSaved = totalSaved,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            avgMonthlyExpense = avgMonthlyExpense,
            savingsRate = savingsRate,
            categoryEvolution = categoryEvolution
        )

        // Hero insight for annual
        val heroInsight = if (yearTxns.isEmpty()) {
            val (headline, subtext) = com.example.insightku.core.i18n.AnalyticsStrings.heroYearAtGlance() to com.example.insightku.core.i18n.AnalyticsStrings.heroYearStartLogging()
            HeroInsight("📊", headline, subtext)
        } else {
            when {
                yearComparison != null && yearComparison.deltaPercent < -10 -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroExpensesDroppedYear(kotlin.math.abs(yearComparison.deltaPercent).toInt()); HeroInsight("🏆", h, s) }
                totalSaved > 0 && savingsRate > 0.15 -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroSavedPctTotal((savingsRate * 100).toInt()); HeroInsight("🎉", h, s) }
                bestMonth != null && bestMonth.savings > 0 -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroBestMonth(bestMonth.monthLabel, formatAmount(bestMonth.savings)); HeroInsight("⭐", h, s) }
                else -> { val (h, s) = com.example.insightku.core.i18n.AnalyticsStrings.heroYearTracked(activeMonths.size); HeroInsight("📈", h, s) }
            }
        }

        val personality = derivePersonality(yearTxns)
        val twoYous = deriveTwoYous(yearTxns)
        val patterns = derivePatterns(yearTxns, allTxns, nowMillis)
        val rhythm = deriveRhythm(yearTxns)
        val slices = deriveCategorySlices(yearTxns)
        val bigDecisions = deriveBigDecisions(yearTxns)
        val streak = deriveStreak(allTxns, nowMillis)
        val spotlight = deriveSpotlight(allTxns, nowMillis)
        val mood = deriveMood(allTxns, nowMillis)
        val noticing = deriveNoticing(yearTxns)

        return AnalyticsInsights(
            personality = personality,
            twoYous = twoYous,
            patterns = patterns,
            noticing = noticing,
            heatmapCells = emptyList(),
            rhythm = rhythm,
            categorySlices = slices,
            bigDecisions = bigDecisions,
            streak = streak,
            spotlight = spotlight,
            mood = mood,
            heroInsight = heroInsight,
            annualGrowth = annualGrowth
        )
    }

    // ── Shared Derivations ────────────────────────────────────────────────────

    private fun derivePersonality(txns: List<Transaction>): SpendingPersonality {
        if (txns.isEmpty()) return SpendingPersonality.ONBOARDING
        val weekendTxns = txns.filter { isWeekend(it.date) }
        val weekendRatio = weekendTxns.sumOf { it.amount } / txns.sumOf { it.amount }.coerceAtLeast(1.0)
        return when {
            weekendRatio > 0.6 -> SpendingPersonality(com.example.insightku.core.i18n.AnalyticsStrings.personalityWeekend(), "🎧")
            weekendRatio < 0.3 -> SpendingPersonality(com.example.insightku.core.i18n.AnalyticsStrings.personalityWeekday(), "🎯")
            else -> SpendingPersonality(com.example.insightku.core.i18n.AnalyticsStrings.personalitySteady(), "🌊")
        }
    }

    private fun deriveTwoYous(txns: List<Transaction>): TwoYousData? {
        if (txns.size < 5) return null
        val weekday = txns.filter { !isWeekend(it.date) }
        val weekend = txns.filter { isWeekend(it.date) }
        if (weekday.isEmpty() || weekend.isEmpty()) return null

        val weekdayDays = weekday.map { dayKey(it.date) }.distinct().size.coerceAtLeast(1)
        val weekendDays = weekend.map { dayKey(it.date) }.distinct().size.coerceAtLeast(1)
        val weekdayAvg = weekday.sumOf { it.amount } / weekdayDays
        val weekendAvg = weekend.sumOf { it.amount } / weekendDays

        val maxAvg = maxOf(weekdayAvg, weekendAvg).coerceAtLeast(1.0)
        val divergence = (kotlin.math.abs(weekdayAvg - weekendAvg) / maxAvg).toFloat().coerceIn(0f, 1f)

        val weekdayCats = topCategorySlices(weekday)
        val weekendCats = topCategorySlices(weekend)

        val headline = when {
            divergence > 0.5 -> com.example.insightku.core.i18n.AnalyticsStrings.twoYousDiverge()
            divergence < 0.15 -> com.example.insightku.core.i18n.AnalyticsStrings.twoYousSame()
            else -> com.example.insightku.core.i18n.AnalyticsStrings.twoYousGap()
        }

        return TwoYousData(
            weekdayDailyAvg = weekdayAvg,
            weekendDailyAvg = weekendAvg,
            weekdayTopCategories = weekdayCats,
            weekendTopCategories = weekendCats,
            divergence = divergence,
            gapHeadline = headline,
            isConfident = txns.size >= 10
        )
    }

    private fun topCategorySlices(txns: List<Transaction>): List<CategorySlice> {
        val total = txns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }.sumOf { it.amount }.coerceAtLeast(1.0)
        return txns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, t) -> cat to t.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(3)
            .map { (cat, amt) -> CategorySlice(cat, (amt / total).toFloat()) }
    }

    private fun derivePatterns(monthTxns: List<Transaction>, allTxns: List<Transaction>, nowMillis: Long): List<BehavioralPattern> {
        val patterns = mutableListOf<BehavioralPattern>()
        val expenseTxns = monthTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        if (expenseTxns.isEmpty()) return patterns

        val weekendRatio = expenseTxns.filter { isWeekend(it.date) }.sumOf { it.amount } / expenseTxns.sumOf { it.amount }
        if (weekendRatio > 0.45) patterns.add(BehavioralPattern("🌙", com.example.insightku.core.i18n.AnalyticsStrings.patternWeekendSpending()))

        val topCat = expenseTxns.groupBy { it.category }.maxByOrNull { it.value.sumOf { t -> t.amount } }
        if (topCat != null) {
            val days = topCat.value.map { dayKey(it.date) }.distinct().size
            if (days >= 4) patterns.add(BehavioralPattern("🔁", com.example.insightku.core.i18n.AnalyticsStrings.patternRegularCategory(topCat.key, days)))
        }

        val avgDaily = expenseTxns.sumOf { it.amount } / expenseTxns.map { dayKey(it.date) }.distinct().size.coerceAtLeast(1)
        val aboveAvgDays = expenseTxns.groupBy { dayKey(it.date) }
            .count { (_, txns) -> txns.sumOf { it.amount } > avgDaily * 1.5 }
        if (aboveAvgDays >= 2) {
            patterns.add(BehavioralPattern("⚡", com.example.insightku.core.i18n.AnalyticsStrings.patternAboveAvgDays(aboveAvgDays)))
        }

        return patterns.take(3)
    }

    private fun deriveHeatmap(txns: List<Transaction>, nowMillis: Long): List<HeatmapCell> {
        val expenseTxns = txns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        if (expenseTxns.isEmpty()) return emptyList()

        val monthAvg = expenseTxns.sumOf { it.amount } / expenseTxns.map { dayKey(it.date) }.distinct().size.coerceAtLeast(1)
        return expenseTxns.groupBy { dayKey(it.date) }.map { (_, dayTxns) ->
            val dayCal = java.util.Calendar.getInstance().apply { timeInMillis = dayTxns.first().date }
            val total = dayTxns.sumOf { it.amount }
            HeatmapCell(
                dayOfWeek = dayCal.get(java.util.Calendar.DAY_OF_WEEK) - 1,
                weekOfMonth = dayCal.get(java.util.Calendar.WEEK_OF_MONTH) - 1,
                dayOfMonth = dayCal.get(java.util.Calendar.DAY_OF_MONTH),
                totalAmount = total,
                transactionCount = dayTxns.size,
                label = "Day ${dayCal.get(java.util.Calendar.DAY_OF_MONTH)}",
                topCategory = dayTxns.groupBy { it.category }.maxByOrNull { it.value.sumOf { t -> t.amount } }?.key,
                topTimeOfDay = null,
                comparison = when {
                    total > monthAvg * 1.5 -> DayComparison.ABOVE
                    total < monthAvg * 0.5 -> DayComparison.BELOW
                    else -> DayComparison.TYPICAL
                }
            )
        }
    }

    private fun deriveRhythm(txns: List<Transaction>): RhythmCaption {
        val expenseTxns = txns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        if (expenseTxns.isEmpty()) return RhythmCaption("")
        // Localized full day names using DateFormatter
        val baseCal = java.util.Calendar.getInstance().apply {
            timeInMillis = java.lang.System.currentTimeMillis()
            firstDayOfWeek = java.util.Calendar.MONDAY
        }
        val dayNames = (0..6).map { dow ->
            val cal = (baseCal.clone() as java.util.Calendar).apply { set(java.util.Calendar.DAY_OF_WEEK, dow + 1) }
            com.example.insightku.core.i18n.DateFormatter.getDayOfWeek(cal.timeInMillis)
        }.toTypedArray()
        val loudestDay = expenseTxns.groupBy {
            java.util.Calendar.getInstance().apply { timeInMillis = it.date }.get(java.util.Calendar.DAY_OF_WEEK) - 1
        }.maxByOrNull { it.value.sumOf { t -> t.amount } }?.key ?: return RhythmCaption("")
        return RhythmCaption(com.example.insightku.core.i18n.AnalyticsStrings.rhythmLoudest(dayNames[loudestDay]))
    }

    private fun deriveCategorySlices(txns: List<Transaction>): List<CategorySlice> = topCategorySlices(txns)

    private fun deriveBigDecisions(txns: List<Transaction>): List<BigDecision> {
        val expenseTxns = txns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        val total = expenseTxns.sumOf { it.amount }.coerceAtLeast(1.0)
        return expenseTxns.sortedByDescending { it.amount }
            .take(3)
            .map { BigDecision(it, (it.amount / total) * 100) }
    }

    private fun deriveStreak(allTxns: List<Transaction>, nowMillis: Long): StreakData {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val elapsed = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val period = AnalyticsPeriod.currentMonth(nowMillis)
        val monthTxns = allTxns.filter { period.contains(it.date) }
        val daysShownUp = monthTxns.map { dayKey(it.date) }.distinct().size
        return StreakData(daysShownUp, elapsed, elapsed - daysShownUp)
    }

    private fun deriveSpotlight(allTxns: List<Transaction>, nowMillis: Long): SpotlightData {
        val period = AnalyticsPeriod.currentMonth(nowMillis)
        val prevPeriod = AnalyticsPeriod.previousMonth(nowMillis)
        val monthTxns = allTxns.filter { period.contains(it.date) }
        val prevMonthTxns = allTxns.filter { prevPeriod.contains(it.date) }
        if (monthTxns.isEmpty()) return SpotlightData()

        val expenseMonth = monthTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        val expensePrev = prevMonthTxns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        val top = expenseMonth.maxByOrNull { it.amount } ?: return SpotlightData()
        val prevTotal = expensePrev.sumOf { it.amount }.coerceAtLeast(1.0)
        val curTotal = expenseMonth.sumOf { it.amount }.coerceAtLeast(1.0)
        val delta = ((curTotal - prevTotal) / prevTotal) * 100

        return SpotlightData(
            topExpenseName = top.title,
            topExpenseAmount = top.amount,
            expenseDeltaPct = delta
        )
    }

    private fun deriveMood(allTxns: List<Transaction>, nowMillis: Long): MoodData? {
        if (allTxns.isEmpty()) return null
        val points = (0..5).map { i ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(java.util.Calendar.MONTH, -5 + i)
            }
            val label = com.example.insightku.core.i18n.DateFormatter.getShortMonthName(cal.timeInMillis)
            val monthPeriod = AnalyticsPeriod.currentMonth(cal.timeInMillis)
            val total = allTxns.filter { monthPeriod.contains(it.date) }.sumOf { it.amount }
            MonthPoint(label, total)
        }
        val recent = points.takeLast(2).map { it.amount }
        val mood = when {
            recent.size < 2 -> SpendingMood.STEADY
            recent.last() > recent.first() * 1.2 -> SpendingMood.RESTLESS
            recent.last() < recent.first() * 0.8 -> SpendingMood.CALM
            else -> SpendingMood.STEADY
        }
        return MoodData(points, mood, "")
    }

    private fun deriveNoticing(txns: List<Transaction>): Noticing? {
        val expenseTxns = txns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
        if (expenseTxns.isEmpty()) return null
        val weekendRatio = expenseTxns.filter { isWeekend(it.date) }.sumOf { it.amount } / expenseTxns.sumOf { it.amount }.coerceAtLeast(1.0)
        return if (weekendRatio > 0.5) Noticing("night-owl", "🌙", com.example.insightku.core.i18n.AnalyticsStrings.noticingNightOwl())
        else null
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun calculateStreakDays(expenseTxns: List<Transaction>, nowMillis: Long): Int {
        if (expenseTxns.isEmpty()) return 0
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        var streak = 0
        for (i in 0..6) {
            val checkDate = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -i) }
            val key = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(checkDate.time)
            if (expenseTxns.any { dayKey(it.date) == key }) streak++ else break
        }
        return streak
    }

    private fun calculateHealthScore(balance: CashflowBalance, categories: List<CategoryRanking>): Int {
        var score = 50
        if (balance.isPositive) score += 20
        if (balance.savingsRate > 0.2) score += 15
        if (balance.savingsRate > 0.3) score += 10
        if (categories.size <= 5) score += 5  // diversified
        return score.coerceIn(0, 100)
    }

    private fun detectRecurring(txns: List<Transaction>): Double {
        // Simple heuristic: group by category + rounded amount, find recurring patterns
        return txns.filter { it.type == com.example.insightku.core.data.model.TransactionType.EXPENSE }
            .groupBy { "${it.category}_${(it.amount / 1000).toInt() * 1000}" }
            .filter { it.value.size >= 2 }
            .values
            .flatten()
            .distinctBy { it.id }
            .sumOf { it.amount }
    }

    private fun formatAmount(amount: Double): String {
        return com.example.insightku.core.i18n.NumberFormatter.formatCurrency(amount)
    }

    private fun isWeekend(timestamp: Long): Boolean {
        val dow = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }.get(java.util.Calendar.DAY_OF_WEEK)
        return dow == java.util.Calendar.SATURDAY || dow == java.util.Calendar.SUNDAY
    }

    private fun dayKey(timestamp: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestamp))
}
