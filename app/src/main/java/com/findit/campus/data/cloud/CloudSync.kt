package com.findit.campus.data.cloud

import android.net.Uri
import androidx.room.withTransaction
import com.findit.campus.data.AccountRepository
import com.findit.campus.data.FindItLog
import com.findit.campus.data.local.AccountEntity
import com.findit.campus.data.local.CategoryEntity
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.ItemEntity
import com.findit.campus.data.local.MessageEntity
import com.findit.campus.data.local.PendingWrite
import com.findit.campus.data.local.ThreadEntity
import com.findit.campus.data.local.NotificationEntity
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class SyncState(val configured: Boolean = false, val connected: Boolean = false, val error: String? = null)

class CloudSync(private val cloud: CloudServices, private val db: FindItDatabase) {
    private val state = MutableStateFlow(SyncState(configured = cloud.configured))
    val status: StateFlow<SyncState> = state
    val pendingCount = db.syncDao().observePendingCount()

    fun start(scope: CoroutineScope, accounts: AccountRepository) {
        if (!cloud.configured) return
        scope.launch {
            accounts.observeAccount().collectLatest { account ->
                val uid = account?.remoteUid ?: run {
                    state.value = SyncState(configured = true)
                    return@collectLatest
                }
                if (cloud.auth?.currentUser?.uid != uid) {
                    state.value = SyncState(true, false, "Sign in online to reconnect campus services.")
                    return@collectLatest
                }
                coroutineScope {
                val store = cloud.store!!
                val key = campusKey(account.campusName)
                launch { watch(store.collection("profiles").whereEqualTo("campusKey", key)) { importProfile(it, uid) } }
                launch { watch(store.collection("items").whereEqualTo("campusKey", key)) { importItem(it) } }
                launch {
                    watch(store.collection("threads").whereArrayContains("participantUids", uid)) { importThread(it) }
                }
                launch {
                    watch(store.collection("messages").whereArrayContains("participantUids", uid)) { importMessage(it) }
                }
                db.syncDao().observePending(uid).collect {
                    // Room may buffer an older snapshot while an upload is in progress.
                    var retry = db.syncDao().pending(uid)
                    while (retry.isNotEmpty()) {
                    for (write in retry.sortedBy {
                        when (it.collection) {
                            // New reports precede new conversations; handovers follow both confirmations.
                            "items" -> if (JSONObject(it.payload).has("returnThreadId")) 3 else 0
                            "threads" -> 1
                            else -> 2
                        }
                    }) {
                        try {
                            withTimeout(30_000) { upload(write) }
                            db.syncDao().acknowledge(write.key, write.payload)
                            state.value = SyncState(true, true)
                    } catch (timeout: TimeoutCancellationException) {
                        state.value = SyncState(true, false, "Sync timed out. Your changes are saved locally and will retry.")
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) {
                        FindItLog.e("Sync will retry", error)
                        state.value = SyncState(true, false, "Changes are saved on this device. Sync will retry when connected.")
                    }
                    }
                    retry = db.syncDao().pending(uid)
                    if (retry.isNotEmpty()) delay(5_000)
                    }
                }
                }
            }
        }
    }

    private suspend fun watch(query: Query, consume: suspend (Map<String, Any?>) -> Unit) {
        while (true) try {
            callbackFlow {
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    if (!snapshot.metadata.isFromCache) state.value = SyncState(true, true)
                    trySend(snapshot.documents.mapNotNull { it.data })
                }
            }
            awaitClose { listener.remove() }
            }.collect { rows -> rows.forEach { consume(it) } }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            FindItLog.e("Campus listener will reconnect", error)
            state.value = SyncState(true, false, "Campus services are unavailable. Your saved data is still here.")
            delay(10_000)
        }
    }

    suspend fun queueItem(id: Long, deleted: Boolean = false, returnThreadId: Long? = null) {
        val item = db.itemDao().getById(id) ?: return
        val category = db.categoryDao().getById(item.categoryId) ?: return
        val payload = mutableMapOf<String, Any?>(
            "id" to id, "title" to item.title, "description" to item.description, "category" to category.name,
            "type" to item.type.name, "status" to item.status.name, "latitude" to item.latitude,
            "longitude" to item.longitude, "placeLabel" to item.placeLabel, "zoneName" to item.zoneName,
            "photoPath" to item.photoPath, "scannedText" to item.scannedText, "dominantColor" to item.dominantColor,
            "createdAt" to item.createdAt, "resolvedAt" to item.resolvedAt, "ownerId" to item.ownerId,
            "ownerUid" to item.ownerUid, "campusKey" to item.campusKey, "hasCoordinates" to item.hasCoordinates,
            "deleted" to deleted,
        )
        if (returnThreadId != null) payload["returnThreadId"] = returnThreadId
        queue("items", id, payload)
    }

    suspend fun queueThread(id: Long) {
        val t = db.chatDao().getThread(id) ?: return
        queue("threads", id, mapOf(
            "id" to id, "itemId" to t.itemId, "ownerId" to t.ownerId, "guestId" to t.guestId,
            "ownerUid" to t.ownerUid, "guestUid" to t.guestUid, "participantUids" to listOf(t.ownerUid, t.guestUid),
            "status" to t.status, "createdAt" to t.createdAt, "ownerChecked" to t.ownerChecked,
            "guestChecked" to t.guestChecked, "ownerReturned" to t.ownerReturned, "guestReturned" to t.guestReturned,
        ))
    }

    suspend fun queueMessage(m: MessageEntity) {
        val t = db.chatDao().getThread(m.threadId) ?: return
        queue("messages", m.id, mapOf(
            "id" to m.id, "threadId" to m.threadId, "senderId" to m.senderId,
            "senderUid" to if (m.senderId == t.ownerId) t.ownerUid else t.guestUid,
            "body" to m.body, "photoPath" to m.photoPath, "createdAt" to m.createdAt,
            "participantUids" to listOf(t.ownerUid, t.guestUid),
        ))
    }

    private suspend fun queue(collection: String, id: Long, data: Map<String, Any?>) {
        if (!cloud.configured) return
        val session = db.accountDao().observeSession()
        // Read the Room session, so an offline write remains associated with its real author.
        val accountId = session.first()?.accountId ?: return
        val account = db.accountDao().getById(accountId) ?: return
        val uid = account.remoteUid ?: return
        val json = JSONObject()
        data.forEach { (key, value) -> json.put(key, if (value is List<*>) org.json.JSONArray(value) else value ?: JSONObject.NULL) }
        db.syncDao().enqueue(PendingWrite("$collection/$id", uid, collection, id.toString(), json.toString(), System.currentTimeMillis()))
    }

    private suspend fun upload(write: PendingWrite) {
        val json = JSONObject(write.payload)
        val payload = json.keys().asSequence().associateWith { key ->
            when (val value = json.get(key)) {
                JSONObject.NULL -> null
                is org.json.JSONArray -> (0 until value.length()).map { value.getString(it) }
                else -> value
            }
        }.toMutableMap()
        val path = payload["photoPath"] as? String
        if (!path.isNullOrBlank() && !path.startsWith("gs://")) {
            val file = File(path)
            check(file.exists()) { "The photo is missing; keep the pending report for review." }
            val objectPath = if (write.collection == "messages") "threads/${payload["threadId"]}/${write.accountUid}/${write.documentId}.jpg"
                else "items/${write.accountUid}/${write.documentId}.jpg"
            val reference = cloud.storage!!.reference.child(objectPath)
            reference.putFile(Uri.fromFile(file)).awaitResult()
            payload["photoPath"] = reference.toString()
        }
        val document = cloud.store!!.collection(write.collection).document(write.documentId)
        if (write.collection == "threads") {
            // Merge only the author's flags. Concurrent confirmations cannot overwrite the other person's side.
            val mineIsOwner = payload["ownerUid"] == write.accountUid
            val existing = document.get().awaitResult()
            if (existing.exists()) {
                val keys = if (mineIsOwner) listOf("ownerChecked", "ownerReturned") else listOf("guestChecked", "guestReturned")
                document.set(payload.filterKeys { it in keys }, SetOptions.merge()).awaitResult()
            } else document.set(payload).awaitResult()
        } else if (write.collection == "items" && payload["ownerUid"] != write.accountUid) {
            document.update(mapOf("status" to payload["status"], "resolvedAt" to payload["resolvedAt"],
                "returnThreadId" to payload["returnThreadId"])).awaitResult()
        } else document.set(payload).awaitResult()
    }

    private suspend fun importProfile(d: Map<String, Any?>, viewerUid: String) {
        val uid = d["uid"] as? String ?: return
        if (uid == viewerUid) return
        val id = stableId(uid)
        if (db.accountDao().getById(id)?.passwordHash?.isNotBlank() == true) return
        db.accountDao().upsert(AccountEntity(id, d.string("displayName"), "$uid@findit.local", "", "",
            d.string("campusName"), 0L, uid))
    }

    private suspend fun importItem(d: Map<String, Any?>) {
        val id = d.number("id")
        if (db.syncDao().hasPending("items/$id") > 0) return
        if (d["deleted"] == true) { db.itemDao().deleteById(id); return }
        val category = d.string("category")
        db.withTransaction {
            val categoryId = db.categoryDao().findByName(category)?.id
                ?: db.categoryDao().insert(CategoryEntity(name = category, symbol = "other"))
            val old = db.itemDao().getById(id)
            db.itemDao().upsert(ItemEntity(
                id, d.string("title"), d.string("description"), categoryId,
                ItemType.valueOf(d.string("type")), ItemStatus.valueOf(d.string("status")),
                (d["latitude"] as? Number)?.toDouble() ?: 0.0, (d["longitude"] as? Number)?.toDouble() ?: 0.0,
                d.string("placeLabel"), d.string("zoneName"), localPhoto(old?.photoPath, d["photoPath"] as? String),
                d.string("scannedText"), d["dominantColor"] as? String, "", d.number("createdAt"),
                (d["resolvedAt"] as? Number)?.toLong(), false, d.number("ownerId"), d["hasCoordinates"] == true,
                old?.verificationSecret, d.string("campusKey"), d.string("ownerUid"),
            ))
        }
    }

    private suspend fun importThread(d: Map<String, Any?>) {
        val id = d.number("id")
        val old = db.chatDao().getThread(id)
        val pending = db.syncDao().hasPending("threads/$id") > 0
        val viewerUid = cloud.auth?.currentUser?.uid
        val t = ThreadEntity(id, d.number("itemId"), d.number("ownerId"), d.number("guestId"), d.string("status"),
            d.number("createdAt"),
            if (pending && viewerUid == d["ownerUid"]) old?.ownerChecked == true else d["ownerChecked"] == true,
            if (pending && viewerUid == d["guestUid"]) old?.guestChecked == true else d["guestChecked"] == true,
            if (pending && viewerUid == d["ownerUid"]) old?.ownerReturned == true else d["ownerReturned"] == true,
            if (pending && viewerUid == d["guestUid"]) old?.guestReturned == true else d["guestReturned"] == true,
            d.string("ownerUid"), d.string("guestUid"))
        db.chatDao().upsertThread(t)
        if (old == null && viewerUid == t.ownerUid) {
            notify(t.ownerId, t.itemId, id, "Someone can help with your report", "Open this conversation to check the item together.")
        }
        if (t.ownerChecked && t.guestChecked && t.ownerReturned && t.guestReturned &&
            db.itemDao().getById(t.itemId)?.status != ItemStatus.REUNITED) {
            db.itemDao().updateStatus(t.itemId, ItemStatus.REUNITED, System.currentTimeMillis())
            db.chatDao().updateStatus(id, "RETURNED")
            queueItem(t.itemId, returnThreadId = id)
        }
    }

    private suspend fun importMessage(d: Map<String, Any?>) {
        val id = d.number("id")
        if (db.syncDao().hasPending("messages/$id") > 0) return
        val isNew = db.chatDao().getMessage(id) == null
        db.chatDao().upsertMessage(MessageEntity(id, d.number("threadId"), d.number("senderId"), d.string("body"),
            d["photoPath"] as? String, d.number("createdAt")))
        if (isNew && d["senderUid"] != cloud.auth?.currentUser?.uid) {
            val t = db.chatDao().getThread(d.number("threadId")) ?: return
            val receiverId = if (d["senderUid"] == t.ownerUid) t.guestId else t.ownerId
            notify(receiverId, t.itemId, t.id, "New message", d.string("body").take(100).ifBlank { "A photo was shared." })
        }
    }

    private suspend fun notify(accountId: Long, itemId: Long, threadId: Long, title: String, body: String) {
        db.accountDao().insertNotification(NotificationEntity(accountId = accountId, itemId = itemId,
            threadId = threadId, title = title, body = body, read = false, createdAt = System.currentTimeMillis()))
    }

    private fun localPhoto(old: String?, remote: String?): String? =
        old?.takeIf { !it.startsWith("gs://") && File(it).exists() } ?: remote
}

private fun Map<String, Any?>.number(key: String): Long = (get(key) as? Number)?.toLong() ?: 0L
private fun Map<String, Any?>.string(key: String): String = get(key) as? String ?: ""
