/*
package com.example.insightku.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recurring_budgets")
data class RecurringBudget(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val frequency: BudgetFrequency,
    val categoryId: String? = null,
    val isActive: Boolean = true,
    val lastProcessed: Long? = null,
    val nextDue: Long
)

enum class BudgetFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}*/
