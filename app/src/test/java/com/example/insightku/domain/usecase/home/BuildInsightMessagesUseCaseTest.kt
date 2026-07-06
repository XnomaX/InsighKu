package com.example.insightku.domain.usecase.home

import com.example.insightku.feature.home.domain.BuildInsightMessagesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [BuildInsightMessagesUseCase] — pure business logic for generating
 * financial insight messages. No Android framework dependencies.
 */
class BuildInsightMessagesUseCaseTest {

    private val useCase = BuildInsightMessagesUseCase()

    @Test
    fun `no income shows getting started message`() {
        val messages = useCase(income = 0.0, expenses = 0.0, savings = 0.0, streak = 0)
        assertTrue(messages.any { it.contains("Start logging income") })
    }

    @Test
    fun `high savings rate shows excellent discipline`() {
        val messages = useCase(income = 1000000.0, expenses = 500000.0, savings = 500000.0, streak = 5)
        assertTrue(messages.any { it.contains("50%") && it.contains("Excellent") })
    }

    @Test
    fun `moderate savings shows on track`() {
        val messages = useCase(income = 1000000.0, expenses = 900000.0, savings = 100000.0, streak = 5)
        assertTrue(messages.any { it.contains("10%") && it.contains("on track") })
    }

    @Test
    fun `negative savings shows overspending warning`() {
        val messages = useCase(income = 500000.0, expenses = 700000.0, savings = -200000.0, streak = 5)
        assertTrue(messages.any { it.contains("exceeded income") })
    }

    @Test
    fun `healthy spending ratio shows recurring payments under control`() {
        val messages = useCase(income = 1000000.0, expenses = 500000.0, savings = 500000.0, streak = 5)
        assertTrue(messages.any { it.contains("Recurring payments are under control") })
    }

    @Test
    fun `long streak shows mastery message`() {
        val messages = useCase(income = 1000000.0, expenses = 500000.0, savings = 500000.0, streak = 35)
        assertTrue(messages.any { it.contains("35 days") && it.contains("mastery") })
    }

    @Test
    fun `medium streak shows habit message`() {
        val messages = useCase(income = 1000000.0, expenses = 500000.0, savings = 500000.0, streak = 10)
        assertTrue(messages.any { it.contains("10-day") && it.contains("habit") })
    }

    @Test
    fun `short streak shows forming habit`() {
        val messages = useCase(income = 1000000.0, expenses = 500000.0, savings = 500000.0, streak = 4)
        assertTrue(messages.any { it.contains("4 days") && it.contains("forming") })
    }

    @Test
    fun `zero streak shows first transaction message`() {
        val messages = useCase(income = 1000000.0, expenses = 500000.0, savings = 500000.0, streak = 0)
        assertTrue(messages.any { it.contains("first transaction") })
    }

    @Test
    fun `maximum three messages returned`() {
        val messages = useCase(income = 1000000.0, expenses = 500000.0, savings = 500000.0, streak = 30)
        assertTrue(messages.size <= 3)
    }

    @Test
    fun `expenses exceeding income shows recurring payment warning`() {
        val messages = useCase(income = 500000.0, expenses = 600000.0, savings = -100000.0, streak = 5)
        assertTrue(messages.any { it.contains("review recurring payments") })
    }
}
