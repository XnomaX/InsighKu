package com.example.insightku.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global error bus — Singleton yang diinject ke semua ViewModel untuk mengirim
 * pesan error, dan ke RootViewModel untuk mengobservasi dan menampilkan Snackbar.
 *
 * Menggunakan SharedFlow (bukan StateFlow) agar setiap event error dikonsumsi
 * tepat sekali — tidak ada "last error" yang tersimpan saat observer baru bergabung.
 *
 * Pattern ini menggantikan injeksi RootViewModel ke sesama ViewModel,
 * karena ViewModel tidak boleh depend pada ViewModel lain (violates single responsibility).
 */
@Singleton
class ErrorBus @Inject constructor() {

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /**
     * SharedFlow yang dapat diobservasi oleh RootViewModel.
     * replay = 0: subscriber baru tidak mendapat event lama.
     */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    /**
     * Kirim pesan error ke bus. Aman dipanggil dari coroutine manapun.
     * tryEmit() tidak suspend — cocok untuk context yang tidak bisa suspend.
     */
    fun send(message: String) {
        _errors.tryEmit(message)
    }
}
