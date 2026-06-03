package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.data.model.Category
import com.example.insightku.domain.usecase.analytics.AnalyticsInsights
import com.example.insightku.domain.usecase.analytics.CategorySlice
import com.example.insightku.domain.usecase.analytics.GetAnalyticsInsightsUseCase
import com.example.insightku.domain.usecase.analytics.Noticing
import com.example.insightku.domain.usecase.analytics.TwoYousData
import com.example.insightku.data.repository.TransactionRepository
import com.example.insightku.ui.components.analytics.AnalyticsDebugScenarios
import com.example.insightku.ui.components.analytics.AnalyticsEvent
import com.example.insightku.ui.components.analytics.AnalyticsUiState
import com.example.insightku.ui.components.analytics.CategoryBubble
import com.example.insightku.ui.components.analytics.SelfCategoryUi
import com.example.insightku.ui.components.analytics.SpendingSelfUi
import com.example.insightku.ui.components.analytics.TwoYousUi
import com.example.insightku.ui.dialogs.CategoryIconResolver
import com.example.insightku.utils.ErrorBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AnalyticsViewModel — thin orchestration over [GetAnalyticsInsightsUseCase].
 *
 * Collects the reactive insights Flow, maps the pure [AnalyticsInsights] into [AnalyticsUiState]
 * (attaching bubble color/icon via the app-wide [CategoryIconResolver] + the user's [Category] list,
 * the same system Home/Budgeting/Edit use). All insight math lives in the pure InsightEngine.
 *
 * Debug mode: when [debugScenarioIndex] >= 0, the UI shows a synthetic scenario from
 * [AnalyticsDebugScenarios] instead of live data. Temporary — removed before final polish.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsInsights: GetAnalyticsInsightsUseCase,
    transactionRepository: TransactionRepository,
    private val prefs: UserPreferencesDataStore,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var debugScenarioIndex: Int = -1
    // The noticing chosen for THIS viewing session. Held stable so it doesn't flip while the user
    // looks at it; a fresh ViewModel (next visit) picks the next unseen one.
    private var sessionNoticing: Noticing? = null

    /** Category list for icon/color resolution — name(lowercased) → Category, like the rest of the app. */
    private val categoryMap: StateFlow<Map<String, Category>> =
        transactionRepository.getAllCategories()
            .map { cats -> cats.associateBy { it.name.trim().lowercase() } }
            .catch { emit(emptyMap()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        load()
        refreshFromRemote()
    }

    fun onEvent(event: AnalyticsEvent) {
        when (event) {
            AnalyticsEvent.LoadAnalytics -> load()
            AnalyticsEvent.RefreshData -> refresh()
            is AnalyticsEvent.ExpandBubble -> _uiState.update {
                it.copy(expandedBubble = if (it.expandedBubble == event.categoryName) null else event.categoryName)
            }
            is AnalyticsEvent.ExpandDay -> _uiState.update {
                it.copy(expandedDay = if (it.expandedDay == event.dayOfMonth) null else event.dayOfMonth)
            }
            AnalyticsEvent.TogglePatterns -> _uiState.update { it.copy(patternsExpanded = !it.patternsExpanded) }
            is AnalyticsEvent.Uncover -> _uiState.update {
                it.copy(uncoveredSections = it.uncoveredSections + event.sectionId)
            }
            AnalyticsEvent.CycleDebugScenario -> cycleDebug()
            AnalyticsEvent.ExitDebug -> { debugScenarioIndex = -1; load() }
        }
    }

    private fun load() {
        if (debugScenarioIndex >= 0) return // debug overrides live data
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, debugLabel = null) }
            try {
                getAnalyticsInsights().collect { result ->
                    if (debugScenarioIndex >= 0) return@collect
                    result
                        .onSuccess { insights ->
                            val noticing = chooseNoticing(insights.noticings)
                            _uiState.update { it.applyInsights(insights, categoryMap.value, noticing) }
                        }
                        .onFailure { e -> surfaceError(e.message) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                surfaceError(e.message)
            }
        }
    }

    /**
     * Pick the noticing to show this session: the first candidate not yet seen, remembered across
     * sessions via DataStore so new observations rotate in over time. Once chosen it's marked seen
     * and pinned for the session (so re-emissions don't flip it). When every candidate has been seen,
     * the seen-set resets and the cycle starts fresh. Reads the seen-set as a one-shot snapshot
     * (`first()`) — NOT a combined flow — so marking-seen never re-triggers this collector.
     */
    private suspend fun chooseNoticing(candidates: List<Noticing>): Noticing? {
        if (candidates.isEmpty()) return null
        sessionNoticing?.let { pinned -> if (candidates.any { it.id == pinned.id }) return pinned }

        val seen = prefs.seenNoticings.first()
        val unseen = candidates.firstOrNull { it.id !in seen }
        val chosen = if (unseen != null) {
            unseen
        } else {
            // All candidates seen — reset and begin the rotation again from the strongest.
            prefs.resetSeenNoticings()
            candidates.first()
        }
        prefs.markNoticingSeen(chosen.id)
        sessionNoticing = chosen
        return chosen
    }

    private fun cycleDebug() {
        loadJob?.cancel()
        val scenarios = AnalyticsDebugScenarios.all
        debugScenarioIndex = (debugScenarioIndex + 1) % scenarios.size
        _uiState.value = scenarios[debugScenarioIndex]
    }

    private fun refresh() = viewModelScope.launch {
        try {
            getAnalyticsInsights.refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            surfaceError(e.message)
        }
    }

    private fun refreshFromRemote() = viewModelScope.launch {
        runCatching { getAnalyticsInsights.refresh() }
    }

    private fun surfaceError(message: String?) {
        val msg = message ?: "Gagal memuat insight"
        _uiState.update { it.copy(isLoading = false, error = msg) }
        errorBus.send(msg)
    }
}

/** Maps pure insights into UI state, attaching color/icon to each category bubble via the resolver. */
private fun AnalyticsUiState.applyInsights(
    i: AnalyticsInsights,
    categoryMap: Map<String, Category>,
    noticing: Noticing?
): AnalyticsUiState = copy(
    isLoading = false,
    error = null,
    isEmpty = i.isEmpty,
    personality = i.personality,
    twoYous = i.twoYous.toUi(categoryMap),
    patterns = i.patterns,
    noticing = noticing,
    heatmapCells = i.heatmap,
    rhythm = i.rhythm,
    categoryBubbles = i.categories.map { it.toBubble(categoryMap) },
    bigDecisions = i.bigDecisions,
    streak = i.streak,
    spotlight = i.spotlight,
    mood = i.mood,
    debugLabel = null
)

/**
 * Resolve a slice's icon + color via the app-wide [CategoryIconResolver], mirroring the logic in
 * Dashboard's PremiumTransactionCard: prefer the stored Category.icon field, fall back to the
 * category name; prefer the stored hex color, fall back to the resolver's color.
 */
private fun CategorySlice.toBubble(categoryMap: Map<String, Category>): CategoryBubble {
    val matched = categoryMap[name.trim().lowercase()]
    val iconKey = matched?.icon?.ifBlank { null } ?: name
    val byIcon = CategoryIconResolver.resolve(iconKey)
    val byName = CategoryIconResolver.resolve(name)
    val resolved = if (byIcon.name != "Others") byIcon else byName
    return CategoryBubble(
        name = name,
        amount = amount,
        proportion = proportion,
        color = resolveCategoryColor(name, categoryMap),
        icon = resolved.icon,
        topTransactions = topTransactions
    )
}

/** Resolve a category name to its display color (stored hex if set, else the resolver's color). */
private fun resolveCategoryColor(
    name: String,
    categoryMap: Map<String, Category>
): androidx.compose.ui.graphics.Color {
    val matched = categoryMap[name.trim().lowercase()]
    val iconKey = matched?.icon?.ifBlank { null } ?: name
    val byIcon = CategoryIconResolver.resolve(iconKey)
    val byName = CategoryIconResolver.resolve(name)
    val resolved = if (byIcon.name != "Others") byIcon else byName
    return if (!matched?.color.isNullOrBlank()) {
        runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(matched!!.color)) }
            .getOrDefault(resolved.color)
    } else resolved.color
}

/** Map the pure [TwoYousData] into the UI model, attaching a resolved color to each category dot. */
private fun TwoYousData.toUi(categoryMap: Map<String, Category>): TwoYousUi {
    if (!isConfident) return TwoYousUi.NOT_CONFIDENT
    val maxPerDay = maxOf(left.perDayAverage, right.perDayAverage).coerceAtLeast(1.0)
    fun selfUi(s: com.example.insightku.domain.usecase.analytics.SpendingSelf) = SpendingSelfUi(
        label = s.label,
        emoji = s.emoji,
        intensity = (s.perDayAverage / maxPerDay).toFloat().coerceIn(0f, 1f),
        categories = s.topCategories.map {
            SelfCategoryUi(it.name, it.proportion, resolveCategoryColor(it.name, categoryMap))
        }
    )
    return TwoYousUi(
        left = selfUi(left),
        right = selfUi(right),
        divergence = divergence,
        gapHeadline = gapHeadline,
        isConfident = true
    )
}
