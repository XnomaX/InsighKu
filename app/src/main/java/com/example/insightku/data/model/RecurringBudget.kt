package com.example.insightku.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_budgets")
data class RecurringBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val frequency: BudgetFrequency,
    val categoryId: String? = null,
    val isActive: Boolean = true,
    val lastProcessed: Long? = null,
    val nextDue: Long,
    val reminderDaysBefore: Int = 3 // Defaulting to 3 days before
)
