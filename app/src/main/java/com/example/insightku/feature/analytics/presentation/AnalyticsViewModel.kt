package com.example.insightku.feature.analytics.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.feature.analytics.domain.AnalyticsInsights
import com.example.insightku.feature.analytics.domain.AnalyticsPeriodType
import com.example.insightku.feature.analytics.domain.GetAnalyticsInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAnalyticsInsights: GetAnalyticsInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    private var insightsJob: Job? = null

    /**
     * Language the copy currently in [uiState] was derived in, or null until the first
     * emission.
     *
     * `InsightEngine` resolves `AnalyticsStrings` at derivation time, so the hero insight,
     * personality, patterns, noticing and rhythm are baked `String`s. `setApplicationLocales`
     * recreates the Activity but this ViewModel is Nav-scoped and survives, and the Room
     * `Flow` only re-emits on data changes — so without a re-derivation trigger those fields
     * stay in the previous language while every `stringResource` around them switches.
     *
     * Read from `AppCompatDelegate` (via [LocaleHelper]) rather than the persisted
     * `appLanguage` preference: `SettingsViewModel.saveLanguage` writes the preference
     * *before* calling `applyLocale`, so the DataStore emission can arrive while the old
     * locale is still applied and re-derive stale copy.
     */
    private var derivedLanguage: String? = null

    init {
        loadAnalytics()
    }

    /**
     * Re-derive when the applied locale differs from the one the current copy was built in.
     *
     * Called from the screen on every composition (a `LaunchedEffect` keyed on the
     * configuration's language tag). Deliberately a no-op when [derivedLanguage] is null —
     * nothing has been derived yet, so `init`'s `loadAnalytics` already covers it — which
     * keeps the first composition from issuing a duplicate load.
     */
    fun onLocaleChanged() {
        val current = LocaleHelper.getCurrentLanguageCode(context)
        val used = derivedLanguage
        if (used != null && used != current) loadAnalytics()
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
                            // Record the applied language alongside the copy it produced:
                            // InsightEngine resolves AnalyticsStrings per Room emission, so
                            // this is the language actually baked into `insights`.
                            derivedLanguage = LocaleHelper.getCurrentLanguageCode(context)
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


// --- Analytics MVI contract (Event.kt, State.kt) -------------------------------

sealed class AnalyticsEvent {
    data class SelectPeriod(val period: AnalyticsPeriodType) : AnalyticsEvent()
    object LoadAnalytics : AnalyticsEvent()
    object RefreshData : AnalyticsEvent()
    object Retry : AnalyticsEvent()
}

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: AnalyticsPeriodType = AnalyticsPeriodType.WEEKLY,
    val insights: AnalyticsInsights? = null,
    val error: String? = null
)
