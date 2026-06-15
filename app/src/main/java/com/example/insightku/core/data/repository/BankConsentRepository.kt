package com.example.insightku.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.notification.SUPPORTED_BANK_PACKAGES
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Satu app keuangan yang didukung parser, beserta status terpasang & izin user.
 *
 * @property isInstalled apakah app benar-benar terpasang di perangkat.
 * @property isAllowed   apakah user MENGIZINKAN InsighKu memproses notifikasinya.
 */
data class MonitorableApp(
    val packageName: String,
    val displayName: String,
    val isInstalled: Boolean,
    val isAllowed: Boolean
)

/**
 * BankConsentRepository — sumber kebenaran tunggal untuk "notifikasi app mana yang
 * boleh diproses".
 *
 * Memisahkan dua lapisan yang sebelumnya tercampur di [SUPPORTED_BANK_PACKAGES]:
 *  1. CAPABILITY — app yang parser-nya kita dukung (data statis kita).
 *  2. CONSENT    — app yang user izinkan secara eksplisit (DataStore, default KOSONG).
 *
 * Sistem hanya boleh memproses notifikasi yang ada di IRISAN keduanya. Service
 * memanggil [isProcessingAllowed] sebagai gerbang sebelum parsing.
 */
@Singleton
class BankConsentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesDataStore
) {
    /** Daftar app yang didukung, diperkaya status terpasang + izin. Untuk layar whitelist. */
    val monitorableApps: Flow<List<MonitorableApp>> =
        prefs.allowedBankPackages.map { allowed ->
            SUPPORTED_BANK_PACKAGES.map { (pkg, name) ->
                MonitorableApp(
                    packageName = pkg,
                    displayName = name,
                    isInstalled = isPackageInstalled(pkg),
                    isAllowed = pkg in allowed
                )
            }.sortedWith(
                // Terpasang dulu, lalu alfabet — agar app milik user muncul di atas.
                compareByDescending<MonitorableApp> { it.isInstalled }.thenBy { it.displayName.lowercase() }
            )
        }

    /** Hanya app yang didukung DAN terpasang — kandidat utama saat onboarding. */
    val installedSupportedApps: Flow<List<MonitorableApp>> =
        monitorableApps.map { list -> list.filter { it.isInstalled } }

    /**
     * Gerbang yang dipanggil service: true hanya jika package didukung parser
     * DAN diizinkan user. Pengecekan sekali ambil (bukan Flow) karena dipanggil
     * per-notifikasi di jalur deteksi.
     */
    suspend fun isProcessingAllowed(packageName: String): Boolean {
        if (!SUPPORTED_BANK_PACKAGES.containsKey(packageName)) return false
        return packageName in prefs.allowedBankPackages.first()
    }

    suspend fun allow(packageName: String) = prefs.allowBankPackage(packageName)
    suspend fun disallow(packageName: String) = prefs.disallowBankPackage(packageName)
    suspend fun setAllowed(packages: Set<String>) = prefs.setAllowedBankPackages(packages)

    private fun isPackageInstalled(packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
}
