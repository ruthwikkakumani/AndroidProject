package com.findit.campus.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.findit.campus.data.FindItLog

class CameraSession(useFront: Boolean) {
    val preview: Preview = Preview.Builder().build()
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setJpegQuality(88)
        .build()
    val selector: CameraSelector =
        if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

    fun takePicture(context: Context, onResult: (Bitmap) -> Unit, onError: (String) -> Unit) {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = try {
                        val source = image.toBitmap()
                        val rotation = image.imageInfo.rotationDegrees
                        if (rotation == 0) source else {
                            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                                Matrix().apply { postRotate(rotation.toFloat()) }, true)
                            if (rotated !== source) source.recycle()
                            rotated
                        }
                    } catch (error: Exception) { FindItLog.e("Photo decoding failed", error); null }
                    finally { image.close() }
                    if (bitmap != null) onResult(bitmap) else onError("Could not read that photo.")
                }

                override fun onError(exception: ImageCaptureException) {
                    FindItLog.e("Camera capture failed", exception)
                    onError(exception.message ?: "The camera could not take a photo.")
                }
            },
        )
    }
}

@Composable
fun FindItCameraPreview(
    session: CameraSession,
    modifier: Modifier = Modifier,
    onUnavailable: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var request by remember(session) { mutableStateOf<SurfaceRequest?>(null) }
    val unavailable by rememberUpdatedState(onUnavailable)

    DisposableEffect(lifecycleOwner, session) {
        val future = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var active = true
        session.preview.setSurfaceProvider(executor) { surfaceRequest ->
            if (active) request = surfaceRequest else surfaceRequest.willNotProvideSurface()
        }
        future.addListener(
            {
                if (!active) return@addListener
                runCatching {
                    val provider = future.get()
                    provider.unbind(session.preview, session.imageCapture)
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        session.selector,
                        session.preview,
                        session.imageCapture,
                    )
                }.onFailure { error ->
                    FindItLog.e("Camera bind failed", error)
                    unavailable("This camera is not available right now.")
                }
            },
            executor,
        )
        onDispose {
            active = false
            session.preview.setSurfaceProvider(null)
            runCatching {
                if (future.isDone) future.get().unbind(session.preview, session.imageCapture)
            }
        }
    }

    val current = request
    if (current != null) {
        CameraXViewfinder(
            surfaceRequest = current,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
