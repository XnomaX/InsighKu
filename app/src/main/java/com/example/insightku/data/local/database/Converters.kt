package com.insightku.data.local.database

import androidx.room.TypeConverter
import com.example.insightku.data.model.*

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(type: String): TransactionType {
        return TransactionType.valueOf(type)
    }

    @TypeConverter
    fun fromBudgetFrequency(frequency: BudgetFrequency): String {
        return frequency.name
    }

    @TypeConverter
    fun toBudgetFrequency(frequency: String): BudgetFrequency {
        return BudgetFrequency.valueOf(frequency)
    }

    @TypeConverter
    fun fromBudgetPeriod(period: BudgetPeriod): String {
        return period.name
    }

    @TypeConverter
    fun toBudgetPeriod(period: String): BudgetPeriod {
        return BudgetPeriod.valueOf(period)
    }
}
