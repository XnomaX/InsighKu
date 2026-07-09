package com.example.insightku.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 18 → 19: Add trigger-specific configuration columns to auto_allocation_rules.
 *
 * New columns:
 * - executionHour (Int, default 8)
 * - executionMinute (Int, default 0)
 * - biweeklyStartDate (Long, default 0)
 * - minRemainingBalance (Double, default 0.0)
 * - categoryBasedCategoryIds (String, default '[]')
 * - categoryBasedExecutionMode (String, default 'every_transaction')
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN executionHour INTEGER NOT NULL DEFAULT 8")
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN executionMinute INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN biweeklyStartDate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN minRemainingBalance REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN categoryBasedCategoryIds TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN categoryBasedExecutionMode TEXT NOT NULL DEFAULT 'every_transaction'")
    }
}
