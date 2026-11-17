package com.findit.campus.notifications

import com.findit.campus.FindItApplication
import com.findit.campus.data.FindItLog
import com.findit.campus.data.cloud.stableId
import com.findit.campus.data.local.NotificationEntity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class FindItMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val graph = (application as FindItApplication).graph
        runBlocking(Dispatchers.IO) {
            try {
                withTimeout(8_000) {
                    val account = graph.accounts.observeAccount().first() ?: return@withTimeout
                    account.remoteUid?.let { graph.cloud.registerPushToken(it, token) }
                }
            } catch (error: Exception) { FindItLog.e("Push token refresh unavailable", error) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val graph = (application as FindItApplication).graph
        runBlocking(Dispatchers.IO) {
            try {
                withTimeout(3_000) {
                    val account = graph.accounts.observeAccount().first() ?: return@withTimeout
                    if (graph.cloud.auth?.currentUser?.uid != account.remoteUid) return@withTimeout
                    val destination = pushDestination(message.data, account.remoteUid, account.id) ?: return@withTimeout
                    val eventId = message.data["eventId"]?.takeIf { it.length in 1..200 } ?: return@withTimeout
                    // The server sends no private message contents or ownership photos to FCM.
                    CampusNotifications(this@FindItMessagingService, graph.accounts).showRemote(
                        NotificationEntity(id = stableId("push:$eventId"), accountId = account.id,
                            title = "New campus update", body = "Open FindIt to view your private conversation.",
                            itemId = destination.itemId, threadId = destination.threadId,
                            read = false, createdAt = System.currentTimeMillis()),
                    )
                }
            } catch (error: Exception) { FindItLog.e("Push update unavailable", error) }
        }
    }
}
