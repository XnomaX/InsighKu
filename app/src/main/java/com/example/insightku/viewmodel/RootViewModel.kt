package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.utils.ErrorBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * RootViewModel — dikelola oleh Hilt sebagai @HiltViewModel.
 *
 * KENAPA INI PENTING:
 * Sebelumnya ada RootViewModelModule yang membuat RootViewModel sebagai Hilt Singleton,
 * sementara RootNavGraph membuat instance kedua lewat viewModel() Compose.
 * Dua instance berbeda = globalError tidak pernah ter-observasi dari yang mengirim.
 *
 * Solusi:
 * 1. Hapus RootViewModelModule (tidak ada lagi manual @Provides untuk ViewModel).
 * 2. Annotasi @HiltViewModel di sini — Hilt mengelola lifecycle-nya via ViewModelStoreOwner.
 * 3. RootNavGraph pakai hiltViewModel() — mendapat instance YANG SAMA dengan Activity scope.
 * 4. ViewModel lain TIDAK inject RootViewModel langsung (ViewModel ≠ Singleton service).
 *    Mereka inject ErrorBus (Singleton) untuk mengirim error.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _globalError = MutableStateFlow<String?>(null)
    val globalError = _globalError.asStateFlow()

    init {
        // Observe errors dari semua ViewModel lewat ErrorBus
        viewModelScope.launch {
            errorBus.errors.collect { errorMessage ->
                _globalError.value = errorMessage
            }
        }
    }

    /** Dipanggil oleh UI setelah Snackbar ditampilkan agar tidak tampil ulang. */
    fun clearGlobalError() {
        _globalError.value = null
    }

    /**
     * Tetap dipertahankan untuk kompatibilitas dengan RootNavGraph yang sudah ada.
     * Internal usage: set null setelah snackbar ditampilkan.
     */
    fun setGlobalError(message: String?) {
        _globalError.value = message
    }
}
