package com.example.insightku.domain.usecase.transaction

import com.example.insightku.data.model.Transaction
import com.example.insightku.data.repository.AuthRepository
import com.example.insightku.data.repository.TransactionRepository
import javax.inject.Inject

/**
 * AddTransactionUseCase — menambah transaksi baru ke Room dan Firestore.
 *
 * SEBELUMNYA:
 * DashboardViewModel memanggil transactionRepository.addTransaction() langsung
 * dan juga memanggil authRepository.getCurrentUserId() — tight coupling.
 *
 * SEKARANG:
 * UseCase mengorkestrasi: cek userId dulu, lalu simpan ke repository.
 * ViewModel tidak perlu tahu detail implementasi penyimpanan.
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User tidak login"))
        return try {
            transactionRepository.addTransaction(transaction, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
