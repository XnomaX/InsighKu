package com.example.insightku.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.insightku.MainActivity
import com.example.insightku.core.data.model.DraftTransaction
import com.example.insightku.core.data.model.DraftType
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.core.i18n.NumberFormatter
private const val TAG = "NotificationDebug"
private const val CHANNEL_ID   = "bank_notification_channel"
private const val CHANNEL_NAME = "Transaksi dari Notifikasi Bank"
private const val ALLOCATION_CHANNEL_ID = "auto_allocation_review_channel"
private const val ALLOCATION_CHANNEL_NAME = "Auto Allocation Review"
private const val DUPLICATE_WINDOW_MS = 60_000L

class DraftTransactionManager {

    companion object {
        @Volatile
        private var instance: DraftTransactionManager? = null

        fun get(): DraftTransactionManager =
            instance ?: synchronized(this) {
                instance ?: DraftTransactionManager().also { instance = it }
            }
    }

    // In-memory dedup: key = "amount|title|timestamp_bucket", value = timestamp last seen.
    // Lapisan cepat untuk burst notifikasi identik dalam <60s. Dedup durable (lintas waktu)
    // ditangani unique dedupHash di tabel draft_transactions.
    private val recentHashes = LinkedHashMap<String, Long>()

    /**
     * Proses notifikasi bank yang sudah diparse:
     * 1. Dedup cepat in-memory (burst <60s).
     * 2. Tulis [DraftTransaction] ke DB (status PENDING) — sumber kebenaran kartu Inbox di Home.
     *    Jika DB menolak (duplikat dedupHash), notifikasi tidak dikirim.
     * 3. Kirim notifikasi sistem yang membawa draftId di deep link.
     *
     * Tidak ada penulisan ke tabel transaksi utama. User tetap mengonfirmasi via AddTransaction.
     */
    suspend fun processParsed(
        context: Context,
        parsed: ParsedBankTransaction,
        draftRepository: DraftTransactionRepository
    ) {
        Log.i(TAG, "[NotificationAudit] Processing Started — amount=${parsed.amount} bank=${parsed.bankName}")

        val now = System.currentTimeMillis()
        // Bucket by 60s window so rapid duplicate notifications from same bank are skipped
        val timeBucket = now / DUPLICATE_WINDOW_MS
        val hash = "${parsed.amount}|${parsed.merchant}|$timeBucket"
        val lastSeen = recentHashes[hash]
        if (lastSeen != null && now - lastSeen < DUPLICATE_WINDOW_MS) {
            Log.d(TAG, "Duplicate detected, hash=$hash, skipping")
            return
        }
        recentHashes[hash] = now
        recentHashes.entries
            .filter { now - it.value > DUPLICATE_WINDOW_MS * 10 }
            .forEach { recentHashes.remove(it.key) }

        Log.d(TAG, "processParsed: amount=${parsed.amount}, merchant=${parsed.merchant}, type=${parsed.type}, bank=${parsed.bankName}")

        // Persist draft. dedupHash + confidence dihitung dari sinyal parser.
        val draft = DraftTransaction(
            amountGuess   = parsed.amount,
            typeGuess     = parsed.type,
            merchantGuess = parsed.merchant,
            bankName      = parsed.bankName,
            sourcePackage = parsed.sourcePackage,
            rawTitle      = parsed.rawTitle,
            rawContent    = parsed.rawContent,
            confidence    = parsed.confidence,
            detectedAt    = now,
            dedupHash     = DraftTransactionRepository.dedupHashOf(parsed.amount, parsed.merchant, parsed.bankName)
        )
        val stored = draftRepository.createDraft(draft)
        if (!stored) {
            Log.d(TAG, "Draft duplicate (dedupHash), not stored & no notification: ${draft.dedupHash}")
            return
        }

        sendTransactionNotification(context, parsed, draft.id, now)
    }

    private fun sendTransactionNotification(
        context: Context,
        parsed: ParsedBankTransaction,
        draftId: String,
        timestamp: Long
    ) {
        createChannelIfNeeded(context)

        val notifId    = draftId.hashCode()
        val amountText = NumberFormatter.formatCurrency(parsed.amount)
        val typeLabel  = when (parsed.type) {
            TransactionType.INCOME  -> "Dana Masuk"
            TransactionType.EXPENSE -> "Pembayaran"
            else -> "Transaksi" // Transfers, goals, etc. are never created by notification parsing
        }

        // Deep link URI: opens AddTransaction with pre-filled data + draftId so the
        // confirm flow can remove the draft after saving.
        // User still picks category and confirms before saving.
        val deepLinkUri = Uri.parse("insightku://add-transaction").buildUpon()
            .appendQueryParameter("draftId",     draftId)
            .appendQueryParameter("amount",      parsed.amount.toString())
            .appendQueryParameter("title",       parsed.merchant)
            .appendQueryParameter("bankName",    parsed.bankName)
            .appendQueryParameter("type",        parsed.type.name)
            .appendQueryParameter("timestamp",   timestamp.toString())
            .appendQueryParameter("description", parsed.rawContent.take(200))
            .build()

        val openIntent = Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action: user does not want to add this transaction.
        val dismissIntent = Intent(context, DismissNotificationReceiver::class.java).apply {
            putExtra(DismissNotificationReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, notifId + 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = buildString {
            append("$amountText dari ${parsed.bankName}")
            if (parsed.rawContent.isNotBlank()) {
                append("\n${parsed.rawContent.take(100)}")
            }
            append("\n\nKetuk untuk memilih kategori dan menyimpan.")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$typeLabel terdeteksi")
            .setContentText("$amountText - ${parsed.bankName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_input_add, "Catat Transaksi", openPending)
            .addAction(android.R.drawable.ic_delete, "Abaikan", dismissPending)
            .build()

        try {
            // Android 13+: check POST_NOTIFICATIONS permission before sending
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    Log.w(
                        TAG,
                        "POST_NOTIFICATIONS not granted, notification suppressed. User must grant permission in app settings."
                    )
                    return
                }
            }
            NotificationManagerCompat.from(context).notify(notifId, notification)
            Log.d(TAG, "Notification sent: notifId=$notifId, amount=${parsed.amount}, bank=${parsed.bankName}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending notification: ${e.message}")
        }
    }

    // ─── Auto-allocation draft creation ────────────────────────────────────

    /**
     * Create an auto-allocation draft for CONFIRMATION_REQUIRED rules.
     * This reuses the DraftTransactionManager infrastructure instead of
     * maintaining a separate pending allocation system.
     *
     * @return draftId if created successfully, null if duplicate or error
     */
    suspend fun createAllocationDraft(
        context: Context,
        ruleId: String,
        goalId: String,
        goalName: String,
        sourceAccountId: String,
        sourceAccountName: String,
        allocationAmount: Double,
        triggerType: String,
        triggerDescription: String,
        draftRepository: DraftTransactionRepository
    ): String? {
        Log.i(TAG, "[AllocationAudit] Creating allocation draft — rule=$ruleId goal=$goalName amount=$allocationAmount")

        val now = System.currentTimeMillis()
        val dedupHash = DraftTransactionRepository.allocationDedupHash(ruleId, goalId, allocationAmount)

        val draft = DraftTransaction(
            amountGuess = allocationAmount,
            typeGuess = TransactionType.EXPENSE,
            merchantGuess = goalName,
            bankName = "Auto Allocation",
            confidence = com.example.insightku.core.data.model.DraftConfidence.HIGH,
            detectedAt = now,
            dedupHash = dedupHash,
            draftType = DraftType.AUTO_ALLOCATION,
            ruleId = ruleId,
            goalId = goalId,
            goalName = goalName,
            sourceAccountId = sourceAccountId,
            sourceAccountName = sourceAccountName,
            allocationAmount = allocationAmount,
            triggerType = triggerType,
            triggerDescription = triggerDescription,
            triggerTimestamp = now
        )

        val stored = draftRepository.createAllocationDraft(draft)
        if (!stored) {
            Log.d(TAG, "Allocation draft duplicate, not stored: $dedupHash")
            return null
        }

        sendAllocationNotification(
            context = context,
            draftId = draft.id,
            goalName = goalName,
            amount = allocationAmount,
            triggerDescription = triggerDescription,
            sourceAccountName = sourceAccountName
        )

        return draft.id
    }

    private fun sendAllocationNotification(
        context: Context,
        draftId: String,
        goalName: String,
        amount: Double,
        triggerDescription: String,
        sourceAccountName: String
    ) {
        createAllocationChannelIfNeeded(context)

        val notifId = draftId.hashCode()
        val amountText = NumberFormatter.formatCurrency(amount)

        // Deep link URI: opens allocation draft review
        val deepLinkUri = Uri.parse("insightku://allocation-draft").buildUpon()
            .appendQueryParameter("draftId", draftId)
            .build()

        val openIntent = Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = buildString {
            append("Save $amountText to $goalName")
            append("\n$triggerDescription")
            append("\nFrom: $sourceAccountName")
            append("\n\nTap to review and confirm.")
        }

        val notification = NotificationCompat.Builder(context, ALLOCATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Auto-Allocation Review")
            .setContentText("$amountText → $goalName")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    Log.w(TAG, "POST_NOTIFICATIONS not granted, allocation notification suppressed")
                    return
                }
            }
            NotificationManagerCompat.from(context).notify(notifId, notification)
            Log.d(TAG, "Allocation notification sent: notifId=$notifId, goal=$goalName")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending allocation notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending allocation notification: ${e.message}")
        }
    }

    private fun createAllocationChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALLOCATION_CHANNEL_ID, ALLOCATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Auto-allocation confirmations requiring review"
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Transaksi terdeteksi dari notifikasi bank/e-wallet"
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
