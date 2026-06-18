package com.example.insightku.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.SelectPeriod -> {
                _uiState.value = _uiState.value.copy(selectedPeriod = event.period)
            }
            else -> { /* Reserved for redesign */ }
        }
    }
}
