package com.findit.campus.ai

import java.util.Locale

data class LabelClue(val label: String, val confidence: Float)

data class ClassifiedClues(val items: List<LabelClue>, val scene: List<LabelClue>)

private const val MIN_LABEL_CONFIDENCE = 0.65f
private const val MAX_ITEM_CLUES = 5

// Scene labels should not become lost-item search queries or ownership evidence.
private val sceneLabels = setOf(
    "room", "tile", "shelf", "space", "building", "floor", "wall", "ceiling", "interior design",
    "architecture", "house", "home", "sky", "cloud", "grass", "plant", "tree", "road", "street",
    "landscape", "nature", "outdoor", "indoor", "window", "furniture", "table", "desk", "chair",
)
private val genericLabels = setOf("product", "material", "pattern", "font", "art", "event", "cool")

fun classifyScanLabels(labels: List<LabelClue>): ClassifiedClues {
    val ranked = labels.asSequence()
        .filter { it.confidence.isFinite() && it.confidence in MIN_LABEL_CONFIDENCE..1f }
        .map { it.copy(label = it.label.trim().lowercase(Locale.ROOT)) }
        .filter { it.label.isNotBlank() && it.label !in genericLabels }
        .sortedByDescending { it.confidence }
        .distinctBy { it.label }
        .toList()
    return ClassifiedClues(
        items = ranked.filterNot { it.label in sceneLabels }.take(MAX_ITEM_CLUES),
        scene = ranked.filter { it.label in sceneLabels }.take(MAX_ITEM_CLUES),
    )
}
