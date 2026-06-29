package com.example.insightku.core.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.UUID

/**
 * Account type representing the category of a financial account.
 */
enum class AccountType(val displayName: String) {
    CASH("Cash"),
    BANK_ACCOUNT("Bank Account"),
    E_WALLET("E-Wallet"),
    CREDIT_CARD("Credit Card");

    companion object {
        fun fromString(value: String): AccountType =
            entries.find { it.name == value } ?: BANK_ACCOUNT
    }
}

/**
 * Account model representing a user's financial account.
 * Used for tracking money, assets, and liabilities.
 */
@Keep
@IgnoreExtraProperties
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val accountType: String = AccountType.BANK_ACCOUNT.name,
    val balance: Double = 0.0,
    val color: String = "#7C4DFF",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("isDefault")
    @set:PropertyName("isDefault")
    var isDefault: Boolean = false
) {
    val type: AccountType get() = AccountType.fromString(accountType)

    /**
     * Net value: positive for assets (Cash, Bank, E-Wallet),
     * negative for liabilities (Credit Card)
     */
    val netValue: Double get() = when (type) {
        AccountType.CREDIT_CARD -> -balance
        else -> balance
    }

    /**
     * Whether this account represents a liability
     */
    val isLiability: Boolean get() = type == AccountType.CREDIT_CARD

    /**
     * Whether this account represents an asset
     */
    val isAsset: Boolean get() = !isLiability
}