package com.example.insightku.feature.home.data.receipt

import android.content.Context
import android.net.Uri
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.feature.home.domain.receipt.ReceiptScanResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Online-only receipt scan. Reads the picked image, checks connectivity, calls
 * the remote Donut API, and maps the outcome to [ReceiptScanResult]. There is
 * no offline path — `Offline` and `Error` carry no cached data.
 */
@Singleton
class ReceiptScanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remote: ReceiptScanRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) {
    suspend fun scan(uri: Uri, deleteAfterRead: Boolean = false): ReceiptScanResult = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline()) {
            return@withContext ReceiptScanResult.Offline
        }

        val (bytes, mime) = try {
            readImage(uri)
        } catch (e: Exception) {
            return@withContext ReceiptScanResult.Error(str(R.string.scanner_error_read_image))
        }

        // For camera-captured photos we inserted a MediaStore row so the receipt
        // image never lands in the user's gallery (receipts carry financial
        // data). The bytes are now in memory, so it's safe to delete the row.
        // Failures here are non-fatal — a leftover temp image is a minor privacy
        // nuisance, not a scan failure.
        if (deleteAfterRead) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
        }

        // Classify by exception type so the user gets an actionable, localized
        // message. The data source deliberately throws code-only exceptions and
        // logs raw bodies itself, so nothing server-internal is forwarded here.
        try {
            ReceiptScanResult.Success(remote.scan(bytes, mime))
        } catch (e: IllegalArgumentException) {
            // require(baseUrl.isNotBlank()) in the data source
            ReceiptScanResult.Error(str(R.string.scanner_error_no_base_url))
        } catch (e: SocketTimeoutException) {
            ReceiptScanResult.Error(str(R.string.scanner_error_timeout))
        } catch (e: ScanServerException) {
            ReceiptScanResult.Error(str(R.string.scanner_error_server, e.code))
        } catch (e: Exception) {
            ReceiptScanResult.Error(e.message ?: str(R.string.scanner_error_scan_failed))
        }
    }

    /**
     * `@ApplicationContext` carries the system locale, not the in-app one, so
     * wrap via [LocaleHelper] — required for the message to follow the user's
     * chosen language on API <= 32. Called once per scan (an error path), never
     * in a loop, so the per-call allocation is not a concern.
     */
    private fun str(resId: Int, vararg args: Any?): String =
        LocaleHelper.wrapContext(context).getString(resId, *args)

    private fun readImage(uri: Uri): Pair<ByteArray, String> {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Cannot open image stream")
        return bytes to mime
    }
}
