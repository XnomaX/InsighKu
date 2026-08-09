package com.example.insightku.feature.auth.domain

import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.feature.auth.data.AuthRepository
import javax.inject.Inject

/**
 * GoogleSignInUseCase — enkapsulasi business logic Google Sign-In.
 *
 * ROOT CAUSE FIX (Data Persistence after Logout+Login):
 * Sama seperti LoginUseCase, setelah Google auth berhasil, langsung
 * pre-fetch transaksi dari Firestore ke Room agar DashboardViewModel
 * tidak mendapatkan data kosong saat pertama kali subscribe ke Room Flow.
 */
class GoogleSignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(idToken: String): Result<Unit> {
        // Langkah 1: Google Firebase auth
        val signInResult = authRepository.signInWithGoogle(idToken)
        if (signInResult.isFailure) return signInResult

        // Langkah 2: Pre-fetch data ke Room (best-effort)
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            runCatching {
                transactionRepository.refreshTransactions(userId)
                categoryRepository.refreshCategories(userId)
            }
        }

        return Result.success(Unit)
    }
}




