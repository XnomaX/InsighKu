package com.example.insightku.feature.home.data.forecast

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.home.data.receipt.NetworkMonitor
import com.example.insightku.feature.home.domain.forecast.ForecastOutcome
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Online-only expense forecasting, mirroring [com.example.insightku.feature.home.data.receipt.ReceiptScanRepository]:
 * gate on connectivity, then map every failure mode onto [ForecastOutcome].
 *
 * Two decisions worth stating, because they are what keep the numbers honest:
 *
 * 1. **Only `TransactionType.EXPENSE` rows with a positive amount are sent.**
 *    That matches how the dashboard itself computes `monthlyExpenses`
 *    (`DashboardViewModel`: `filter { it.type == TransactionType.EXPENSE }`), so
 *    the forecast and the figures beside it describe the same population. The
 *    server also filters on `kind`, so this is belt-and-braces, not the only gate.
 *
 * 2. **History is truncated to the most recent [HISTORY_WINDOW_DAYS] days.**
 *    The model's level window is 84 days (`warm_model_sf/bundle.json`:
 *    `level_window`), so 180 days is ample, while bounding the JSON payload well
 *    under the server's `FORECAST_MAX_TX` (20000). Rows after the origin are
 *    dropped here too — the server drops them again as its leakage guard, so a
 *    future row can never influence the answer even if one slipped through.
 */
@Singleton
class ForecastRepository @Inject constructor(
    private val remote: ForecastRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) {

    suspend fun forecast(
        accountId: String,
        originMillis: Long,
        transactions: List<Transaction>
    ): ForecastOutcome {
        if (!networkMonitor.isOnline()) return ForecastOutcome.Offline

        val cutoff = originMillis - HISTORY_WINDOW_DAYS * MILLIS_PER_DAY
        val rows = transactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .filter { it.amount > 0.0 }
            .filter { it.date <= originMillis && it.date >= cutoff }
            .map { ForecastTransactionRow(isoDate(it.date), it.amount) }
            .toList()

        // Short-circuit rather than POST a body the server answers with 400
        // "'transactions' is empty": the user has no usable expense history,
        // which is a refusal to explain, not a failure to report.
        if (rows.isEmpty()) {
            return ForecastOutcome.Refused(
                reason = REFUSAL_NO_EXPENSE_HISTORY,
                detail = "",
                observedDays = 0
            )
        }

        return try {
            remote.forecast(accountId.ifBlank { "user" }, isoDate(originMillis), rows)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ForecastOutcome.Error(e.message ?: "Forecast request failed")
        }
    }

    // ThreadLocal because SimpleDateFormat is not thread-safe; the supplier
    // never returns null, so requireNotNull is total here and keeps us from
    // allocating a formatter per transaction row.
    private fun isoDate(millis: Long): String =
        requireNotNull(ISO_FORMAT.get()).format(Date(millis))

    companion object {
        /** Server-side refusal reason we synthesise for the no-history case. */
        const val REFUSAL_NO_EXPENSE_HISTORY = "no_expense_history"

        private const val HISTORY_WINDOW_DAYS = 180L
        private const val MILLIS_PER_DAY = 86_400_000L

        /**
         * ISO-8601 day resolution with a fixed locale: the server feeds this
         * string straight to `pd.to_datetime`, so it must never be localised.
         */
        private val ISO_FORMAT = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        }
    }
}
