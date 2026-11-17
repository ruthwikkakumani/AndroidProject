package com.findit.campus.data

import androidx.room.withTransaction
import com.findit.campus.ai.ImageVerdict
import com.findit.campus.ai.PhotoCheckEngine
import com.findit.campus.data.cloud.CloudSync
import com.findit.campus.data.cloud.newId
import com.findit.campus.data.cloud.stableId
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.MessageEntity
import com.findit.campus.data.local.NotificationEntity
import com.findit.campus.data.local.ThreadEntity
import com.findit.campus.data.local.ConversationSummary
import com.findit.campus.model.ItemStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(
    private val database: FindItDatabase,
    private val items: ItemRepository,
    private val photos: PhotoStore,
    private val checker: PhotoCheckEngine,
    private val sync: CloudSync,
) {
    private val chat = database.chatDao()
    fun observeThreads(accountId: Long): Flow<List<ThreadEntity>> = chat.observeThreads(accountId)
    fun observeConversations(accountId: Long): Flow<List<ConversationSummary>> = chat.observeConversations(accountId)
    fun observeMessages(threadId: Long): Flow<List<MessageEntity>> = chat.observeMessages(threadId)
    fun observeThread(threadId: Long): Flow<ThreadEntity?> = chat.observeThread(threadId)

    suspend fun open(itemId: Long, accountId: Long): Long? = database.withTransaction {
        val item = database.itemDao().getById(itemId) ?: return@withTransaction null
        val account = database.accountDao().getById(accountId) ?: return@withTransaction null
        require(item.campusKey == com.findit.campus.data.cloud.campusKey(account.campusName)) { "This report belongs to another campus." }
        // Owners choose an exact conversation from the inbox; never pick an arbitrary claimant.
        if (accountId == item.ownerId) return@withTransaction null
        require(item.status == ItemStatus.OPEN) { "This item has already been returned." }
        chat.findThread(itemId, accountId)?.let { return@withTransaction it.id }
        val id = stableId("thread:$itemId:$accountId")
        chat.insertThread(ThreadEntity(id, itemId, item.ownerId, accountId, "OPEN", System.currentTimeMillis(),
            ownerUid = item.ownerUid, guestUid = account.remoteUid.orEmpty()))
        sync.queueThread(id)
        notify(item.ownerId, itemId, id, "Someone can help with ${item.title}", "Open Messages to check the item together before arranging a return.")
        id
    }

    private suspend fun participant(threadId: Long, accountId: Long): ThreadEntity {
        val thread = chat.getThread(threadId) ?: error("This conversation is unavailable.")
        require(accountId == thread.ownerId || accountId == thread.guestId) { "This is a private conversation." }
        return thread
    }

    suspend fun send(threadId: Long, senderId: Long, body: String, photoPath: String? = null) = database.withTransaction {
        val thread = participant(threadId, senderId)
        val text = body.trim().take(1000)
        require(text.isNotEmpty() || photoPath != null) { "Write a message first." }
        val message = MessageEntity(newId(), threadId, senderId, text, photoPath, System.currentTimeMillis())
        chat.insertMessage(message)
        sync.queueMessage(message)
        notify(if (senderId == thread.ownerId) thread.guestId else thread.ownerId, thread.itemId, threadId,
            "New message", text.take(100).ifBlank { "A proof photo was shared." })
    }

    suspend fun checkPhoto(threadId: Long, senderId: Long, proofPath: String): ImageVerdict {
        val thread = participant(threadId, senderId)
        val item = items.observeItem(thread.itemId).first()
        val referencePath = item?.photoPath
        val verdict = if (referencePath.isNullOrBlank()) {
            ImageVerdict(false, 0, "This report has no reference photo. Ask about private details and check the item together.")
        } else {
            val reference = photos.loadDisplay(referencePath, 640)
            val proof = photos.loadDisplay(proofPath, 640)
            try {
                if (reference == null || proof == null) ImageVerdict(false, 0, "A photo could not be read. Please retry or check private details.")
                else checker.compare(reference, proof)
            } finally { reference?.recycle(); proof?.recycle() }
        }
        send(threadId, senderId, "AI photo evidence: ${verdict.explanation}", proofPath)
        // Neither ML confidence nor a photo upload silently confirms either person's ownership check.
        chat.updateStatus(threadId, if (verdict.sameItem) "REVIEW_EVIDENCE" else "NEEDS_QUESTIONS")
        return verdict
    }

    suspend fun confirmSide(threadId: Long, accountId: Long) = database.withTransaction {
        val thread = participant(threadId, accountId)
        require(thread.status != "RETURNED") { "This handover is already complete." }
        val evidence = chat.observeMessages(threadId).first().any {
            it.senderId == accountId && (it.photoPath != null || it.body.startsWith("Ownership details:"))
        }
        require(evidence) { "Share a proof photo or your ownership details before confirming your check." }
        if (accountId == thread.ownerId) chat.setOwnerChecked(threadId, true) else chat.setGuestChecked(threadId, true)
        sync.queueThread(threadId)
        send(threadId, accountId, "I reviewed the photos and private answers, and confirm my ownership check.")
    }

    suspend fun confirmReturn(threadId: Long, accountId: Long) = database.withTransaction {
        val thread = participant(threadId, accountId)
        require(!(thread.ownerReturned && thread.guestReturned)) { "The handover is already complete." }
        require(thread.ownerChecked && thread.guestChecked) { "Both people must verify the item before handover." }
        if (accountId == thread.ownerId) chat.setOwnerReturned(threadId, true) else chat.setGuestReturned(threadId, true)
        sync.queueThread(threadId)
        send(threadId, accountId, "I confirm the physical handover is complete.")
        val updated = chat.getThread(threadId) ?: return@withTransaction
        if (updated.ownerReturned && updated.guestReturned) {
            items.markReunited(updated.itemId, threadId)
            chat.updateStatus(threadId, "RETURNED")
        }
    }

    private suspend fun notify(accountId: Long, itemId: Long, threadId: Long, title: String, body: String) {
        database.accountDao().insertNotification(NotificationEntity(accountId = accountId, title = title,
            body = body, itemId = itemId, threadId = threadId, read = false, createdAt = System.currentTimeMillis()))
    }
}
