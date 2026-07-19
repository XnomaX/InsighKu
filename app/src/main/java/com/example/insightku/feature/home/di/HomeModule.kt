package com.example.insightku.feature.home.di

import com.example.insightku.feature.home.data.DataStoreStreakPreferences
import com.example.insightku.feature.home.domain.StreakPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Feature-local Hilt bindings for the home feature.
 * Keeps core DI free of feature-type dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    @Singleton
    abstract fun bindStreakPreferences(impl: DataStoreStreakPreferences): StreakPreferences
}
