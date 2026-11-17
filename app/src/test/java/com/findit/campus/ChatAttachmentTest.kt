package com.findit.campus

import com.findit.campus.data.*
import org.junit.Assert.*
import org.junit.Test

class ChatAttachmentTest {
    @Test fun liveFixesStayQuietButOtherMessagesKeepTheirAlerts() {
        val pin = com.findit.campus.location.MeetingPin(13.0827, 80.2703, 5f, System.currentTimeMillis(), true)
        assertFalse(shouldAlertForChat(pin.encode()))
        assertTrue(shouldAlertForChat(pin.copy(live = false).encode()))
        assertTrue(shouldAlertForChat("Let's meet now"))
        assertTrue(shouldAlertForChat(PrivateContact("+91 98765 43210").encode()))
    }
    @Test fun privateContactRoundTripsAndKeepsSafeDialNumber() {
        val contact = PrivateContact("+91 (98765) 43210", "After class")
        assertEquals(contact, PrivateContact.decode(contact.encode()))
        assertEquals("+919876543210", contact.dialNumber)
        assertEquals("Private contact details shared", chatPreview(contact.encode()))
    }
    @Test fun invalidPhonesCannotBecomeDialLinks() {
        listOf("123", "++919876543210", "tel:9876543210", "9876543210?call=true", "١٢٣٤٥٦٧٨٩٠", "1234567890123456").forEach {
            assertFalse(it, PrivateContact.validPhone(it))
        }
    }
    @Test fun meetingDetailsPreserveUnicodeAndSeparators() {
        val plan = MeetingPlan("Library | entrance", "Today, 3:30 PM", "Meet beside the café\nBlue backpack")
        assertEquals(plan, MeetingPlan.decode(plan.encode()))
        assertEquals("A meeting plan was shared", chatPreview(plan.encode()))
    }
    @Test fun malformedCardsFallBackWithoutCrashing() {
        listOf("FindIt private contact\n!!|", "FindIt meeting plan\nYQ==|Yg==", "FindIt private contact\nMTIz|", "FindIt meeting plan\n||").forEach {
            assertNull(PrivateContact.decode(it)); assertNull(MeetingPlan.decode(it))
        }
    }
    @Test fun oversizedUnicodeIsRejectedBeforeMessageTruncation() {
        assertThrows(IllegalArgumentException::class.java) {
            MeetingPlan("界".repeat(80), "界".repeat(80), "界".repeat(200)).encode()
        }
    }
}
