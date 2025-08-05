package com.example.insightku.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val name: String,
    val amount: Double,
    val spent: Double = 0.0,
    val period: BudgetPeriod,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BudgetPeriod {
    DAILY, WEEKLY, MONTHLY, YEARLY
}
