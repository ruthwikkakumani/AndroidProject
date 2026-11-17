package com.findit.campus

import com.findit.campus.notifications.pushDestination
import org.junit.Assert.*
import org.junit.Test

class PushDestinationTest {
    private val payload = mapOf("recipientUid" to "student-a", "threadId" to "42", "itemId" to "81")

    @Test fun directsOnlyTheSignedInRecipientToTheirConversation() {
        val route = pushDestination(payload, "student-a", 7)!!
        assertEquals(7L, route.accountId)
        assertEquals(42L, route.threadId)
        assertEquals(81L, route.itemId)
        assertNull(pushDestination(payload, "student-b", 8))
        assertNull(pushDestination(payload, null, 7))
        assertNull(pushDestination(payload, "student-a", 0))
    }

    @Test fun rejectsInvalidOrOutOfRangeThreadIdentifiers() {
        listOf("0", "-1", "not-an-id", "9007199254740992", "9223372036854775808").forEach {
            assertNull(pushDestination(payload + ("threadId" to it), "student-a", 7))
        }
        assertNull(pushDestination(payload - "threadId", "student-a", 7))
        assertNull(pushDestination(payload + ("itemId" to "-1"), "student-a", 7)!!.itemId)
    }
}
