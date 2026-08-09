package com.example.insightku.core.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.insightku.core.data.repository.DraftTransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NotificationDebug"

/**
 * Dismiss action on a transaction/allocation notification.
 *
 * Two tasks:
 * 1. Cancel the OS notification (original behavior).
 * 2. When the notification carries an EXTRA_DRAFT_ID, dismiss the related draft row so the
 *    "detected transaction" doesn't linger in the Draft Inbox.
 *
 * The Inbox undo snackbar is a UI concern (this receiver has none), so we soft-dismiss then
 * purge immediately — the draft simply disappears, matching what the user asked for by
 * tapping "Dismiss". Hilt injects the repository (broadcast receivers are system-constructed,
 * so it must be @AndroidEntryPoint + field injection).
 */
@AndroidEntryPoint
class DismissNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var draftRepository: DraftTransactionRepository

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        if (notifId != -1) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notifId)
            Log.d(TAG, "DismissNotificationReceiver: dismissed notifId=$notifId")
        }

        val draftId = intent.getStringExtra(EXTRA_DRAFT_ID)
        if (draftId.isNullOrBlank()) return

        // Repository methods are suspend; keep the broadcast alive while we purge off-main.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                draftRepository.dismiss(draftId)
                draftRepository.purgeDismissed(draftId)
                Log.d(TAG, "DismissNotificationReceiver: purged draft=$draftId")
            } catch (e: Exception) {
                Log.e(TAG, "DismissNotificationReceiver: failed to purge draft=$draftId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_NOTIF_ID = "extra_dismiss_notif_id"
        const val EXTRA_DRAFT_ID = "extra_dismiss_draft_id"
    }
}