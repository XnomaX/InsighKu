package com.example.insightku.domain.usecase.auth

import com.example.insightku.data.repository.AuthRepository
import com.example.insightku.data.repository.TransactionRepository
import javax.inject.Inject

/**
 * LoginUseCase — enkapsulasi business logic proses login.
 *
 * ROOT CAUSE FIX (Data Persistence after Logout+Login):
 * ---------------------------------------------------------
 * SEBELUMNYA: LoginUseCase hanya memanggil authRepository.loginUser().
 * Setelah login, DashboardViewModel dibuat dan subscribe ke Room Flow yang KOSONG
 * (karena LogoutUseCase sudah menghapus semua data Room saat logout).
 * refreshData() di ViewModel.init() dipanggil terlambat dan di background —
 * ada window kosong di mana UI menampilkan data empty.
 *
 * ROOT CAUSE: Tidak ada langkah "isi Room dari Firestore" setelah login berhasil.
 *
 * SEKARANG (benar):
 * Setelah Firebase auth berhasil, langsung fetch transaksi dari Firestore dan
 * simpan ke Room SEBELUM return Result.success(). Ini memastikan:
 * 1. Ketika DashboardViewModel dibuat dan collect Room Flow → data sudah ada
 * 2. Tidak ada race condition antara "ViewModel subscribe" dan "data tersedia"
 * 3. Tidak ada layar kosong setelah login
 *
 * Kenapa di UseCase dan bukan di ViewModel?
 * "Fetch data after auth" adalah business rule, bukan UI concern.
 * UseCase adalah layer yang tepat untuk orchestrate antara AuthRepository
 * dan TransactionRepository.
 *
 * Kenapa return Result<Unit> dan bukan throw?
 * Pre-fetch adalah best-effort. Jika gagal (network issue), user tetap
 * bisa masuk ke app — Room kosong, ViewModel akan coba refresh lagi.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        // Langkah 1: Firebase auth
        val loginResult = authRepository.loginUser(email, password)
        if (loginResult.isFailure) return loginResult

        // Langkah 2: Pre-fetch data ke Room segera setelah login berhasil
        // agar Room tidak kosong saat DashboardViewModel pertama kali subscribe.
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            runCatching {
                // Best-effort: jika gagal (offline/network), tetap lanjut login.
                // ViewModel.init() akan retry refresh secara background.
                transactionRepository.refreshTransactions(userId)
                transactionRepository.refreshCategories(userId)
            }
        }

        return Result.success(Unit)
    }
}

