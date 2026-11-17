package com.findit.campus

import com.findit.campus.ai.InsightEngine
import com.findit.campus.ai.SmartMatchEngine
import com.findit.campus.auth.PasswordHasher
import com.findit.campus.auth.secretMatches
import com.findit.campus.auth.sealSecret
import com.findit.campus.auth.validateNewAccount
import com.findit.campus.location.distanceMeters
import com.findit.campus.model.Item
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.model.Matchable
import com.findit.campus.ui.home.FeedFilter
import com.findit.campus.ui.home.applyFeedFilters
import com.findit.campus.ui.home.buildHomeState
import com.findit.campus.ui.post.PostForm
import com.findit.campus.ui.post.validatePost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import com.findit.campus.ai.compareSamples
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindItLogicTest {
    private val engine = SmartMatchEngine()
    private val insights = InsightEngine()
    private val now = 1_700_000_000_000L

    @Test
    fun strongPairScoresHighAndExplainsWhy() {
        val lost = matchable(1, ItemType.LOST, "Bottles", "Library", "Navy bottle", "navy bottle mountain sticker", "#112233")
        val found = matchable(2, ItemType.FOUND, "Bottles", "Library", "Blue bottle", "navy bottle mountain sticker", "#112233")
        val score = engine.score(lost, found, now)
        assertTrue(score != null && score.score >= 70)
        assertTrue(score!!.reasons.any { it.contains("category", ignoreCase = true) })
        assertTrue(score.reasons.any { it.contains("spot", ignoreCase = true) })
        assertTrue(score.reasons.any { it.contains("color", ignoreCase = true) })
    }

    @Test
    fun sameTypeIsNotAMatch() {
        val lost = matchable(1, ItemType.LOST, "Bottles", "Library", "Navy bottle", "navy bottle", "#112233")
        val other = matchable(2, ItemType.LOST, "Bottles", "Library", "Navy bottle", "navy bottle", "#112233")
        assertNull(engine.score(lost, other, now))
    }

    @Test
    fun unrelatedReportsStayApart() {
        val hoodie = matchable(
            id = 1,
            type = ItemType.LOST,
            category = "Clothing",
            zone = "Recreation Center",
            title = "Black hoodie",
            description = "sleeve logo cotton",
            color = "#111111",
            latitude = 0.0,
            longitude = 0.0,
            createdAt = now,
        )
        val card = matchable(
            id = 2,
            type = ItemType.FOUND,
            category = "IDs",
            zone = "Admin Building",
            title = "Campus card",
            description = "printer badge photo",
            color = "#EEEEEE",
            latitude = 12.0,
            longitude = 40.0,
            createdAt = now - 40L * 24 * 60 * 60 * 1000,
        )
        assertNull(engine.score(hoodie, card, now))
    }

    @Test
    fun reunitedCandidateIsSkipped() {
        val lost = matchable(1, ItemType.LOST, "Keys", "Campus Bus Stop", "Keys", "red keychain", "#C0392B")
        val found = matchable(2, ItemType.FOUND, "Keys", "Campus Bus Stop", "Keys", "red keychain", "#C0392B")
            .copy(status = ItemStatus.REUNITED)
        assertNull(engine.score(lost, found, now))
    }

    @Test
    fun bestPairsLinksLostToFound() {
        val lost = matchable(4, ItemType.LOST, "Accessories", "Main Quad", "Green umbrella", "wooden handle", "#2E7D32")
        val found = matchable(5, ItemType.FOUND, "Accessories", "Main Quad", "Green umbrella", "wooden handle", "#2E7D32")
        val pairs = engine.bestPairs(listOf(lost, found), now)
        assertEquals(1, pairs.size)
        assertEquals(4L, pairs.first().lostId)
        assertEquals(5L, pairs.first().foundId)
    }

    @Test
    fun scannedNameFindsTheBook() {
        val book = matchable(
            id = 8,
            type = ItemType.FOUND,
            category = "Books",
            zone = "Engineering Hall",
            title = "Math textbook",
            description = "Calculus book with A. Mehta inside",
            color = null,
        )
        val hits = engine.rankByWords("A. Mehta", listOf(book))
        assertTrue(hits.isNotEmpty())
        assertEquals(8L, hits.first().itemId)
    }

    @Test
    fun pulseNamesTheBusiestSpot() {
        val items = listOf(
            item(1, "Library", "Bottles"),
            item(2, "Library", "Bottles"),
            item(3, "Library", "Books"),
            item(4, "Cafeteria", "Keys"),
        )
        val pulse = insights.pulse(items, now)
        assertEquals("Library", pulse.busiestZone)
        assertEquals("Bottles", pulse.topCategory)
        assertTrue(pulse.headline.contains("Library"))
    }

    @Test
    fun likelyPlacesPreferTheOppositeType() {
        val items = listOf(
            item(1, "Library", "Bottles", ItemType.FOUND),
            item(2, "Library", "Bottles", ItemType.FOUND),
            item(3, "Cafeteria", "Bottles", ItemType.LOST),
        )
        val places = insights.likelyPlaces(items, "Bottles", ItemType.LOST)
        assertEquals("Library", places.first())
    }

    @Test
    fun openFilterHidesReunitedReports() {
        val open = item(1, "Library", "Books")
        val done = item(2, "Library", "Books").copy(status = ItemStatus.REUNITED)
        val visible = applyFeedFilters(listOf(open, done), FeedFilter(openOnly = true))
        assertEquals(listOf(1L), visible.map { it.id })
    }

    @Test
    fun homeStateKeepsPulseWhenSearchNarrowsTheList() {
        val bottle = item(1, "Library", "Bottles", title = "Navy bottle")
        val keys = item(2, "Campus Bus Stop", "Keys", title = "Red keys")
        val state = buildHomeState(
            filter = FeedFilter(query = "navy"),
            searched = listOf(bottle),
            all = listOf(bottle, keys),
            matcher = engine,
            insights = insights,
        )
        assertEquals(listOf("Navy bottle"), state.items.map { it.title })
        assertEquals(2, state.pulse.openCount)
    }

    @Test
    fun postValidationRequiresANameDetailsAndPlace() {
        val empty = PostForm()
        assertEquals("Give the item a short name.", validatePost(empty, now))
        val named = empty.copy(title = "Blue bottle")
        assertEquals("Add a few details someone could recognize.", validatePost(named, now))
        val detailed = named.copy(description = "Navy bottle with a mountain sticker")
        assertEquals("Name the place on campus.", validatePost(detailed, now))
        val ready = detailed.copy(zoneName = "Library", whenMillis = now)
        assertNull(validatePost(ready, now))
        val future = ready.copy(whenMillis = now + 5L * 24 * 60 * 60 * 1000)
        assertEquals("The date can't be in the future.", validatePost(future, now))
    }

    @Test
    fun distanceDoesNotDependOnAPresetCampus() {
        assertEquals(0.0, distanceMeters(37.422, -122.084, 37.422, -122.084), 0.01)
        val oneDegree = distanceMeters(0.0, 0.0, 1.0, 0.0)
        assertTrue(oneDegree in 110_000.0..112_000.0)
    }

    @Test
    fun accountRulesRejectWeakAndMismatchedPasswords() {
        assertEquals(
            "Use at least 8 characters.",
            validateNewAccount("Ada", "ada@campus.edu", "short", "short", "North Campus"),
        )
        assertEquals(
            "Passwords do not match.",
            validateNewAccount("Ada", "ada@campus.edu", "longpassword", "otherpassword", "North Campus"),
        )
        assertNull(validateNewAccount("Ada", "ada@campus.edu", "longpassword", "longpassword", "North Campus"))
    }

    @Test
    fun photoCheckAcceptsTheSamePictureAndRejectsADifferentColor() {
        val red = IntArray(16 * 16) { 0xFFFF0000.toInt() }
        val blue = IntArray(16 * 16) { 0xFF0000FF.toInt() }
        assertTrue(compareSamples(red, red.copyOf(), 16).sameItem)
        assertFalse(compareSamples(red, blue, 16).sameItem)
    }

    @Test
    fun privateCheckDoesNotMatchAWrongAnswer() {
        val sealed = sealSecret("blue sticker")
        assertTrue(secretMatches("Blue Sticker", sealed))
        assertTrue(!secretMatches("red sticker", sealed))
        val salt = PasswordHasher.salt()
        val hash = PasswordHasher.hash("longpassword", salt)
        assertTrue(PasswordHasher.verify("longpassword", java.util.Base64.getEncoder().encodeToString(salt), hash))
        assertTrue(!PasswordHasher.verify("wrongpassword", java.util.Base64.getEncoder().encodeToString(salt), hash))
    }

    private fun matchable(
        id: Long,
        type: ItemType,
        category: String,
        zone: String,
        title: String,
        description: String,
        color: String?,
        latitude: Double = 37.4221,
        longitude: Double = -122.0840,
        createdAt: Long = now,
    ) = Matchable(
        id = id,
        title = title,
        description = description,
        category = category,
        type = type,
        status = ItemStatus.OPEN,
        latitude = latitude,
        longitude = longitude,
        zone = zone,
        scannedText = "",
        colorHex = color,
        createdAt = createdAt,
    )

    private fun item(
        id: Long,
        zone: String,
        category: String,
        type: ItemType = ItemType.LOST,
        title: String = "$category $id",
    ) = Item(
        id = id,
        title = title,
        description = title,
        category = category,
        type = type,
        status = ItemStatus.OPEN,
        zoneName = zone,
        createdAt = now,
    )
}
