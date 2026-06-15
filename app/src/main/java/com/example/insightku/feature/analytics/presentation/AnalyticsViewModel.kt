package com.example.insightku.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.utils.CategoryUtils
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.feature.analytics.domain.AnalyticsInsights
import com.example.insightku.feature.analytics.domain.GetAnalyticsInsightsUseCase
import com.example.insightku.core.ui.components.dialogs.CategoryIconInfo
import com.example.insightku.feature.analytics.domain.SpendingPersonality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsInsights: GetAnalyticsInsightsUseCase,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    // Cycle through debug scenarios — temporary dev tool
    private var debugIndex = 0

    init {
        loadAnalytics()
        refreshFromRemote()
    }

    fun onEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.LoadAnalytics -> loadAnalytics()
            is AnalyticsEvent.RefreshData -> refreshFromRemote()
            is AnalyticsEvent.ExpandBubble -> _uiState.update {
                it.copy(expandedBubble = if (it.expandedBubble == event.categoryName) null else event.categoryName)
            }
            is AnalyticsEvent.ExpandDay -> _uiState.update {
                it.copy(expandedDay = if (it.expandedDay == event.dayOfMonth) null else event.dayOfMonth)
            }
            is AnalyticsEvent.TogglePatterns -> _uiState.update { it.copy(patternsExpanded = !it.patternsExpanded) }
            is AnalyticsEvent.Uncover -> _uiState.update { it.copy(uncoveredSections = it.uncoveredSections + event.sectionId) }
            is AnalyticsEvent.CycleDebugScenario -> {
                debugIndex = (debugIndex + 1) % AnalyticsDebugScenarios.all.size
                _uiState.value = AnalyticsDebugScenarios.all[debugIndex]
            }
            is AnalyticsEvent.ExitDebug -> loadAnalytics()
        }
    }

    private fun loadAnalytics() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                getAnalyticsInsights().collect { result ->
                    result.fold(
                        onSuccess = { insights -> _uiState.value = mapToUiState(insights) },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, error = e.message) }
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: "Gagal memuat analitik"
                _uiState.update { it.copy(isLoading = false, error = msg) }
                errorBus.send(msg)
            }
        }
    }

    private fun refreshFromRemote() {
        viewModelScope.launch {
            try { getAnalyticsInsights.refresh() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* silent — Room cache still usable */ }
        }
    }

    private fun mapToUiState(insights: AnalyticsInsights): AnalyticsUiState {
        val isEmpty = insights.categorySlices.isEmpty() && insights.heatmapCells.isEmpty()

        val bubbles = run {
            val maxAmt = insights.categorySlices.maxOfOrNull { it.proportion }?.coerceAtLeast(0.01f) ?: 1f
            insights.categorySlices.map { slice ->
                val info = CategoryIconResolver.resolve(slice.name)
                CategoryBubble(
                    name = slice.name,
                    amount = 0.0,
                    proportion = slice.proportion / maxAmt,
                    color = info.color,
                    icon = info.icon,
                    topTransactions = emptyList()
                )
            }
        }

        val twoYousUi = insights.twoYous?.let { data ->
            val wdMax = data.weekdayTopCategories.maxOfOrNull { it.proportion }?.coerceAtLeast(0.01f) ?: 1f
            val weMax = data.weekendTopCategories.maxOfOrNull { it.proportion }?.coerceAtLeast(0.01f) ?: 1f
            val wdAvgMax = maxOf(data.weekdayDailyAvg, data.weekendDailyAvg).coerceAtLeast(1.0)
            TwoYousUi(
                left = SpendingSelfUi(
                    label = "Weekday You", emoji = "😌",
                    intensity = (data.weekdayDailyAvg / wdAvgMax).toFloat(),
                    categories = data.weekdayTopCategories.map { s ->
                        SelfCategoryUi(s.name, s.proportion / wdMax, CategoryIconResolver.resolve(s.name).color)
                    }
                ),
                right = SpendingSelfUi(
                    label = "Weekend You", emoji = "🎧",
                    intensity = (data.weekendDailyAvg / wdAvgMax).toFloat(),
                    categories = data.weekendTopCategories.map { s ->
                        SelfCategoryUi(s.name, s.proportion / weMax, CategoryIconResolver.resolve(s.name).color)
                    }
                ),
                divergence = data.divergence,
                gapHeadline = data.gapHeadline,
                isConfident = data.isConfident
            )
        } ?: TwoYousUi.NOT_CONFIDENT

        return AnalyticsUiState(
            isLoading = false,
            isEmpty = isEmpty,
            personality = insights.personality,
            twoYous = twoYousUi,
            patterns = insights.patterns,
            noticing = insights.noticing,
            heatmapCells = insights.heatmapCells,
            rhythm = insights.rhythm,
            categoryBubbles = bubbles,
            bigDecisions = insights.bigDecisions,
            streak = insights.streak,
            spotlight = insights.spotlight,
            mood = insights.mood
        )
    }
}

