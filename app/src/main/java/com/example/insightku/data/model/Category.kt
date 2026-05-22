package com.example.insightku.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
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
    val isSystemCategory: Boolean = false
) {
    val type: CategoryType get() = runCatching {
        CategoryType.valueOf(categoryType)
    }.getOrDefault(CategoryType.EXPENSE)

    // True if this is a protected system category — checked by ID prefix
    // since Firestore stores "systemCategory" but Room uses "isSystemCategory"
    val isProtected: Boolean get() = id.startsWith("system-")
}

