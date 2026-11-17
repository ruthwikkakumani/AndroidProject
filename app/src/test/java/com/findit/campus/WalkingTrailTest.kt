package com.findit.campus

import com.findit.campus.data.local.TrailPoint
import com.findit.campus.location.*
import org.junit.Assert.*
import org.junit.Test

class WalkingTrailTest {
    private val previous = TrailPoint(accountId = 1, segmentId = "walk", latitude = 13.0,
        longitude = 80.0, accuracyMeters = 5f, capturedAt = 10_000)
    @Test fun trailRejectsDriftPoorAccuracyStaleFixesAndGpsJumps() {
        val valid = DeviceFix(13.0001, 80.0, 5f, 15_000)
        assertTrue(acceptsTrailFix(previous, "walk", valid, 15_000))
        assertFalse(acceptsTrailFix(previous, "walk", valid.copy(latitude = 13.000001), 15_000))
        assertFalse(acceptsTrailFix(previous, "walk", valid.copy(accuracyMeters = 500f), 15_000))
        assertFalse(acceptsTrailFix(previous, "walk", valid, 50_000))
        assertFalse(acceptsTrailFix(previous, "walk", valid.copy(latitude = 14.0), 15_000))
        assertFalse(acceptsTrailFix(previous, "walk", valid.copy(latitude = Double.NaN), 15_000))
        assertFalse(acceptsTrailFix(previous, "walk", valid.copy(capturedAtMillis = 10_000), 15_000))
    }
    @Test fun pausedSegmentsDoNotCreateFictionalConnectingDistance() {
        val a = previous
        val b = previous.copy(latitude = 13.0001, capturedAt = 15_000)
        val c = previous.copy(segmentId = "next", latitude = 14.0, capturedAt = 20_000)
        val d = c.copy(latitude = 14.0001, capturedAt = 25_000)
        assertEquals(22.24, trailDistanceMeters(listOf(a, b, c, d)), 0.2)
        assertTrue(acceptsTrailFix(b, "next", DeviceFix(c.latitude, c.longitude, 5f, c.capturedAt), c.capturedAt))
    }
}
