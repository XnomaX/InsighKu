package com.example.insightku.core.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.UUID

@Keep
@IgnoreExtraProperties
@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val categoryId: String? = null,
    /**
     * ID of the Account to use for transactions generated from this installment.
     */
    val accountId: String? = null,
    val iconName: String? = null,
    val color: String? = null,
    val totalAmount: Double = 0.0,
    val monthlyPayment: Double = 0.0,
    val totalMonths: Int = 0,
    val paidMonths: Int = 0,
    val firstPaymentDate: Long = System.currentTimeMillis(),
    val nextDueDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = ""
) {
    val remainingMonths: Int get() = (totalMonths - paidMonths).coerceAtLeast(0)
    val remainingBalance: Double get() = (monthlyPayment * remainingMonths).coerceAtLeast(0.0)
    val progressFraction: Float get() = if (totalMonths > 0) (paidMonths.toFloat() / totalMonths).coerceIn(0f, 1f) else 0f
    val progressPercent: Int get() = (progressFraction * 100).toInt()
    val isCompleted: Boolean get() = paidMonths >= totalMonths
}

