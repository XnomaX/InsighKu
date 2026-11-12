package com.example.insightku.feature.home.presentation

import android.content.Context
import com.example.insightku.R
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * Tests for the render-time localization of goal-completion transfers.
 *
 * These rows are written by `GoalRepository` with English `title`/`description`
 * and replicated to Firestore, and `TRANSFER_OUT`/`TRANSFER_IN` have
 * `allowsEdit = false` — so users cannot correct the text themselves. The
 * helpers resolve the display string from `sourceModule`/`goalName` markers
 * instead of rewriting stored data.
 *
 * Two properties must hold, because both fail silently if broken:
 *  1. transfer rows go THROUGH `getString` — the mock returns a `«localized»`
 *     marker that differs from the stored English text, so a helper that
 *     returned the stored value instead would fail the assertion (no tautology);
 *  2. ordinary rows do NOT touch `getString` — verified with `exactly = 0`, so a
 *     helper that over-matched cannot silently rewrite a user's own
 *     income/expense history.
 *
 * Each test builds its own mock: mockk records calls on the instance, and a
 * spec-level mock would let counts from earlier tests leak into a later
 * `verify(exactly = 0)`.
 */
class TransactionDisplayTextTest : FunSpec({

    /** Strict mock — `getString` with unexpected arguments throws, not defaults. */
    fun localizedContext(): Context = mockk<Context>().apply {
        every { getString(R.string.tx_transfer_from_goal_title, "Emergency Fund") } returns
            "«title» Emergency Fund"
        every { getString(R.string.tx_transfer_from_goal_desc, "Emergency Fund") } returns
            "«desc» Emergency Fund"
    }

    // Exactly what GoalRepository persisted: English title + description.
    val storedTitle = "Transfer: Emergency Fund"
    val storedDesc = "Funds from completed goal \"Emergency Fund\""

    fun transfer(type: TransactionType = TransactionType.TRANSFER_OUT) = Transaction(
        title = storedTitle,
        description = storedDesc,
        type = type,
        goalName = "Emergency Fund",
        sourceModule = TX_SOURCE_TRANSFER
    )

    test("a transfer row resolves title and description via getString") {
        val context = localizedContext()
        txDisplayTitle(transfer(), context) shouldBe "«title» Emergency Fund"
        txDisplayDescription(transfer(), context) shouldBe "«desc» Emergency Fund"
        verify(exactly = 1) { context.getString(R.string.tx_transfer_from_goal_title, "Emergency Fund") }
        verify(exactly = 1) { context.getString(R.string.tx_transfer_from_goal_desc, "Emergency Fund") }
    }

    test("the inbound side of the same transfer resolves identically") {
        txDisplayTitle(transfer(TransactionType.TRANSFER_IN), localizedContext()) shouldBe
            "«title» Emergency Fund"
    }

    test("an ordinary expense keeps its stored text and never calls getString") {
        val context = localizedContext()
        val tx = Transaction(
            title = "Kopi Kenangan",
            description = "manual entry",
            type = TransactionType.EXPENSE,
            sourceModule = "transaction"   // the default
        )
        txDisplayTitle(tx, context) shouldBe "Kopi Kenangan"
        txDisplayDescription(tx, context) shouldBe "manual entry"
        verify(exactly = 0) { context.getString(any(), any<String>()) }
    }

    test("goal and auto-allocation modules are not treated as goal transfers") {
        val context = localizedContext()
        for (module in listOf("goal", "auto_allocation")) {
            val tx = Transaction(
                title = "Auto: Netflix",
                description = "Auto: Netflix",
                type = TransactionType.EXPENSE,
                goalName = "Emergency Fund",   // present, but module differs
                sourceModule = module
            )
            txDisplayTitle(tx, context) shouldBe "Auto: Netflix"
            txDisplayDescription(tx, context) shouldBe "Auto: Netflix"
        }
        // goalName alone must not trigger localization; only sourceModule does.
        verify(exactly = 0) { context.getString(any(), any<String>()) }
    }

    test("a transfer marker without a usable goal name falls back to stored text") {
        // Guards against getString rendering a dangling "Transfer: " with no
        // goal name substituted.
        val context = localizedContext()
        for (name in listOf<String?>(null, "", "   ")) {
            val tx = transfer().copy(goalName = name)
            txDisplayTitle(tx, context) shouldBe storedTitle
            txDisplayDescription(tx, context) shouldBe storedDesc
        }
        verify(exactly = 0) { context.getString(any(), any<String>()) }
    }

    test("a null description stays null instead of becoming an empty notes row") {
        val context = localizedContext()
        val tx = transfer().copy(description = null)
        txDisplayDescription(tx, context) shouldBe null
        // Title still localizes when description is absent.
        txDisplayTitle(tx, context) shouldBe "«title» Emergency Fund"
    }
})
