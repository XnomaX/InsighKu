package com.example.insightku.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.BudgetAllocationDao
import com.example.insightku.core.data.local.dao.BudgetDao
import com.example.insightku.core.data.local.dao.CategoryDao
import com.example.insightku.core.data.local.dao.DraftTransactionDao
import com.example.insightku.core.data.local.dao.InstallmentDao
import com.example.insightku.core.data.local.dao.RecurringBudgetDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.local.dao.UserDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Budget
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.DraftTransaction
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.User
import com.example.insightku.feature.planning.goal.data.local.dao.AutoAllocationRuleDao
import com.example.insightku.feature.planning.goal.data.local.dao.ContributionDao
import com.example.insightku.feature.planning.goal.data.local.dao.DailyTargetDao
import com.example.insightku.feature.planning.goal.data.local.dao.GoalAccountDao
import com.example.insightku.feature.planning.goal.data.local.dao.GoalDao
import com.example.insightku.feature.planning.goal.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.planning.goal.data.model.ContributionEntity
import com.example.insightku.feature.planning.goal.data.model.DailyTargetEntity
import com.example.insightku.feature.planning.goal.data.model.GoalAccountEntity
import com.example.insightku.feature.planning.goal.data.model.GoalEntity

/**
 * InsightKuDatabase — single Room database.
 *
 * No migrations: the app has never shipped to users, so the schema starts at version 1.
 * A future schema change must add an explicit migration (never `fallbackToDestructiveMigration`).
 */
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
        GoalEntity::class,
        ContributionEntity::class,
        GoalAccountEntity::class,
        AutoAllocationRuleEntity::class,
        DailyTargetEntity::class
    ],
    version = 1,
    exportSchema = true
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
    abstract fun goalDao(): GoalDao
    abstract fun contributionDao(): ContributionDao
    abstract fun goalAccountDao(): GoalAccountDao
    abstract fun autoAllocationRuleDao(): AutoAllocationRuleDao
    abstract fun dailyTargetDao(): DailyTargetDao
    abstract fun budgetAllocationDao(): BudgetAllocationDao
}
