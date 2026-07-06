package com.example.insightku.feature.home.data

import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.feature.home.domain.StreakPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStoreStreakPreferences — data-layer implementation of [StreakPreferences].
 *
 * Delegates to [UserPreferencesDataStore] for the actual persistence,
 * keeping the domain layer free of Android framework dependencies.
 */
@Singleton
class DataStoreStreakPreferences @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : StreakPreferences {

    override val freezeCount: Flow<Int> = dataStore.freezeCount
    override val lastFreezeDate: Flow<String> = dataStore.lastFreezeDate
    override val isPerfectStreak: Flow<Boolean> = dataStore.isPerfectStreak
    override val streakGoal: Flow<Int> = dataStore.streakGoal
    override val repairAvailable: Flow<Boolean> = dataStore.repairAvailable
    override val repairExpiry: Flow<Long> = dataStore.repairExpiry
}
