package com.example.insightku.core.data.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transaction — model tunggal untuk Room (local cache) dan Firestore (remote).
 *
 * CRITICAL FIX (sebelumnya): Semua field wajib punya default value agar Firestore
 * `toObjects()` bisa membuat no-arg constructor.
 *
 * ISSUE 2 FIX (Offline-First):
 * Tambah dua field untuk tracking sync status:
 *
 * - [isSynced]: false = hanya ada di Room lokal, belum dikirim ke Firestore.
 *   `addTransaction()` di Repository menyimpan ke Room dengan isSynced=false.
 *   Jika Firestore berhasil → update isSynced=true.
 *   WorkManager mem-query semua isSynced=false dan retry upload saat network available.
 *
 * - [createdAt]: timestamp saat transaksi pertama dibuat di device (berbeda dari [date]
 *   yang merupakan tanggal transaksi yang dipilih user). Berguna untuk sorting dan
 *   deduplication saat sync.
 *
 * @PropertyName("isSynced"): Eksplisit mapping nama field ke Firestore agar tidak
 * terdampak ProGuard renaming di release build.
 *
 * @IgnoreExtraProperties: Jika dokumen Firestore punya field yang belum ada di model,
 * deserialisasi tidak crash. Penting untuk forward compatibility.
 *
 * @Keep: Mencegah ProGuard menghapus/rename field → Firestore mapping tetap valid.
 */
@Keep
@IgnoreExtraProperties
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["accountId"]),
        Index(value = ["goalId"]),
        Index(value = ["transferId"]),
        Index(value = ["type"])
    ]
)
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val description: String? = null,
    val receiptPath: String? = null,
    val time: String = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date()),
    val location: String? = null,
    /**
     * ID of the Account this transaction belongs to.
     * Every transaction MUST belong to exactly one Account.
     */
    val accountId: String = "",

    // ── Extended Metadata (All Transactions as Single Source of Truth) ──────
    /** For transfers: the other account involved. */
    val relatedAccountId: String? = null,
    /** For goal contributions/withdrawals/auto-allocation. */
    val goalId: String? = null,
    /** Human-readable goal name (denormalized for display). */
    val goalName: String? = null,
    /** For transfers: shared ID linking the pair of transactions. */
    val transferId: String? = null,
    /** Module that created this transaction: transaction, transfer, goal, auto_allocation, adjustment. */
    @ColumnInfo(defaultValue = "'transaction'")
    val sourceModule: String = "transaction",
    /** Reference to the originating entity (e.g. contribution ID). */
    val referenceId: String? = null,
    /** True if created by Auto Allocation rule. Shows "Auto" badge. */
    @ColumnInfo(defaultValue = "0")
    val isAuto: Boolean = false,

    // ── Offline-First Sync Fields ───────────────────────────────────────────
    /** true = sudah ada di Firestore, false = hanya di Room (belum sync) */
    @get:PropertyName("isSynced")
    @set:PropertyName("isSynced")
    var isSynced: Boolean = false,

    /**
     * LEGACY — tidak lagi dipakai. Draft kini hidup di tabel terpisah.
     * Selalu false untuk transaksi baru.
     */
    @get:PropertyName("isDraft")
    @set:PropertyName("isDraft")
    var isDraft: Boolean = false,

    /** Waktu transaksi dibuat di device (epoch ms). Berbeda dari [date] (tanggal transaksi). */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * All transaction types supported by the unified ledger.
 * Every financial event that changes an account balance creates a Transaction
 * with one of these types.
 */
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER_OUT,
    TRANSFER_IN,
    GOAL_CONTRIBUTION,
    GOAL_WITHDRAWAL,
    AUTO_ALLOCATION,
    BALANCE_ADJUSTMENT;

    companion object {
        fun fromString(value: String): TransactionType =
            entries.find { it.name == value } ?: EXPENSE

        /**
         * Calculate the balance delta for a given transaction type.
         * Positive = money comes in, negative = money goes out.
         * Used by TransactionRepository, AccountDao, and AccountsViewModel.
         */
        fun balanceDelta(type: TransactionType, amount: Double): Double = when (type) {
            INCOME -> amount
            EXPENSE -> -amount
            TRANSFER_IN -> amount
            TRANSFER_OUT -> -amount
            GOAL_CONTRIBUTION -> -amount
            GOAL_WITHDRAWAL -> amount
            AUTO_ALLOCATION -> -amount
            BALANCE_ADJUSTMENT -> amount
        }
    }
}



