package com.findit.campus

import com.findit.campus.location.MeetingPin
import org.junit.Assert.*
import org.junit.Test

class MeetingPinTest {
    @Test fun privatePinRoundTripsAndStalePinsAreNotLive() {
        val pin = MeetingPin(31.2, 75.4, 12f, 100_000L, true)
        assertEquals(pin, MeetingPin.decode(pin.encode()))
        assertTrue(pin.recent(120_000))
        assertFalse(pin.recent(200_000))
        assertFalse(pin.recent(50_000))
    }
    @Test fun malformedCoordinatesCannotBecomeMapLocations() {
        assertNull(MeetingPin.decode("Meet near the library"))
        assertNull(MeetingPin.decode("FindIt meeting location\nNaN|75|1|100|true"))
        assertNull(MeetingPin.decode("FindIt meeting location\n91|75|1|100|true"))
        assertNull(MeetingPin.decode("FindIt meeting location\n31|181|1|100|true"))
        assertNull(MeetingPin.decode("FindIt meeting location\n31|75|-1|100|true"))
        assertNull(MeetingPin.decode("FindIt meeting location\n31|75|1|100|yes"))
    }
}
