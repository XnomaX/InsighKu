/*
package com.example.insightku.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType,
    val description: String? = null,
    val receiptPath: String? = null
)

enum class TransactionType {
    INCOME, EXPENSE
}*/
