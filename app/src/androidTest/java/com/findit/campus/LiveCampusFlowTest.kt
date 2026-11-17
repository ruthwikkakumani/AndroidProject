package com.findit.campus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.findit.campus.ai.*
import com.findit.campus.data.*
import com.findit.campus.data.cloud.*
import com.findit.campus.data.local.*
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.ui.post.PostForm
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/** Explicit opt-in: two independent Firebase auth instances and Room databases, never the student's session. */
class LiveCampusFlowTest {
    @Test fun reportPrivateMessagesAndReturnReachAnotherDatabase() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("findit.liveCloud") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val run = UUID.randomUUID().toString()
        val campus = "QA Campus $run"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val databases = mutableListOf<FindItDatabase>()
        val clouds = mutableListOf<CloudServices>()
        val paths = mutableListOf<String>()
        var migratedItemId: Long? = null
        val manifest = File(context.filesDir, "live-campus-cleanup.json")
        fun saveManifest() { manifest.writeText(JSONObject().put("documents", JSONArray(paths)).toString()) }
        suspend fun student(index: Int): Triple<AccountRepository, ItemRepository, ChatRepository> {
            val db = Room.inMemoryDatabaseBuilder(context, FindItDatabase::class.java).build().also { databases += it }
            val cloud = CloudServices(context, appName = "qa-$run-$index").also { clouds += it }
            assertTrue("Backend must be configured", cloud.configured)
            val sync = CloudSync(cloud, db)
            val accounts = AccountRepository(db, cloud)
            val items = ItemRepository(db, SmartMatchEngine(), InsightEngine(), accounts, sync)
            val chat = ChatRepository(db, items, PhotoStore(context, cloud), PhotoCheckEngine(TextScanner()), sync)
            val email = "findit-sdk-qa-$index-$run@example.test"
            val password = UUID.randomUUID().toString() + "Aa1!"
            val account = if (index == 0) {
                val offline = CloudServices(context, enabled = false)
                val offlineAccounts = AccountRepository(db, offline)
                assertTrue(offlineAccounts.register("QA Student $index", email, password, password, campus) is AuthResult.Created)
                val local = offlineAccounts.login(email, password) as AuthResult.SignedIn
                val offlineItems = ItemRepository(db, SmartMatchEngine(), InsightEngine(), offlineAccounts, CloudSync(offline, db))
                migratedItemId = offlineItems.add(PostForm(type = ItemType.LOST, title = "QA Navy Bottle",
                    description = "Blue bottle with a mountain sticker", category = "Bottles", zoneName = "Library"), local.account.id)
                paths += "items/$migratedItemId"; saveManifest()
                val connected = withTimeout(40_000) { accounts.connectCurrentAccount(password) }
                assertTrue("Connect the older local account: $connected", connected is AuthResult.SignedIn)
                (connected as AuthResult.SignedIn).also {
                    assertEquals(it.account.id, db.itemDao().getById(migratedItemId!!)!!.ownerId)
                    assertEquals(it.account.remoteUid, db.itemDao().getById(migratedItemId!!)!!.ownerUid)
                }
            } else {
                assertTrue("Register cloud student", withTimeout(40_000) {
                    accounts.register("QA Student $index", email, password, password, campus)
                } is AuthResult.Created)
                withTimeout(30_000) { accounts.login(email, password) } as AuthResult.SignedIn
            }
            paths += "profiles/${account.account.remoteUid}"; saveManifest()
            sync.start(scope, accounts)
            return Triple(accounts, items, chat)
        }
        try {
            val (owner, ownerItems, ownerChat) = student(0)
            val (guest, guestItems, guestChat) = student(1)
            val ownerId = owner.observeAccount().first()!!.id
            val guestId = guest.observeAccount().first()!!.id
            val itemId = migratedItemId!!
            paths += "items/$itemId"; saveManifest()
            withTimeout(45_000) { guestItems.observeAll().first { it.any { item -> item.id == itemId } } }
            assertEquals(ownerId, guestItems.observeItem(itemId).first()!!.ownerId)
            val threadId = guestChat.open(itemId, guestId)!!
            paths += "threads/$threadId"; saveManifest()
            guestChat.send(threadId, guestId, "I found your bottle near the library.")
            paths += databases[1].chatDao().observeMessages(threadId).first().map { "messages/${it.id}" }; saveManifest()
            withTimeout(45_000) { ownerChat.observeConversations(ownerId).first { it.any { t -> t.id == threadId } } }
            withTimeout(45_000) { ownerChat.observeMessages(threadId).first { it.any { m -> m.body.contains("found your bottle") } } }
            ownerChat.send(threadId, ownerId, "Thank you. Let's verify the mountain sticker.")
            withTimeout(45_000) { guestChat.observeMessages(threadId).first { it.any { m -> m.body.startsWith("Thank you") } } }
            val pin = com.findit.campus.location.MeetingPin(13.0827, 80.2703, 5f, System.currentTimeMillis())
            guestChat.send(threadId, guestId, pin.encode())
            withTimeout(45_000) { ownerChat.observeMessages(threadId).first { rows ->
                rows.any { com.findit.campus.location.MeetingPin.decode(it.body) == pin }
            } }
            guestChat.send(threadId, guestId, "Ownership details: navy, mountain sticker, library")
            guestChat.confirmSide(threadId, guestId)
            withTimeout(45_000) { ownerChat.observeThread(threadId).first { it?.guestChecked == true } }
            ownerChat.send(threadId, ownerId, "Ownership details: I checked the sticker and cap")
            ownerChat.confirmSide(threadId, ownerId)
            withTimeout(45_000) { guestChat.observeThread(threadId).first { it?.ownerChecked == true } }
            ownerChat.confirmReturn(threadId, ownerId)
            withTimeout(45_000) { guestChat.observeThread(threadId).first { it?.ownerReturned == true } }
            guestChat.confirmReturn(threadId, guestId)
            withTimeout(45_000) { ownerItems.observeItem(itemId).first { it?.status == ItemStatus.REUNITED } }
            withTimeout(45_000) { guestItems.observeItem(itemId).first { it?.status == ItemStatus.REUNITED } }
            for (db in databases) {
                paths += db.chatDao().observeMessages(threadId).first().map { "messages/${it.id}" }
                saveManifest()
                withTimeout(45_000) { db.syncDao().observePendingCount().first { it == 0 } }
            }
        } finally {
            scope.cancel()
            for (db in databases) {
                // Record every locally authored message even when a preceding assertion fails.
                db.accountDao().observeSession().first()?.accountId?.let { id ->
                    for (t in db.chatDao().observeThreads(id).first()) {
                        paths += "threads/${t.id}"
                        paths += db.chatDao().observeMessages(t.id).first().map { "messages/${it.id}" }
                    }
                }
            }
            saveManifest()
            for (cloud in clouds) withTimeoutOrNull(10_000) { cloud.auth?.currentUser?.delete()?.awaitResult() }
            databases.forEach { it.close() }
        }
    }
}
