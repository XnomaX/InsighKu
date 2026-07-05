package com.example.insightku.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 15 → 16: Extend auto_allocation_rules for Smart Auto Allocation.
 * Adds sourceAccountId, confirmationMode, incomeCategoryIds, minIncomeAmount,
 * roundUpEnabled, roundUpIncrement, scheduledFrequency, scheduledDayOfWeek,
 * scheduledDayOfMonth, lastExecutedAt.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Source account for the allocation
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN sourceAccountId TEXT")
        // Confirmation mode: AUTO or CONFIRMATION_REQUIRED
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN confirmationMode TEXT NOT NULL DEFAULT 'auto'")
        // JSON array of income category IDs to filter
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN incomeCategoryIds TEXT NOT NULL DEFAULT '[]'")
        // Minimum income amount to trigger allocation
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN minIncomeAmount REAL NOT NULL DEFAULT 0")
        // Round-up enabled flag
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN roundUpEnabled INTEGER NOT NULL DEFAULT 0")
        // Round-up increment (1000, 5000, 10000)
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN roundUpIncrement REAL NOT NULL DEFAULT 5000")
        // Scheduled frequency
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN scheduledFrequency TEXT NOT NULL DEFAULT 'daily'")
        // Day of week for weekly/biweekly (1=Monday..7=Sunday)
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN scheduledDayOfWeek INTEGER NOT NULL DEFAULT 1")
        // Day of month for monthly (1-31)
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN scheduledDayOfMonth INTEGER NOT NULL DEFAULT 1")
        // Last time the allocation was executed
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN lastExecutedAt INTEGER NOT NULL DEFAULT 0")
    }
}
