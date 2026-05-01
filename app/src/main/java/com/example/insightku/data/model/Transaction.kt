package com.example.insightku.data.model

import androidx.annotation.Keep
import androidx.room.Entity
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
@Entity(tableName = "transactions")
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
    val paymentMethod: String? = null,

    // ── Offline-First Sync Fields ───────────────────────────────────────────
    /** true = sudah ada di Firestore, false = hanya di Room (belum sync) */
    @get:PropertyName("isSynced")
    @set:PropertyName("isSynced")
    var isSynced: Boolean = false,

    /** Waktu transaksi dibuat di device (epoch ms). Berbeda dari [date] (tanggal transaksi). */
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType {
    INCOME, EXPENSE
}


