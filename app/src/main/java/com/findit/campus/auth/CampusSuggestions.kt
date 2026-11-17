package com.findit.campus.auth

/** Bundled suggestions are conveniences; students can enter any campus. */
fun campusSuggestions(query: String, names: List<String>, limit: Int = 8): List<String> {
    val clue = query.trim()
    return names.distinct().filter { it.contains(clue, ignoreCase = true) }
        .sortedWith(compareBy<String> { !it.startsWith(clue, ignoreCase = true) }.thenBy { it })
        .take(limit.coerceAtLeast(0))
}
