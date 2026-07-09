package com.example.insightku.core.ui.components

import androidx.compose.runtime.Composable
import com.example.insightku.core.navigation.RootNavGraph
import com.example.insightku.core.notification.NotificationTransactionData

@Composable
fun InsightKuApp(notificationData: NotificationTransactionData? = null, allocationDraftId: String? = null) {
    RootNavGraph(notificationData = notificationData, allocationDraftId = allocationDraftId)
}
