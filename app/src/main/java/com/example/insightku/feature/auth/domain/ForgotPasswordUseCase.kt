package com.example.insightku.feature.auth.domain

import android.content.Context
import com.example.insightku.R
import com.example.insightku.feature.auth.data.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(Exception(context.getString(R.string.error_email_empty)))
        return authRepository.sendPasswordResetEmail(email)
    }
}



