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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions (date)"
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE accounts (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                accountType TEXT NOT NULL,
                balance REAL NOT NULL DEFAULT 0.0,
                color TEXT NOT NULL DEFAULT '#7C4DFF',
                notes TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                isDefault INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

/**
 * Migration 10 → 11: Replace paymentMethod with accountId in transactions table.
 * SQLite doesn't support DROP COLUMN, so we recreate the table.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Create new transactions table without paymentMethod, with accountId
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS transactions_new (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                date INTEGER NOT NULL,
                type TEXT NOT NULL,
                description TEXT,
                receiptPath TEXT,
                time TEXT NOT NULL,
                location TEXT,
                accountId TEXT NOT NULL DEFAULT '',
                isSynced INTEGER NOT NULL DEFAULT 0,
                isDraft INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        // Step 2: Copy data from old table to new table
        db.execSQL("""
            INSERT INTO transactions_new (id, title, amount, category, date, type, description, receiptPath, time, location, accountId, isSynced, isDraft, createdAt)
            SELECT id, title, amount, category, date, type, description, receiptPath, time, location, '', isSynced, isDraft, createdAt FROM transactions
        """.trimIndent())

        // Step 3: Drop the old table
        db.execSQL("DROP TABLE transactions")

        // Step 4: Rename new table to original name
        db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

        // Step 5: Recreate the index on date
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions (date)")
    }
}

/**
 * Migration 11 → 12: Add accountId to recurring_budgets and installments tables.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add accountId to recurring_budgets
        db.execSQL("ALTER TABLE recurring_budgets ADD COLUMN accountId TEXT")

        // Add accountId to installments
        db.execSQL("ALTER TABLE installments ADD COLUMN accountId TEXT")
    }
}

/**
 * Migration 12 → 13: Add Goals feature tables.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create goals table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS goals (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                targetAmount REAL NOT NULL,
                deadline INTEGER,
                status TEXT NOT NULL DEFAULT 'active',
                autoAllocate INTEGER NOT NULL DEFAULT 0,
                allocationPriority INTEGER NOT NULL DEFAULT 0,
                iconName TEXT NOT NULL DEFAULT 'savings',
                color TEXT NOT NULL DEFAULT '#7C4DFF',
                notes TEXT NOT NULL DEFAULT '''' ,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL DEFAULT '0',
                updatedAt INTEGER NOT NULL DEFAULT '0'
            )
        """.trimIndent())

        // Create contributions table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS contributions (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                accountId TEXT NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL DEFAULT 'manual',
                transactionId TEXT,
                notes TEXT NOT NULL DEFAULT '''' ,
                createdAt INTEGER NOT NULL DEFAULT '0',
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create goal_accounts table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS goal_accounts (
                goalId TEXT NOT NULL,
                accountId TEXT NOT NULL,
                allocationPercent REAL NOT NULL DEFAULT 100.0,
                isPrimary INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY (goalId, accountId),
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create reserved_balances table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reserved_balances (
                accountId TEXT NOT NULL PRIMARY KEY,
                amount REAL NOT NULL DEFAULT 0.0,
                goalId TEXT,
                updatedAt INTEGER NOT NULL DEFAULT '0',
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create auto_allocation_rules table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS auto_allocation_rules (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                triggerType TEXT NOT NULL DEFAULT 'income_received',
                triggerParams TEXT,
                allocationType TEXT NOT NULL DEFAULT 'percent',
                allocationValue REAL NOT NULL DEFAULT 10.0,
                isEnabled INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL DEFAULT '0',
                updatedAt INTEGER NOT NULL DEFAULT '0',
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create daily_targets table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS daily_targets (
                id TEXT NOT NULL PRIMARY KEY DEFAULT 'global_daily_target',
                targetAmount REAL NOT NULL DEFAULT 0.0,
                date INTEGER NOT NULL DEFAULT '0',
                createdAt INTEGER NOT NULL DEFAULT '0',
                updatedAt INTEGER NOT NULL DEFAULT '0'
            )
        """.trimIndent())

        // Create indexes for contributions
        db.execSQL("CREATE INDEX IF NOT EXISTS index_contributions_goalId ON contributions (goalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_contributions_accountId ON contributions (accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_contributions_createdAt ON contributions (createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_contributions_transactionId ON contributions (transactionId)")

        // Create indexes for goal_accounts
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_accounts_goalId ON goal_accounts (goalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_accounts_accountId ON goal_accounts (accountId)")

        // Create indexes for auto_allocation_rules
        db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_allocation_rules_goalId ON auto_allocation_rules (goalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_allocation_rules_isEnabled ON auto_allocation_rules (isEnabled)")

        // Create indexes for reserved_balances
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reserved_balances_goalId ON reserved_balances (goalId)")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add isSynced column to goals table (default true = existing data is already synced)
        db.execSQL("ALTER TABLE goals ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1")
        // Add isSynced column to contributions table
        db.execSQL("ALTER TABLE contributions ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1")
    }
}

/**
 * Migration 14 → 15: Extend transactions for unified ledger.
 * All Transactions becomes the single source of truth for every financial activity.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Extended metadata for unified ledger
        db.execSQL("ALTER TABLE transactions ADD COLUMN relatedAccountId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN goalId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN goalName TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN transferId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN sourceModule TEXT NOT NULL DEFAULT 'transaction'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN referenceId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isAuto INTEGER NOT NULL DEFAULT 0")
        // Indexes for new query patterns
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions (accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_goalId ON transactions (goalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_transferId ON transactions (transferId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions (type)")
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
        DraftTransaction::class,
        Account::class,
        com.example.insightku.feature.planning.goal.data.model.GoalEntity::class,
        com.example.insightku.feature.planning.goal.data.model.ContributionEntity::class,
        com.example.insightku.feature.planning.goal.data.model.GoalAccountEntity::class,
        com.example.insightku.feature.planning.goal.data.model.ReservedBalanceEntity::class,
        com.example.insightku.feature.planning.goal.data.model.AutoAllocationRuleEntity::class,
        com.example.insightku.feature.planning.goal.data.model.DailyTargetEntity::class
    ],
    version = 17,
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
    abstract fun accountDao(): AccountDao

    // Goals feature DAOs
    abstract fun goalDao(): com.example.insightku.feature.planning.goal.data.local.dao.GoalDao
    abstract fun contributionDao(): com.example.insightku.feature.planning.goal.data.local.dao.ContributionDao
    abstract fun goalAccountDao(): com.example.insightku.feature.planning.goal.data.local.dao.GoalAccountDao
    abstract fun reservedBalanceDao(): com.example.insightku.feature.planning.goal.data.local.dao.ReservedBalanceDao
    abstract fun autoAllocationRuleDao(): com.example.insightku.feature.planning.goal.data.local.dao.AutoAllocationRuleDao
    abstract fun dailyTargetDao(): com.example.insightku.feature.planning.goal.data.local.dao.DailyTargetDao

    // Budget allocation DAOs
    abstract fun budgetAllocationDao(): BudgetAllocationDao
}


