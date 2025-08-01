package com.example.insightku.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String,
    val budgetLimit: Double? = null,
    val icon: String? = null,
    val isActive: Boolean = true
)