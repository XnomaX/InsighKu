package com.example.insightku.feature.analytics.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.feature.analytics.domain.GetAnalyticsInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAnalyticsInsights: GetAnalyticsInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    private var insightsJob: Job? = null

    init {
        loadAnalytics()
    }

    fun onEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.SelectPeriod -> {
                if (event.period != _uiState.value.selectedPeriod) {
                    _uiState.update { it.copy(selectedPeriod = event.period) }
                    loadAnalytics()
                }
            }
            is AnalyticsEvent.LoadAnalytics -> loadAnalytics()
            is AnalyticsEvent.RefreshData -> {
                viewModelScope.launch {
                    try { getAnalyticsInsights.refresh() } catch (_: Exception) {}
                }
                loadAnalytics()
            }
            is AnalyticsEvent.Retry -> {
                _uiState.update { it.copy(error = null, isLoading = true) }
                loadAnalytics()
            }
        }
    }

    private fun loadAnalytics() {
        insightsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }

        insightsJob = viewModelScope.launch {
            getAnalyticsInsights(_uiState.value.selectedPeriod)
                .collect { result ->
                    result.fold(
                        onSuccess = { insights ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    insights = insights,
                                    error = null
                                )
                            }
                        },
                        onFailure = { throwable ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = throwable.message ?: context.getString(R.string.error_something_went_wrong)
                                )
                            }
                        }
                    )
                }
        }
    }
}
