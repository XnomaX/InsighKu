package com.example.insightku.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.data.repository.BankConsentRepository
import com.example.insightku.core.data.repository.MonitorableApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel layar Whitelist Bank — daftar app keuangan yang didukung parser,
 * dengan status terpasang & izin user. Hanya app yang DIIZINKAN yang diproses
 * oleh [BankConsentRepository.isProcessingAllowed].
 */
@HiltViewModel
class BankWhitelistViewModel @Inject constructor(
    private val consentRepository: BankConsentRepository
) : ViewModel() {

    val apps: StateFlow<List<MonitorableApp>> = consentRepository.monitorableApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setAllowed(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            if (allowed) consentRepository.allow(packageName)
            else consentRepository.disallow(packageName)
        }
    }
}
