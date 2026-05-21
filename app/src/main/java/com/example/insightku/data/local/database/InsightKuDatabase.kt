package com.example.insightku.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.insightku.data.local.dao.*
import com.example.insightku.data.model.*

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN recurringPeriod TEXT")
    }
}

@Database(
    entities = [
        Transaction::class,
        Category::class,
        RecurringBudget::class,
        Budget::class,
        User::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InsightKuDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringBudgetDao(): RecurringBudgetDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao
}

