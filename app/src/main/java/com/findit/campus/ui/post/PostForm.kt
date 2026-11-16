package com.findit.campus.ui.post

import com.findit.campus.auth.sealSecret
import com.findit.campus.data.local.ItemEntity
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

data class PostForm(
    val type: ItemType = ItemType.LOST,
    val title: String = "",
    val description: String = "",
    val category: String = "Other",
    val whenMillis: Long = 0L,
    val photoPath: String? = null,
    val colorHex: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeLabel: String = "",
    val zoneName: String = "",
    val contact: String = "",
    val scannedText: String = "",
    val privateDetail: String = "",
)

fun validatePost(form: PostForm, now: Long = System.currentTimeMillis()): String? {
    if (form.title.trim().length < 3) return "Give the item a short name."
    if (form.description.trim().length < 8) return "Add a few details someone could recognize."
    if (form.zoneName.isBlank()) return "Name the place on campus."
    if (form.whenMillis > 0 && isFutureDate(form.whenMillis, now)) {
        return "The date can't be in the future."
    }
    return null
}

fun isFutureDate(localMillis: Long, now: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val selected = Instant.ofEpochMilli(localMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return selected.isAfter(today)
}

fun datePickerMillisToLocal(utcMillis: Long): Long {
    val picked = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return picked.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun PostForm.toEntity(categoryId: Long, ownerId: Long): ItemEntity {
    val pinned = latitude != null && longitude != null
    return ItemEntity(
        title = title.trim(),
        description = description.trim(),
        categoryId = categoryId,
        type = type,
        status = ItemStatus.OPEN,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        placeLabel = placeLabel.ifBlank { zoneName },
        zoneName = zoneName.trim(),
        photoPath = photoPath,
        scannedText = scannedText.trim(),
        dominantColor = colorHex,
        contact = contact.trim(),
        createdAt = whenMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
        resolvedAt = null,
        isSample = false,
        ownerId = ownerId,
        hasCoordinates = pinned,
        verificationSecret = privateDetail.trim().takeIf { it.isNotEmpty() }?.let(::sealSecret),
    )
}
