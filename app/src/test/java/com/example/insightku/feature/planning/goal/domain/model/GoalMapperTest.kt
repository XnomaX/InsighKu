package com.example.insightku.feature.planning.goal.domain.model

import com.example.insightku.feature.planning.goal.data.model.GoalEntity
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Tests for [Goal] ↔ [GoalEntity] mapping — ensures no field is dropped or
 * silently overwritten when a goal is edited and persisted.
 */
class GoalMapperTest {

    private fun sampleGoal(reminderEnabled: Boolean = true) = Goal(
        id = "goal-1",
        name = "Emergency Fund",
        targetAmount = 5_000_000.0,
        deadline = LocalDate.of(2026, 12, 31),
        status = GoalStatus.ACTIVE,
        autoAllocate = true,
        allocationPriority = 2,
        iconName = "Shield",
        color = "#123456",
        notes = "Save every payday",
        reminderEnabled = reminderEnabled,
        currentAmount = 1_500_000.0,
        createdAt = Instant.ofEpochMilli(1_700_000_000_000L),
        updatedAt = Instant.ofEpochMilli(1_700_000_999_000L)
    )

    @Test
    fun `reminderEnabled survives round trip`() {
        val goal = sampleGoal(reminderEnabled = true)
        val restored = Goal.fromEntity(goal.toEntity(), currentAmount = goal.currentAmount)
        assertTrue(restored.reminderEnabled)

        val disabled = sampleGoal(reminderEnabled = false)
        assertFalse(Goal.fromEntity(disabled.toEntity()).reminderEnabled)
    }

    @Test
    fun `toEntity preserves domain updatedAt instead of overwriting`() {
        val goal = sampleGoal()
        val entity = goal.toEntity()
        assertEquals(goal.updatedAt.toEpochMilli(), entity.updatedAt)
    }

    @Test
    fun `round trip preserves identity and edited fields`() {
        val goal = sampleGoal()
        val restored = Goal.fromEntity(goal.toEntity(), currentAmount = goal.currentAmount)

        assertEquals(goal.id, restored.id)
        assertEquals(goal.name, restored.name)
        assertEquals(goal.targetAmount, restored.targetAmount, 0.0)
        assertEquals(goal.deadline, restored.deadline)
        assertEquals(goal.status, restored.status)
        assertEquals(goal.autoAllocate, restored.autoAllocate)
        assertEquals(goal.allocationPriority, restored.allocationPriority)
        assertEquals(goal.iconName, restored.iconName)
        assertEquals(goal.color, restored.color)
        assertEquals(goal.notes, restored.notes)
        assertEquals(goal.currentAmount, restored.currentAmount, 0.0)
        assertEquals(goal.createdAt, restored.createdAt)
        assertEquals(goal.updatedAt, restored.updatedAt)
    }

    @Test
    fun `fromEntity defaults reminderEnabled from entity column`() {
        val entity = GoalEntity(
            id = "g", name = "n", targetAmount = 1.0,
            reminderEnabled = true, createdAt = 0L, updatedAt = 0L
        )
        assertTrue(Goal.fromEntity(entity).reminderEnabled)
    }
}
