package com.example.insightku.core.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.feature.auth.data.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AutoTransactionDebug"

@AndroidEntryPoint
class DeleteTransactionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onReceive(context: Context, intent: Intent) {
        val txId    = intent.getStringExtra(EXTRA_TX_ID)    ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)

        Log.d(TAG, "DeleteTransactionReceiver: txId=$txId")

        // Dismiss the notification immediately
        if (notifId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notifId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    Log.d(TAG, "Delete skipped — no authenticated user")
                    pendingResult.finish()
                    return@launch
                }
                transactionRepository.deleteTransaction(txId, userId)
                Log.d(TAG, "Delete success: txId=$txId")
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed: txId=$txId, error=${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TX_ID    = "extra_tx_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}
