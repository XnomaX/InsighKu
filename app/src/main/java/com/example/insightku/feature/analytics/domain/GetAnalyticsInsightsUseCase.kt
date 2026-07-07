package com.example.insightku.feature.analytics.domain

import com.example.insightku.feature.home.domain.GetTransactionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Single source of behavioral insight derivation. Reactive: re-derives whenever the underlying
 * transaction [Flow] emits (insert/update/delete). Pure interpretation lives in [InsightEngine];
 * the only impurity here is reading the current time per emission.
 */
class GetAnalyticsInsightsUseCase @Inject constructor(
    private val getTransactions: GetTransactionsUseCase,
    private val engine: InsightEngine
) {
    /** Emits derived insights for a specific period type; wraps derivation in [Result]. */
    operator fun invoke(periodType: AnalyticsPeriodType = AnalyticsPeriodType.MONTHLY): Flow<Result<AnalyticsInsights>> =
        getTransactions()
            .map { txns -> runCatching { engine.derive(txns, System.currentTimeMillis(), periodType) } }
            .flowOn(Dispatchers.Default)

    /** Pull remote → Room; the Room Flow then re-emits and re-derives. */
    suspend fun refresh() = getTransactions.refresh()
}


