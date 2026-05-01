
package com.example.insightku.ui.components.budgeting

import com.example.insightku.data.model.BudgetFrequency
import com.example.insightku.data.model.RecurringBudget

object BudgetingDataSource {

    fun getDummyBudgetCategories(): List<BudgetCategory> {
        return listOf(
            BudgetCategory("1", "Food & Dining", 2000000.0, 1650000.0, "#FF6B6B", "restaurant"),
            BudgetCategory("2", "Transportation", 1000000.0, 750000.0, "#4ECDC4", "directions_car"),
            BudgetCategory("3", "Shopping", 1500000.0, 1800000.0, "#45B7D1", "shopping_bag"),
            BudgetCategory("4", "Entertainment", 500000.0, 320000.0, "#96CEB4", "movie"),
            BudgetCategory("5", "Bills & Utilities", 1200000.0, 1150000.0, "#FECA57", "receipt")
        )
    }

    fun getDummyRecurringBudgets(): List<RecurringBudget> {
        return listOf(
            RecurringBudget(id = 1, name = "Netflix", amount = 186000.0, frequency = BudgetFrequency.MONTHLY, nextDue = System.currentTimeMillis() + 86400000 * 5, reminderDaysBefore = 3),
            RecurringBudget(id = 2, name = "Spotify", amount = 54000.0, frequency = BudgetFrequency.MONTHLY, nextDue = System.currentTimeMillis() + 86400000 * 10, reminderDaysBefore = 7)
        )
    }
}
