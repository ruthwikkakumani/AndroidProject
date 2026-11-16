package com.findit.campus.ui.home

import com.findit.campus.ai.InsightEngine
import com.findit.campus.ai.SmartMatchEngine
import com.findit.campus.model.Item
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.model.PulseInsight
import com.findit.campus.model.toMatchable

data class FeedFilter(
    val query: String = "",
    val type: ItemType? = null,
    val openOnly: Boolean = true,
    val category: String? = null,
    val selectedId: Long = -1L,
)

data class MatchPreview(
    val item: Item,
    val score: Int,
    val reasons: List<String>,
)

data class ReunionSuggestion(
    val lost: Item,
    val found: Item,
    val score: Int,
    val reasons: List<String>,
)

data class HomeUiState(
    val query: String = "",
    val type: ItemType? = null,
    val openOnly: Boolean = true,
    val category: String? = null,
    val items: List<Item> = emptyList(),
    val pulse: PulseInsight = PulseInsight.empty(),
    val reunions: List<ReunionSuggestion> = emptyList(),
    val matchCounts: Map<Long, Int> = emptyMap(),
    val categories: List<String> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val selectedId: Long = -1L,
    val selectedMatches: List<MatchPreview> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

fun applyFeedFilters(items: List<Item>, filter: FeedFilter): List<Item> =
    items.filter { item ->
        (filter.type == null || item.type == filter.type) &&
            (!filter.openOnly || item.status == ItemStatus.OPEN) &&
            (filter.category == null || item.category == filter.category)
    }

fun buildHomeState(
    filter: FeedFilter,
    searched: List<Item>,
    all: List<Item>,
    matcher: SmartMatchEngine,
    insights: InsightEngine,
): HomeUiState {
    val visible = applyFeedFilters(searched, filter)
    val open = all.filter { it.status == ItemStatus.OPEN }
    val matchables = open.map { it.toMatchable() }
    val byId = all.associateBy { it.id }
    val counts = open.associate { item ->
        item.id to matcher.rank(item.toMatchable(), matchables).size
    }
    val reunions = matcher.bestPairs(matchables).mapNotNull { pair ->
        val lost = byId[pair.lostId] ?: return@mapNotNull null
        val found = byId[pair.foundId] ?: return@mapNotNull null
        ReunionSuggestion(lost, found, pair.score, pair.reasons)
    }
    val selected = byId[filter.selectedId]
    val selectedMatches = selected?.let { item ->
        matcher.rank(item.toMatchable(), matchables).mapNotNull { scored ->
            val other = byId[scored.itemId] ?: return@mapNotNull null
            MatchPreview(other, scored.score, scored.reasons)
        }
    }.orEmpty()
    return HomeUiState(
        query = filter.query,
        type = filter.type,
        openOnly = filter.openOnly,
        category = filter.category,
        items = visible,
        pulse = insights.pulse(all),
        reunions = reunions,
        matchCounts = counts,
        categories = all.map { it.category }.distinct().sorted(),
        categoryCounts = all.groupingBy { it.category }.eachCount(),
        selectedId = filter.selectedId,
        selectedMatches = selectedMatches,
        loading = false,
    )
}
