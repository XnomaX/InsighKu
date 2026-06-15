package com.example.insightku.feature.analytics.domain

import com.example.insightku.core.data.model.Transaction
import javax.inject.Inject

// ── Personality ───────────────────────────────────────────────────────────────

data class SpendingPersonality(val headline: String, val emoji: String) {
    val summaryLine: String get() = headline
    val sentence: String get() = headline
    companion object {
        val ONBOARDING = SpendingPersonality("Keep logging — insights appear after a few transactions", "🌱")
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
    // Aliases used by AnalyticsSections UI
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
        SpendingMood.CALM -> "Calmer lately 🌤️"
        SpendingMood.STEADY -> "Staying steady 🌊"
        SpendingMood.RESTLESS -> "A little restless 🌧️"
    }
    val summaryLine: String get() = caption.ifBlank { headline }
}

// ── Analytics Insights (output of InsightEngine) ──────────────────────────────

data class AnalyticsInsights(
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
    val mood: MoodData?
)

class InsightEngine @Inject constructor() {

    fun derive(transactions: List<Transaction>, nowMillis: Long): AnalyticsInsights {
        val period = AnalyticsPeriod.currentMonth(nowMillis)
        val monthTxns = transactions.filter { period.contains(it.date) }

        val personality = derivePersonality(monthTxns)
        val twoYous = deriveTwoYous(monthTxns)
        val patterns = derivePatterns(monthTxns, transactions, nowMillis)
        val heatmap = deriveHeatmap(monthTxns, nowMillis)
        val rhythm = deriveRhythm(monthTxns)
        val slices = deriveCategorySlices(monthTxns)
        val bigDecisions = deriveBigDecisions(monthTxns)
        val streak = deriveStreak(transactions, nowMillis)
        val spotlight = deriveSpotlight(transactions, nowMillis)
        val mood = deriveMood(transactions, nowMillis)
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
            mood = mood
        )
    }

    private fun derivePersonality(txns: List<Transaction>): SpendingPersonality {
        if (txns.isEmpty()) return SpendingPersonality.ONBOARDING
        val weekendTxns = txns.filter { isWeekend(it.date) }
        val weekendRatio = weekendTxns.sumOf { it.amount } / txns.sumOf { it.amount }.coerceAtLeast(1.0)
        return when {
            weekendRatio > 0.6 -> SpendingPersonality("Weekends are where your money goes loud", "🎧")
            weekendRatio < 0.3 -> SpendingPersonality("You spend in focused bursts during the week", "🎯")
            else -> SpendingPersonality("You keep a steady, easy rhythm with your money", "🌊")
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
            divergence > 0.5 -> "Weekend You and Weekday You are basically two different people."
            divergence < 0.15 -> "Turns out you're pretty much the same person all week."
            else -> "There's a gap between your weekday and weekend spending."
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
        val total = txns.sumOf { it.amount }.coerceAtLeast(1.0)
        return txns.groupBy { it.category }
            .map { (cat, t) -> cat to t.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(3)
            .map { (cat, amt) -> CategorySlice(cat, (amt / total).toFloat()) }
    }

    private fun derivePatterns(monthTxns: List<Transaction>, allTxns: List<Transaction>, nowMillis: Long): List<BehavioralPattern> {
        val patterns = mutableListOf<BehavioralPattern>()
        val weekendRatio = if (monthTxns.isEmpty()) 0.0 else
            monthTxns.filter { isWeekend(it.date) }.sumOf { it.amount } / monthTxns.sumOf { it.amount }
        if (weekendRatio > 0.45) patterns.add(BehavioralPattern("🌙", "About half your spending lands on weekends."))

        val topCat = monthTxns.groupBy { it.category }.maxByOrNull { it.value.sumOf { t -> t.amount } }
        if (topCat != null) {
            val days = topCat.value.map { dayKey(it.date) }.distinct().size
            if (days >= 8) patterns.add(BehavioralPattern("🔁", "${topCat.key} has been a regular — it showed up on $days different days."))
        }
        return patterns.take(3)
    }

    private fun deriveHeatmap(txns: List<Transaction>, nowMillis: Long): List<HeatmapCell> {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val monthAvg = if (txns.isEmpty()) 1.0 else txns.sumOf { it.amount } / txns.map { dayKey(it.date) }.distinct().size.coerceAtLeast(1)
        return txns.groupBy { dayKey(it.date) }.map { (_, dayTxns) ->
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
        if (txns.isEmpty()) return RhythmCaption("")
        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val loudestDay = txns.groupBy {
            java.util.Calendar.getInstance().apply { timeInMillis = it.date }.get(java.util.Calendar.DAY_OF_WEEK) - 1
        }.maxByOrNull { it.value.sumOf { t -> t.amount } }?.key ?: return RhythmCaption("")
        return RhythmCaption("${dayNames[loudestDay]}s are your loudest spending days")
    }

    private fun deriveCategorySlices(txns: List<Transaction>): List<CategorySlice> = topCategorySlices(txns)

    private fun deriveBigDecisions(txns: List<Transaction>): List<BigDecision> {
        val total = txns.sumOf { it.amount }.coerceAtLeast(1.0)
        return txns.sortedByDescending { it.amount }
            .take(3)
            .map { BigDecision(it, (it.amount / total) * 100) }
    }

    private fun deriveStreak(allTxns: List<Transaction>, nowMillis: Long): StreakData {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val period = AnalyticsPeriod.currentMonth(nowMillis)
        val monthTxns = allTxns.filter { period.contains(it.date) }
        val daysShownUp = monthTxns.map { dayKey(it.date) }.distinct().size
        val elapsed = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return StreakData(daysShownUp, elapsed, elapsed - daysShownUp)
    }

    private fun deriveSpotlight(allTxns: List<Transaction>, nowMillis: Long): SpotlightData {
        val period = AnalyticsPeriod.currentMonth(nowMillis)
        val monthTxns = allTxns.filter { period.contains(it.date) }
        if (monthTxns.isEmpty()) return SpotlightData()
        val top = monthTxns.maxByOrNull { it.amount } ?: return SpotlightData()
        return SpotlightData(topExpenseName = top.title, topExpenseAmount = top.amount)
    }

    private fun deriveMood(allTxns: List<Transaction>, nowMillis: Long): MoodData? {
        if (allTxns.isEmpty()) return null
        val points = (0..5).map { i ->
            val period = AnalyticsPeriod.trailingMonths(nowMillis, 6)
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = period.startMs
                add(java.util.Calendar.MONTH, i)
            }
            val label = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(cal.time)
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
        if (txns.isEmpty()) return null
        val weekendRatio = txns.filter { isWeekend(it.date) }.sumOf { it.amount } / txns.sumOf { it.amount }.coerceAtLeast(1.0)
        return if (weekendRatio > 0.5) Noticing("night-owl", "🌙", "A good slice of your spending happens on weekends — the days off add up more than they feel like.")
        else null
    }

    private fun isWeekend(timestamp: Long): Boolean {
        val dow = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }.get(java.util.Calendar.DAY_OF_WEEK)
        return dow == java.util.Calendar.SATURDAY || dow == java.util.Calendar.SUNDAY
    }

    private fun dayKey(timestamp: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}
