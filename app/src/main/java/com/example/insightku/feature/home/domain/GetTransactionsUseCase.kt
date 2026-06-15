package com.example.insightku.feature.home.domain

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * GetTransactionsUseCase — mengambil semua transaksi milik user yang sedang login.
 *
 * SEBELUMNYA:
 * DashboardViewModel dan AnalyticsViewModel masing-masing memanggil
 * transactionRepository.getAllTransactions() langsung, dan juga memanggil
 * authRepository.getCurrentUserId() sendiri-sendiri — duplikasi.
 *
 * SEKARANG:
 * UseCase mengkombinasikan keduanya. Jika userId null (tidak login),
 * return emptyFlow() yang aman.
 *
 * Kenapa return Flow dan bukan suspend?
 * Flow bersifat reaktif — setiap kali data di Room berubah (insert/update/delete),
 * observer (ViewModel) otomatis mendapat update terbaru tanpa perlu polling.
 */
class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        val userId = authRepository.getCurrentUserId()
            ?: return emptyFlow()
        return transactionRepository.getAllTransactions()
    }

    suspend fun refresh() {
        val userId = authRepository.getCurrentUserId() ?: return
        transactionRepository.refreshTransactions(userId)
    }
}



