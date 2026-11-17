package com.findit.campus.data

import com.findit.campus.location.MeetingPin
import java.util.Base64

/** These cards travel through the existing participant-only message channel. */
data class PrivateContact(val phone: String, val note: String = "") {
    init {
        require(validPhone(phone)) { "Enter a valid phone number, including country code if needed." }
        require(note.length <= 80) { "Keep the contact note under 80 characters." }
    }
    val dialNumber: String get() = phone.filter { it.isDigit() || it == '+' }
    fun encode(): String = CardCodec.encode(PREFIX, listOf(phone.trim(), note.trim()))
    companion object {
        private const val PREFIX = "FindIt private contact\n"
        fun validPhone(value: String): Boolean {
            val phone = value.trim()
            return phone.matches(Regex("\\+?[0-9 ()-]+")) && phone.count { it in '0'..'9' } in 7..15 && phone.length <= 32
        }
        fun decode(body: String): PrivateContact? = CardCodec.decode(body, PREFIX, 2)?.let {
            runCatching { PrivateContact(it[0], it[1]) }.getOrNull()
        }
    }
}

data class MeetingPlan(val place: String, val time: String, val note: String = "") {
    init {
        require(place.isNotBlank() && place.length <= 80) { "Add a public meeting place (up to 80 characters)." }
        require(time.isNotBlank() && time.length <= 80) { "Add a day and time (up to 80 characters)." }
        require(note.length <= 200) { "Keep meeting notes under 200 characters." }
    }
    fun encode(): String = CardCodec.encode(PREFIX, listOf(place.trim(), time.trim(), note.trim()))
    companion object {
        private const val PREFIX = "FindIt meeting plan\n"
        fun decode(body: String): MeetingPlan? = CardCodec.decode(body, PREFIX, 3)?.let {
            runCatching { MeetingPlan(it[0], it[1], it[2]) }.getOrNull()
        }
    }
}

private object CardCodec {
    fun encode(prefix: String, fields: List<String>): String {
        val body = prefix + fields.joinToString("|") { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) }
        require(body.length <= 1000) { "These details are too long. Please shorten them." }
        return body
    }
    fun decode(body: String, prefix: String, count: Int): List<String>? {
        if (!body.startsWith(prefix) || body.length > 1000) return null
        val fields = body.removePrefix(prefix).split('|')
        if (fields.size != count) return null
        return runCatching { fields.map { String(Base64.getDecoder().decode(it), Charsets.UTF_8) } }.getOrNull()
    }
}

/** No private phone numbers or coordinates in inbox/notification previews. */
fun chatPreview(body: String?): String = when {
    body.isNullOrBlank() -> "A proof photo was shared."
    PrivateContact.decode(body) != null -> "Private contact details shared"
    MeetingPlan.decode(body) != null -> "A meeting plan was shared"
    MeetingPin.decode(body) != null -> "A meeting location was shared"
    else -> body
}

/** Foreground live fixes update the meeting map without filling the alerts inbox. */
fun shouldAlertForChat(body: String): Boolean = MeetingPin.decode(body)?.live != true
