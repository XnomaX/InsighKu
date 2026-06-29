
package com.example.insightku.core.di

import android.content.Context
import androidx.room.Room
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.BudgetDao
import com.example.insightku.core.data.local.dao.CategoryDao
import com.example.insightku.core.data.local.dao.DraftTransactionDao
import com.example.insightku.core.data.local.dao.InstallmentDao
import com.example.insightku.core.data.local.dao.RecurringBudgetDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.local.dao.UserDao
import com.example.insightku.core.data.local.database.InsightKuDatabase
import com.example.insightku.core.data.local.database.MIGRATION_3_4
import com.example.insightku.core.data.local.database.MIGRATION_4_5
import com.example.insightku.core.data.local.database.MIGRATION_5_6
import com.example.insightku.core.data.local.database.MIGRATION_6_7
import com.example.insightku.core.data.local.database.MIGRATION_7_8
import com.example.insightku.core.data.local.database.MIGRATION_8_9
import com.example.insightku.core.data.local.database.MIGRATION_9_10
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): InsightKuDatabase {
        return Room.databaseBuilder(
            context,
            InsightKuDatabase::class.java,
            "insightku_database"
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(database: InsightKuDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: InsightKuDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideRecurringBudgetDao(database: InsightKuDatabase): RecurringBudgetDao {
        return database.recurringBudgetDao()
    }

    @Provides
    fun provideBudgetDao(database: InsightKuDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    fun provideUserDao(database: InsightKuDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideInstallmentDao(database: InsightKuDatabase): InstallmentDao {
        return database.installmentDao()
    }

    @Provides
    fun provideDraftTransactionDao(database: InsightKuDatabase): DraftTransactionDao {
        return database.draftTransactionDao()
    }

    @Provides
    fun provideAccountDao(database: InsightKuDatabase): AccountDao {
        return database.accountDao()
    }
}
