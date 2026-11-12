package com.example.insightku.core.i18n

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.Locale

/**
 * Tests for [DateFormatter.getNarrowDayName], the single-letter day labels used by the
 * streak card's two weekday strips.
 *
 * Two things are pinned here, both invisible to a green build:
 *
 *  1. **Derivation.** The value is `getShortDayName(...).take(1)`, *not* the `EEEEE` narrow
 *     pattern. `java.text.SimpleDateFormat` does not implement narrow day names: on the JVM
 *     `EEEEE` and `EEEEEE` both return the **full** name ("Senin", "Monday"), so a
 *     "simplification" to that pattern compiles, passes no test, and renders full day names
 *     inside 34dp cells. The English assertion below is byte-identical to the list that was
 *     previously hardcoded in `DashboardStreak`, so this also proves English behaviour is
 *     unchanged by the localization work.
 *
 *  2. **Ordering.** Both strips are Monday-first: `activeDays = minOf(currentStreak, 7)` fills
 *     index 0 first, and the history grid indexes cells by `(DAY_OF_WEEK + 5) % 7`. The
 *     expected sequences are therefore written out explicitly rather than generated, so a
 *     switch to a Sunday-first build (which `set(DAY_OF_WEEK, dow + 1)` produces, since
 *     `firstDayOfWeek` affects `get` but not `set`) fails here instead of silently shifting
 *     which day each cell claims to be.
 */
class DateFormatterNarrowDayTest : FunSpec({

    /** Monday..Sunday timestamps, built by stepping forward from a real Monday. */
    fun mondayFirstTimestamps(): List<Long> {
        val monday = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return (0..6).map { offset ->
            (monday.clone() as Calendar)
                .apply { add(Calendar.DAY_OF_YEAR, offset) }
                .timeInMillis
        }
    }

    test("English narrow day names are Monday-first and match the previously hardcoded strip") {
        val labels = mondayFirstTimestamps().map {
            DateFormatter.getNarrowDayName(it, Locale.forLanguageTag("en"))
        }
        // Mon Tue Wed Thu Fri Sat Sun
        labels shouldBe listOf("M", "T", "W", "T", "F", "S", "S")
    }

    test("Indonesian narrow day names are Monday-first and localized") {
        val labels = mondayFirstTimestamps().map {
            DateFormatter.getNarrowDayName(it, Locale.forLanguageTag("id"))
        }
        // Senin Selasa Rabu Kamis Jumat Sabtu Minggu
        labels shouldBe listOf("S", "S", "R", "K", "J", "S", "M")
    }

    test("narrow names are a single character in both supported languages") {
        // Guards the 34dp cell: anything longer than one grapheme overflows the streak strip.
        for (tag in listOf("en", "id")) {
            val locale = Locale.forLanguageTag(tag)
            mondayFirstTimestamps().forEach { ts ->
                val narrow = DateFormatter.getNarrowDayName(ts, locale)
                (narrow.length == 1) shouldBe true
                narrow shouldBe DateFormatter.getShortDayName(ts, locale).take(1)
            }
        }
    }

    test("narrow names differ across locales for the same instant") {
        // Proves the locale parameter is actually honoured rather than defaulting to
        // Locale.getDefault() at the call site — the system-locale trap documented in
        // ForecastExpenseUseCase.
        val timestamps = mondayFirstTimestamps()
        val en = timestamps.map { DateFormatter.getNarrowDayName(it, Locale.forLanguageTag("en")) }
        val id = timestamps.map { DateFormatter.getNarrowDayName(it, Locale.forLanguageTag("id")) }
        (en != id) shouldBe true
        // Wednesday is "W" in English but "R" (Rabu) in Indonesian — the sharpest difference.
        en[2] shouldBe "W"
        id[2] shouldBe "R"
    }
})
