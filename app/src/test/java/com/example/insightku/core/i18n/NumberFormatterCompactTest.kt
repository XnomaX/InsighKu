package com.example.insightku.core.i18n

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.util.Locale

/**
 * Tests for [NumberFormatter.formatCurrencyCompact], [NumberFormatter.formatCurrency],
 * [NumberFormatter.getCurrencySymbol] and [LocaleHelper.isIndonesianCode].
 *
 * These pin the defect that reached a user's device: one goal row rendered "IDR0 / Rp200 Rb"
 * — a bare currency code beside a Rupiah symbol, in a single language. The cause was two
 * branches with different symbol rules: amounts under 1000 delegated to CLDR, which renders
 * IDR as the code "IDR" under an English locale, while the compact branch hardcoded "Rp".
 *
 * The rule now, per the user's decision: the glyph is always the currency's own symbol
 * ("Rp" for IDR) in every language — no locale may put a bare code in front of an amount.
 * What follows the language is the digit grouping and the decimal separator. IDR keeps its
 * own units (Rb/Jt/M/T) and their spacing in both languages, as it always has in this app;
 * other currencies follow the locale's unit vocabulary.
 *
 * Every assertion passes an explicit locale, so nothing here touches `Locale.getDefault()`
 * and the spec stays pure JVM.
 */
class NumberFormatterCompactTest : FunSpec({

    val en = Locale.ENGLISH
    val id = Locale.forLanguageTag("id")

    test("the symbol is the currency's own in every language, small and large alike") {
        // The reported defect: these two used to disagree ("IDR0" vs "Rp200 Rb").
        NumberFormatter.getCurrencySymbol("IDR") shouldBe "Rp"
        for (locale in listOf(en, id)) {
            NumberFormatter.formatCurrencyCompact(0.0, "IDR", locale) shouldStartWith "Rp"
            NumberFormatter.formatCurrencyCompact(200_000.0, "IDR", locale) shouldStartWith "Rp"
            NumberFormatter.formatCurrency(1_234.0, "IDR", locale) shouldStartWith "Rp"
        }
    }

    test("English compact keeps IDR units with English separators") {
        NumberFormatter.formatCurrencyCompact(0.0, "IDR", en) shouldBe "Rp0"
        NumberFormatter.formatCurrencyCompact(200_000.0, "IDR", en) shouldBe "Rp200 Rb"
        NumberFormatter.formatCurrencyCompact(9_800_000.0, "IDR", en) shouldBe "Rp9.8 Jt"
    }

    test("Indonesian compact keeps IDR units with Indonesian separators") {
        NumberFormatter.formatCurrencyCompact(0.0, "IDR", id) shouldBe "Rp0"
        NumberFormatter.formatCurrencyCompact(200_000.0, "IDR", id) shouldBe "Rp200 Rb"
        NumberFormatter.formatCurrencyCompact(9_800_000.0, "IDR", id) shouldBe "Rp9,8 Jt"
    }

    test("non-IDR currencies follow the locale's unit vocabulary") {
        NumberFormatter.formatCurrencyCompact(200_000.0, "USD", en) shouldBe "$200K"
        NumberFormatter.formatCurrencyCompact(200_000.0, "USD", id) shouldBe "$200 Rb"
    }

    test("the full formatter groups digits per locale under one symbol") {
        NumberFormatter.formatCurrency(1_234.0, "IDR", en) shouldBe "Rp1,234"
        NumberFormatter.formatCurrency(1_234.0, "IDR", id) shouldBe "Rp1.234"
    }

    test("negative amounts keep their sign in both branches") {
        // Contribution sheets colour negatives red; a dropped sign would read as income.
        NumberFormatter.formatCurrencyCompact(-500.0, "IDR", id) shouldBe "-Rp500"
        NumberFormatter.formatCurrencyCompact(-200_000.0, "IDR", id) shouldBe "-Rp200 Rb"
        NumberFormatter.formatCurrencyCompact(-200_000.0, "IDR", en) shouldBe "-Rp200 Rb"
    }

    test("the legacy Indonesian code still means Indonesian") {
        // On API <= 34 a device set to Indonesian can report "in"; every language gate goes
        // through this predicate, so dropping either code silently renders English dates,
        // units and insight copy inside an Indonesian UI. This is the one part of the
        // legacy-code handling a JVM test can actually prove — `Locale` constructors convert
        // codes at construction, so the object-level path cannot be exercised here.
        LocaleHelper.isIndonesianCode("id") shouldBe true
        LocaleHelper.isIndonesianCode("in") shouldBe true
        LocaleHelper.isIndonesianCode("en") shouldBe false
        LocaleHelper.isIndonesian(id) shouldBe true
        LocaleHelper.isIndonesian(en) shouldBe false
    }
})
