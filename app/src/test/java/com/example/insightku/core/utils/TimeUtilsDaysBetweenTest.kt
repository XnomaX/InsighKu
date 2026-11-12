package com.example.insightku.core.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Regression tests for [TimeUtils.daysBetween].
 *
 * The bug these pin down: callers used to compute
 * `((due - now) / 86_400_000).toInt()`, and integer division truncates toward
 * zero. Because `nextDue` is midnight-normalised by the date picker while `now`
 * is an arbitrary clock reading, a payment due *tomorrow* computed to `0` and
 * was announced as due *today* — also forcing the wrong `PRIORITY_HIGH` branch
 * and tripping the "> 7 days" filter a day early. The same truncation made a
 * transaction from yesterday evening viewed shortly after midnight miss the
 * YESTERDAY grouping.
 *
 * Expected values are built from the host's own zone so the assertions hold on
 * any machine, while the scenarios themselves are fixed clock readings.
 */
class TimeUtilsDaysBetweenTest : FunSpec({

    val zone = ZoneId.systemDefault()

    // Fixed local dates so the scenarios are readable and stable.
    val BASE = java.time.LocalDate.of(2026, 11, 8)

    /** Epoch millis for [BASE] + [offset] days at [hour]:00, in the host zone. */
    fun day(offset: Int, hour: Int = 0): Long =
        ZonedDateTime.of(BASE.plusDays(offset.toLong()),
            java.time.LocalTime.of(hour, 0), zone).toInstant().toEpochMilli()

    test("payment due tomorrow at midnight, viewed at 14:00 today, is 1 day away") {
        // Old code: (10h / 24h).toInt() == 0  -> "due today" (wrong)
        TimeUtils.daysBetween(day(0, 14), day(1, 0)) shouldBe 1
    }

    test("payment due today at midnight, viewed at 14:00 today, is 0 days away") {
        // This case was already correct before the fix; kept so the fix cannot
        // over-correct into reporting tomorrow.
        TimeUtils.daysBetween(day(0, 14), day(0, 0)) shouldBe 0
    }

    test("payment due in 2, 3 and 4 days reports the true day count") {
        TimeUtils.daysBetween(day(0, 14), day(2, 0)) shouldBe 2
        TimeUtils.daysBetween(day(0, 14), day(3, 0)) shouldBe 3
        TimeUtils.daysBetween(day(0, 14), day(4, 0)) shouldBe 4
    }

    test("payment due 8 days away is not filtered as within 7 days") {
        // The reminder worker drops anything > 7 days; truncation used to make
        // 8 days read as 7 and send an early notification.
        val daysUntil = TimeUtils.daysBetween(day(0, 14), day(8, 0))
        daysUntil shouldBe 8
        (daysUntil > 7) shouldBe true
    }

    test("overdue payment stays negative regardless of the clock hour") {
        TimeUtils.daysBetween(day(0, 14), day(-1, 0)) shouldBe -1
        TimeUtils.daysBetween(day(0, 1), day(-1, 0)) shouldBe -1
        TimeUtils.daysBetween(day(0, 23), day(-2, 0)) shouldBe -2
    }

    test("same calendar day is always 0, whatever the hour gap") {
        TimeUtils.daysBetween(day(0, 0), day(0, 23)) shouldBe 0
        TimeUtils.daysBetween(day(0, 23), day(0, 0)) shouldBe 0
    }

    test("transaction from yesterday 23:00 viewed at 01:00 is 1 day old") {
        // Feeds getTxGroup: diffDays must be 1 so it lands on YESTERDAY rather
        // than falling through to THIS_WEEK.
        TimeUtils.daysBetween(day(-1, 23), day(0, 1)) shouldBe 1
    }

    test("transaction from yesterday 23:00 viewed at 23:00 is also 1 day old") {
        TimeUtils.daysBetween(day(-1, 23), day(0, 23)) shouldBe 1
    }

    test("today's transaction viewed late at night is 0 days old") {
        TimeUtils.daysBetween(day(0, 1), day(0, 23)) shouldBe 0
    }

    test("spanning a month and a year boundary counts calendar days") {
        val nov30 = ZonedDateTime.of(java.time.LocalDate.of(2026, 11, 30),
            java.time.LocalTime.of(22, 0), zone).toInstant().toEpochMilli()
        val dec1 = ZonedDateTime.of(java.time.LocalDate.of(2026, 12, 1),
            java.time.LocalTime.of(2, 0), zone).toInstant().toEpochMilli()
        TimeUtils.daysBetween(nov30, dec1) shouldBe 1

        val dec31 = ZonedDateTime.of(java.time.LocalDate.of(2026, 12, 31),
            java.time.LocalTime.of(23, 0), zone).toInstant().toEpochMilli()
        val jan1 = ZonedDateTime.of(java.time.LocalDate.of(2027, 1, 1),
            java.time.LocalTime.of(1, 0), zone).toInstant().toEpochMilli()
        TimeUtils.daysBetween(dec31, jan1) shouldBe 1
    }
})
