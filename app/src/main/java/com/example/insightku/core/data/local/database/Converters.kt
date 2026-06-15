package com.example.insightku.core.data.local.database

import androidx.room.TypeConverter
import com.example.insightku.core.data.model.*

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

    @TypeConverter
    fun fromDraftConfidence(confidence: DraftConfidence): String {
        return confidence.name
    }

    @TypeConverter
    fun toDraftConfidence(confidence: String): DraftConfidence {
        return runCatching { DraftConfidence.valueOf(confidence) }.getOrDefault(DraftConfidence.MEDIUM)
    }

    @TypeConverter
    fun fromDraftStatus(status: DraftStatus): String {
        return status.name
    }

    @TypeConverter
    fun toDraftStatus(status: String): DraftStatus {
        return runCatching { DraftStatus.valueOf(status) }.getOrDefault(DraftStatus.PENDING)
    }
}


