package com.example.insightku.domain.usecase.analytics

import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Tests for [InsightEngine.twoYous] — the "Two Yous" centerpiece. The most important guard is the
 * per-day normalization (the "trap"): weekdays outnumber weekend days ~5:2, so comparing raw totals
 * would always inflate the gap. These tests pin a fixed reference month so weekday/weekend dates are
 * deterministic regardless of when the suite runs.
 */
class InsightEngineTwoYousTest {

    private val engine = InsightEngine()

    // Reference month: May 2026. The 1st is a Friday; weekends fall on 2,3,9,10,16,17,23,24,30,31.
    private val refYear = 2026
    private val refMonth = Calendar.MAY

    /** Epoch millis for a given day-of-month at noon in the reference month. */
    private fun dayMillis(dayOfMonth: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(refYear, refMonth, dayOfMonth, hour, 0, 0)
        }.timeInMillis

    /** "now" = end of the reference month, so the whole month counts as elapsed. */
    private val now: Long = dayMillis(31, hour = 23)

    private fun expense(day: Int, amount: Double, category: String = "Food", hour: Int = 12) =
        Transaction(
            title = category,
            amount = amount,
            category = category,
            date = dayMillis(day, hour),
            type = TransactionType.EXPENSE
        )

    private val weekdays = listOf(4, 5, 6, 7, 8)        // Mon–Fri of week 2
    private val weekendDays = listOf(2, 3, 9, 10)        // Saturdays/Sundays

    @Test
    fun `empty input is not confident and does not throw`() {
        val result = engine.twoYous(emptyList(), now)
        assertFalse(result.isConfident)
        assertTrue(result.divergence in 0f..1f)
    }

    @Test
    fun `sparse data (too few on one side) is not confident`() {
        // Plenty of weekday data, but only one weekend transaction → cannot honestly compare.
        val txns = weekdays.map { expense(it, 50_000.0) } + listOf(expense(2, 50_000.0))
        val result = engine.twoYous(txns, now)
        assertFalse(result.isConfident)
    }

    @Test
    fun `divergence is always within 0 to 1`() {
        val txns = weekdays.map { expense(it, 30_000.0, "Transport") } +
            weekendDays.map { expense(it, 900_000.0, "Entertainment") }
        val result = engine.twoYous(txns, now)
        assertTrue("divergence=${result.divergence}", result.divergence in 0f..1f)
    }

    @Test
    fun `right self is always the weekend self (fixed valence)`() {
        val txns = weekdays.map { expense(it, 40_000.0) } + weekendDays.map { expense(it, 40_000.0) }
        val result = engine.twoYous(txns, now)
        assertEquals("Weekday You", result.left.label)
        assertEquals("Weekend You", result.right.label)
    }

    @Test
    fun `TRAP - equal per-day spend yields low divergence despite unequal totals`() {
        // One identical transaction on EVERY day of the month, same category. There are 21 weekdays
        // and 10 weekend days in May 2026, so the weekday TOTAL is ~2x the weekend total — but the
        // per-day average is identical. An honest engine sees the same person; a naive totals-based
        // one would invent a big gap. This is the core normalization guard.
        val perDay = 100_000.0
        val txns = (1..31).map { expense(it, perDay, "Food") }
        val result = engine.twoYous(txns, now)
        assertTrue(result.isConfident)
        assertTrue(
            "expected low divergence for identical per-day behavior, got ${result.divergence}",
            result.divergence < 0.3f
        )
        assertTrue(result.gapHeadline.contains("same person"))
    }

    @Test
    fun `weekend-heavy per-day spend yields high divergence and weekend-leaning headline`() {
        // Weekend per-day spend dwarfs weekday, and the category mix differs → high divergence.
        val txns = weekdays.map { expense(it, 20_000.0, "Transport") } +
            weekendDays.map { expense(it, 600_000.0, "Entertainment") }
        val result = engine.twoYous(txns, now)
        assertTrue(result.isConfident)
        assertTrue("got ${result.divergence}", result.divergence >= 0.6f)
        assertTrue(result.gapHeadline.isNotBlank())
    }

    @Test
    fun `derive never throws for arbitrary input`() {
        val mixed = listOf(
            expense(1, 10_000.0),
            expense(2, 0.0),
            Transaction(amount = 5_000.0, date = dayMillis(15), type = TransactionType.INCOME)
        )
        // Should not throw, and twoYous is always populated.
        val insights = engine.derive(mixed, now)
        assertTrue(insights.twoYous.divergence in 0f..1f)
    }
}
