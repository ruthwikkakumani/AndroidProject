package com.findit.campus

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.findit.campus.data.local.MessageEntity
import com.findit.campus.data.local.ThreadEntity
import com.findit.campus.location.MeetingPin
import com.findit.campus.data.MeetingPlan
import com.findit.campus.data.PrivateContact
import com.findit.campus.ui.chat.*
import com.findit.campus.ui.home.*
import com.findit.campus.ui.theme.FindItTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class SharedFlowUiTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "quality-screens/$name.png")
        file.parentFile?.mkdirs()
        compose.waitForIdle()
        file.outputStream().use { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun savedAccountHasAnActionableConnectionNotice() {
        var connected = false
        var reportOpened = false
        compose.setContent { FindItTheme { Surface(Modifier.fillMaxSize()) {
            DiscoverScreen(HomeUiState(loading = false), false, {}, {}, {}, {}, {}, {}, {}, {}, {},
                displayName = "Campus student", campusName = "City College",
                syncMessage = "Connect your saved account to share reports with other students.",
                onConnect = { connected = true }, onPostLost = { reportOpened = true })
        } } }
        capture("home-connect")
        compose.onNodeWithText("Connect account").performClick()
        compose.runOnIdle { assertTrue(connected) }
        compose.onNodeWithText("Report a lost item").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(reportOpened) }
    }

    @Test fun pendingMessagesAndMeetingLocationRemainUsable() {
        val now = System.currentTimeMillis()
        val thread = ThreadEntity(91, 81, 55, 99, "OPEN", now)
        val state = ChatUiState(thread = thread, viewerId = 99, personName = "Campus student",
            onlineAccount = true, loading = false, pendingMessageIds = setOf(2),
            messages = listOf(MessageEntity(1, 91, 55, "Let's meet by the library.", null, now),
                MessageEntity(2, 91, 99, MeetingPin(13.0827, 80.2703, 5f, now).encode(), null, now)))
        val actions = ChatActions({}, {}, {}, { it.recycle() }, {}, {}, {}, {}, {}, {}, {})
        compose.setContent { FindItTheme { ChatContent(state, actions, {}) } }
        compose.onNodeWithText("View map & directions").assertIsDisplayed()
        compose.onNodeWithText("Waiting to upload", substring = true).assertIsDisplayed()
        capture("chat-meeting-pin")
        compose.onNodeWithContentDescription("Meeting location").performClick()
        compose.onNodeWithText("Share current location").assertIsEnabled()
        compose.onNodeWithText("Share live while chatting · 15 min").assertIsEnabled()
        capture("chat-meeting-options")
    }

    @Test fun contactMeetingAndAiActionsOpenWorkingPrivatePanels() {
        var contact: PrivateContact? = null
        var plan: MeetingPlan? = null
        val state = ChatUiState(thread = ThreadEntity(91, 81, 55, 99, "OPEN", System.currentTimeMillis()),
            viewerId = 99, personName = "Alex", onlineAccount = true, loading = false)
        val actions = ChatActions({}, {}, {}, { it.recycle() }, {}, {}, {}, {}, {}, {}, {},
            shareContact = { phone, note -> contact = PrivateContact(phone, note) },
            shareMeeting = { place, time, note -> plan = MeetingPlan(place, time, note) })
        compose.setContent { FindItTheme { ChatContent(state, actions, {}) } }
        compose.onNodeWithContentDescription("Message attachments").performClick()
        compose.onNodeWithText("Private contact").performClick()
        compose.onNodeWithText("Share contact privately").assertIsNotEnabled()
        compose.onNodeWithText("Phone number").performTextInput("+91 98765 43210")
        compose.onNodeWithText("Availability or extra details (optional)").performTextInput("After class")
        capture("chat-private-contact")
        compose.onNodeWithText("Share contact privately").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("+919876543210", contact?.dialNumber) }
        compose.onNodeWithContentDescription("Message attachments").performClick()
        compose.onNodeWithText("Meeting plan").performClick()
        compose.onNodeWithText("Library entrance").performClick()
        compose.onNodeWithText("Day and time").performTextInput("Today, 3:30 PM")
        compose.onNodeWithText("How to find you (optional)").performTextInput("Blue backpack")
        capture("chat-plan")
        compose.onNodeWithText("Send meeting plan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(MeetingPlan("Library entrance", "Today, 3:30 PM", "Blue backpack"), plan) }
        compose.onNodeWithContentDescription("Message attachments").performClick()
        compose.onNodeWithText("AI photo evidence").assertIsDisplayed().performClick()
        compose.onNodeWithText("1  Share photo evidence").assertExists()
    }

    @Test fun bothMeetingLocationsUpdateAndEndWithoutShowingOldLivePins() {
        val now = System.currentTimeMillis()
        val own = MessageEntity(2, 91, 99, MeetingPin(13.0827, 80.2703, 5f, now, true).encode(), null, now)
        val peer = MessageEntity(3, 91, 55, MeetingPin(13.0831, 80.2712, 6f, now, true).encode(), null, now + 1)
        var state by mutableStateOf(ChatUiState(thread = ThreadEntity(91, 81, 55, 99, "OPEN", now),
            viewerId = 99, personName = "Alex", onlineAccount = true, loading = false, sharingLocation = true,
            messages = listOf(own, peer)))
        compose.setContent { FindItTheme { ChatContent(state, ChatActions({}, {}, {}, { it.recycle() }, {}, {}, {}, {}, {}, {}, {}), {}) } }
        compose.onAllNodesWithText("View map & directions").assertCountEquals(2)
        compose.onNodeWithText("Meet-up map").performClick()
        compose.onNodeWithText("Meet halfway").assertExists()
        compose.onNodeWithText("You").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Alex") and hasAnyAncestor(hasTestTag("meeting-person-Alex"))).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Live update · ±5 m").assertExists()
        compose.onNodeWithText("Live update · ±6 m").assertExists()
        compose.runOnIdle { state = state.copy(sharingLocation = false, messages = state.messages + own.copy(id = 4,
            body = MeetingPin(13.0827, 80.2703, 5f, now, false).encode(), createdAt = now + 2)) }
        compose.onNodeWithText("Live update · ±5 m").assertDoesNotExist()
        compose.onNodeWithText("Last shared", substring = true).assertExists()
        compose.onNodeWithText("Directions to Alex").performScrollTo().assertIsEnabled()
        compose.mainClock.advanceTimeBy(1000)
        capture("chat-both-locations")
    }
}
