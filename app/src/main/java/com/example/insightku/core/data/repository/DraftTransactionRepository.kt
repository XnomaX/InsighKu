package com.example.insightku.core.data.repository

import com.example.insightku.core.data.local.dao.DraftTransactionDao
import com.example.insightku.core.data.model.DraftStatus
import com.example.insightku.core.data.model.DraftTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DraftTransactionRepository — siklus hidup draft hasil deteksi notifikasi bank.
 *
 * DEVICE-LOCAL: repository ini sengaja TIDAK menyentuh Firestore. Draft adalah
 * niat sementara, bukan data keuangan, dan tidak boleh meninggalkan perangkat.
 *
 * Repo ini juga TIDAK menulis ke tabel transaksi utama. Konfirmasi (draft → Transaction)
 * mengalir lewat AddTransaction yang sudah ada; setelah simpan berhasil, pemanggil
 * memanggil [confirmAndRemove] untuk menghapus draft. Pemisahan ini menjaga prinsip
 * human-confirmation: tidak ada jalur yang menulis transaksi tanpa aksi user.
 */
@Singleton
class DraftTransactionRepository @Inject constructor(
    private val draftDao: DraftTransactionDao
) {
    /** Draft pending untuk kartu Draft Inbox di Home. */
    fun observePendingDrafts(): Flow<List<DraftTransaction>> = draftDao.getPendingDrafts()

    /** Jumlah pending untuk badge & visibilitas kartu. */
    fun observePendingCount(): Flow<Int> = draftDao.getPendingCount()

    suspend fun getById(id: String): DraftTransaction? = draftDao.getById(id)

    /** Jumlah draft pending sekali ambil — dipakai reminder worker. */
    suspend fun getPendingCountOnce(): Int = draftDao.getPendingCountOnce()

    /** Draft pending tertua — dipakai reminder worker untuk cek usia. */
    suspend fun getOldestPending(): DraftTransaction? = draftDao.getOldestPending()

    /**
     * Simpan draft baru. Return true jika benar-benar tersimpan, false jika
     * diabaikan karena duplikat (dedupHash sama). Insert pakai IGNORE.
     */
    suspend fun createDraft(draft: DraftTransaction): Boolean {
        val rowId = draftDao.insert(draft)
        return rowId != -1L
    }

    /**
     * Dismiss draft: tandai DISMISSED dulu supaya langsung hilang dari Inbox,
     * tanpa hard delete — pemanggil (UI) menjalankan snackbar undo 5 detik.
     * Jika user menekan undo → [restore]. Jika tidak → [purgeDismissed].
     */
    suspend fun dismiss(id: String) = draftDao.setStatus(id, DraftStatus.DISMISSED)

    /** Batalkan dismiss (user menekan undo dalam 5 detik). */
    suspend fun restore(id: String) = draftDao.setStatus(id, DraftStatus.PENDING)

    /** Hard delete setelah jendela undo habis. Tidak ada tombstone (hemat memori). */
    suspend fun purgeDismissed(id: String) = draftDao.deleteById(id)

    /** Hapus draft setelah transaksi berhasil dikonfirmasi & disimpan via AddTransaction. */
    suspend fun confirmAndRemove(id: String) = draftDao.deleteById(id)

    /** Bersihkan semua draft saat logout. */
    suspend fun deleteAll() = draftDao.deleteAll()

    companion object {
        /** Kunci dedup stabil dari hasil parser. */
        fun dedupHashOf(amount: Double, merchant: String, bankName: String): String =
            "${amount.toLong()}|${merchant.trim().lowercase()}|${bankName.trim().lowercase()}"
    }
}
