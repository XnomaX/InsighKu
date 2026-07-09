package com.example.insightku.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 17 → 18: Extend draft_transactions for Auto Allocation integration.
 *
 * Adds columns to support AUTO_ALLOCATION draft type alongside BANK_NOTIFICATION.
 * This allows the Confirm First execution mode to reuse the DraftTransactionManager
 * infrastructure instead of maintaining a separate pending allocation system.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Draft type: BANK_NOTIFICATION (default) or AUTO_ALLOCATION
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN draftType TEXT NOT NULL DEFAULT 'BANK_NOTIFICATION'")

        // Auto-allocation specific fields
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN ruleId TEXT")
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN goalId TEXT")
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN goalName TEXT")
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN sourceAccountId TEXT")
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN sourceAccountName TEXT")
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN allocationAmount REAL")
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN triggerType TEXT")
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN triggerDescription TEXT")
        db.execSQL("ALTER TABLE draft_transactions ADD COLUMN triggerTimestamp INTEGER")

        // Index for querying allocation drafts efficiently
        db.execSQL("CREATE INDEX IF NOT EXISTS index_draft_transactions_draftType ON draft_transactions (draftType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_draft_transactions_status ON draft_transactions (status)")
    }
}
