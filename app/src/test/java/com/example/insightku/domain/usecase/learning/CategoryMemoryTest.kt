package com.example.insightku.domain.usecase.learning

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.home.domain.CategoryMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [CategoryMemory] — the backend-free merchant→category learning. Guards that the memory
 * reflects the user's most-frequent choice, never surfaces low-confidence single sightings, and never
 * throws on degenerate input.
 */
class CategoryMemoryTest {

    private val memory = CategoryMemory()

    private fun expense(title: String, category: String) =
        Transaction(title = title, category = category, type = TransactionType.EXPENSE, amount = 10_000.0)

    @Test
    fun `empty input yields no memories and no suggestion`() {
        assertTrue(memory.derive(emptyList()).isEmpty())
        assertNull(memory.suggestCategory("GoFood", emptyList()))
    }

    @Test
    fun `single sighting is not surfaced as a pattern`() {
        val txns = listOf(expense("GoFood", "Food & Drinks"))
        assertTrue(memory.derive(txns).isEmpty())
    }

    @Test
    fun `most-frequent category wins for a merchant`() {
        val txns = listOf(
            expense("GoFood", "Food & Drinks"),
            expense("GoFood", "Food & Drinks"),
            expense("GoFood", "Groceries") // minority — should lose
        )
        val result = memory.derive(txns)
        assertEquals(1, result.size)
        assertEquals("Food & Drinks", result.first().category)
        assertEquals(2, result.first().timesSeen)
        assertEquals(3, result.first().totalForMerchant)
    }

    @Test
    fun `suggestion is case-insensitive and trimmed`() {
        val txns = listOf(expense("GoFood", "Food & Drinks"), expense("GoFood", "Food & Drinks"))
        assertEquals("Food & Drinks", memory.suggestCategory("  gofood ", txns))
    }

    @Test
    fun `blank title suggests nothing`() {
        val txns = listOf(expense("GoFood", "Food & Drinks"), expense("GoFood", "Food & Drinks"))
        assertNull(memory.suggestCategory("   ", txns))
    }

    @Test
    fun `income transactions are ignored`() {
        val txns = listOf(
            Transaction(title = "Salary", category = "Income", type = TransactionType.INCOME, amount = 1.0),
            Transaction(title = "Salary", category = "Income", type = TransactionType.INCOME, amount = 1.0)
        )
        assertTrue(memory.derive(txns).isEmpty())
    }
}
