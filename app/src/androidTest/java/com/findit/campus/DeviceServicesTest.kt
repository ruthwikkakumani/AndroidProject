package com.findit.campus

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.findit.campus.location.LocationDataSource
import com.findit.campus.ui.camera.CameraSession
import com.findit.campus.ui.camera.FindItCameraPreview
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Runs against a camera-capable emulator/device with location services enabled. */
class DeviceServicesTest {
    @get:Rule val rule = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    @Before fun permissions() {
        listOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .forEach { instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it) }
    }
    @Test fun bothCamerasProduceUsablePhotos() {
        var front by mutableStateOf(false)
        var active: CameraSession? = null
        val result = AtomicReference<Bitmap?>()
        val failure = AtomicReference<String?>()
        rule.setContent {
            val camera = remember(front) { CameraSession(front) }
            SideEffect { active = camera }
            FindItCameraPreview(camera, Modifier.fillMaxSize(), onUnavailable = { failure.set(it) })
        }
        for (useFront in listOf(false, true)) {
            rule.runOnIdle { front = useFront }
            rule.waitUntil(15_000) { active?.imageCapture?.resolutionInfo != null || failure.get() != null }
            assertNull(failure.get())
            rule.runOnIdle { active!!.takePicture(context, { result.set(it) }, { failure.set(it) }) }
            rule.waitUntil(15_000) { result.get() != null || failure.get() != null }
            assertNull(failure.get())
            val photo = result.getAndSet(null)!!
            assertTrue(photo.width > 100 && photo.height > 100)
            photo.recycle()
        }
    }
    @Test fun fusedLocationReturnsFreshCoordinatesAndUpdates() = runBlocking(Dispatchers.Main) {
        val location = LocationDataSource(context)
        assertTrue(location.hasPrecisePermission())
        val update = withTimeout(20_000) { location.updates().first() }
        val current = location.current(15_000)
        assertNotNull(current)
        assertTrue(update.latitude in -90.0..90.0 && update.longitude in -180.0..180.0)
        assertTrue(current!!.accuracyMeters >= 0)
        assertEquals(update.latitude, current.latitude, 0.01)
        assertEquals(update.longitude, current.longitude, 0.01)
    }
}
