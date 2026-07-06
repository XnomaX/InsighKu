package com.example.insightku.core.data.repository

import com.example.insightku.core.data.local.dao.InstallmentDao
import com.example.insightku.core.data.model.Installment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * InstallmentRepository — single source of truth for installment operations.
 *
 * Handles Room (offline cache) + Firestore (remote sync) for installments.
 * Extracted from TransactionRepository to follow single-responsibility principle.
 */
@Singleton
class InstallmentRepository @Inject constructor(
    private val installmentDao: InstallmentDao,
    private val firestore: FirebaseFirestore
) {
    // ─── Query ─────────────────────────────────────────────────────────────────

    fun getAllInstallments(): Flow<List<Installment>> = installmentDao.getAllInstallments()

    suspend fun hasAnyInstallments(): Boolean = installmentDao.countActiveInstallments() > 0

    // ─── Write (Room + Firestore) ─────────────────────────────────────────────

    suspend fun insertInstallment(installment: Installment, userId: String) {
        installmentDao.insertInstallment(installment)
        try {
            firestore.collection("users").document(userId)
                .collection("installments").document(installment.id).set(installment).await()
        } catch (e: Exception) {
            // Offline — Room already saved
        }
    }

    suspend fun updateInstallment(installment: Installment, userId: String) {
        installmentDao.updateInstallment(installment)
        try {
            firestore.collection("users").document(userId)
                .collection("installments").document(installment.id).set(installment).await()
        } catch (e: Exception) {
            // Offline — Room already updated
        }
    }

    suspend fun deleteInstallment(installmentId: String, userId: String) {
        installmentDao.deleteInstallment(installmentId)
        try {
            firestore.collection("users").document(userId)
                .collection("installments").document(installmentId).delete().await()
        } catch (e: Exception) {
            // Offline — Room already deleted
        }
    }

    // ─── Firestore Sync ───────────────────────────────────────────────────────

    suspend fun refreshInstallments(userId: String) {
        val snapshot = firestore.collection("users").document(userId)
            .collection("installments").get().await()
        val installments = snapshot.toObjects(Installment::class.java)
        installmentDao.insertInstallmentsFromRemote(installments)
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    suspend fun deleteAllLocalInstallments() {
        installmentDao.deleteAllInstallments()
    }
}
