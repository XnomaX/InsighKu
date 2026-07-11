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

    // ─── Auto-allocation draft methods ──────────────────────────────────────

    /** Observe pending allocation drafts for Draft Inbox. */
    fun observePendingAllocationDrafts(): Flow<List<DraftTransaction>> =
        draftDao.getPendingAllocationDrafts()

    /** Count pending allocation drafts. */
    fun observePendingAllocationDraftCount(): Flow<Int> =
        draftDao.getPendingAllocationDraftCount()

    /**
     * Create an auto-allocation draft. Returns true if created, false if duplicate.
     * Dedup is based on ruleId — only one pending draft per rule at a time.
     */
    suspend fun createAllocationDraft(draft: DraftTransaction): Boolean {
        // Check if there's already a pending draft for this rule
        val existing = draft.ruleId?.let { draftDao.getPendingAllocationDraftByRule(it) }
        if (existing != null) {
            // Update existing draft with latest data instead of creating duplicate
            draftDao.update(draft.copy(id = existing.id, detectedAt = existing.detectedAt))
            return true
        }
        val rowId = draftDao.insert(draft)
        return rowId != -1L
    }    /** Delete allocation drafts for a specific rule (when rule is deleted). */
    suspend fun deleteAllocationDraftsByRule(ruleId: String) = draftDao.deleteAllocationDraftsByRule(ruleId)

    /**
     * Auto-expire stale allocation drafts older than the given TTL.
     * @param ttlMs Time-to-live in milliseconds (default 48 hours)
     * @return number of expired drafts
     */
    suspend fun expireStaleAllocationDrafts(ttlMs: Long = ALLOCATION_DRAFT_TTL_MS): Int {
        val expireBefore = System.currentTimeMillis() - ttlMs
        return draftDao.expireStaleAllocationDrafts(expireBefore)
    }

    companion object {
        /** Default TTL for pending allocation drafts: 48 hours. */
        const val ALLOCATION_DRAFT_TTL_MS = 48L * 60 * 60 * 1000

        /** Kunci dedup stabil dari hasil parser. */
        fun dedupHashOf(amount: Double, merchant: String, bankName: String): String =
            "${amount.toLong()}|${merchant.trim().lowercase()}|${bankName.trim().lowercase()}"

        /** Generate dedup hash for auto-allocation drafts. */
        fun allocationDedupHash(ruleId: String, goalId: String, amount: Double): String =
            "alloc|${ruleId}|${goalId}|${amount.toLong()}"
    }
}
