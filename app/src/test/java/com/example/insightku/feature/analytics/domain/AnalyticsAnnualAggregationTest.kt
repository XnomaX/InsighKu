package com.example.insightku.feature.analytics.domain

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

class AnalyticsAnnualAggregationTest : FunSpec({
    fun millis(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, 12, 0, 0)
    }.timeInMillis

    fun transaction(date: Long, type: TransactionType, amount: Double, category: String = "General") =
        Transaction(date = date, type = type, amount = amount, category = category)

    test("annual aggregation buckets transactions by their actual month near month end") {
        val now = millis(2025, Calendar.DECEMBER, 31)
        val transactions = listOf(
            transaction(millis(2025, Calendar.JANUARY, 31), TransactionType.INCOME, 1_000.0),
            transaction(millis(2025, Calendar.JANUARY, 31), TransactionType.EXPENSE, 300.0, "Food"),
            transaction(millis(2025, Calendar.FEBRUARY, 1), TransactionType.EXPENSE, 200.0, "Bills"),
            transaction(millis(2025, Calendar.NOVEMBER, 15), TransactionType.INCOME, 400.0),
            transaction(millis(2025, Calendar.DECEMBER, 2), TransactionType.EXPENSE, 99.0, "Travel"),
            transaction(millis(2024, Calendar.DECEMBER, 15), TransactionType.INCOME, 500.0),
            transaction(millis(2024, Calendar.DECEMBER, 20), TransactionType.EXPENSE, 50.0, "Food"),
            transaction(millis(2025, Calendar.APRIL, 1), TransactionType.TRANSFER_IN, 900.0)
        )

        val insights = InsightEngine().derive(transactions, now, AnalyticsPeriodType.ANNUAL)
        val growth = requireNotNull(insights.annualGrowth)
        val months = growth.monthlyPoints.associateBy { it.monthIndex }

        months.getValue(Calendar.JANUARY).income shouldBe 1_000.0
        months.getValue(Calendar.JANUARY).expense shouldBe 300.0
        months.getValue(Calendar.FEBRUARY).expense shouldBe 200.0
        months.getValue(Calendar.NOVEMBER).income shouldBe 400.0
        months.getValue(Calendar.DECEMBER).expense shouldBe 99.0
        growth.monthlyPoints.map { it.monthLabel }.distinct().size shouldBe 12

        val comparison = requireNotNull(growth.yearComparison)
        comparison.thisYearTotal shouldBe 599.0
        comparison.lastYearTotal shouldBe 50.0
        comparison.thisYearIncome shouldBe 1_400.0
        comparison.lastYearIncome shouldBe 500.0
    }
})
