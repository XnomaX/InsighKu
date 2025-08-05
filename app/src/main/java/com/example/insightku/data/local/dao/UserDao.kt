package com.example.insightku.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.insightku.data.model.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("UPDATE users SET streakCount = :streakCount WHERE id = :userId")
    suspend fun updateStreakCount(userId: String, streakCount: Int)

    @Query("UPDATE users SET bestStreak = :bestStreak WHERE id = :userId")
    suspend fun updateBestStreak(userId: String, bestStreak: Int)

    @Query("UPDATE users SET totalTransactions = :totalTransactions WHERE id = :userId")
    suspend fun updateTotalTransactions(userId: String, totalTransactions: Int)

    @Query("UPDATE users SET lastLoginAt = :lastLoginAt WHERE id = :userId")
    suspend fun updateLastLoginAt(userId: String, lastLoginAt: Long)
}
