
package com.example.insightku.ui.components.budgeting

import com.example.insightku.data.model.BudgetFrequency
import com.example.insightku.data.model.RecurringBudget

object BudgetingDataSource {

    fun getDummyBudgetCategories(): List<BudgetCategory> {
        return listOf(
            BudgetCategory("1", "Food", 2000000.0, 1250000.0, "#F59E0B", "Food & Drinks"),
            BudgetCategory("2", "Transport", 1000000.0, 720000.0, "#3B82F6", "Transportation"),
            BudgetCategory("3", "Bills", 1500000.0, 1675000.0, "#EF4444", "Bills & Utilities"),
            BudgetCategory("4", "Lifestyle", null, 410000.0, "#8B5CF6", "Entertainment"),
            BudgetCategory("5", "Health", 800000.0, 210000.0, "#10B981", "Healthcare")
        )
    }

    fun getDummyRecurringBudgets(): List<RecurringBudget> {
        return listOf(
            RecurringBudget(id = 1, name = "Netflix", amount = 186000.0, frequency = BudgetFrequency.MONTHLY, nextDue = System.currentTimeMillis() + 86400000 * 5, reminderDaysBefore = 3),
            RecurringBudget(id = 2, name = "Spotify", amount = 54000.0, frequency = BudgetFrequency.MONTHLY, nextDue = System.currentTimeMillis() + 86400000 * 10, reminderDaysBefore = 7)
        )
    }
}
