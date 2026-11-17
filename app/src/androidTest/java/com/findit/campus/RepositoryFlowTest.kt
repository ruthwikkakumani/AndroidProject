package com.findit.campus

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.findit.campus.ai.InsightEngine
import com.findit.campus.ai.PhotoCheckEngine
import com.findit.campus.ai.SmartMatchEngine
import com.findit.campus.ai.TextScanner
import com.findit.campus.data.AccountRepository
import com.findit.campus.data.AuthResult
import com.findit.campus.data.ChatRepository
import com.findit.campus.data.ItemRepository
import com.findit.campus.data.PhotoStore
import com.findit.campus.data.cloud.CloudServices
import com.findit.campus.data.cloud.CloudSync
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.PendingWrite
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.ui.post.PostForm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryFlowTest {
    private lateinit var db: FindItDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var items: ItemRepository
    private lateinit var chat: ChatRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FindItDatabase::class.java).build()
        val cloud = CloudServices(context, enabled = false)
        val sync = CloudSync(cloud, db)
        accounts = AccountRepository(db, cloud)
        items = ItemRepository(db, SmartMatchEngine(), InsightEngine(), accounts, sync)
        chat = ChatRepository(db, items, PhotoStore(context, cloud), PhotoCheckEngine(TextScanner()), sync)
    }
    @After fun tearDown() { db.close() }

    @Test fun googleRequiresBackendAndCannotCreateAnUnverifiedLocalSession() = runBlocking {
        assertTrue(accounts.signInGoogle("invalid-test-token") is com.findit.campus.data.GoogleAuthResult.Rejected)
        assertTrue(accounts.completeGoogleProfile("Test Student", "Test Campus") is AuthResult.Rejected)
        assertNull(accounts.observeAccount().first())
    }

    @Test fun loginPersistsAcrossDatabaseReopenUntilExplicitLogout() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "session-regression-${java.util.UUID.randomUUID()}.db"
        val cloud = CloudServices(context, enabled = false)
        var saved = Room.databaseBuilder(context, FindItDatabase::class.java, name).build()
        try {
            var repository = AccountRepository(saved, cloud)
            assertTrue(repository.register("Test student", "session@example.edu", "Secret123!", "Secret123!", "Test Campus") is AuthResult.Created)
            assertNull(repository.observeAccount().first())
            val signedIn = repository.login("session@example.edu", "Secret123!") as AuthResult.SignedIn
            saved.close()
            saved = Room.databaseBuilder(context, FindItDatabase::class.java, name).build()
            repository = AccountRepository(saved, cloud)
            assertEquals(signedIn.account.id, repository.observeAccount().first()?.id)
            assertTrue(repository.login("session@example.edu", "wrong-password") is AuthResult.Rejected)
            assertEquals(signedIn.account.id, repository.observeAccount().first()?.id)
            repository.logout()
            assertNull(repository.observeAccount().first())
        } finally { saved.close(); context.deleteDatabase(name) }
    }

    @Test fun connectingFirebasePreservesLocalAccountLoginWithoutConvertingItsIdentity() = runBlocking {
        assertTrue(accounts.register("Local student", "local@example.edu", "Secret123!", "Secret123!", "Test Campus") is AuthResult.Created)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val connectedCloud = CloudServices(context)
        org.junit.Assume.assumeTrue(connectedCloud.configured)
        val connectedAccounts = AccountRepository(db, connectedCloud)
        assertTrue(connectedAccounts.login("local@example.edu", "wrong-password") is AuthResult.Rejected)
        assertNull(connectedAccounts.observeAccount().first())
        val result = connectedAccounts.login("local@example.edu", "Secret123!") as AuthResult.SignedIn
        assertNull(result.account.remoteUid)
        assertEquals(result.account.id, connectedAccounts.observeAccount().first()?.id)
    }

    @Test fun accountCreationRequiresLoginAndCampusBoardIsPrivate() = runBlocking {
        val owner = student("owner")
        val lostId = items.add(report(ItemType.LOST), owner)
        val finder = student("finder")
        assertEquals(lostId, items.observeAll().first().single().id)
        assertTrue(accounts.register("Other student", "other@example.edu", "Secret123!", "Secret123!", "Other College") is AuthResult.Created)
        accounts.login("other@example.edu", "Secret123!")
        val other = accounts.observeAccount().first()!!.id
        assertTrue(items.observeAll().first().isEmpty())
        assertNull(items.observeItem(lostId).first())
        rejects { chat.open(lostId, other) }
        rejects { items.delete(lostId) }
        assertNotNull(db.itemDao().getById(lostId))
        assertNotEquals(owner, finder)
    }

    @Test fun foundActionNotifiesReporterAndOpensAnExactPrivateConversation() = runBlocking {
        val owner = student("owner")
        val itemId = items.add(report(ItemType.LOST), owner)
        val guest = student("guest")
        val thread = chat.open(itemId, guest)!!
        chat.send(thread, guest, "I found this by the library desk.")
        assertEquals(thread, chat.open(itemId, guest))
        val secondGuest = student("second")
        val secondThread = chat.open(itemId, secondGuest)!!
        assertNotEquals(thread, secondThread)
        rejects { chat.send(thread, secondGuest, "Access someone else's messages") }
        login("owner")
        assertNull(chat.open(itemId, owner))
        val conversations = chat.observeConversations(owner).first()
        assertEquals(setOf(thread, secondThread), conversations.map { it.id }.toSet())
        assertTrue(conversations.any { it.personName == "guest" && it.preview?.contains("library") == true })
        assertTrue(accounts.observeNotifications(owner).first().any { it.threadId == thread && !it.read })
        db.accountDao().markThreadRead(owner, thread)
        assertEquals(0, chat.observeConversations(owner).first().first { it.id == thread }.unread)
    }

    @Test fun bothSidesMustShareEvidenceVerifyAndConfirmPhysicalReturn() = runBlocking {
        val owner = student("owner")
        val itemId = items.add(report(ItemType.FOUND), owner)
        val guest = student("guest")
        val thread = chat.open(itemId, guest)!!
        rejects { items.markReunited(itemId) }
        rejects { chat.confirmReturn(thread, guest) }
        rejects { chat.confirmSide(thread, guest) }
        chat.send(thread, guest, "Ownership details: blue bottle, mountain sticker, library.")
        chat.confirmSide(thread, guest)
        assertTrue(db.chatDao().getThread(thread)!!.guestChecked)
        assertFalse(db.chatDao().getThread(thread)!!.ownerChecked)
        rejects { chat.confirmReturn(thread, guest) }
        login("owner")
        rejects { chat.confirmSide(thread, owner) }
        chat.send(thread, owner, "Ownership details: I checked the sticker and the cap against your photo.")
        chat.confirmSide(thread, owner)
        chat.confirmReturn(thread, owner)
        assertEquals(ItemStatus.OPEN, db.itemDao().getById(itemId)!!.status)
        login("guest")
        chat.confirmReturn(thread, guest)
        assertEquals(ItemStatus.REUNITED, db.itemDao().getById(itemId)!!.status)
        assertEquals("RETURNED", db.chatDao().getThread(thread)!!.status)
    }

    @Test fun syncQueueCoalescesWithoutAcknowledgingANewerEdit() = runBlocking {
        val first = PendingWrite("items/1", "student", "items", "1", "first", 1)
        val updated = first.copy(payload = "updated", createdAt = 2)
        db.syncDao().enqueue(first)
        db.syncDao().enqueue(updated)
        db.syncDao().acknowledge(first.key, first.payload)
        assertEquals(updated, db.syncDao().pending("student").single())
        assertTrue(db.syncDao().pending("different-student").isEmpty())
        db.syncDao().acknowledge(updated.key, updated.payload)
        assertTrue(db.syncDao().pending("student").isEmpty())
    }

    private suspend fun student(name: String): Long {
        accounts.logout()
        assertTrue(accounts.register(name, "$name@example.edu", "Secret123!", "Secret123!", "City College") is AuthResult.Created)
        assertNull(accounts.observeAccount().first())
        return login(name)
    }
    private suspend fun login(name: String): Long {
        val result = accounts.login("$name@example.edu", "Secret123!") as AuthResult.SignedIn
        return result.account.id
    }
    private fun report(type: ItemType) = PostForm(type = type, title = "Navy bottle",
        description = "A navy bottle with a distinctive sticker", category = "Bottles", zoneName = "Library")
    private suspend fun rejects(block: suspend () -> Unit) {
        try { block(); fail("This action should have been rejected") }
        catch (_: IllegalArgumentException) { }
    }
}
