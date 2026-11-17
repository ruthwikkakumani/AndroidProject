package com.findit.campus

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.findit.campus.data.AccountRepository
import com.findit.campus.data.AuthResult
import com.findit.campus.data.cloud.CloudServices
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.NotificationEntity
import com.findit.campus.notifications.CampusNotifications
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry

class NotificationDeliveryTest {
    @Test fun roomReplyCreatesPrivateAndroidNotificationAndReadingRemovesIt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        if (android.os.Build.VERSION.SDK_INT >= 33) automation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val db = Room.inMemoryDatabaseBuilder(context, FindItDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val manager = context.getSystemService(NotificationManager::class.java)
        var notificationId: Int? = null
        try {
            val repository = AccountRepository(db, CloudServices(context, enabled = false))
            repository.register("Notification Student", "notify@example.edu", "TestPass123", "TestPass123", "Test Campus")
            val account = (repository.login("notify@example.edu", "TestPass123") as AuthResult.SignedIn).account
            val delivery = CampusNotifications(context, repository)
            delivery.start(scope)
            val row = NotificationEntity(id = com.findit.campus.data.cloud.newId(), accountId = account.id,
                title = "Notification test reply", body = "Private test message", itemId = 81,
                read = false, createdAt = System.currentTimeMillis(), threadId = 82)
            val route = CampusNotifications.destination(delivery.intentFor(row))!!
            assertEquals(account.id, route.accountId)
            assertEquals(82L, route.threadId)
            assertEquals(81L, route.itemId)
            val id = db.accountDao().insertNotification(row)
            notificationId = (id xor (id ushr 32)).toInt()
            withTimeout(5000) { while (manager.activeNotifications.none { it.id == notificationId }) delay(50) }
            val notification = manager.activeNotifications.first { it.id == notificationId }.notification
            assertEquals(android.app.Notification.VISIBILITY_PRIVATE, notification.visibility)
            assertNotNull(notification.contentIntent)
            assertNotNull(notification.publicVersion)
            assertEquals("Notification test reply", notification.extras.getString(android.app.Notification.EXTRA_TITLE))
            repository.markNotificationsRead(account.id)
            withTimeout(5000) { while (manager.activeNotifications.any { it.id == notificationId }) delay(50) }
            assertTrue(manager.activeNotifications.none { it.id == notificationId })
        } finally {
            scope.cancel()
            notificationId?.let(manager::cancel)
            db.close()
        }
    }
}
