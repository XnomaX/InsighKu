package com.example.insightku.data.repository

import com.example.insightku.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository — satu-satunya pintu masuk ke Firebase Auth dan session management.
 *
 * SEBELUMNYA:
 * - loginUser() ada di sini (benar)
 * - signUp() langsung di SignUpViewModel via FirebaseAuth.getInstance() (SALAH)
 * - logout() tidak ada (SALAH — SettingsViewModel hanya reset UI state)
 * - SessionManager tidak dipakai setelah login
 *
 * SEKARANG:
 * - Semua operasi auth melewati repository ini
 * - Setiap operasi return Result<T> — tidak ada exception yang bocor ke ViewModel
 * - Session DataStore diupdate di sini, bukan di ViewModel
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) {

    // ─── Read ──────────────────────────────────────────────────────────────────

    fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    fun getCurrentUser() = firebaseAuth.currentUser

    // ─── Login ─────────────────────────────────────────────────────────────────

    /**
     * Login dengan email & password ke Firebase Auth, lalu simpan session ke DataStore.
     *
     * Kenapa session disimpan di sini dan bukan di ViewModel?
     * Karena saat ini jika app restart, SplashViewModel membaca DataStore untuk menentukan
     * apakah user sudah login. Jika session tidak disimpan setelah login, user harus login
     * ulang setiap buka app.
     */
    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Login gagal: user tidak ditemukan"))

            // Simpan session ke DataStore segera setelah login berhasil
            sessionManager.saveLoginSession(
                email = user.email ?: email,
                name = user.displayName ?: "",
                userId = user.uid
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Sign Up ───────────────────────────────────────────────────────────────

    /**
     * Daftar akun baru: buat user di Firebase Auth, set display name, simpan profil
     * ke Firestore collection "users/", lalu simpan session ke DataStore.
     *
     * Kenapa profil disimpan ke Firestore juga?
     * Firebase Auth hanya menyimpan email & display name. Data tambahan (foto profil,
     * preferensi, dsb.) perlu dokumen sendiri di Firestore agar bisa di-query.
     */
    suspend fun signUpUser(email: String, password: String, name: String): Result<Unit> {
        return try {
            // 1. Buat user di Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Registrasi gagal"))

            // 2. Update display name di Firebase Auth
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdate).await()

            // 3. Simpan profil user ke Firestore
            val userProfile = mapOf(
                "uid" to user.uid,
                "email" to email,
                "name" to name,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid).set(userProfile).await()

            // 4. Simpan session ke DataStore agar tidak perlu login ulang
            sessionManager.saveLoginSession(
                email = email,
                name = name,
                userId = user.uid
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Logout ────────────────────────────────────────────────────────────────

    /**
     * Logout lengkap: sign out dari Firebase, hapus DataStore session.
     * Pembersihan data lokal Room dilakukan oleh LogoutUseCase agar
     * repository tetap fokus pada satu tanggung jawab.
     *
     * SEBELUMNYA: logout() di SettingsViewModel hanya mereset UI state lokal.
     * DataStore tidak di-clear → FirebaseAuth token masih aktif → user "tetap login"
     * saat buka app lagi.
     */
    suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Password Reset ────────────────────────────────────────────────────────

    /**
     * Kirim email reset password via Firebase Auth.
     *
     * Flow:
     * 1. Firebase kirim email dengan link ke halaman reset resmi Firebase
     * 2. User klik link → browser terbuka (halaman Firebase)
     * 3. User input password baru di browser
     * 4. Selesai — tidak perlu deep link atau ResetPasswordScreen di dalam app
     *
     * Ini adalah cara paling simpel dan aman karena seluruh flow
     * dikelola Firebase tanpa perlu konfigurasi tambahan.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Google Sign-In ────────────────────────────────────────────────────────

    /**
     * Login/Register via Google menggunakan ID Token dari Credential Manager.
     * Alur:
     * 1. UI menampilkan Google picker lewat Credential Manager
     * 2. User pilih akun → Credential Manager kembalikan idToken
     * 3. idToken ditukar dengan Firebase credential
     * 4. Firebase verifikasi → simpan session
     *
     * Kenapa pakai Credential Manager dan bukan GoogleSignInClient lama?
     * GoogleSignInClient sudah deprecated sejak 2023. Credential Manager adalah
     * pengganti resmi yang mendukung Passkeys + Google Sign-In sekaligus.
     */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("Google Sign-In gagal"))

            // Simpan session ke DataStore
            sessionManager.saveLoginSession(
                email = user.email ?: "",
                name = user.displayName ?: "",
                userId = user.uid
            )

            // Jika user baru, simpan profil ke Firestore
            if (result.additionalUserInfo?.isNewUser == true) {
                val userProfile = mapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "name" to (user.displayName ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid).set(userProfile).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
