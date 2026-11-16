package com.findit.campus.ui.format

import com.findit.campus.model.Item
import com.findit.campus.model.ItemType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun formatWhen(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}

fun formatFull(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a"))

fun Item.shareText(): String = buildString {
    append(if (type == ItemType.LOST) "Lost" else "Found")
    append(": ")
    append(title)
    append('\n')
    append(zoneName)
    if (placeLabel.isNotBlank() && placeLabel != zoneName) {
        append(" · ")
        append(placeLabel)
    }
    append('\n')
    append(description)
    if (contact.isNotBlank()) {
        append("\nContact: ")
        append(contact)
    }
    append("\nShared from FindIt")
}
