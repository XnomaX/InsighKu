package com.example.insightku.core.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.insightku.core.data.local.dao.*
import com.example.insightku.core.data.model.*

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN recurringPeriod TEXT")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'EXPENSE'")
        db.execSQL("ALTER TABLE categories ADD COLUMN isSystemCategory INTEGER NOT NULL DEFAULT 0")
        // Seed system categories for existing users
        db.execSQL("""
            INSERT OR IGNORE INTO categories (id, name, color, budgetLimit, icon, isActive, alertThreshold, recurringPeriod, categoryType, isSystemCategory)
            VALUES
            ('system-uncategorized-expense', 'Uncategorized', '#79747E', NULL, 'Others', 1, 80, NULL, 'EXPENSE', 1),
            ('system-uncategorized-income', 'Uncategorized Income', '#79747E', NULL, 'Others', 1, 80, NULL, 'INCOME', 1)
        """.trimIndent())
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS installments (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL DEFAULT '',
                categoryId TEXT,
                totalAmount REAL NOT NULL DEFAULT 0.0,
                monthlyPayment REAL NOT NULL DEFAULT 0.0,
                totalMonths INTEGER NOT NULL DEFAULT 0,
                paidMonths INTEGER NOT NULL DEFAULT 0,
                firstPaymentDate INTEGER NOT NULL DEFAULT 0,
                nextDueDate INTEGER NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL DEFAULT 1,
                notes TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS draft_transactions (
                id TEXT NOT NULL PRIMARY KEY,
                amountGuess REAL NOT NULL DEFAULT 0.0,
                typeGuess TEXT NOT NULL DEFAULT 'EXPENSE',
                merchantGuess TEXT NOT NULL DEFAULT '',
                bankName TEXT NOT NULL DEFAULT '',
                sourcePackage TEXT NOT NULL DEFAULT '',
                rawTitle TEXT NOT NULL DEFAULT '',
                rawContent TEXT NOT NULL DEFAULT '',
                confidence TEXT NOT NULL DEFAULT 'MEDIUM',
                status TEXT NOT NULL DEFAULT 'PENDING',
                detectedAt INTEGER NOT NULL DEFAULT 0,
                dedupHash TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_draft_transactions_dedupHash ON draft_transactions (dedupHash)"
        )
    }
}

@Database(
    entities = [
        Transaction::class,
        Category::class,
        RecurringBudget::class,
        Budget::class,
        User::class,
        Installment::class,
        DraftTransaction::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InsightKuDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringBudgetDao(): RecurringBudgetDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun draftTransactionDao(): DraftTransactionDao
}


