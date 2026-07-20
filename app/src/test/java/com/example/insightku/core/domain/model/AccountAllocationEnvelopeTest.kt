package com.example.insightku.core.domain.model

import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the envelope-model money logic:
 * - [AccountAllocation.availableCash] = balance − set-aside (budgets NOT subtracted)
 * - [TransactionType.balanceDelta] treats goal transaction types as informational (0)
 */
class AccountAllocationEnvelopeTest {

    private fun account(balance: Double) = Account(
        id = "acc-1", name = "Bank", balance = balance, createdAt = 0L, updatedAt = 0L
    )

    @Test
    fun `availableCash subtracts only goal set-aside, not budgets`() {
        val allocation = AccountAllocation(
            account = account(balance = 1_000_000.0),
            allocatedToGoals = 100_000.0,
            allocatedToBudgets = 300_000.0  // informational — already reflected in balance
        )
        assertEquals(900_000.0, allocation.availableCash, 0.0)
    }

    @Test
    fun `availableCash is coerced to zero when over-allocated`() {
        val allocation = AccountAllocation(
            account = account(balance = 50_000.0),
            allocatedToGoals = 100_000.0
        )
        assertEquals(0.0, allocation.availableCash, 0.0)
    }

    @Test
    fun `availableCash equals balance with no allocations`() {
        val allocation = AccountAllocation(account = account(balance = 250_000.0))
        assertEquals(250_000.0, allocation.availableCash, 0.0)
    }

    @Test
    fun `goal transaction types produce zero balance delta`() {
        assertEquals(0.0, TransactionType.balanceDelta(TransactionType.GOAL_CONTRIBUTION, 100.0), 0.0)
        assertEquals(0.0, TransactionType.balanceDelta(TransactionType.GOAL_WITHDRAWAL, 100.0), 0.0)
        assertEquals(0.0, TransactionType.balanceDelta(TransactionType.AUTO_ALLOCATION, 100.0), 0.0)
    }

    @Test
    fun `real money movement types produce non-zero balance delta`() {
        assertEquals(100.0, TransactionType.balanceDelta(TransactionType.INCOME, 100.0), 0.0)
        assertEquals(-100.0, TransactionType.balanceDelta(TransactionType.EXPENSE, 100.0), 0.0)
        assertEquals(100.0, TransactionType.balanceDelta(TransactionType.TRANSFER_IN, 100.0), 0.0)
        assertEquals(-100.0, TransactionType.balanceDelta(TransactionType.TRANSFER_OUT, 100.0), 0.0)
        assertEquals(100.0, TransactionType.balanceDelta(TransactionType.BALANCE_ADJUSTMENT, 100.0), 0.0)
    }

    @Test
    fun `GoalAllocationDetail carries status and sorts by amount`() {
        val details = listOf(
            GoalAllocationDetail("g1", "Small", "savings", "#111", "active", 50_000.0, 100_000.0, 50.0),
            GoalAllocationDetail("g2", "Big", "savings", "#222", "paused", 500_000.0, 1_000_000.0, 50.0)
        )
        val sorted = details.sortedByDescending { it.allocatedAmount }
        assertEquals("g2", sorted.first().goalId)
        assertEquals("paused", sorted.first().goalStatus)
    }
}
