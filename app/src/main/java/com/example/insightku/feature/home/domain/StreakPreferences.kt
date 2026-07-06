package com.example.insightku.feature.home.domain

import kotlinx.coroutines.flow.Flow

/**
 * StreakPreferences — domain-level interface for streak-related user preferences.
 *
 * Abstracts the persistence mechanism (DataStore, Room, etc.) so that domain
 * use cases remain independent of Android framework components.
 */
interface StreakPreferences {
    val freezeCount: Flow<Int>
    val lastFreezeDate: Flow<String>
    val isPerfectStreak: Flow<Boolean>
    val streakGoal: Flow<Int>
    val repairAvailable: Flow<Boolean>
    val repairExpiry: Flow<Long>
}
