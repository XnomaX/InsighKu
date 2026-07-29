
package com.example.insightku.core.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.BudgetAllocationDao
import com.example.insightku.core.data.local.dao.BudgetDao
import com.example.insightku.core.data.local.dao.CategoryDao
import com.example.insightku.core.data.local.dao.DraftTransactionDao
import com.example.insightku.core.data.local.dao.InstallmentDao
import com.example.insightku.core.data.local.dao.RecurringBudgetDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.local.dao.UserDao
import com.example.insightku.core.data.local.database.InsightKuDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val ROOM_PERF = "RoomPerf"

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
            .addMigrations(*InsightKuDatabase.ALL_MIGRATIONS)
            .fallbackToDestructiveMigration(true)
            .setQueryCallback(
                androidx.room.RoomDatabase.QueryCallback { query, bindArgs ->
                    Log.d(ROOM_PERF, "SQL fired: $query | args=$bindArgs")
                },
                java.util.concurrent.Executors.newSingleThreadExecutor()
            )
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
    fun provideBudgetAllocationDao(database: InsightKuDatabase): BudgetAllocationDao {
        return database.budgetAllocationDao()
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

    // Goals feature DAOs
    @Provides
    fun provideGoalDao(database: InsightKuDatabase): com.example.insightku.feature.planning.goal.data.local.dao.GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideContributionDao(database: InsightKuDatabase): com.example.insightku.feature.planning.goal.data.local.dao.ContributionDao {
        return database.contributionDao()
    }

    @Provides
    fun provideGoalAccountDao(database: InsightKuDatabase): com.example.insightku.feature.planning.goal.data.local.dao.GoalAccountDao {
        return database.goalAccountDao()
    }

    @Provides
    fun provideReservedBalanceDao(database: InsightKuDatabase): com.example.insightku.feature.planning.goal.data.local.dao.ReservedBalanceDao {
        return database.reservedBalanceDao()
    }

    @Provides
    fun provideAutoAllocationRuleDao(database: InsightKuDatabase): com.example.insightku.feature.planning.goal.data.local.dao.AutoAllocationRuleDao {
        return database.autoAllocationRuleDao()
    }

    @Provides
    fun provideDailyTargetDao(database: InsightKuDatabase): com.example.insightku.feature.planning.goal.data.local.dao.DailyTargetDao {
        return database.dailyTargetDao()
    }
}
