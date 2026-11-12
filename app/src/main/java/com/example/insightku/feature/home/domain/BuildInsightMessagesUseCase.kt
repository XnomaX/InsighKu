package com.example.insightku.feature.home.domain

import androidx.annotation.StringRes
import com.example.insightku.R
import javax.inject.Inject

/**
 * BuildInsightMessagesUseCase — pure business logic for generating financial insight messages.
 *
 * Extracted from DashboardViewModel.buildInsightMessages() to follow MVVM architecture:
 * ViewModels should not contain business logic.
 *
 * Returns [InsightMessage] descriptors rather than ready-made `String`s: this layer has no
 * `Context`, so it selects *which* advice applies plus its arguments, and the UI layer
 * resolves the text through `stringResource`. Same split as `ForecastNotice`, which keeps
 * localisation in the UI while the domain stays `Context`-free.
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
    ): List<InsightMessage> {
        val messages = mutableListOf<InsightMessage>()
        val savingsRate = if (income > 0) savings / income else 0.0

        // Savings insight
        when {
            income == 0.0 -> messages += InsightMessage(R.string.dashboard_advice_no_income)
            savingsRate >= 0.3 -> messages += InsightMessage(
                R.string.dashboard_advice_saving_excellent,
                (savingsRate * 100).toInt()
            )
            savingsRate >= 0.1 -> messages += InsightMessage(
                R.string.dashboard_advice_saving_on_track,
                (savingsRate * 100).toInt()
            )
            savings < 0 -> messages += InsightMessage(R.string.dashboard_advice_overspent)
            else -> messages += InsightMessage(R.string.dashboard_advice_healthy_habits)
        }

        // Spending ratio insight
        when {
            expenses > 0 && income > 0 && expenses / income < 0.7 ->
                messages += InsightMessage(R.string.dashboard_advice_spending_ratio_ok)
            expenses > income ->
                messages += InsightMessage(R.string.dashboard_advice_review_recurring)
        }

        // Streak insight
        when {
            streak >= 30 -> messages += InsightMessage(R.string.dashboard_advice_streak_30, streak)
            streak >= 7 -> messages += InsightMessage(R.string.dashboard_advice_streak_7, streak)
            streak >= 3 -> messages += InsightMessage(R.string.dashboard_advice_streak_3, streak)
            streak == 0 -> messages += InsightMessage(R.string.dashboard_advice_start_streak)
        }

        return messages.take(3)
    }
}

/**
 * A single insight/advice line, expressed as a string resource plus its format arguments.
 *
 * [args] are passed verbatim to `stringResource(resId, *args)`, so every argument type must
 * match the placeholder declared in both `values/strings.xml` and `values-id/strings.xml`
 * (`%1$d` for the Int percentages and day counts used here).
 */
data class InsightMessage(
    @StringRes val resId: Int,
    val args: List<Any> = emptyList()
) {
    constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())
}
