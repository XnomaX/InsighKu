package com.example.insightku.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 21 → 22: Add reminderEnabled column to goals.
 * Enables per-goal deadline reminder notifications (default off).
 */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
    }
}
