package com.findit.campus.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

fun formatDistance(meters: Double): String = when {
    meters < 80 -> "A short walk"
    meters < 1_000 -> "${meters.roundToInt()} m away"
    else -> String.format(Locale.US, "%.1f km away", meters / 1_000.0)
}

data class CampusZone(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

object Campus {
    const val NAME = "Riverside Campus"
    const val CENTER_LAT = 37.422094
    const val CENTER_LNG = -122.083922
    const val ON_CAMPUS_METERS = 1_500.0

    val zones: List<CampusZone> = listOf(
        CampusZone("Library", 37.42240, -122.08430),
        CampusZone("Student Union", 37.42170, -122.08340),
        CampusZone("Engineering Hall", 37.42280, -122.08320),
        CampusZone("Cafeteria", 37.42190, -122.08470),
        CampusZone("Recreation Center", 37.42140, -122.08390),
        CampusZone("Residence Hall", 37.42260, -122.08510),
        CampusZone("Main Quad", 37.42210, -122.08400),
        CampusZone("Science Block", 37.42300, -122.08440),
        CampusZone("Campus Bus Stop", 37.42150, -122.08490),
        CampusZone("Admin Building", 37.42230, -122.08280),
    )

    fun nearest(latitude: Double, longitude: Double): CampusZone =
        zones.minBy { distanceMeters(latitude, longitude, it.latitude, it.longitude) }

    fun isOnCampus(latitude: Double, longitude: Double): Boolean =
        distanceMeters(latitude, longitude, CENTER_LAT, CENTER_LNG) <= ON_CAMPUS_METERS

    fun zoneOrOffCampus(latitude: Double, longitude: Double): String =
        if (isOnCampus(latitude, longitude)) nearest(latitude, longitude).name else "Off campus"
}

data class MapBounds(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
)

fun enclosingBounds(points: List<Pair<Double, Double>>): MapBounds {
    val lats = points.map { it.first }
    val lngs = points.map { it.second }
    val latSpan = (lats.max() - lats.min()).coerceAtLeast(0.003)
    val lngSpan = (lngs.max() - lngs.min()).coerceAtLeast(0.003)
    return MapBounds(
        south = lats.min() - latSpan * 0.28,
        north = lats.max() + latSpan * 0.28,
        west = lngs.min() - lngSpan * 0.28,
        east = lngs.max() + lngSpan * 0.28,
    )
}
