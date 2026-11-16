package com.findit.campus.ai

import com.findit.campus.location.distanceMeters
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.model.Matchable
import com.findit.campus.model.ScoredMatch
import com.findit.campus.model.ScoredPair
import kotlin.math.abs
import kotlin.math.roundToInt

class SmartMatchEngine {
    fun rank(
        source: Matchable,
        candidates: List<Matchable>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<ScoredMatch> = candidates
        .mapNotNull { score(source, it, nowMillis) }
        .sortedByDescending { it.score }

    fun score(source: Matchable, candidate: Matchable, nowMillis: Long = System.currentTimeMillis()): ScoredMatch? {
        if (source.id == candidate.id) return null
        if (source.type == candidate.type) return null
        if (source.status != ItemStatus.OPEN || candidate.status != ItemStatus.OPEN) return null

        val reasons = mutableListOf<String>()
        var points = 0

        if (source.category.equals(candidate.category, ignoreCase = true)) {
            points += CATEGORY_POINTS
            reasons += "Same category"
        }

        val overlap = jaccard(tokens(source), tokens(candidate))
        val textPoints = (overlap * TEXT_POINTS).roundToInt()
        if (textPoints > 0) {
            points += textPoints
            reasons += if (textPoints >= 10) "Descriptions overlap" else "A few words in common"
        }

        if (source.zone.isNotBlank() && source.zone.equals(candidate.zone, ignoreCase = true)) {
            points += ZONE_POINTS
            reasons += "Same spot on campus"
        }

        if (source.hasCoordinates && candidate.hasCoordinates) {
            val meters = distanceMeters(source.latitude, source.longitude, candidate.latitude, candidate.longitude)
            val distancePoints = ((1.0 - (meters / DISTANCE_FALLOFF_METERS).coerceIn(0.0, 1.0)) * DISTANCE_POINTS).roundToInt()
            if (distancePoints > 0) {
                points += distancePoints
                reasons += if (meters < 80) "Within a short walk" else "About ${meters.roundToInt()} m apart"
            }
        }

        if (colorsClose(source.colorHex, candidate.colorHex)) {
            points += COLOR_POINTS
            reasons += "Similar color"
        }

        if (abs(source.createdAt - candidate.createdAt) <= RECENCY_WINDOW_MILLIS) {
            points += RECENCY_POINTS
            reasons += "Reported around the same time"
        }

        if (points < THRESHOLD) return null
        return ScoredMatch(
            itemId = candidate.id,
            score = points.coerceAtMost(100),
            reasons = reasons,
        )
    }

    fun bestPairs(items: List<Matchable>, nowMillis: Long = System.currentTimeMillis()): List<ScoredPair> {
        val open = items.filter { it.status == ItemStatus.OPEN }
        val lost = open.filter { it.type == ItemType.LOST }
        val ranked = lost.mapNotNull { source ->
            val best = rank(source, open, nowMillis).firstOrNull() ?: return@mapNotNull null
            ScoredPair(source.id, best.itemId, best.score, best.reasons)
        }.sortedByDescending { it.score }
        val usedFound = mutableSetOf<Long>()
        return ranked.filter { usedFound.add(it.foundId) }.take(6)
    }

    fun rankByWords(query: String, candidates: List<Matchable>): List<ScoredMatch> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()
        return candidates.mapNotNull { candidate ->
            if (candidate.status != ItemStatus.OPEN) return@mapNotNull null
            val itemTokens = tokens(candidate)
            val covered = queryTokens.intersect(itemTokens).size.toDouble() / queryTokens.size
            val points = (covered * 100).roundToInt()
            if (points < WORD_THRESHOLD) {
                null
            } else {
                ScoredMatch(
                    itemId = candidate.id,
                    score = points,
                    reasons = listOf("Words from the scan show up here"),
                )
            }
        }.sortedByDescending { it.score }
    }

    private fun tokens(item: Matchable): Set<String> =
        tokenize("${item.title} ${item.description} ${item.scannedText} ${item.category}")

    companion object {
        const val CATEGORY_POINTS = 32
        const val TEXT_POINTS = 32
        const val ZONE_POINTS = 12
        const val DISTANCE_POINTS = 14
        const val COLOR_POINTS = 6
        const val RECENCY_POINTS = 4
        const val THRESHOLD = 56
        const val WORD_THRESHOLD = 50
        const val DISTANCE_FALLOFF_METERS = 1_200.0
        const val RECENCY_WINDOW_MILLIS = 21L * 24 * 60 * 60 * 1000
    }
}

private val STOP_WORDS = setOf(
    "a", "an", "the", "of", "in", "on", "at", "to", "for", "and", "or", "with",
    "from", "near", "by", "it", "my", "is", "was", "found", "lost", "item", "campus",
)

internal fun tokenize(text: String): Set<String> {
    val compact = text.lowercase().replace(Regex("(?<=[a-z0-9])\\.(?=[a-z0-9])"), "")
    return compact
        .split(Regex("[^a-z0-9]+"))
        .map { it.trim() }
        .filter { it.length >= 2 && it !in STOP_WORDS }
        .toSet()
}

internal fun jaccard(left: Set<String>, right: Set<String>): Double {
    if (left.isEmpty() || right.isEmpty()) return 0.0
    val intersection = left.intersect(right).size.toDouble()
    val union = left.union(right).size.toDouble()
    return intersection / union
}

internal fun colorsClose(first: String?, second: String?): Boolean {
    val left = parseColor(first) ?: return false
    val right = parseColor(second) ?: return false
    val dr = left.first - right.first
    val dg = left.second - right.second
    val db = left.third - right.third
    return dr * dr + dg * dg + db * db < 55 * 55
}

private fun parseColor(hex: String?): Triple<Int, Int, Int>? {
    val cleaned = hex?.trim()?.removePrefix("#") ?: return null
    if (cleaned.length != 6) return null
    val value = cleaned.toIntOrNull(16) ?: return null
    return Triple((value shr 16) and 0xFF, (value shr 8) and 0xFF, value and 0xFF)
}
