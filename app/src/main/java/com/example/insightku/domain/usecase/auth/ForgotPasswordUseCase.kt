package com.example.insightku.domain.usecase.auth

import com.example.insightku.data.repository.AuthRepository
import javax.inject.Inject

/**
 * ForgotPasswordUseCase — mengirim email reset password via Firebase.
 *
 * SEBELUMNYA (salah):
 * ForgotPasswordViewModel mensimulasikan pengiriman dengan delay(1500) —
 * tidak ada email yang benar-benar terkirim ke user.
 * ForgotPasswordViewModel juga tidak @HiltViewModel sehingga tidak bisa
 * menerima dependency injection.
 *
 * SEKARANG: Email reset sungguhan via Firebase Auth.
 */
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(Exception("Email tidak boleh kosong"))
        return authRepository.sendPasswordResetEmail(email)
    }
}
