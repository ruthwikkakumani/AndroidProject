package com.findit.campus.data

import android.graphics.BitmapFactory
import com.findit.campus.ai.ImageVerdict
import com.findit.campus.ai.compareBitmaps
import com.findit.campus.data.local.ChatDao
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.MessageEntity
import com.findit.campus.data.local.ThreadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ChatRepository(
    private val database: FindItDatabase,
    private val items: ItemRepository,
) {
    private val chat: ChatDao = database.chatDao()

    fun observeThreads(accountId: Long): Flow<List<ThreadEntity>> = chat.observeThreads(accountId)

    fun observeMessages(threadId: Long): Flow<List<MessageEntity>> = chat.observeMessages(threadId)

    fun observeThread(threadId: Long): Flow<ThreadEntity?> = chat.observeThread(threadId)

    suspend fun open(itemId: Long, accountId: Long): Long? {
        val item = items.observeItem(itemId).first() ?: return null
        if (accountId == item.ownerId) {
            return chat.observeThreads(accountId).first().firstOrNull { it.itemId == itemId }?.id
        }
        val existing = chat.findThread(itemId, accountId)
        if (existing != null) return existing.id
        val id = chat.insertThread(
            ThreadEntity(
                itemId = itemId,
                ownerId = item.ownerId,
                guestId = accountId,
                status = "OPEN",
                createdAt = System.currentTimeMillis(),
            ),
        )
        if (item.ownerId > 0) {
            database.accountDao().insertNotification(
                com.findit.campus.data.local.NotificationEntity(
                    accountId = item.ownerId,
                    title = "Someone can help with ${item.title}",
                    body = "Open Messages to check the item together before it is returned.",
                    itemId = itemId,
                    read = false,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
        return id
    }

    suspend fun send(threadId: Long, senderId: Long, body: String) {
        val text = body.trim()
        if (text.isEmpty()) return
        chat.insertMessage(
            MessageEntity(
                threadId = threadId,
                senderId = senderId,
                body = text,
                photoPath = null,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun checkPhoto(threadId: Long, senderId: Long, proofPath: String): ImageVerdict {
        val thread = chat.observeThread(threadId).first()
            ?: return ImageVerdict(false, 0, "This conversation is no longer available.")
        val item = items.observeItem(thread.itemId).first()
        val referencePath = item?.photoPath
        val verdict = if (referencePath.isNullOrBlank()) {
            ImageVerdict(false, 0, "This report has no photo yet, so the pictures cannot be compared.")
        } else {
            withContext(Dispatchers.Default) {
                val reference = BitmapFactory.decodeFile(referencePath)
                val proof = BitmapFactory.decodeFile(proofPath)
                if (reference == null || proof == null) {
                    ImageVerdict(false, 0, "One of the photos could not be read.")
                } else {
                    val result = compareBitmaps(reference, proof)
                    reference.recycle()
                    proof.recycle()
                    result
                }
            }
        }
        chat.insertMessage(
            MessageEntity(
                threadId = threadId,
                senderId = senderId,
                body = "Photo check: ${verdict.score}/100. ${verdict.explanation}",
                photoPath = proofPath,
                createdAt = System.currentTimeMillis(),
            ),
        )
        chat.updateStatus(threadId, if (verdict.sameItem) "CHECKED" else "NEEDS_QUESTIONS")
        if (verdict.sameItem) {
            if (senderId == thread.ownerId) chat.setOwnerChecked(threadId, true) else chat.setGuestChecked(threadId, true)
            finishIfBothChecked(threadId)
        }
        return verdict
    }

    suspend fun confirmSide(threadId: Long, accountId: Long) {
        val thread = chat.getThread(threadId) ?: return
        if (accountId == thread.ownerId) chat.setOwnerChecked(threadId, true)
        else if (accountId == thread.guestId) chat.setGuestChecked(threadId, true)
        finishIfBothChecked(threadId)
        chat.insertMessage(
            MessageEntity(
                threadId = threadId,
                senderId = accountId,
                body = "I checked the answers and confirm my side.",
                photoPath = null,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun finishIfBothChecked(threadId: Long) {
        val thread = chat.getThread(threadId) ?: return
        if (thread.ownerChecked && thread.guestChecked) {
            items.markReunited(thread.itemId)
            chat.updateStatus(threadId, "RETURNED")
        }
    }

    suspend fun confirmReturn(threadId: Long, ownerId: Long) {
        confirmSide(threadId, ownerId)
    }
}
