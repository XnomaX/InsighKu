package com.example.insightku.feature.home.data.forecast

import android.content.Context
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.feature.home.domain.forecast.ForecastOutcome
import com.example.insightku.feature.home.domain.forecast.ForecastResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Calls the Flask `/forecast` endpoint (warm-start scale-free model) over OkHttp.
 *
 * Reuses the same base URL / API key as the receipt scanner because both routes
 * are served by the one Flask app in `server/` — no second configuration source.
 *
 * Contract notes that drive the parsing below (verified against the live server):
 *  - HTTP 200 + `status == "ok"`  -> totals and a 28-value `daily_profile`
 *  - HTTP 200 + `status != "ok"`  -> a REFUSAL: value fields are null and
 *    `detail` explains why. This is a valid answer, not an error.
 *  - HTTP 400 / 413               -> our request was malformed or too large
 *  - HTTP 401                     -> API key missing or wrong
 *  - HTTP 503                     -> forecast bundle failed to load server-side
 */
@Singleton
class ForecastRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
    @Named("receiptApiBaseUrl") private val baseUrl: String,
    @Named("receiptApiKey") private val apiKey: String,
    @ApplicationContext private val context: Context
) {

    /**
     * These messages reach the UI verbatim (`ForecastNotice.Error` →
     * `ForecastEmptyState`), so they are localized. `@ApplicationContext` carries
     * the system locale rather than the in-app one, hence the [LocaleHelper] wrap —
     * the same pattern `ReceiptScanRepository` uses in this data layer.
     *
     * Not cached: `wrapContext` must re-resolve after an in-app language switch,
     * and this only runs on the rare error path, never in a loop.
     */
    private fun str(resId: Int, vararg args: Any?): String =
        LocaleHelper.wrapContext(context).getString(resId, *args)

    suspend fun forecast(
        accountId: String,
        origin: String,
        transactions: List<ForecastTransactionRow>
    ): ForecastOutcome = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            return@withContext ForecastOutcome.Error(str(R.string.forecast_error_no_base_url))
        }

        val payload = JSONObject().apply {
            put("account_id", accountId)
            put("origin", origin)
            put("transactions", JSONArray().apply {
                transactions.forEach { row ->
                    put(JSONObject().apply {
                        put("date", row.date)
                        put("amount", row.amount)
                        put("kind", row.kind)
                    })
                }
            })
        }

        val requestBuilder = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/forecast")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
        if (apiKey.isNotBlank()) requestBuilder.header("X-API-Key", apiKey)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> parseOutcome(raw)
                    else -> ForecastOutcome.Error(describeHttpError(response.code, raw))
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            ForecastOutcome.Error(str(R.string.forecast_error_timeout))
        } catch (e: IOException) {
            ForecastOutcome.Error(e.message ?: str(R.string.forecast_error_request_failed))
        }
    }

    private fun parseOutcome(raw: String): ForecastOutcome {
        val obj = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return ForecastOutcome.Error(str(R.string.forecast_error_invalid_json))
        }

        val status = obj.optString("status")
        if (status != "ok") {
            // A refusal: 200 with null value fields. Surface the server's own
            // reason/detail rather than inventing a zero.
            return ForecastOutcome.Refused(
                reason = status.ifBlank { "refused" },
                detail = obj.optString("detail"),
                observedDays = obj.optInt("observed_days", 0)
            )
        }

        val profileArr = obj.optJSONArray("daily_profile")
            ?: return ForecastOutcome.Error(str(R.string.forecast_error_no_profile))

        val profile = ArrayList<Double>(profileArr.length())
        for (i in 0 until profileArr.length()) {
            val v = profileArr.opt(i)
            // A null entry inside an ok-response would mean a fabricated gap;
            // refuse rather than silently coalesce to 0.0.
            if (v == null || v == JSONObject.NULL) {
                android.util.Log.w(TAG, "daily_profile[$i] was null in an ok response")
                return ForecastOutcome.Error(str(R.string.forecast_error_null_in_profile))
            }
            profile.add(profileArr.getDouble(i))
        }

        return ForecastOutcome.Success(
            ForecastResult(
                total7 = obj.optDouble("total_7", Double.NaN),
                total28 = obj.optDouble("total_28", Double.NaN),
                dailyProfile = profile,
                observedDays = obj.optInt("observed_days", 0),
                scale = obj.optDouble("scale", Double.NaN),
                modelStatus = obj.optString("model_status"),
                origin = obj.optString("origin")
            )
        )
    }

    private fun describeHttpError(code: Int, raw: String): String {
        val serverMessage = try {
            JSONObject(raw).optString("error").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
        return when (code) {
            401 -> str(R.string.forecast_error_unauthorized)
            413 -> serverMessage ?: str(R.string.forecast_error_too_many)
            503 -> serverMessage ?: str(R.string.forecast_error_unavailable)
            // The raw body is logged, never shown: it can carry server internals,
            // and this string is rendered to the user in the forecast card.
            else -> serverMessage ?: str(R.string.forecast_error_server, code).also {
                android.util.Log.w(TAG, "forecast server error $code: ${raw.take(200)}")
            }
        }
    }

    companion object {
        private const val TAG = "ForecastRemoteDS"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/** One `{date, amount, kind}` row of the request payload. */
data class ForecastTransactionRow(
    val date: String,
    val amount: Double,
    val kind: String = "expense"
)
