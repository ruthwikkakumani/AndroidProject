package com.findit.campus.ai

import com.findit.campus.model.Item
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.model.PulseInsight

class InsightEngine {
    fun pulse(items: List<Item>, nowMillis: Long = System.currentTimeMillis()): PulseInsight {
        val open = items.filter { it.status == ItemStatus.OPEN }
        if (open.isEmpty()) return PulseInsight.empty()
        val weekAgo = nowMillis - WEEK_MILLIS
        val recent = open.filter { it.createdAt >= weekAgo }
        val pool = if (recent.size >= 3) recent else open
        val topCategory = pool.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key
        val busiest = pool.groupingBy { it.zoneName }.eachCount().maxByOrNull { it.value }?.key
        val categoryZone = topCategory?.let { category ->
            pool.filter { it.category == category }
                .groupingBy { it.zoneName }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
        }
        val headline = when {
            topCategory != null && categoryZone != null ->
                "$topCategory turn up most often around $categoryZone."
            topCategory != null -> "$topCategory are the most reported items right now."
            busiest != null -> "Most reports are clustering around $busiest."
            else -> "The board is filling in."
        }
        val detail = buildString {
            append("${open.size} open")
            append(if (open.size == 1) " report" else " reports")
            if (busiest != null) {
                append(" · busiest spot is ")
                append(busiest)
            }
        }
        return PulseInsight(
            headline = headline,
            detail = detail,
            busiestZone = busiest,
            topCategory = topCategory,
            openCount = open.size,
        )
    }

    fun likelyPlaces(items: List<Item>, category: String, posting: ItemType): List<String> {
        val opposite = if (posting == ItemType.LOST) ItemType.FOUND else ItemType.LOST
        val fromOpposite = rankedZones(
            items.filter { it.category.equals(category, true) && it.type == opposite && it.status == ItemStatus.OPEN },
        )
        if (fromOpposite.isNotEmpty()) return fromOpposite
        return rankedZones(items.filter { it.category.equals(category, true) })
    }

    private fun rankedZones(items: List<Item>): List<String> =
        items.groupingBy { it.zoneName }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .filter { it.isNotBlank() }
            .take(3)

    companion object {
        const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
