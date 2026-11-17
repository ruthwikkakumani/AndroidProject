package com.findit.campus

import com.findit.campus.auth.campusSuggestions
import com.findit.campus.notifications.AlertPolicy
import com.findit.campus.data.local.NotificationEntity
import org.junit.Assert.*
import org.junit.Test

class AlertAndCampusTest {
    private fun alert(id: Long, account: Long = 7, read: Boolean = false) =
        NotificationEntity(id, account, "Reply", "A student replied", 11, read, id, 12)

    @Test fun notificationsArePrivateDeduplicatedAndReadAlertsStayQuiet() {
        val policy = AlertPolicy()
        val rows = listOf(alert(1), alert(2, read = true), alert(3, account = 99))
        assertEquals(listOf(1L), policy.newUnread(7, rows).map { it.id })
        assertTrue(policy.newUnread(7, rows).isEmpty())
        assertEquals(listOf(4L), policy.newUnread(7, rows + alert(4)).map { it.id })
        assertTrue(policy.newUnread(null, rows).isEmpty())
        assertEquals(listOf(3L), policy.newUnread(99, rows).map { it.id })
    }

    @Test fun collegeSearchHandlesCaseWhitespaceDuplicatesAndCustomCampuses() {
        val names = listOf("VIT — Chennai", "VIT — Vellore", "SRM IST — Chennai", "VIT — Chennai")
        assertEquals(2, campusSuggestions(" vit ", names).size)
        assertEquals(listOf("SRM IST — Chennai", "VIT — Chennai"), campusSuggestions("chennai", names))
        assertTrue(campusSuggestions("My own college", names).isEmpty())
        assertEquals(1, campusSuggestions("", names, 1).size)
        assertTrue(campusSuggestions("", names, 0).isEmpty())
    }
}
