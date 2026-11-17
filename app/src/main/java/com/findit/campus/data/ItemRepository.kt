package com.findit.campus.data

import androidx.room.withTransaction
import com.findit.campus.ai.InsightEngine
import com.findit.campus.ai.SmartMatchEngine
import com.findit.campus.data.local.CategoryEntity
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.ItemEntity
import com.findit.campus.auth.secretMatches
import com.findit.campus.data.local.ItemWithCategory
import com.findit.campus.data.local.NotificationEntity
import com.findit.campus.data.cloud.CloudSync
import com.findit.campus.data.cloud.campusKey
import com.findit.campus.data.cloud.newId
import com.findit.campus.model.Item
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.model.ScoredMatch
import com.findit.campus.model.toMatchable
import com.findit.campus.ui.post.PostForm
import com.findit.campus.ui.post.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

class ItemRepository(
    private val database: FindItDatabase,
    private val matcher: SmartMatchEngine,
    private val insights: InsightEngine,
    private val accountRepository: AccountRepository,
    private val sync: CloudSync,
) {
    private val items = database.itemDao()
    private val categories = database.categoryDao()
    private val accounts = database.accountDao()

    fun observeAll(): Flow<List<Item>> = scoped(items.observeAll())

    fun observeMine(ownerId: Long): Flow<List<Item>> =
        items.observeMine(ownerId).map { rows -> rows.map { it.toItem() } }

    fun observeItem(id: Long): Flow<Item?> = combine(items.observeById(id), accountRepository.observeAccount()) { row, account ->
        row?.takeIf { account != null && it.item.campusKey == campusKey(account.campusName) }?.toItem()
    }

    fun observeSearch(query: String): Flow<List<Item>> =
        scoped(items.observeSearch(escapeLike(query)))

    private fun scoped(source: Flow<List<ItemWithCategory>>): Flow<List<Item>> =
        combine(source, accountRepository.observeAccount()) { rows, account ->
            if (account == null) emptyList() else rows.filter {
                it.item.campusKey == campusKey(account.campusName)
            }.map { it.toItem() }
        }

    fun observeCategoryNames(): Flow<List<String>> = categories.observeNames()

    suspend fun ensureCategories() {
        if (categories.count() > 0) return
        CampusCatalog.categories.forEach { category ->
            categories.insert(CategoryEntity(name = category.name, symbol = category.symbol))
        }
    }

    suspend fun add(form: PostForm, ownerId: Long): Long = database.withTransaction {
        val account = accounts.getById(ownerId) ?: error("Sign in before posting.")
        val categoryId = categories.findByName(form.category)?.id
            ?: categories.insert(CategoryEntity(name = form.category, symbol = "other"))
        val id = newId()
        items.insert(form.toEntity(categoryId, ownerId).copy(id = id,
            campusKey = campusKey(account.campusName), ownerUid = account.remoteUid.orEmpty()))
        sync.queueItem(id)
        FindItLog.i("Posted ${form.type.name} #$id at ${form.zoneName}")
        notifyMatches(id, ownerId)
        id
    }

    suspend fun claim(itemId: Long, claimantId: Long, answer: String): String {
        val record = items.observeById(itemId).map { it?.toItem() }
        val item = record.first() ?: return "That report is no longer available."
        if (item.ownerId == claimantId) return "You posted this report."
        val secret = items.verificationSecret(itemId)
        if (!secret.isNullOrBlank()) {
            if (!secretMatches(answer, secret)) {
                return "That detail does not match the private check."
            }
            notify(
                accountId = item.ownerId,
                title = "Private check matched",
                body = "A claim matched the private detail on ${item.title}.",
                itemId = itemId,
            )
            return "The private detail matched. Check the item together in Messages before confirming handover."
        }
        notify(
            accountId = item.ownerId,
            title = "Claim request",
            body = "Someone asked to collect ${item.title}.",
            itemId = itemId,
        )
        return "The person who posted it has been asked to review your request."
    }

    private suspend fun notifyMatches(itemId: Long, ownerId: Long) {
        val key = items.getById(itemId)?.campusKey ?: return
        val all = items.listAll().filter { it.item.campusKey == key }.map { it.toItem() }
        val posted = all.firstOrNull { it.id == itemId } ?: return
        val matches = matchesFor(posted, all)
        matches.forEach { match ->
            val other = all.firstOrNull { it.id == match.itemId } ?: return@forEach
            if (other.ownerId > 0) {
                notify(
                    accountId = other.ownerId,
                    title = "Possible match",
                    body = "${posted.title} may match ${other.title}.",
                    itemId = other.id,
                )
            }
        }
        if (ownerId > 0 && matches.isNotEmpty()) {
            notify(
                accountId = ownerId,
                title = "Possible match",
                body = "${posted.title} has ${matches.size} possible match${if (matches.size == 1) "" else "es"}.",
                itemId = itemId,
            )
        }
    }

    private suspend fun notify(accountId: Long, title: String, body: String, itemId: Long) {
        if (accountId <= 0) return
        accounts.insertNotification(
            NotificationEntity(
                accountId = accountId,
                title = title,
                body = body,
                itemId = itemId,
                read = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markReunited(id: Long, returnThreadId: Long? = null) {
        val account = accountRepository.observeAccount().first() ?: error("Sign in to confirm handover.")
        val thread = returnThreadId?.let { database.chatDao().getThread(it) }
        require(thread != null && thread.itemId == id &&
            (account.id == thread.ownerId || account.id == thread.guestId) &&
            thread.ownerChecked && thread.guestChecked && thread.ownerReturned && thread.guestReturned) {
            "Both students must verify ownership and confirm the handover in Messages."
        }
        items.updateStatus(id, ItemStatus.REUNITED, System.currentTimeMillis())
        sync.queueItem(id, returnThreadId = returnThreadId)
        FindItLog.i("Reunited #$id")
    }

    suspend fun delete(id: Long) {
        val account = accountRepository.observeAccount().first() ?: return
        val item = items.getById(id) ?: return
        require(item.ownerId == account.id) { "Only the report owner can remove it." }
        sync.queueItem(id, deleted = true)
        items.deleteById(id)
        FindItLog.i("Removed #$id")
    }

    fun matchesFor(item: Item, all: List<Item>): List<ScoredMatch> =
        matcher.rank(item.toMatchable(), all.map { it.toMatchable() })

    fun textHits(query: String, all: List<Item>): List<ScoredMatch> =
        matcher.rankByWords(query, all.map { it.toMatchable() })

    fun likelyPlaces(all: List<Item>, category: String, type: ItemType): List<String> =
        insights.likelyPlaces(all, category, type)
}

internal fun escapeLike(raw: String): String =
    raw.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

fun ItemWithCategory.toItem(): Item = Item(
    id = item.id,
    title = item.title,
    description = item.description,
    category = categoryName,
    categoryId = item.categoryId,
    type = item.type,
    status = item.status,
    latitude = item.latitude,
    longitude = item.longitude,
    placeLabel = item.placeLabel,
    zoneName = item.zoneName,
    photoPath = item.photoPath,
    scannedText = item.scannedText,
    dominantColor = item.dominantColor,
    contact = item.contact,
    createdAt = item.createdAt,
    resolvedAt = item.resolvedAt,
    isSample = item.isSample,
    ownerId = item.ownerId,
    hasCoordinates = item.hasCoordinates,
    hasPrivateCheck = !item.verificationSecret.isNullOrBlank(),
)
