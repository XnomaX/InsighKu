package com.example.insightku.domain.usecase.auth

import com.example.insightku.data.repository.AuthRepository
import javax.inject.Inject

/**
 * SignUpUseCase — menggantikan logika Firebase langsung di SignUpViewModel.
 *
 * SEBELUMNYA (salah):
 * SignUpViewModel langsung memanggil FirebaseAuth.getInstance() — ini melanggar
 * Dependency Inversion Principle. ViewModel depend pada implementasi konkret (Firebase),
 * bukan pada abstraksi (Repository/UseCase).
 *
 * SEKARANG (benar):
 * SignUpViewModel → SignUpUseCase → AuthRepository → FirebaseAuth
 * Jika besok ganti Firebase ke Supabase, hanya AuthRepository yang perlu diubah.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, name: String): Result<Unit> {
        return authRepository.signUpUser(email, password, name)
    }
}
