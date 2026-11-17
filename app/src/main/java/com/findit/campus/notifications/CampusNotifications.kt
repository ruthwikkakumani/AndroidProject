package com.findit.campus.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.findit.campus.MainActivity
import com.findit.campus.R
import com.findit.campus.data.AccountRepository
import com.findit.campus.data.local.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/** Delivers Room alerts while this app process is running; Firebase sync can supply them. */
class CampusNotifications(private val context: Context, private val accounts: AccountRepository) {
    private val manager = NotificationManagerCompat.from(context)
    private val policy = AlertPolicy()
    private val displayed = mutableMapOf<Long, Int>()

    fun start(scope: CoroutineScope) {
        createChannel()
        scope.launch {
            accounts.observeAccount().distinctUntilChangedBy { it?.id }.collectLatest { account ->
                // Includes pushes displayed by the service while the process was stopped.
                manager.cancelAll()
                displayed.clear()
                policy.newUnread(account?.id, emptyList())
                if (account != null) accounts.observeNotifications(account.id).collect { rows ->
                    val unread = rows.filter { !it.read }.map { it.id }.toSet()
                    displayed.keys.filter { it !in unread }.forEach { id ->
                        displayed.remove(id)?.let(manager::cancel)
                    }
                    if (manager.areNotificationsEnabled()) {
                        policy.newUnread(account.id, rows).take(5).forEach { row ->
                            val id = notificationId(row.id)
                            try {
                                manager.notify(id, notification(row))
                                displayed[row.id] = id
                            } catch (_: SecurityException) { /* Permission can change between checks. */ }
                        }
                    }
                }
            }
        }
    }

    fun showRemote(row: NotificationEntity) {
        createChannel()
        if (!manager.areNotificationsEnabled()) return
        try { manager.notify(notificationId(row.id), notification(row)) }
        catch (_: SecurityException) { /* Respect a permission revoked during delivery. */ }
    }

    private fun notificationId(id: Long) = (id xor (id ushr 32)).toInt()

    private fun createChannel() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Campus replies & returns", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Private replies, found-item requests and handover updates"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            },
        )
    }

    internal fun intentFor(row: NotificationEntity): Intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.parse("findit://alert/${row.accountId}/${row.id}")
            putExtra(ACCOUNT, row.accountId)
            row.threadId?.let { putExtra(THREAD, it) }
            row.itemId?.let { putExtra(ITEM, it) }
        }

    private fun notification(row: NotificationEntity): android.app.Notification {
        val pending = PendingIntent.getActivity(context, 0, intentFor(row),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(row.title)
            .setContentText(row.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(row.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("FindIt campus update")
                .setContentText("Open FindIt to view your private update").build())
            .build()
    }

    companion object {
        private const val CHANNEL = "campus_replies"
        private const val ACCOUNT = "alert_account"
        private const val THREAD = "alert_thread"
        private const val ITEM = "alert_item"

        fun destination(intent: Intent?): AlertDestination? {
            val id = intent?.getLongExtra(ACCOUNT, 0)?.takeIf { it > 0 } ?: return null
            return AlertDestination(id, intent.getLongExtra(THREAD, 0).takeIf { it > 0 },
                intent.getLongExtra(ITEM, 0).takeIf { it > 0 })
        }
    }
}
