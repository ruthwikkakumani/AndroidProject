package com.findit.campus

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.findit.campus.ui.components.LocalFeedback
import com.findit.campus.ui.home.DiscoverScreen
import com.findit.campus.ui.home.HomeUiState
import com.findit.campus.ui.mine.MineScreen
import com.findit.campus.ui.mine.MineUiState
import com.findit.campus.ui.theme.FindItTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ProfileToolsUiTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "quality-screens/$name.png")
        file.parentFile?.mkdirs()
        val root = compose.onRoot().captureToImage().asAndroidBitmap()
        if (name != "profile-updated") android.os.SystemClock.sleep(250)
        file.outputStream().use { (if (name == "profile-updated") root else instrumentation.uiAutomation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun profileControlsOpenPrivateSettingsAndRealToolDestinations() {
        var copied = ""
        var recovery = false
        var messages = false
        compose.setContent { FindItTheme { CompositionLocalProvider(LocalFeedback provides { copied = it }) {
            Surface(Modifier.fillMaxSize().statusBarsPadding()) { MineScreen(MineUiState(loading = false), {}, "Alex Student", "City College",
                email = "alex@example.edu", onMessages = { messages = true }, onResetPassword = { recovery = true }) }
        } } }
        capture("profile-updated")
        compose.onNodeWithContentDescription("Account settings").performClick()
        compose.onNodeWithText("alex@example.edu").assertIsDisplayed()
        compose.onNodeWithText("Password recovery").performScrollTo().performClick()
        compose.onNodeWithText("Request email").performClick()
        compose.runOnIdle { assertTrue(recovery) }
        compose.onNodeWithText("Privacy & permissions").performScrollTo().performClick()
        compose.onNodeWithText("You're in control").assertExists()
        compose.onNodeWithText("Camera & location permissions").performScrollTo().assertIsEnabled()
        capture("profile-privacy")
        compose.onNodeWithText("Done").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Account settings").performClick()
        compose.onNodeWithText("Copy email").performClick()
        compose.runOnIdle { assertEquals("Email copied", copied) }
        compose.onNodeWithText("Done").performScrollTo().performClick()
        compose.onNodeWithText("Campus tools").performScrollTo().performClick()
        compose.onNodeWithText("Private messages").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(messages) }
    }

    @Test fun homeToolsRemainReachableAtNarrowWidthAndLargeText() {
        var scanned = false
        var mapped = false
        var cleared = false
        compose.setContent { FindItTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                Surface(Modifier.width(320.dp).height(520.dp)) {
                    DiscoverScreen(HomeUiState(loading = false, query = "Notebook"), false,
                        { cleared = it.isEmpty() }, {}, {}, {}, {}, {}, {}, {}, {},
                        displayName = "Alex Student", campusName = "A college name that needs room to wrap",
                        onScan = { scanned = true }, onMap = { mapped = true })
                }
            }
        } }
        compose.onNodeWithTag("campus-board").performScrollToNode(hasContentDescription("Clear search"))
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.runOnIdle { assertTrue(cleared) }
        openHomeTools()
        compose.onNodeWithText("AI photo clues").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(scanned) }
        openHomeTools()
        compose.onNodeWithText("Explore campus map").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(mapped) }
        openHomeTools()
        compose.onNodeWithText("Private messages").performScrollTo().assertIsDisplayed()
        capture("home-tools-large-text")
    }
    private fun openHomeTools() {
        compose.onNodeWithTag("campus-board").performScrollToNode(hasText("Find it faster"))
        compose.onNodeWithText("Find it faster").performClick()
    }
}
