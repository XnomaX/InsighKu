package com.example.insightku.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 22 → 23: switch goal contributions to the envelope model.
 *
 * Before: contributing to a goal decremented accounts.balance (money moved out).
 * After: contributions only set money aside — balance is the total pool and
 * Available Cash = balance − set-aside.
 *
 * This data-only migration restores the pool: every account's balance is
 * increased by its net goal contributions (which had been subtracted under
 * the old model). Post-migration, balance reconciles with
 * calculateBalanceFromTransactions once goal transaction types are treated
 * as informational (0) there.
 */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE accounts SET balance = balance + (
                SELECT COALESCE(SUM(amount), 0.0)
                FROM contributions
                WHERE contributions.accountId = accounts.id
            )
            """.trimIndent()
        )
    }
}
