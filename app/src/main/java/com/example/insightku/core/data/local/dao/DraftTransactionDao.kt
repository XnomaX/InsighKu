package com.example.insightku.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.insightku.core.data.model.DraftStatus
import com.example.insightku.core.data.model.DraftTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftTransactionDao {

    /** Draft yang menunggu ditinjau, terbaru di atas. Sumber data kartu Draft Inbox di Home. */
    @Query("SELECT * FROM draft_transactions WHERE status = 'PENDING' ORDER BY detectedAt DESC")
    fun getPendingDrafts(): Flow<List<DraftTransaction>>

    /** Jumlah draft pending — dipakai untuk badge & pemicu reminder. */
    @Query("SELECT COUNT(*) FROM draft_transactions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    /** Pending count sekali ambil (non-reaktif) — dipakai reminder worker. */
    @Query("SELECT COUNT(*) FROM draft_transactions WHERE status = 'PENDING'")
    suspend fun getPendingCountOnce(): Int

    /** Draft pending tertua — dipakai reminder worker untuk cek usia > ambang. */
    @Query("SELECT * FROM draft_transactions WHERE status = 'PENDING' ORDER BY detectedAt ASC LIMIT 1")
    suspend fun getOldestPending(): DraftTransaction?

    @Query("SELECT * FROM draft_transactions WHERE id = :id")
    suspend fun getById(id: String): DraftTransaction?

    /**
     * Insert draft baru. IGNORE pada unique dedupHash → notifikasi yang sama
     * tidak membuat draft ganda. Return rowId; -1 berarti diabaikan (duplikat).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(draft: DraftTransaction): Long

    @Update
    suspend fun update(draft: DraftTransaction)

    /** Tandai dismissed (soft-delete selama jendela undo). */
    @Query("UPDATE draft_transactions SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: DraftStatus)

    /** Hard delete satu draft — dipakai setelah confirm atau setelah jendela undo habis. */
    @Query("DELETE FROM draft_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Bersihkan semua draft — dipakai saat logout. */
    @Query("DELETE FROM draft_transactions")
    suspend fun deleteAll()

    // ─── Auto-allocation draft queries ──────────────────────────────────────

    /** Pending allocation drafts for Draft Inbox. */
    @Query("SELECT * FROM draft_transactions WHERE status = 'PENDING' AND draftType = 'AUTO_ALLOCATION' ORDER BY detectedAt DESC")
    fun getPendingAllocationDrafts(): Flow<List<DraftTransaction>>

    /** Count pending allocation drafts. */
    @Query("SELECT COUNT(*) FROM draft_transactions WHERE status = 'PENDING' AND draftType = 'AUTO_ALLOCATION'")
    fun getPendingAllocationDraftCount(): Flow<Int>

    /** Get allocation draft by rule ID (for dedup). */
    @Query("SELECT * FROM draft_transactions WHERE ruleId = :ruleId AND status = 'PENDING' AND draftType = 'AUTO_ALLOCATION' LIMIT 1")
    suspend fun getPendingAllocationDraftByRule(ruleId: String): DraftTransaction?

    /** Delete all allocation drafts for a specific rule (when rule is deleted). */
    @Query("DELETE FROM draft_transactions WHERE ruleId = :ruleId AND draftType = 'AUTO_ALLOCATION'")
    suspend fun deleteAllocationDraftsByRule(ruleId: String)

    /**
     * Auto-expire stale allocation drafts older than the given timestamp.
     * Marks them as DISMISSED so they disappear from the Inbox.
     */
    @Query("UPDATE draft_transactions SET status = 'DISMISSED' WHERE status = 'PENDING' AND draftType = 'AUTO_ALLOCATION' AND detectedAt < :expireBeforeMillis")
    suspend fun expireStaleAllocationDrafts(expireBeforeMillis: Long): Int
}
