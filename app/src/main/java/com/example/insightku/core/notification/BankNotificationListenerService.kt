package com.example.insightku.core.notification

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.insightku.core.data.repository.BankConsentRepository
import com.example.insightku.core.data.repository.DraftTransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

private const val TAG = "BankService"

// Global instance counter — survives across service restarts within the same process
private val instanceCounter = AtomicInteger(0)

@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var consentRepository: BankConsentRepository
    @Inject lateinit var draftRepository: DraftTransactionRepository

    // Each service instance gets a unique number for log correlation
    private val instanceId = instanceCounter.incrementAndGet()
    private val prefix get() = "[BankService #$instanceId]"

    private val dateFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private fun ts() = dateFmt.format(Date())
    private fun thread() = Thread.currentThread().name

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "$prefix Coroutine exception: ${throwable.message}", throwable)
        NotificationDebugLog.recordServiceEvent("$prefix COROUTINE ERROR: ${throwable.message}")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    private val draftTransactionManager = DraftTransactionManager.get()

    private var notificationCount = 0
    private var connectedAt: Long = 0L

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        val mem = memoryInfo()
        Log.d(TAG, "$prefix onCreate | ts=${ts()} thread=${thread()} pid=${Process.myPid()} $mem")
        NotificationDebugLog.recordServiceEvent("$prefix onCreate | ${ts()}")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedAt = System.currentTimeMillis()
        val cn = ComponentName(this, BankNotificationListenerService::class.java).flattenToString()
        Log.d(TAG, "$prefix onListenerConnected | ts=${ts()} thread=${thread()}")
        Log.d(TAG, "$prefix ComponentName = $cn")
        NotificationDebugLog.recordServiceEvent("$prefix CONNECTED | ${ts()}")
        NotificationListenerKeepaliveService.start(applicationContext)
    }

    override fun onListenerDisconnected() {
        // NOTE: Android often calls onDestroy BEFORE onListenerDisconnected.
        // This is expected behavior — do NOT call requestRebind() here.
        // requestRebind() in onListenerDisconnected causes a restart loop.
        // The system will rebind automatically when conditions are met.
        super.onListenerDisconnected()
        val uptime = if (connectedAt > 0) System.currentTimeMillis() - connectedAt else -1
        Log.w(TAG, "$prefix onListenerDisconnected | ts=${ts()} uptime=${uptime}ms thread=${thread()}")
        NotificationDebugLog.recordServiceEvent("$prefix DISCONNECTED | ${ts()} uptime=${uptime}ms")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "$prefix onDestroy | ts=${ts()} thread=${thread()}", Throwable("Destroy stacktrace"))
        NotificationDebugLog.recordServiceEvent("$prefix DESTROYED | ${ts()}")
    }

    // ── Notification handling ─────────────────────────────────────────────────

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: "(null)"
        notificationCount++
        val seq = "#$notificationCount"

        Log.d(TAG, "$prefix onNotificationPosted $seq | ts=${ts()} thread=${thread()} pkg=$packageName")

        val extras  = sbn.notification?.extras
        val title   = extras?.getString("android.title") ?: ""
        val content = extras?.getCharSequence("android.text")?.toString() ?: ""
        val category = sbn.notification?.category ?: ""

        Log.d(TAG, "$prefix   title='$title'")
        Log.d(TAG, "$prefix   content='${content.take(80)}'")
        Log.d(TAG, "$prefix   category='$category'")

        NotificationDebugLog.addRaw(
            RawNotificationEntry(packageName = packageName, title = title,
                content = content, category = category)
        )

        Log.i(TAG, "[NotificationAudit] Service Received Notification — pkg=$packageName ts=${ts()}")

        // Cheap synchronous pre-check: is this package even in our CAPABILITY list?
        // The CONSENT check (did the user allow it?) happens in the coroutine below,
        // since it reads DataStore. Both must pass before we process anything.
        val isSupported = BankNotificationParser.isSupportedPackage(packageName)
        Log.d(TAG, "$prefix   isSupported=$isSupported")
        if (!isSupported) {
            Log.d(TAG, "$prefix   SKIP: package not in supported parser list")
            return
        }

        if (extras == null) {
            Log.w(TAG, "$prefix   SKIP: notification extras=null")
            return
        }
        if (title.isBlank() && content.isBlank()) {
            Log.w(TAG, "$prefix   SKIP: blank title+content")
            return
        }

        Log.i(TAG, "[NotificationAudit] Manager Initialized = true")

        scope.launch {
            // CONSENT gate: user must have explicitly allowed this package.
            if (!consentRepository.isProcessingAllowed(packageName)) {
                Log.d(TAG, "$prefix   SKIP: package not allowed by user consent — pkg=$packageName")
                return@launch
            }

            Log.d(TAG, "$prefix   parsing... thread=${thread()}")
            try {
                val parsed = BankNotificationParser.parse(packageName, title, content)
                if (parsed == null) {
                    Log.w(TAG, "$prefix   PARSE FAILED: no pattern matched for pkg=$packageName title='$title'")
                    NotificationDebugLog.add(
                        NotificationDebugEntry(
                            packageName  = packageName,
                            bankName     = BankNotificationParser.getBankName(packageName),
                            rawTitle     = title,
                            rawContent   = content,
                            parseSuccess = false,
                            errorMessage = "No transaction pattern matched"
                        )
                    )
                    return@launch
                }

                Log.d(TAG, "$prefix   PARSE OK: amount=${parsed.amount} merchant='${parsed.merchant}' type=${parsed.type} bank=${parsed.bankName}")

                draftTransactionManager.processParsed(applicationContext, parsed, draftRepository)

                NotificationDebugLog.add(
                    NotificationDebugEntry(
                        packageName        = packageName,
                        bankName           = parsed.bankName,
                        rawTitle           = title,
                        rawContent         = content,
                        parsedAmount       = parsed.amount,
                        parsedMerchant     = parsed.merchant,
                        parsedType         = parsed.type,
                        parsedCategory     = parsed.category,
                        parseSuccess       = true,
                        transactionCreated = true
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "$prefix   Exception during parse/process: ${e.message}", e)
                NotificationDebugLog.recordServiceEvent("$prefix PROCESS ERROR: ${e.message}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "$prefix onNotificationRemoved | ts=${ts()} pkg=${sbn.packageName}")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun memoryInfo(): String {
        return try {
            val mi = ActivityManager.MemoryInfo()
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
            val usedMb = (mi.totalMem - mi.availMem) / 1_048_576
            val totalMb = mi.totalMem / 1_048_576
            "mem=${usedMb}/${totalMb}MB"
        } catch (e: Exception) { "mem=unknown" }
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val flat = ComponentName(context, BankNotificationListenerService::class.java)
                .flattenToString()
            val raw = android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            )
            Log.d(TAG, "isEnabled: flat=$flat raw=$raw")
            if (raw.isNullOrBlank()) return false
            return raw.split(":").any { it.trim() == flat }
        }

        fun openSettings(context: Context) {
            context.startActivity(
                android.content.Intent(
                    android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
            )
        }

        fun forceReconnect(context: Context) {
            val cn = ComponentName(context, BankNotificationListenerService::class.java)
            try {
                requestRebind(cn)
                Log.d(TAG, "forceReconnect: requestRebind($cn)")
                NotificationDebugLog.recordServiceEvent("Manual requestRebind at ${
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}")
            } catch (e: Exception) {
                Log.e(TAG, "forceReconnect error: ${e.message}")
            }
        }
    }
}
