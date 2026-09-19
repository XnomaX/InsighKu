package com.example.insightku.feature.home.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.feature.home.data.receipt.ReceiptScanRepository
import com.example.insightku.feature.home.domain.receipt.ReceiptScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScanState { READY, PROCESSING, SCANNED, ERROR }

data class ScannerUiState(
    val state: ScanState = ScanState.READY,
    val form: TransactionFormData = TransactionFormData(),
    /** True only for the offline outcome — lets the UI pick a specific message. */
    val offline: Boolean = false,
    /** Raw error detail for non-offline failures (may be shown alongside a generic message). */
    val error: String? = null
)

/**
 * Drives the online-only receipt scanner. Calls [ReceiptScanRepository] (which
 * gates on connectivity) and maps the outcome to [ScannerUiState]. No offline
 * caching — `Offline`/`Error` carry no partial data.
 */
@HiltViewModel
class ReceiptScannerViewModel @Inject constructor(
    private val repository: ReceiptScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun scanReceipt(uri: Uri, isIncome: Boolean, deleteAfterRead: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(state = ScanState.PROCESSING, offline = false, error = null) }
            when (val result = repository.scan(uri, deleteAfterRead = deleteAfterRead)) {
                is ReceiptScanResult.Success -> _uiState.update {
                    it.copy(
                        state = ScanState.SCANNED,
                        form = result.data.toTransactionFormData(isIncome),
                        error = null
                    )
                }
                ReceiptScanResult.Offline -> _uiState.update {
                    it.copy(state = ScanState.ERROR, offline = true, error = null)
                }
                is ReceiptScanResult.Error -> _uiState.update {
                    it.copy(state = ScanState.ERROR, offline = false, error = result.message)
                }
            }
        }
    }

    /** Return to the capture screen (used on dismiss and on cancelled camera/gallery). */
    fun reset() {
        _uiState.update { ScannerUiState() }
    }
}
