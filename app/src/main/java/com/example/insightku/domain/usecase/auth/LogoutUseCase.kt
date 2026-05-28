package com.example.insightku.domain.usecase.auth

import com.example.insightku.data.repository.AuthRepository
import com.example.insightku.data.repository.TransactionRepository
import javax.inject.Inject

/**
 * LogoutUseCase — implementasi logout yang BENAR dan lengkap.
 *
 * SEBELUMNYA (SALAH — 3 hal tidak dilakukan):
 * ```kotlin
 * // SettingsViewModel.kt
 * private fun logout() {
 *     // sessionManager.clearSession() ← DI-COMMENT OUT
 *     _uiState.value = SettingsUiState(userEmail = "", userName = "")
 * }
 * ```
 * Akibatnya:
 * 1. Firebase Auth token masih aktif (security risk)
 * 2. DataStore masih isLoggedIn = true → buka app lagi langsung ke Home
 * 3. Data transaksi user sebelumnya masih ada di Room → user berbeda melihat data lama
 *
 * SEKARANG (benar — 3 langkah dijalankan semua):
 * 1. Firebase Auth sign out → invalidate token
 * 2. DataStore clear → SplashScreen akan arahkan ke Auth saat buka app lagi
 * 3. Room cache clear → tidak ada data leak antar user
 *
 * Kenapa UseCase dan bukan langsung di Repository?
 * Logout perlu koordinasi 2 repository (AuthRepository + TransactionRepository).
 * Repository seharusnya tidak depend satu sama lain → UseCase adalah tempat yang tepat.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            // Langkah 1 & 2: Firebase signOut + DataStore clear
            authRepository.logout().getOrThrow()

            // Langkah 3: Bersihkan semua cache Room
            transactionRepository.deleteAllLocalTransactions()
            transactionRepository.deleteAllLocalCategories()
            transactionRepository.deleteAllLocalRecurringBudgets()
            transactionRepository.deleteAllLocalInstallments()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
