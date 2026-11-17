package com.findit.campus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.findit.campus.ui.scan.ScanScreen
import com.findit.campus.ui.scan.ScanUiState
import com.findit.campus.ui.theme.FindItTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ScannerExperienceTest {
    @get:Rule val rule = createComposeRule()

    private fun capture(name: String) {
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "quality-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun sceneOnlyPhotoOffersRetakeAndManualSearchWithoutFalseItemClues() {
        var query by mutableStateOf("")
        rule.setContent { FindItTheme { Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScanScreen(ScanUiState(text = query, scene = listOf("room", "tile", "shelf"),
                message = "Only the surroundings were recognized. Move closer to the item or choose another photo."),
                { query = it }, { it.recycle() }, {}, {})
        } } }
        rule.onNodeWithText("Item clues").assertDoesNotExist()
        rule.onNodeWithText("Background only: room, tile, shelf").assertExists()
        rule.onNodeWithTag("scan_query").performScrollTo().performTextInput("blue bottle")
        rule.runOnIdle { assertEquals("blue bottle", query) }
        rule.onNodeWithText("Use in a lost report").performScrollTo().assertIsEnabled()
        capture("scanner-scene-guidance")
    }

    @Test fun photoPreviewConfidenceAndHelpWorkOnSmallScreenWithLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val photo = context.assets.open("recognition-dog.jpg").use { BitmapFactory.decodeStream(it) }
        var query by mutableStateOf("")
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                FindItTheme { Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Box(Modifier.width(320.dp)) {
                        ScanScreen(ScanUiState(text = query, photo = photo, objects = listOf("dog"), confidences = mapOf("dog" to .92f)),
                            { query = it }, { it.recycle() }, {}, {})
                    }
                } }
            }
        }
        rule.onNodeWithTag("scan_photo").assertExists()
        capture("scanner-small-preview")
        rule.onNodeWithText("dog").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("dog", query) }
        rule.onNodeWithText("92%").assertExists()
        rule.onNodeWithText("Use in a lost report").performScrollTo().assertIsEnabled()
        rule.onNodeWithContentDescription("How the scanner works").performScrollTo().performClick()
        rule.onNodeWithText("Google ML Kit, on your phone").assertExists()
        rule.onNodeWithText("Got it").performScrollTo().performClick()
        rule.onNodeWithText("Google ML Kit, on your phone").assertDoesNotExist()
    }

    @Test fun busyScannerPreventsSecondCaptureAndDisplaysActualPhoto() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val photo = context.assets.open("recognition-dog.jpg").use { BitmapFactory.decodeStream(it) }
        rule.setContent { FindItTheme { Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScanScreen(ScanUiState(busy = true, photo = photo), {}, { it.recycle() }, {}, {})
        } } }
        rule.onNodeWithTag("scan_photo").assertExists()
        rule.onNodeWithText("Retake photo").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Choose photo").assertIsNotEnabled()
        rule.onNodeWithText("Use in a lost report").performScrollTo().assertIsNotEnabled()
    }
}
