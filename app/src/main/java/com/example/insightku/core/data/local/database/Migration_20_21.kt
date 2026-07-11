package com.example.insightku.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 20 → 21: Add roundUpMode column to auto_allocation_rules.
 * Supports ROUND_UP, ROUND_DOWN, and ROUND_NEAREST rounding modes.
 */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Rounding mode for round-up trigger (round_up, round_down, round_nearest)
        db.execSQL("ALTER TABLE auto_allocation_rules ADD COLUMN roundUpMode TEXT NOT NULL DEFAULT 'round_up'")
    }
}
