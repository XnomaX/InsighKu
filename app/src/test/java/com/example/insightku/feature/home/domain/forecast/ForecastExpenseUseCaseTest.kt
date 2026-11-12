package com.example.insightku.feature.home.domain.forecast

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.home.data.forecast.ForecastRemoteDataSource
import com.example.insightku.feature.home.data.forecast.ForecastRepository
import com.example.insightku.feature.home.data.forecast.ForecastTransactionRow
import com.example.insightku.feature.home.data.receipt.NetworkMonitor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

/**
 * Tests for [ForecastExpenseUseCase]: the mapping from a repository outcome to
 * chart-ready bars, and the honesty guarantees around refusals.
 *
 * Label text is locale-dependent (`SimpleDateFormat` on `Locale.getDefault()`),
 * so these assert structure and values — not exact weekday strings — to stay
 * deterministic across hosts.
 */
class ForecastExpenseUseCaseTest : FunSpec({

    val origin = 1_731_000_000_000L // any fixed epoch millis; only the offsets matter
    val repo = mockk<ForecastRepository>()
    val useCase = ForecastExpenseUseCase(repo)

    test("ok outcome maps 28 daily values to 7 weekly bars and 4 weekly buckets") {
        val profile = (1..28).map { it * 1000.0 }
        coEvery { repo.forecast(any(), any(), any()) } returns ForecastOutcome.Success(
            ForecastResult(
                total7 = profile.take(7).sum(),
                total28 = profile.sum(),
                dailyProfile = profile,
                observedDays = 90,
                scale = 1.0,
                modelStatus = "research_not_validated_on_idr",
                origin = "2026-11-08"
            )
        )

        val ui = useCase("acc", origin, emptyList())

        ui.weeklyBars shouldHaveSize 7
        ui.monthlyBars shouldHaveSize 4
        ui.notice shouldBe null

        // weekly bars are the first 7 daily values, in order
        ui.weeklyBars.map { it.value } shouldBe profile.take(7).map { it.toFloat() }

        // each monthly bucket is the sum of its 7-day slice
        ui.monthlyBars[0].value shouldBe profile.subList(0, 7).sum().toFloat()
        ui.monthlyBars[3].value shouldBe profile.subList(21, 28).sum().toFloat()

        // totals carried through verbatim
        ui.weeklyTotal shouldBe profile.take(7).sum()
        ui.monthlyTotal shouldBe profile.sum()
        ui.observedDays shouldBe 90
        ui.modelStatus shouldBe "research_not_validated_on_idr"

        // every bar carries a non-blank, date-derived label
        (ui.weeklyBars + ui.monthlyBars).forEach { it.label.isNotBlank() shouldBe true }
    }

    test("an ok outcome is never fabricated as zero when the total is positive") {
        val profile = List(28) { 500.0 }
        coEvery { repo.forecast(any(), any(), any()) } returns ForecastOutcome.Success(
            ForecastResult(7 * 500.0, 28 * 500.0, profile, 30, 1.0, "research_not_validated_on_idr", "2026-11-08")
        )
        val ui = useCase("acc", origin, emptyList())
        ui.isEmpty shouldBe false
        ui.insight.isNotBlank() shouldBe true
    }

    test("a short profile is refused, not padded with invented zeros") {
        coEvery { repo.forecast(any(), any(), any()) } returns ForecastOutcome.Success(
            ForecastResult(0.0, 0.0, List(10) { 1.0 }, 10, 1.0, "research_not_validated_on_idr", "2026-11-08")
        )
        val ui = useCase("acc", origin, emptyList())

        ui.weeklyBars.shouldBeEmpty()
        ui.monthlyBars.shouldBeEmpty()
        ui.notice.shouldBeInstanceOf<ForecastNotice.Error>()
        ui.insight shouldBe ""
    }

    test("a model refusal stays a refusal — empty bars, no error, blank insight") {
        coEvery { repo.forecast(any(), any(), any()) } returns ForecastOutcome.Refused(
            reason = "insufficient_history", detail = "need 7 days, got 3", observedDays = 3
        )
        val ui = useCase("acc", origin, emptyList())

        ui.weeklyBars.shouldBeEmpty()
        ui.notice.shouldBeInstanceOf<ForecastNotice.Refused>().let {
            it.reason shouldBe "insufficient_history"
            it.detail shouldBe "need 7 days, got 3"
        }
        ui.insight shouldBe ""
    }

    test("offline maps to an Offline notice with no bars") {
        coEvery { repo.forecast(any(), any(), any()) } returns ForecastOutcome.Offline
        val ui = useCase("acc", origin, emptyList())
        ui.notice shouldBe ForecastNotice.Offline
        ui.weeklyBars.shouldBeEmpty()
        ui.insight shouldBe ""
    }

    test("transport error carries its message and draws nothing") {
        coEvery { repo.forecast(any(), any(), any()) } returns ForecastOutcome.Error("401 unauthorized")
        val ui = useCase("acc", origin, emptyList())
        ui.notice.shouldBeInstanceOf<ForecastNotice.Error>().message shouldBe "401 unauthorized"
        ui.weeklyBars.shouldBeEmpty()
    }
})

/**
 * Tests for [ForecastRepository]'s request shaping, which is where the
 * honesty/leakage guarantees actually live: only positive EXPENSE rows are
 * sent, future rows are dropped, and an empty selection short-circuits to a
 * refusal rather than a 400 from the server.
 */
class ForecastRepositoryTest : FunSpec({

    val day = 86_400_000L
    val origin = 1_731_000_000_000L

    fun tx(amount: Double, type: TransactionType, dateOffsetDays: Long) = Transaction(
        amount = amount, type = type, date = origin - dateOffsetDays * day
    )

    fun onlineMonitor(): NetworkMonitor = mockk<NetworkMonitor>().apply {
        every { isOnline() } returns true
    }

    test("offline short-circuits before any network call") {
        val remote = mockk<ForecastRemoteDataSource>()
        val off = mockk<NetworkMonitor> { every { isOnline() } returns false }
        val repo = ForecastRepository(remote, off)

        repo.forecast("a", origin, listOf(tx(1000.0, TransactionType.EXPENSE, 1))) shouldBe ForecastOutcome.Offline
        coVerify(exactly = 0) { remote.forecast(any(), any(), any()) }
    }

    test("only positive EXPENSE rows within the window are sent; income, future, and stale rows dropped") {
        val rows = slot<List<ForecastTransactionRow>>()
        val remote = mockk<ForecastRemoteDataSource>()
        coEvery { remote.forecast(any(), any(), capture(rows)) } returns ForecastOutcome.Offline
        val repo = ForecastRepository(remote, onlineMonitor())

        val transactions = listOf(
            tx(5000.0, TransactionType.EXPENSE, 1),    // keep
            tx(2500.0, TransactionType.INCOME, 2),     // drop: not an expense
            tx(-300.0, TransactionType.EXPENSE, 3),    // drop: non-positive
            tx(0.0, TransactionType.EXPENSE, 4),       // drop: non-positive
            tx(9000.0, TransactionType.EXPENSE, -1),   // drop: future (after origin)
            tx(7000.0, TransactionType.EXPENSE, 400)   // drop: older than the 180-day window
        )
        repo.forecast("acc", origin, transactions)

        rows.captured shouldHaveSize 1
        rows.captured.single().amount shouldBe 5000.0
        rows.captured.single().kind shouldBe "expense"
    }

    test("empty usable history short-circuits to a refusal, never a server 400") {
        val remote = mockk<ForecastRemoteDataSource>()
        val repo = ForecastRepository(remote, onlineMonitor())

        val outcome = repo.forecast(
            "acc", origin,
            listOf(tx(1000.0, TransactionType.INCOME, 1)) // only income -> no expense rows
        )

        outcome.shouldBeInstanceOf<ForecastOutcome.Refused>().reason shouldBe
            ForecastRepository.REFUSAL_NO_EXPENSE_HISTORY
        coVerify(exactly = 0) { remote.forecast(any(), any(), any()) }
    }
})
