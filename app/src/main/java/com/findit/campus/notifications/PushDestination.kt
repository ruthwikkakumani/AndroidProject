package com.findit.campus.notifications

/** Ignore malformed, stale-account and out-of-range push destinations. */
fun pushDestination(data: Map<String, String>, activeUid: String?, accountId: Long): AlertDestination? {
    if (activeUid.isNullOrBlank() || data["recipientUid"] != activeUid || accountId <= 0) return null
    fun id(key: String) = data[key]?.toLongOrNull()?.takeIf { it in 1..0x1FFFFFFFFFFFFFL }
    val thread = id("threadId") ?: return null
    return AlertDestination(accountId, thread, id("itemId"))
}
