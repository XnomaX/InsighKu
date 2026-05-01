package com.example.insightku.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * CRITICAL FIX: RecurringBudget juga butuh no-arg constructor untuk Firestore toObjects().
 */
@Keep
@IgnoreExtraProperties
@Entity(tableName = "recurring_budgets")
data class RecurringBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val amount: Double = 0.0,
    val frequency: BudgetFrequency = BudgetFrequency.MONTHLY,
    val categoryId: String? = null,
    val isActive: Boolean = true,
    val lastProcessed: Long? = null,
    val nextDue: Long = System.currentTimeMillis(),
    val reminderDaysBefore: Int = 3
)

