package com.findit.campus.data

import com.findit.campus.location.Campus
import com.findit.campus.model.ItemType
import kotlin.math.cos
import kotlin.math.sin

data class CatalogCategory(val name: String, val symbol: String)

object CampusCatalog {
    val categories: List<CatalogCategory> = listOf(
        CatalogCategory("Accessories", "accessory"),
        CatalogCategory("Bags", "bag"),
        CatalogCategory("Books", "book"),
        CatalogCategory("Bottles", "bottle"),
        CatalogCategory("Clothing", "clothing"),
        CatalogCategory("Electronics", "electronics"),
        CatalogCategory("IDs", "id"),
        CatalogCategory("Keys", "keys"),
        CatalogCategory("Other", "other"),
    )

    val categoryNames: List<String> = categories.map { it.name }
}

data class SampleReport(
    val type: ItemType,
    val category: String,
    val zone: String,
    val title: String,
    val description: String,
    val hoursAgo: Int,
    val colorHex: String? = null,
    val scannedText: String = "",
    val jitter: Int = 0,
)

fun SampleReport.coordinates(): Pair<Double, Double> {
    val zone = Campus.zones.first { it.name == this.zone }
    val angle = jitter * 0.85
    val lat = zone.latitude + 0.00008 * cos(angle)
    val lng = zone.longitude + 0.00008 * sin(angle)
    return lat to lng
}

fun sampleReports(): List<SampleReport> = listOf(
    SampleReport(
        type = ItemType.LOST,
        category = "Bottles",
        zone = "Library",
        title = "Navy Hydro Flask",
        description = "Navy 32oz bottle with a mountain sticker. Last seen in a quiet study carrel on the second floor.",
        hoursAgo = 6,
        colorHex = "#1B3A4B",
        jitter = 1,
    ),
    SampleReport(
        type = ItemType.FOUND,
        category = "Bottles",
        zone = "Library",
        title = "Blue water bottle",
        description = "Navy bottle with a mountain sticker, turned in from the quiet study desks.",
        hoursAgo = 4,
        colorHex = "#1A394A",
        jitter = 2,
    ),
    SampleReport(
        type = ItemType.LOST,
        category = "Books",
        zone = "Engineering Hall",
        title = "Calculus textbook",
        description = "Stewart Calculus. The name A. Mehta is written on the inside cover.",
        hoursAgo = 20,
        jitter = 3,
    ),
    SampleReport(
        type = ItemType.FOUND,
        category = "Books",
        zone = "Engineering Hall",
        title = "Math textbook",
        description = "Calculus book with A. Mehta written inside the cover.",
        hoursAgo = 10,
        scannedText = "A. Mehta",
        jitter = 4,
    ),
    SampleReport(
        type = ItemType.LOST,
        category = "Electronics",
        zone = "Student Union",
        title = "White earbud case",
        description = "White charging case only, small scratch on the lid. Earbuds are not inside.",
        hoursAgo = 8,
        colorHex = "#F4F4F4",
        jitter = 1,
    ),
    SampleReport(
        type = ItemType.FOUND,
        category = "Electronics",
        zone = "Cafeteria",
        title = "Scratched white case",
        description = "White earbud case with a scratch on the lid. No earbuds inside.",
        hoursAgo = 3,
        colorHex = "#F7F7F7",
        jitter = 2,
    ),
    SampleReport(
        type = ItemType.LOST,
        category = "Clothing",
        zone = "Recreation Center",
        title = "Black hoodie",
        description = "Black medium hoodie with a small white logo on the left sleeve.",
        hoursAgo = 30,
        colorHex = "#1A1A1A",
        jitter = 1,
    ),
    SampleReport(
        type = ItemType.FOUND,
        category = "IDs",
        zone = "Library",
        title = "Campus ID card",
        description = "Student ID for M. Alvarez, found near the library printers.",
        hoursAgo = 2,
        scannedText = "M. Alvarez",
        jitter = 5,
    ),
    SampleReport(
        type = ItemType.LOST,
        category = "Keys",
        zone = "Campus Bus Stop",
        title = "House keys with red keychain",
        description = "Three house keys on a red circular keychain.",
        hoursAgo = 12,
        colorHex = "#C0392B",
        jitter = 1,
    ),
    SampleReport(
        type = ItemType.FOUND,
        category = "Keys",
        zone = "Campus Bus Stop",
        title = "Keys on a red ring",
        description = "Three keys with a red circular keychain, left on the bus stop bench.",
        hoursAgo = 9,
        colorHex = "#C0392B",
        jitter = 2,
    ),
    SampleReport(
        type = ItemType.LOST,
        category = "Electronics",
        zone = "Science Block",
        title = "Graphing calculator",
        description = "Silver TI-84. Initials A.K. scratched on the back.",
        hoursAgo = 48,
        colorHex = "#B0B7C3",
        scannedText = "A.K.",
        jitter = 1,
    ),
    SampleReport(
        type = ItemType.FOUND,
        category = "Electronics",
        zone = "Science Block",
        title = "TI calculator",
        description = "Silver graphing calculator, initials A.K. on the back, found in a lab slot.",
        hoursAgo = 40,
        colorHex = "#B0B7C3",
        scannedText = "A.K.",
        jitter = 3,
    ),
    SampleReport(
        type = ItemType.LOST,
        category = "Accessories",
        zone = "Main Quad",
        title = "Green umbrella",
        description = "Compact green umbrella with a wooden handle.",
        hoursAgo = 15,
        colorHex = "#2E7D32",
        jitter = 1,
    ),
    SampleReport(
        type = ItemType.FOUND,
        category = "Accessories",
        zone = "Main Quad",
        title = "Green compact umbrella",
        description = "Green umbrella with a wooden handle, by the quad benches.",
        hoursAgo = 7,
        colorHex = "#2F7D4A",
        jitter = 2,
    ),
    SampleReport(
        type = ItemType.LOST,
        category = "Bags",
        zone = "Residence Hall",
        title = "Grey backpack",
        description = "Grey backpack with a bottle pocket. Left in the laundry room.",
        hoursAgo = 26,
        jitter = 1,
    ),
)
