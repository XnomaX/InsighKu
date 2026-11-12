package com.example.insightku.feature.home.domain.forecast

/**
 * Result of one expense-forecast call against the Flask `/forecast` endpoint.
 *
 * The endpoint deliberately answers refusals with **HTTP 200** and
 * `status != "ok"` (`insufficient_history` / `unknown_key`), carrying `null`
 * value fields — "never a fabricated 0". [Refused] preserves that distinction:
 * a refusal is a valid, honest answer from the model, not a transport failure,
 * so it must never be rendered as an error toast nor as a zero bar.
 */
sealed interface ForecastOutcome {

    /** Model produced a usable 28-day profile. */
    data class Success(val result: ForecastResult) : ForecastOutcome

    /**
     * Model declined to answer (HTTP 200, `status != ok`). [detail] is the
     * server's own explanation, e.g. `"need 7 days, got 3"`.
     */
    data class Refused(val reason: String, val detail: String, val observedDays: Int) : ForecastOutcome

    /** No connectivity — the online-only gate tripped before any request. */
    data object Offline : ForecastOutcome

    /** Request-level failure: malformed body (400), auth (401), model 503, timeout. */
    data class Error(val message: String) : ForecastOutcome
}

/**
 * Parsed `status == "ok"` payload.
 *
 * [dailyProfile] holds exactly 28 consecutive currency-unit values starting the
 * day *after* the forecast origin — the model's fixed profile length
 * (`PROFILE_LEN = 28`, `horizons = [7, 28]`). [total7] equals the sum of the
 * first 7 values; the server rounds the profile first and derives the totals
 * from the rounded values, so the two stay consistent by construction.
 */
data class ForecastResult(
    val total7: Double,
    val total28: Double,
    val dailyProfile: List<Double>,
    val observedDays: Int,
    val scale: Double,
    val modelStatus: String,
    val origin: String
)
