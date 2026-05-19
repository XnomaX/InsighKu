package com.example.insightku.data.local.database

import androidx.room.TypeConverter
import com.example.insightku.data.model.*

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(type: String): TransactionType {
        return runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE)
    }

    @TypeConverter
    fun fromBudgetFrequency(frequency: BudgetFrequency): String {
        return frequency.name
    }

    @TypeConverter
    fun toBudgetFrequency(frequency: String): BudgetFrequency {
        return runCatching { BudgetFrequency.valueOf(frequency) }.getOrDefault(BudgetFrequency.MONTHLY)
    }

    @TypeConverter
    fun fromBudgetPeriod(period: BudgetPeriod): String {
        return period.name
    }

    @TypeConverter
    fun toBudgetPeriod(period: String): BudgetPeriod {
        return runCatching { BudgetPeriod.valueOf(period) }.getOrDefault(BudgetPeriod.MONTHLY)
    }
}
