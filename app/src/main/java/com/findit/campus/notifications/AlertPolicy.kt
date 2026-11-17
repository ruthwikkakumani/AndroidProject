package com.findit.campus.notifications

import com.findit.campus.data.local.NotificationEntity

/** Account boundaries matter even when two students share one demo device. */
class AlertPolicy {
    private var accountId: Long? = null
    private var seen = emptySet<Long>()

    fun newUnread(account: Long?, rows: List<NotificationEntity>): List<NotificationEntity> {
        if (accountId != account) {
            accountId = account
            seen = emptySet()
        }
        val own = rows.filter { it.accountId == account }
        val fresh = own.filter { !it.read && it.id !in seen }.sortedByDescending { it.createdAt }
        seen = seen + own.map { it.id }
        return fresh
    }
}

data class AlertDestination(val accountId: Long, val threadId: Long?, val itemId: Long?)
