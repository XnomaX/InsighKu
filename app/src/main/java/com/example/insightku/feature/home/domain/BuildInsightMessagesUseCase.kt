package com.example.insightku.feature.home.domain

import javax.inject.Inject

/**
 * BuildInsightMessagesUseCase — pure business logic for generating financial insight messages.
 *
 * Extracted from DashboardViewModel.buildInsightMessages() to follow MVVM architecture:
 * ViewModels should not contain business logic.
 *
 * This is a pure function with no side effects — easy to test.
 */
class BuildInsightMessagesUseCase @Inject constructor() {

    /**
     * Generate insight messages based on financial data and streak.
     *
     * @param income Monthly income amount
     * @param expenses Monthly expenses amount
     * @param savings Monthly savings (income - expenses)
     * @param streak Current tracking streak in days
     * @return List of insight messages (max 3)
     */
    operator fun invoke(
        income: Double,
        expenses: Double,
        savings: Double,
        streak: Int
    ): List<String> {
        val messages = mutableListOf<String>()
        val savingsRate = if (income > 0) savings / income else 0.0

        // Savings insight
        when {
            income == 0.0 -> messages.add("Start logging income to see your financial picture.")
            savingsRate >= 0.3 -> messages.add("You're saving ${(savingsRate * 100).toInt()}% of your income this month. Excellent discipline.")
            savingsRate >= 0.1 -> messages.add("Savings are on track at ${(savingsRate * 100).toInt()}% of income. Keep the momentum.")
            savings < 0 -> messages.add("Expenses exceeded income this month. Review your spending to get back on track.")
            else -> messages.add("You're building healthy spending habits this month.")
        }

        // Spending ratio insight
        when {
            expenses > 0 && income > 0 && expenses / income < 0.7 ->
                messages.add("Recurring payments are under control \u2014 spending ratio looks healthy.")
            expenses > income ->
                messages.add("Consider reviewing recurring payments to reduce monthly outflow.")
        }

        // Streak insight
        when {
            streak >= 30 -> messages.add("$streak days of consistent tracking. Financial mastery in motion.")
            streak >= 7  -> messages.add("$streak-day tracking streak. Your habit is becoming automatic.")
            streak >= 3  -> messages.add("$streak days in \u2014 the habit is forming. Don't break the chain.")
            streak == 0  -> messages.add("Log your first transaction today to start building your streak.")
        }

        return messages.take(3)
    }
}
