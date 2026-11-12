package com.example.insightku.feature.home.domain.forecast

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.feature.home.data.forecast.ForecastRepository
import java.util.Locale
import javax.inject.Inject

/** One chart bar: its own label plus its value, so no label can be hardcoded. */
data class ForecastBar(
    val label: String,
    val value: Float
)

/**
 * Chart-ready forecast for the dashboard card.
 *
 * The model emits a fixed **28 consecutive daily** values starting the day after
 * the origin. The two views are honest slices of that one profile:
 *
 *  - [weeklyBars]  — the first 7 daily values, each labelled with its real weekday
 *  - [monthlyBars] — all 28 days folded into 4 weekly buckets, each labelled with
 *    the bucket's start date
 *
 * There is deliberately **no 12-month view**: the endpoint cannot produce one
 * (`horizons = [7, 28]`, `profile_len = 28`). Inventing 12 bars from 28 days of
 * output would be fabrication, so the "monthly" toggle shows the 28-day window
 * the model actually supports.
 *
 * [notice] carries refusals and transport failures, letting the UI show the real
 * reason instead of drawing zeros.
 */
data class ForecastUi(
    val weeklyBars: List<ForecastBar> = emptyList(),
    val monthlyBars: List<ForecastBar> = emptyList(),
    val weeklyTotal: Double = 0.0,
    val monthlyTotal: Double = 0.0,
    val observedDays: Int = 0,
    val modelStatus: String = "",
    val notice: ForecastNotice? = null
) {
    /** True when there is nothing honest to draw. */
    val isEmpty: Boolean get() = weeklyBars.isEmpty() && monthlyBars.isEmpty()

    /** Localised one-line summary shown under the chart. Empty when there is none. */
    val insight: String get() = when {
        notice == null && weeklyTotal > 0.0 -> buildInsight()
        else -> ""
    }

    private fun buildInsight(): String {
        // Pass the app locale explicitly. formatCurrencyCompact defaults to
        // Locale.getDefault() — the SYSTEM locale — and it drives both the
        // decimal separator and the scale suffix ("Jt"/"Rb" vs "M"/"K"). Without
        // this, an app=id / system=en user gets an Indonesian sentence wrapping
        // an English-formatted number.
        val locale = currentLocale()
        val projected = NumberFormatter.formatCurrencyCompact(weeklyTotal, locale = locale)
        val perMonth = NumberFormatter.formatCurrencyCompact(monthlyTotal, locale = locale)
        return if (LocaleHelper.isIndonesian(locale)) {
            "Proyeksi pengeluaran: $projected (7 hari) · $perMonth (28 hari), dari $observedDays hari riwayat."
        } else {
            "Projected spending: $projected (7 days) · $perMonth (28 days), from $observedDays days of history."
        }
    }

    companion object {
        val EMPTY = ForecastUi()
    }
}

/**
 * Why there is no chart. [Refused] is the model's own honest "I can't answer
 * yet"; the others are infrastructure outcomes. A sealed type keeps the UI layer
 * in charge of localisation while this layer stays free of `Context`.
 */
sealed interface ForecastNotice {
    /** Model answered 200 with `status != ok`. [detail] is the server's wording. */
    data class Refused(val reason: String, val detail: String, val observedDays: Int) : ForecastNotice
    data object Offline : ForecastNotice
    data class Error(val message: String) : ForecastNotice
}

/**
 * Turns a repository outcome into chart-ready bars.
 *
 * Pure apart from the repository call and locale-aware formatting — no Android
 * view types — so the bucketing and label maths are unit-testable.
 */
class ForecastExpenseUseCase @Inject constructor(
    private val repository: ForecastRepository
) {

    suspend operator fun invoke(
        accountId: String,
        originMillis: Long,
        transactions: List<Transaction>
    ): ForecastUi = when (val outcome = repository.forecast(accountId, originMillis, transactions)) {

        is ForecastOutcome.Offline -> ForecastUi.EMPTY.copy(notice = ForecastNotice.Offline)

        is ForecastOutcome.Error ->
            ForecastUi.EMPTY.copy(notice = ForecastNotice.Error(outcome.message))

        is ForecastOutcome.Refused -> ForecastUi.EMPTY.copy(
            notice = ForecastNotice.Refused(outcome.reason, outcome.detail, outcome.observedDays)
        )

        is ForecastOutcome.Success -> {
            val r = outcome.result
            val profile = r.dailyProfile
            if (profile.size < PROFILE_LEN) {
                // Fewer than 28 values would silently mislabel every bar after
                // the shortfall; refuse rather than pad with invented zeros.
                ForecastUi.EMPTY.copy(
                    notice = ForecastNotice.Error(
                        // Inline bilingual copy, same convention as buildInsight()
                        // above: this domain class has no Context and its unit test
                        // constructs it positionally, so a resource lookup here
                        // would cost an injected dependency for one error string.
                        if (LocaleHelper.isIndonesian(currentLocale())) {
                            "Profil proyeksi berisi ${profile.size} hari, seharusnya $PROFILE_LEN"
                        } else {
                            "Forecast profile had ${profile.size} days, expected $PROFILE_LEN"
                        }
                    )
                )
            } else {
                val weekdays = weekdayLabelsFrom(originMillis)
                ForecastUi(
                    weeklyBars = profile.take(DAYS_PER_WEEK).mapIndexed { i, v ->
                        ForecastBar(weekdays[i], v.toFloat())
                    },
                    monthlyBars = profile.chunked(DAYS_PER_WEEK).mapIndexed { bucket, week ->
                        ForecastBar(bucketLabel(originMillis, bucket), week.sum().toFloat())
                    },
                    weeklyTotal = r.total7,
                    monthlyTotal = r.total28,
                    observedDays = r.observedDays,
                    modelStatus = r.modelStatus,
                    notice = null
                )
            }
        }
    }

    /**
     * Weekday labels for origin+1 .. origin+7, derived from the real dates.
     * A hardcoded Mon..Sun list would be wrong whenever the origin is not a
     * Sunday, since the profile starts the day *after* the origin.
     *
     * Uses [DateFormatter] (the project's single source of truth for date
     * formatting) with the *app* locale, so bar labels and the insight line
     * below agree even when system=en but app=Bahasa Indonesia.
     */
    private fun weekdayLabelsFrom(originMillis: Long): List<String> {
        val locale = currentLocale()
        return (1..DAYS_PER_WEEK).map { offset ->
            DateFormatter.getShortDayName(originMillis + offset * MILLIS_PER_DAY, locale)
        }
    }

    /** e.g. `"9 Nov"` — the first day of a 7-day bucket. */
    private fun bucketLabel(originMillis: Long, bucketIndex: Int): String {
        val start = originMillis + (bucketIndex * DAYS_PER_WEEK + 1) * MILLIS_PER_DAY
        return DateFormatter.getDayMonth(start, currentLocale())
    }

    private companion object {
        const val PROFILE_LEN = 28
        const val DAYS_PER_WEEK = 7
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

/**
 * The app's effective locale, shared by [ForecastUi] (insight text) and
 * [ForecastExpenseUseCase] (bar labels) so both follow the in-app language
 * switch. Same lookup as `AnalyticsStrings.currentLocale()` /
 * `LocaleHelper.getCurrentLanguageCode()`.
 */
internal fun currentLocale(): Locale = LocaleHelper.currentLocale()
