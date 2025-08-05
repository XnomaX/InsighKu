package com.example.insightku.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val profilePictureUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val streakCount: Int = 0,
    val bestStreak: Int = 0,
    val totalTransactions: Int = 0
)
