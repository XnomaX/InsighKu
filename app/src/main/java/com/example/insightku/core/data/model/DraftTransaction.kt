package com.example.insightku.core.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * DraftTransaction — hasil deteksi parser dari notifikasi bank yang MENUNGGU konfirmasi user.
 *
 * Tabel ini DEVICE-LOCAL saja: TIDAK pernah disinkronkan ke Firestore.
 * Alasan: draft adalah niat sementara, bukan data keuangan. Ini juga memperkuat
 * janji privasi "isi notifikasi tidak dikirim ke server".
 *
 * ISOLASI: draft hidup di tabelnya sendiri, terpisah dari [Transaction]. Query
 * Analytics/Budgeting/Balance tidak akan pernah menyentuh tabel ini, sehingga
 * prinsip "Analytics & Budgeting hanya pakai transaksi terkonfirmasi" terjaga
 * by construction — bukan by disiplin query.
 *
 * Field bernama *Guess karena ini tebakan parser yang boleh salah. User selalu
 * berhak mengoreksi sebelum [DraftStatus.CONFIRMED].
 */
@Entity(
    tableName = "draft_transactions",
    indices = [Index(value = ["dedupHash"], unique = true)]
)
data class DraftTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amountGuess: Double = 0.0,
    val typeGuess: TransactionType = TransactionType.EXPENSE,
    val merchantGuess: String = "",
    val bankName: String = "",
    /** package sumber notifikasi (untuk audit & cross-check whitelist consent) */
    val sourcePackage: String = "",
    /** judul & isi notifikasi mentah — dipakai saat user mengoreksi hasil parser */
    val rawTitle: String = "",
    val rawContent: String = "",
    val confidence: DraftConfidence = DraftConfidence.MEDIUM,
    val status: DraftStatus = DraftStatus.PENDING,
    /** epoch ms saat notifikasi terdeteksi */
    val detectedAt: Long = System.currentTimeMillis(),
    /**
     * Kunci dedup stabil "amount|merchant|bank". Insert pakai OnConflictStrategy.IGNORE
     * pada kolom ini untuk mencegah notifikasi yang sama membuat draft ganda.
     * Catatan: tombstone retensi panjang sengaja TIDAK dipakai (hemat memori) —
     * perlindungan duplikat jangka pendek tetap ada di dedup window in-memory parser.
     */
    val dedupHash: String = ""
)

/**
 * Tingkat keyakinan parser, ditampilkan ke user (bukan disembunyikan):
 * - [HIGH]   amount + type + merchant jelas → ring penuh, tanpa peringatan.
 * - [MEDIUM] amount jelas, type/merchant agak ragu → ring separuh + "perlu kamu pastikan".
 * - [LOW]    merchant fallback ke nama bank / type tebakan → tanda ⚠, draft TETAP dibuat.
 */
enum class DraftConfidence { HIGH, MEDIUM, LOW }

/**
 * Siklus hidup draft. CONFIRMED & DISMISSED bersifat terminal di tabel ini:
 * - CONFIRMED → ditulis sebagai [Transaction] utuh lalu draft dihapus.
 * - DISMISSED → soft-delete HANYA selama jendela undo 5 detik, lalu hard delete.
 */
enum class DraftStatus { PENDING, DISMISSED }
