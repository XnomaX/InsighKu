package com.example.insightku.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.insightku.data.local.dao.*
import com.example.insightku.data.model.*

@Database(
    entities = [
        Transaction::class,
        Category::class,
        RecurringBudget::class,
        Budget::class,
        User::class
    ],
    version = 3,       // v3: Tambah isSynced + createdAt ke Transaction (offline-first)
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

