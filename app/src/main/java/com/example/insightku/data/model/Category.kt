package com.example.insightku.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.UUID

/**
 * CRITICAL FIX: Sama seperti Transaction, Category juga butuh no-arg constructor
 * agar Firestore toObjects() bisa deserialize. Semua field wajib punya default value.
 */
@Keep
@IgnoreExtraProperties
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val color: String = "#6200EE",
    val budgetLimit: Double? = null,
    val icon: String? = null,
    val isActive: Boolean = true,
    val alertThreshold: Int = 80,
    val recurringPeriod: String? = null  // "Weekly", "Monthly", "Yearly", or null
)

