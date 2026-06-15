package com.example.insightku.feature.analytics.presentation

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.analytics.domain.BehavioralPattern
import com.example.insightku.feature.analytics.domain.BigDecision
import com.example.insightku.feature.analytics.domain.ConsistencyDirection
import com.example.insightku.feature.analytics.domain.DayComparison
import com.example.insightku.feature.analytics.domain.HeatmapCell
import com.example.insightku.feature.analytics.domain.MonthPoint
import com.example.insightku.feature.analytics.domain.MoodData
import com.example.insightku.feature.analytics.domain.Noticing
import com.example.insightku.feature.analytics.domain.RhythmCaption
import com.example.insightku.feature.analytics.domain.SpendingMood
import com.example.insightku.feature.analytics.domain.SpendingPersonality
import com.example.insightku.feature.analytics.domain.SpotlightData
import com.example.insightku.feature.analytics.domain.StreakData
import com.example.insightku.core.ui.components.dialogs.CategoryIconInfo

/**
 * TEMPORARY — synthetic Analytics scenarios for the hidden debug/preview mode.
 *
 * Long-pressing the Analytics header cycles these so all states/edge cases can be inspected on a
 * real device before final polish. REMOVE this file, the debug events, [AnalyticsUiState.debugLabel],
 * and the header long-press wiring when the user asks to clean up.
 */
object AnalyticsDebugScenarios {

    private fun bubble(name: String, amount: Double, proportion: Float): CategoryBubble {
        val info = CategoryIconResolver.resolve(name)
        return CategoryBubble(name, amount, proportion, info.color, info.icon, topTransactions = listOf(
            Transaction(title = "$name purchase", amount = amount / 2, category = name, type = TransactionType.EXPENSE)
        ))
    }

    private fun selfCat(name: String, proportion: Float) =
        SelfCategoryUi(name, proportion, CategoryIconResolver.resolve(name).color)

    /** Build a confident Two Yous for previews. [divergence] drives the headline tier. */
    private fun twoYous(
        gapHeadline: String,
        divergence: Float,
        weekdayIntensity: Float,
        weekdayCats: List<SelfCategoryUi>,
        weekendIntensity: Float,
        weekendCats: List<SelfCategoryUi>
    ) = TwoYousUi(
        left = SpendingSelfUi("Weekday You", "😌", weekdayIntensity, weekdayCats),
        right = SpendingSelfUi("Weekend You", "🎧", weekendIntensity, weekendCats),
        divergence = divergence,
        gapHeadline = gapHeadline,
        isConfident = true
    )

    private fun tx(title: String, amount: Double, category: String) =
        Transaction(title = title, amount = amount, category = category, type = TransactionType.EXPENSE)

    private fun cell(dom: Int, dow: Int, week: Int, amount: Double, count: Int, cat: String?, time: String?, cmp: DayComparison) =
        HeatmapCell(dow, week, dom, amount, count, "Day $dom", cat, time, cmp)

    private val empty = AnalyticsUiState(
        isLoading = false, isEmpty = true, debugLabel = "1/6 · Empty (no data)"
    )

    private val low = AnalyticsUiState(
        isLoading = false,
        personality = SpendingPersonality("You spend in focused bursts, then go quiet", "🎯"),
        patterns = listOf(BehavioralPattern("🍃", "You had a calm stretch — 5 days in a row without spending.")),
        noticing = Noticing("early-month", "🌱", "You tend to spend earlier in the month — things get quieter as the weeks go on."),
        heatmapCells = listOf(
            cell(3, 2, 0, 45_000.0, 1, "Food & Drinks", "Evening", DayComparison.TYPICAL),
            cell(9, 1, 1, 120_000.0, 2, "Transport", "Morning", DayComparison.ABOVE)
        ),
        rhythm = RhythmCaption("Tuesdays are your loudest spending days"),
        categoryBubbles = listOf(bubble("Food & Drinks", 90_000.0, 1f), bubble("Transport", 60_000.0, 0.66f)),
        bigDecisions = listOf(BigDecision(tx("Bus pass", 120_000.0, "Transport"), 44.0)),
        streak = StreakData(daysShownUp = 3, daysElapsedThisMonth = 31, quietDays = 28, direction = ConsistencyDirection.NEW),
        spotlight = SpotlightData(topExpenseName = "Warung", topExpenseAmount = 90_000.0, expenseDeltaPct = null),
        mood = MoodData(
            listOf(MonthPoint("Apr", 0.0), MonthPoint("May", 165_000.0)),
            SpendingMood.STEADY, "Your mood shows up once you've logged a couple of months. ✨"
        ),
        debugLabel = "2/6 · Low activity"
    )

    private val high = AnalyticsUiState(
        isLoading = false,
        personality = SpendingPersonality("Weekends are where your money goes loud", "🎧"),
        twoYous = twoYous(
            gapHeadline = "Weekend You and Weekday You are basically two different people.",
            divergence = 0.72f,
            weekdayIntensity = 0.45f,
            weekdayCats = listOf(selfCat("Transport", 1f), selfCat("Food & Drinks", 0.7f), selfCat("Coffee & Cafes", 0.4f)),
            weekendIntensity = 1f,
            weekendCats = listOf(selfCat("Food & Drinks", 1f), selfCat("Entertainment", 0.8f), selfCat("Shopping", 0.6f))
        ),
        patterns = listOf(
            BehavioralPattern("🕓", "Your spending's been shifting toward the morning — it leaned evening last month."),
            BehavioralPattern("🌙", "About half your spending lands on weekends."),
            BehavioralPattern("🔁", "Food & Drinks has been a regular — it showed up on 14 different days.")
        ),
        noticing = Noticing("night-owl", "🌙", "A good slice of your spending happens at night — the late hours add up more than they feel like."),
        heatmapCells = buildList {
            var dom = 1
            for (w in 0..4) for (d in 0..6) {
                if (dom > 28) break
                add(cell(dom, d, w, (d + 1) * 35_000.0, (d % 3) + 1,
                    listOf("Food & Drinks", "Transport", "Shopping").random(),
                    listOf("Morning", "Evening", "Night").random(),
                    if (d >= 5) DayComparison.ABOVE else DayComparison.TYPICAL))
                dom++
            }
        },
        rhythm = RhythmCaption("Saturdays are your loudest spending days"),
        categoryBubbles = listOf(
            bubble("Food & Drinks", 1_200_000.0, 1f),
            bubble("Transport", 600_000.0, 0.5f),
            bubble("Shopping", 480_000.0, 0.4f),
            bubble("Entertainment", 300_000.0, 0.25f),
            bubble("Coffee & Cafes", 180_000.0, 0.15f)
        ),
        bigDecisions = listOf(
            BigDecision(tx("New headphones", 800_000.0, "Shopping"), 31.0),
            BigDecision(tx("Concert ticket", 450_000.0, "Entertainment"), 17.0),
            BigDecision(tx("Monthly groceries", 420_000.0, "Groceries"), 16.0)
        ),
        streak = StreakData(daysShownUp = 24, daysElapsedThisMonth = 31, quietDays = 7, direction = ConsistencyDirection.UP, prevDaysShownUp = 19),
        spotlight = SpotlightData(
            topExpenseName = "GoFood", topExpenseAmount = 540_000.0, expenseDeltaPct = 23.0,
            topIncomeName = "Salary", topIncomeAmount = 5_000_000.0, incomeDeltaPct = 0.0
        ),
        mood = MoodData(
            listOf("Dec", "Jan", "Feb", "Mar", "Apr", "May").mapIndexed { idx, m -> MonthPoint(m, (6 - idx) * 450_000.0) },
            SpendingMood.RESTLESS, "A little more restless lately — spending's been running warmer. 🌧️"
        ),
        debugLabel = "3/6 · High activity"
    )

    private val calmMood = high.copy(
        personality = SpendingPersonality("You keep a steady, easy rhythm with your money", "🌊"),
        mood = MoodData(
            listOf("Dec", "Jan", "Feb", "Mar", "Apr", "May").mapIndexed { idx, m -> MonthPoint(m, (6 - idx) * 300_000.0) },
            SpendingMood.CALM, "Calmer than usual — your recent spending has been easing off. 🌤️"
        ),
        debugLabel = "4/6 · Calm mood + steady personality"
    )

    private val singleCategory = AnalyticsUiState(
        isLoading = false,
        personality = SpendingPersonality("Food & Drinks has been your happy place this month", "💜"),
        twoYous = twoYous(
            gapHeadline = "Turns out you're pretty much the same person all week.",
            divergence = 0.12f,
            weekdayIntensity = 0.95f,
            weekdayCats = listOf(selfCat("Food & Drinks", 1f), selfCat("Transport", 0.2f)),
            weekendIntensity = 1f,
            weekendCats = listOf(selfCat("Food & Drinks", 1f), selfCat("Transport", 0.18f))
        ),
        patterns = listOf(BehavioralPattern("🔁", "Food & Drinks has been a regular — it showed up on 20 different days.")),
        heatmapCells = high.heatmapCells.map { it.copy(topCategory = "Food & Drinks") },
        rhythm = RhythmCaption("Fridays are your loudest spending days"),
        categoryBubbles = listOf(bubble("Food & Drinks", 2_000_000.0, 1f), bubble("Transport", 120_000.0, 0.06f)),
        bigDecisions = high.bigDecisions.take(1),
        streak = StreakData(daysShownUp = 20, daysElapsedThisMonth = 31, quietDays = 11),
        spotlight = SpotlightData(topExpenseName = "GoFood", topExpenseAmount = 1_400_000.0, expenseDeltaPct = 60.0),
        mood = calmMood.mood,
        debugLabel = "5/6 · Edge: one dominant category"
    )

    private val unusual = AnalyticsUiState(
        isLoading = false,
        personality = SpendingPersonality("You spend in focused bursts, then go quiet", "🎯"),
        patterns = listOf(
            BehavioralPattern("🍃", "You had a calm stretch — 12 days in a row without spending."),
            BehavioralPattern("📉", "You've been spending lighter than last month.")
        ),
        heatmapCells = listOf(
            cell(15, 1, 2, 3_500_000.0, 1, "Electronics", "Afternoon", DayComparison.ABOVE)
        ),
        rhythm = RhythmCaption("Wednesdays are your loudest spending days"),
        categoryBubbles = listOf(bubble("Electronics", 3_500_000.0, 1f)),
        bigDecisions = listOf(BigDecision(tx("Laptop", 3_500_000.0, "Electronics"), 100.0)),
        streak = StreakData(daysShownUp = 1, daysElapsedThisMonth = 31, quietDays = 30),
        spotlight = SpotlightData(topExpenseName = "Laptop", topExpenseAmount = 3_500_000.0, expenseDeltaPct = null),
        mood = MoodData(
            listOf(MonthPoint("Apr", 200_000.0), MonthPoint("May", 3_500_000.0)),
            SpendingMood.RESTLESS, "A little more restless lately — spending's been running warmer. 🌧️"
        ),
        debugLabel = "6/6 · Edge: single huge purchase"
    )

    // Ordered so the FIRST tap lands on a rich Two Yous (high divergence), then the low-divergence
    // "same person" case, then the rest. Empty/sparse states are last so the preview never opens blank.
    val all: List<AnalyticsUiState> = listOf(high, singleCategory, calmMood, unusual, low, empty)
}





