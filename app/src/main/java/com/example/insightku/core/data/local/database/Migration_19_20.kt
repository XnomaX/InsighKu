package com.example.insightku.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 19 → 20: Add auto-allocation draft columns and indices to draft_transactions.
 *
 * This migration is idempotent — it checks for existing columns before adding them,
 * because older versions may have partially applied these changes via MIGRATION_18_19.
 */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Get existing column names for draft_transactions
        val existingColumns = mutableSetOf<String>()
        db.query("PRAGMA table_info(draft_transactions)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                existingColumns.add(cursor.getString(nameIndex))
            }
        }

        // Only add columns that don't already exist
        val columnsToAdd = listOf(
            "draftType TEXT NOT NULL DEFAULT 'BANK_NOTIFICATION'",
            "ruleId TEXT",
            "goalId TEXT",
            "goalName TEXT",
            "sourceAccountId TEXT",
            "sourceAccountName TEXT",
            "allocationAmount REAL",
            "triggerType TEXT",
            "triggerDescription TEXT",
            "triggerTimestamp INTEGER"
        )

        for (colDef in columnsToAdd) {
            val colName = colDef.substringBefore(" ").trim()
            if (colName !in existingColumns) {
                db.execSQL("ALTER TABLE draft_transactions ADD COLUMN $colDef")
            }
        }

        // Create indices (IF NOT EXISTS is safe for idempotency)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_draft_transactions_draftType ON draft_transactions (draftType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_draft_transactions_status ON draft_transactions (status)")
    }
}
