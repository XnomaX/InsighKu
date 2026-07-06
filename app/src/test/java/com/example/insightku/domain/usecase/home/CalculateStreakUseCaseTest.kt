package com.example.insightku.domain.usecase.home

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.home.domain.CalculateStreakUseCase
import com.example.insightku.feature.home.domain.StreakPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Tests for [CalculateStreakUseCase] — pure business logic for streak calculation.
 * Uses a fake [StreakPreferences] to avoid Android framework dependencies.
 */
class CalculateStreakUseCaseTest {

    private fun expense(title: String, daysAgo: Int): Transaction {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        return Transaction(
            title = title,
            amount = 10_000.0,
            category = "Food",
            type = TransactionType.EXPENSE,
            date = cal.timeInMillis
        )
    }

    private fun fakePrefs(
        freezeCount: Int = 0,
        lastFreezeDate: String = "",
        isPerfectStreak: Boolean = false,
        streakGoal: Int = 7,
        repairAvailable: Boolean = false,
        repairExpiry: Long = 0L
    ) = object : StreakPreferences {
        override val freezeCount: Flow<Int> = flowOf(freezeCount)
        override val lastFreezeDate: Flow<String> = flowOf(lastFreezeDate)
        override val isPerfectStreak: Flow<Boolean> = flowOf(isPerfectStreak)
        override val streakGoal: Flow<Int> = flowOf(streakGoal)
        override val repairAvailable: Flow<Boolean> = flowOf(repairAvailable)
        override val repairExpiry: Flow<Long> = flowOf(repairExpiry)
    }

    private val useCase = CalculateStreakUseCase(fakePrefs())

    @Test
    fun `empty transactions yields zero streak`() = runTest {
        val result = useCase(emptyList())
        assertEquals(0, result.currentStreak)
        assertFalse(result.hasTrackedToday)
    }

    @Test
    fun `transaction today yields streak of 1`() = runTest {
        val txns = listOf(expense("Lunch", 0))
        val result = useCase(txns)
        assertEquals(1, result.currentStreak)
        assertTrue(result.hasTrackedToday)
    }

    @Test
    fun `consecutive days yield correct streak`() = runTest {
        val txns = listOf(
            expense("Day 0", 0),
            expense("Day 1", 1),
            expense("Day 2", 2),
            expense("Day 3", 3)
        )
        val result = useCase(txns)
        assertEquals(4, result.currentStreak)
    }

    @Test
    fun `gap breaks streak`() = runTest {
        val txns = listOf(
            expense("Day 0", 0),
            expense("Day 1", 1),
            // Gap at day 2
            expense("Day 3", 3)
        )
        val result = useCase(txns)
        assertEquals(2, result.currentStreak) // Only today + yesterday
    }

    @Test
    fun `best streak is calculated correctly`() = runTest {
        val txns = listOf(
            // Current streak: today + yesterday = 2
            expense("Day 0", 0),
            expense("Day 1", 1),
            // Gap at day 2
            // Earlier streak: 3 consecutive days
            expense("Day 3", 3),
            expense("Day 4", 4),
            expense("Day 5", 5)
        )
        val result = useCase(txns)
        assertEquals(2, result.currentStreak)
        assertEquals(3, result.bestStreak)
    }

    @Test
    fun `freeze is consumed when yesterday was missed`() = runTest {
        val txns = listOf(
            expense("Day 3", 3),
            expense("Day 4", 4),
            expense("Day 5", 5)
            // Gap: yesterday (day 1) and today (day 0) not tracked
        )
        val prefs = fakePrefs(freezeCount = 1)
        val engine = CalculateStreakUseCase(prefs)
        val result = engine(txns)
        assertEquals(0, result.currentStreak)
        // Freeze should be consumed
        assertTrue(result.prefUpdates.any { it is CalculateStreakUseCase.PrefUpdate.FreezeConsumed })
    }

    @Test
    fun `repair is enabled when streak breaks`() = runTest {
        val txns = listOf(
            expense("Day 5", 5),
            expense("Day 4", 4),
            expense("Day 3", 3)
            // Gap: yesterday and today not tracked, and yesterday is NOT the missed day
        )
        val prefs = fakePrefs(repairAvailable = false)
        val engine = CalculateStreakUseCase(prefs)
        val result = engine(txns)
        assertEquals(0, result.currentStreak)
        assertTrue(result.repairAvailable)
    }

    @Test
    fun `milestone at 7 days awards freeze`() = runTest {
        val txns = (0..6).map { expense("Day $it", it) }
        val prefs = fakePrefs(freezeCount = 0)
        val engine = CalculateStreakUseCase(prefs)
        val result = engine(txns)
        assertEquals(7, result.currentStreak)
        assertEquals(1, result.freezeCount)
    }

    @Test
    fun `streak goal is passed through`() = runTest {
        val prefs = fakePrefs(streakGoal = 30)
        val engine = CalculateStreakUseCase(prefs)
        val result = engine(emptyList())
        assertEquals(30, result.streakGoal)
    }
}
