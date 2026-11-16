package com.findit.campus.model

enum class ItemType {
    LOST,
    FOUND,
}

enum class ItemStatus {
    OPEN,
    REUNITED,
}

data class Item(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val categoryId: Long = 0,
    val type: ItemType,
    val status: ItemStatus = ItemStatus.OPEN,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val placeLabel: String = "",
    val zoneName: String = "",
    val photoPath: String? = null,
    val scannedText: String = "",
    val dominantColor: String? = null,
    val contact: String = "",
    val createdAt: Long = 0,
    val resolvedAt: Long? = null,
    val isSample: Boolean = false,
    val ownerId: Long = 0,
    val hasCoordinates: Boolean = true,
    val hasPrivateCheck: Boolean = false,
)

data class Matchable(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val type: ItemType,
    val status: ItemStatus,
    val latitude: Double,
    val longitude: Double,
    val zone: String,
    val scannedText: String,
    val colorHex: String?,
    val createdAt: Long,
    val hasCoordinates: Boolean = true,
)

data class ScoredMatch(
    val itemId: Long,
    val score: Int,
    val reasons: List<String>,
)

data class ScoredPair(
    val lostId: Long,
    val foundId: Long,
    val score: Int,
    val reasons: List<String>,
)

data class PulseInsight(
    val headline: String,
    val detail: String,
    val busiestZone: String?,
    val topCategory: String?,
    val openCount: Int,
) {
    companion object {
        fun empty() = PulseInsight(
            headline = "New reports will shape the campus pulse.",
            detail = "No open reports yet",
            busiestZone = null,
            topCategory = null,
            openCount = 0,
        )
    }
}

fun Item.toMatchable(): Matchable = Matchable(
    id = id,
    title = title,
    description = description,
    category = category,
    type = type,
    status = status,
    latitude = latitude,
    longitude = longitude,
    zone = zoneName,
    scannedText = scannedText,
    colorHex = dominantColor,
    createdAt = createdAt,
    hasCoordinates = hasCoordinates,
)

fun matchLabel(score: Int): String = when {
    score >= 80 -> "Strong match"
    score >= 68 -> "Promising"
    else -> "Worth a look"
}
