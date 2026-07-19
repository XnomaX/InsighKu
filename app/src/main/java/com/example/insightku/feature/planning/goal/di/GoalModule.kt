package com.example.insightku.feature.planning.goal.di

import com.example.insightku.feature.planning.goal.data.repository.AutoAllocationDataSourceImpl
import com.example.insightku.feature.planning.goal.domain.engine.AutoAllocationDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Feature-local Hilt bindings for the goal feature.
 * Keeps core DI free of feature-type dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GoalModule {

    @Binds
    @Singleton
    abstract fun bindAutoAllocationDataSource(impl: AutoAllocationDataSourceImpl): AutoAllocationDataSource
}
