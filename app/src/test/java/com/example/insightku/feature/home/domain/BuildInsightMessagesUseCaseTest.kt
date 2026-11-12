package com.example.insightku.feature.home.domain

import com.example.insightku.R
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for [BuildInsightMessagesUseCase].
 *
 * The use case no longer returns finished sentences — it returns [InsightMessage]
 * descriptors that the UI resolves through `stringResource`, which is what lets the advice
 * follow the app language. That moves the contract from "what text" to "which resource id
 * plus which argument types", so these assert the selection logic and the argument shapes.
 *
 * Argument types matter and are not tautological: every advice key that takes a number
 * declares `%1$d` in both `values/strings.xml` and `values-id/strings.xml`. A `Double`
 * leaking into `args` would compile fine and throw at render time, so the Int assertions
 * below are the only thing standing between this refactor and a runtime crash.
 *
 * Each of the three branches (savings, spending ratio, streak) contributes at most one
 * message, and the list is capped at three.
 */
class BuildInsightMessagesUseCaseTest : FunSpec({

    val useCase = BuildInsightMessagesUseCase()

    test("no income asks the user to start logging income, and still reports a long streak") {
        val msgs = useCase(income = 0.0, expenses = 0.0, savings = 0.0, streak = 40)
        msgs.map { it.resId } shouldBe listOf(
            R.string.dashboard_advice_no_income,
            R.string.dashboard_advice_streak_30
        )
        msgs[0].args.shouldBeEmpty()
        msgs[1].args shouldBe listOf(40)
    }

    test("savings rate picks the excellent tier at exactly 30%") {
        val msgs = useCase(income = 100_000.0, expenses = 70_000.0, savings = 30_000.0, streak = 0)
        msgs.first().resId shouldBe R.string.dashboard_advice_saving_excellent
        msgs.first().args shouldBe listOf(30)
    }

    test("savings rate picks the on-track tier at exactly 10%, not excellent") {
        val msgs = useCase(income = 100_000.0, expenses = 90_000.0, savings = 10_000.0, streak = 0)
        msgs.first().resId shouldBe R.string.dashboard_advice_saving_on_track
        msgs.first().args shouldBe listOf(10)
    }

    test("negative savings reports overspending instead of a savings tier") {
        val msgs = useCase(income = 100_000.0, expenses = 150_000.0, savings = -50_000.0, streak = 0)
        msgs.first().resId shouldBe R.string.dashboard_advice_overspent
    }

    test("healthy-habits is the fallback when savings is positive but below 10%") {
        val msgs = useCase(income = 100_000.0, expenses = 95_000.0, savings = 5_000.0, streak = 0)
        msgs.first().resId shouldBe R.string.dashboard_advice_healthy_habits
    }

    test("a low spending ratio reports recurring payments under control") {
        // expenses/income = 0.5 < 0.7 and expenses < income, so only the ratio branch fires
        // on top of the savings and zero-streak messages.
        val msgs = useCase(income = 100_000.0, expenses = 50_000.0, savings = 50_000.0, streak = 0)
        msgs.map { it.resId } shouldBe listOf(
            R.string.dashboard_advice_saving_excellent,
            R.string.dashboard_advice_spending_ratio_ok,
            R.string.dashboard_advice_start_streak
        )
        msgs[0].args shouldBe listOf(50)
    }

    test("expenses above income ask for a recurring review, not a healthy ratio") {
        val msgs = useCase(income = 100_000.0, expenses = 150_000.0, savings = -50_000.0, streak = 0)
        val ids = msgs.map { it.resId }
        ids.contains(R.string.dashboard_advice_review_recurring) shouldBe true
        ids.contains(R.string.dashboard_advice_spending_ratio_ok) shouldBe false
    }

    test("streak tiers fire at their thresholds and carry the day count as an Int") {
        mapOf(
            30 to R.string.dashboard_advice_streak_30,
            7 to R.string.dashboard_advice_streak_7,
            3 to R.string.dashboard_advice_streak_3
        ).forEach { (days, resId) ->
            val msgs = useCase(income = 0.0, expenses = 0.0, savings = 0.0, streak = days)
            msgs.last().resId shouldBe resId
            msgs.last().args shouldBe listOf(days)
            msgs.last().args.single().shouldBeInstanceOf<Int>()
        }
    }

    test("zero streak prompts a first transaction, and 1-2 days produce no streak message") {
        useCase(0.0, 0.0, 0.0, streak = 0).last().resId shouldBe R.string.dashboard_advice_start_streak
        for (days in 1..2) {
            val msgs = useCase(income = 0.0, expenses = 0.0, savings = 0.0, streak = days)
            // Only the no-income message: the streak `when` has no branch for 1..2.
            msgs shouldHaveSize 1
            msgs.single().resId shouldBe R.string.dashboard_advice_no_income
        }
    }

    test("at most three messages are returned") {
        // All three branches fire at once: income > 0 with a high savings rate, expenses
        // below the ratio threshold, and a long streak.
        val msgs = useCase(income = 100_000.0, expenses = 50_000.0, savings = 50_000.0, streak = 60)
        msgs shouldHaveSize 3
    }

    test("the percentage argument is truncated, not rounded") {
        // 29.9% -> 29, so the excellent tier must not be claimed and the shown value must
        // match the branch actually taken.
        val msgs = useCase(income = 100_000.0, expenses = 70_100.0, savings = 29_900.0, streak = 0)
        msgs.first().resId shouldBe R.string.dashboard_advice_saving_on_track
        msgs.first().args shouldBe listOf(29)
    }
})
