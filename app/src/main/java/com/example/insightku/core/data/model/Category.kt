package com.example.insightku.core.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.UUID

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
    val recurringPeriod: String? = null,
    val categoryType: String = CategoryType.EXPENSE.name,
    @get:PropertyName("isSystemCategory")
    @set:PropertyName("isSystemCategory")
    var isSystemCategory: Boolean = false
) {
    val type: CategoryType get() = runCatching {
        CategoryType.valueOf(categoryType)
    }.getOrDefault(CategoryType.EXPENSE)
}


