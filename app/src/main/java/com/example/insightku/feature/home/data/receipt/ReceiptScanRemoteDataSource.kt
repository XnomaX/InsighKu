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
 * Calls the Flask/Donut receipt-scan API over OkHttp. Online-only — the caller
 * (ReceiptScanRepository) gates on connectivity first. No caching of any kind.
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

            try {
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IOException("Server error ${response.code}: ${raw.take(200)}")
                    }
                    parseResponse(raw)
                }
            } catch (e: java.net.SocketTimeoutException) {
                throw IOException("Request timed out — server may be slow or offline", e)
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
}
