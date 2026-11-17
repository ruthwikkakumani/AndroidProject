package com.findit.campus

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.findit.campus.model.*
import com.findit.campus.ui.auth.*
import com.findit.campus.ui.components.*
import com.findit.campus.ui.detail.*
import com.findit.campus.ui.home.*
import com.findit.campus.ui.map.*
import com.findit.campus.ui.mine.*
import com.findit.campus.ui.post.*
import com.findit.campus.ui.scan.*
import com.findit.campus.ui.theme.FindItTheme
import com.findit.campus.ui.chat.*
import com.findit.campus.data.local.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Test data stays in Compose state; no student records are created or overwritten. */
class ScreenQualityTest {
    @get:Rule val rule = createComposeRule()
    private fun sample(type: ItemType) = Item(id = if (type == ItemType.LOST) 81 else 82,
        title = if (type == ItemType.LOST) "Navy bottle" else "Blue notebook",
        description = "Has a small mountain sticker on the front.", category = "Other", type = type,
        ownerId = 55, zoneName = "Library", hasCoordinates = false, createdAt = System.currentTimeMillis())
    private fun capture(name: String) {
        rule.waitForIdle()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "quality-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun collegeDropdownFiltersSelectsAndAllowsCustomNames() {
        var campus by mutableStateOf("")
        rule.setContent { FindItTheme { Surface(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column {
                CampusField(campus, { campus = it }, Modifier.testTag("campus_search"))
            }
        } } }
        rule.onNodeWithTag("campus_search").performTextInput("VIT")
        rule.onNodeWithText("VIT — Chennai").assertExists()
        val dropdownFile = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "quality-screens/college-dropdown.png")
        dropdownFile.parentFile?.mkdirs()
        dropdownFile.outputStream().use { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
        rule.onNodeWithText("VIT — Chennai").performClick()
        rule.runOnIdle { assertEquals("VIT — Chennai", campus) }
        rule.onNodeWithTag("campus_search").performTextReplacement("My Independent College")
        rule.runOnIdle { assertEquals("My Independent College", campus) }
    }

    @Test fun googleButtonGivesUsefulFeedbackWithoutConfiguration() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) return
        var error: String? = null
        rule.setContent { FindItTheme { LoginScreen({}, { _, _ -> }, {}, {}, error = null,
            onGoogleToken = {}, onGoogleError = { error = it }) } }
        capture("google-login")
        rule.onNodeWithText("Continue with Google").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(error?.contains("email and password") == true) }
    }

    @Test fun smallScreenAndLargeTextKeepSignupReachable() {
        var submitted = false
        rule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(androidx.compose.ui.platform.LocalDensity.current.density, 1.3f)) {
                FindItTheme { Surface(Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                        RegisterScreen({}, { _, _, _, _, _ -> submitted = true }, {}, error = null)
                    }
                } }
            }
        }
        rule.onNodeWithTag("register_submit").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertTrue(submitted) }
        capture("signup-small-large-text")
    }

    @Test fun boardsKeepActionsAndRecipientsSeparate() {
        var selected: Long? = null; var postFound = false
        rule.setContent {
            var type by remember { mutableStateOf<ItemType?>(null) }
            FindItTheme { Surface(Modifier.fillMaxSize()) { DiscoverScreen(
                HomeUiState(items = listOf(sample(ItemType.LOST), sample(ItemType.FOUND)), loading = false, type = type), false,
                {}, { type = it }, {}, {}, {}, {}, {}, {}, {}, viewerId = 99,
                onFound = { selected = it }, onPostFound = { postFound = true }) } }
        }
        capture("home")
        rule.onNodeWithText("Found it").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(81L, selected) }
        rule.onNodeWithText("Found board").performScrollTo().performClick()
        capture("found-board")
        rule.onNodeWithText("Found it").assertDoesNotExist()
        rule.onNodeWithText("This is mine").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(82L, selected) }
        rule.onNodeWithText("Post an item you found").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(postFound) }
    }

    @Test fun accountCreationLeadsToLoginBeforeDashboard() {
        var mode by mutableStateOf(0)
        var credentials: List<String>? = null
        rule.setContent { FindItTheme { Surface(Modifier.fillMaxSize()) {
            when (mode) {
                0 -> WelcomeScreen({ mode = 1 }, { mode = 2 })
                1 -> RegisterScreen({}, { name, email, password, confirm, campus ->
                    credentials = listOf(name, email, password, confirm, campus); mode = 2
                }, { mode = 2 }, error = null)
                2 -> LoginScreen({}, { _, _ -> mode = 3 }, {}, {}, error = null,
                    notice = "Account created. Log in to open your dashboard.")
                else -> TextDashboard()
            }
        } } }
        capture("welcome")
        rule.onNodeWithTag("welcome_create").performScrollTo().performClick()
        capture("register")
        listOf("register_name" to "Test Student", "register_email" to "student@example.com",
            "register_password" to "samplepass8", "register_confirm" to "samplepass8", "register_campus" to "Test Campus")
            .forEach { (tag, value) -> rule.onNodeWithTag(tag).performScrollTo().performTextInput(value) }
        rule.onNodeWithTag("register_submit").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("Test Campus", credentials?.last()); assertEquals(2, mode) }
        rule.onNodeWithTag("login_submit").assertExists()
        capture("login")
        rule.onNodeWithTag("login_email").performScrollTo().performTextInput("student@example.com")
        rule.onNodeWithTag("login_password").performScrollTo().performTextInput("samplepass8")
        rule.onNodeWithTag("login_submit").performScrollTo().performClick()
        rule.onNodeWithText("Dashboard opened").assertExists()
    }

    @Test fun reportEntryPreservesLostKindAndSeparateFoundKind() {
        var form by mutableStateOf(PostForm(type = ItemType.LOST, whenMillis = System.currentTimeMillis()))
        var posted: ItemType? = null
        rule.setContent { FindItTheme { Surface(Modifier.fillMaxSize()) { PostScreen(
            PostUiState(form = form), { form = form.copy(type = it) }, { form = form.copy(title = it) },
            { form = form.copy(description = it) }, { form = form.copy(category = it) }, {}, {},
            { form = form.copy(zoneName = it) }, {}, {}, { it.recycle() }, {}, { posted = form.type }, {}, {}, {}, {}) } } }
        rule.onNodeWithText("Report a lost item").assertExists()
        rule.onNodeWithText("Found").assertDoesNotExist()
        capture("report-lost")
        rule.onNodeWithText("Post lost report").performClick()
        rule.runOnIdle { assertEquals(ItemType.LOST, posted); form = form.copy(type = ItemType.FOUND) }
        rule.onNodeWithText("Post a found item").assertExists()
        capture("report-found")
        rule.onNodeWithText("Post found item").performClick()
        rule.runOnIdle { assertEquals(ItemType.FOUND, posted) }
    }

    @Test fun detailScannerMapAndProfileRenderWithRealisticEmptyStates() {
        var page by mutableStateOf(0)
        var draft by mutableStateOf("")
        var scanned: String? = null
        rule.setContent { FindItTheme { Surface(Modifier.fillMaxSize()) { when(page) {
            0 -> DetailScreen(DetailUiState(item = sample(ItemType.LOST)), {}, {}, {}, {}, viewerId = 99, onReachOut = {})
            1 -> ScanScreen(ScanUiState(text = draft), { draft = it }, { it.recycle() }, {}, { scanned = it })
            2 -> MineScreen(MineUiState(listOf(sample(ItemType.LOST)), false), {}, "Test Student", "Test Campus")
            else -> MapScreen(MapUiState(), {}, {}, {}, {}, {}, {}, {})
        } } } }
        capture("detail")
        rule.runOnIdle { page = 1 }
        capture("scan")
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Notebook name")
        rule.onNodeWithText("Use in a lost report").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("Notebook name", scanned); page = 2 }
        capture("profile")
        rule.onAllNodesWithText("Reunited", useUnmergedTree = true).assertCountEquals(2)
        rule.runOnIdle { page = 3 }
        capture("map")
        rule.onNodeWithText("No location pins yet").assertExists()
    }

    @Test fun messagesOpenExactThreadAndHandoverWaitsForBothChecks() {
        val now = System.currentTimeMillis()
        var page by mutableStateOf(0)
        var opened: Long? = null
        val report = sample(ItemType.LOST)
        val thread = ThreadEntity(id = 91, itemId = report.id, ownerId = 55, guestId = 99, status = "OPEN", createdAt = now)
        val chat = ChatUiState(thread = thread, item = report, personName = "Test Student", viewerId = 99, loading = false,
            messages = listOf(MessageEntity(1, 91, 55, "Does it have a mountain sticker?", null, now),
                MessageEntity(2, 91, 99, "Yes. I found it by the library desk.", null, now)))
        val actions = ChatActions({}, {}, {}, { it.recycle() }, {}, {}, {}, {}, {}, {}, {})
        rule.setContent { FindItTheme { Surface(Modifier.fillMaxSize()) {
            if (page == 0) InboxContent(listOf(ConversationSummary(91, report.id, report.title, "Test Student", null,
                "Does it have a mountain sticker?", now, 1, false, false, false, false)), emptyList(),
                { opened = it; page = 1 }, {})
            else ChatContent(chat, actions, {})
        } } }
        capture("inbox")
        rule.onNodeWithText("Test Student").performClick()
        rule.runOnIdle { assertEquals(91L, opened) }
        capture("conversation")
        rule.onNodeWithContentDescription("Ownership checks").performClick()
        rule.onNodeWithText("A thoughtful return").assertExists()
        capture("ownership-checks")
        rule.onNodeWithText("The handover is complete").performScrollTo().assertIsNotEnabled()
    }
}
@Composable private fun TextDashboard() { androidx.compose.material3.Text("Dashboard opened") }
