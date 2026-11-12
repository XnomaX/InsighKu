package com.example.insightku.feature.home.data.receipt

import com.example.insightku.feature.home.domain.receipt.ReceiptScanData
import com.example.insightku.feature.home.domain.receipt.ReceiptScanItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Server returned a non-2xx response. Carries the status code so the caller can
 * localize a user-facing message without forwarding the raw response body, which
 * may contain server internals.
 */
class ScanServerException(val code: Int) : IOException("Receipt scan server error $code")

/**
 * Calls the Flask/Donut receipt-scan API over OkHttp. Online-only — the caller
 * (ReceiptScanRepository) gates on connectivity first. No caching of any kind.
 *
 * Messages thrown here are developer diagnostics for logcat; the user-facing
 * copy is resolved by [ReceiptScanRepository], which has a Context.
 */
@Singleton
class ReceiptScanRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
    @Named("receiptApiBaseUrl") private val baseUrl: String,
    @Named("receiptApiKey") private val apiKey: String
) {
    suspend fun scan(imageBytes: ByteArray, mimeType: String): ReceiptScanData =
        withContext(Dispatchers.IO) {
            require(baseUrl.isNotBlank()) { "Receipt API base URL is not configured" }

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "receipt",
                    imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()

            val requestBuilder = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/scan-receipt")
                .post(body)
            if (apiKey.isNotBlank()) requestBuilder.header("X-API-Key", apiKey)

            // No catch here: ScanServerException and SocketTimeoutException both
            // propagate unchanged so the Repository can classify them by type.
            client.newCall(requestBuilder.build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // Log the body for diagnosis; never put it in the exception
                    // message, which can reach the UI.
                    android.util.Log.w(TAG, "scan-receipt ${response.code}: ${raw.take(300)}")
                    throw ScanServerException(response.code)
                }
                parseResponse(raw)
            }
        }

    private fun parseResponse(json: String): ReceiptScanData {
        val obj = JSONObject(json)
        val items = obj.optJSONArray("items")?.let { arr ->
            (0 until arr.length()).map { i ->
                val it = arr.getJSONObject(i)
                ReceiptScanItem(
                    name = it.optString("name"),
                    qty = it.optString("qty"),
                    price = it.optString("price")
                )
            }
        } ?: emptyList()
        return ReceiptScanData(
            merchant = if (obj.isNull("merchant")) null else obj.optString("merchant"),
            total = obj.optString("total"),
            items = items,
            rawText = obj.optString("raw_text")
        )
    }

    private companion object {
        const val TAG = "ReceiptScanDS"
    }
}
