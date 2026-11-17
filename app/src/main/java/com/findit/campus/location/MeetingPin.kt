package com.findit.campus.location

/** Private message payload; no location is shared until the student explicitly chooses to send it. */
data class MeetingPin(val latitude: Double, val longitude: Double, val accuracyMeters: Float,
    val capturedAt: Long, val live: Boolean = false) {
    fun encode(): String = "$PREFIX$latitude|$longitude|$accuracyMeters|$capturedAt|$live"
    fun recent(now: Long = System.currentTimeMillis()): Boolean = capturedAt in (now - 90_000)..(now + 10_000)

    companion object {
        private const val PREFIX = "FindIt meeting location\n"
        fun decode(body: String): MeetingPin? {
            if (!body.startsWith(PREFIX)) return null
            val values = body.removePrefix(PREFIX).split('|')
            if (values.size != 5) return null
            val lat = values[0].toDoubleOrNull() ?: return null
            val lng = values[1].toDoubleOrNull() ?: return null
            val accuracy = values[2].toFloatOrNull() ?: return null
            val time = values[3].toLongOrNull() ?: return null
            val live = values[4].toBooleanStrictOrNull() ?: return null
            if (!lat.isFinite() || !lng.isFinite() || !accuracy.isFinite() || lat !in -90.0..90.0 ||
                lng !in -180.0..180.0 || accuracy < 0 || time <= 0) return null
            return MeetingPin(lat, lng, accuracy, time, live)
        }
    }
}
