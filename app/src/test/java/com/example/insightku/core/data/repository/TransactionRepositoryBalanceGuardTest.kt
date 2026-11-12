package com.example.insightku.core.data.repository

import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.data.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [TransactionRepository.wouldGoNegative], the rule that keeps asset
 * accounts from going negative when an expense, transfer-out or a reversal
 * (edit/delete of an already-spent income) is applied.
 *
 * Pins the user-reported defect: recording an expense larger than the account
 * balance silently drove the balance below zero. Credit cards are liabilities —
 * a growing debt balance is their normal state and must never be blocked.
 */
class TransactionRepositoryBalanceGuardTest : FunSpec({

    fun account(type: AccountType, balance: Double) = Account(
        name = "Test",
        accountType = type.name,
        balance = balance
    )

    test("expense within balance keeps the account solvent") {
        TransactionRepository.wouldGoNegative(account(AccountType.CASH, 100_000.0), -80_000.0) shouldBe false
    }

    test("expense beyond balance is refused") {
        TransactionRepository.wouldGoNegative(account(AccountType.CASH, 50_000.0), -80_000.0) shouldBe true
    }

    test("expense exactly equal to balance is allowed") {
        TransactionRepository.wouldGoNegative(account(AccountType.BANK_ACCOUNT, 80_000.0), -80_000.0) shouldBe false
    }

    test("income and other positive deltas never trip the guard") {
        TransactionRepository.wouldGoNegative(account(AccountType.E_WALLET, 0.0), 150_000.0) shouldBe false
    }

    test("credit card debt may grow past zero") {
        TransactionRepository.wouldGoNegative(account(AccountType.CREDIT_CARD, 10_000.0), -90_000.0) shouldBe false
    }

    test("unknown account is not blocked here") {
        TransactionRepository.wouldGoNegative(null, -90_000.0) shouldBe false
    }

    context("projectedBalanceAfterEdit mirrors the repository net-delta rule") {
        val cash = account(AccountType.CASH, 100_000.0)

        test("same-account expense raise refunds the original first") {
            // Rp80k expense edited to Rp120k on Rp100k: 100k + 80k refund - 120k = 60k.
            TransactionRepository.projectedBalanceAfterEdit(
                account = cash,
                originalAccountId = cash.id,
                originalType = TransactionType.EXPENSE,
                originalAmount = 80_000.0,
                newType = TransactionType.EXPENSE,
                newAmount = 120_000.0
            ) shouldBe 60_000.0
        }

        test("moving that edit to another account must cover the full new amount") {
            TransactionRepository.projectedBalanceAfterEdit(
                account = cash,
                originalAccountId = "other-account",
                originalType = TransactionType.EXPENSE,
                originalAmount = 80_000.0,
                newType = TransactionType.EXPENSE,
                newAmount = 120_000.0
            ) shouldBe -20_000.0
        }
    }
})
