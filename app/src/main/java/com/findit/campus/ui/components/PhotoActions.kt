package com.findit.campus.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.findit.campus.FindItApplication
import com.findit.campus.ui.camera.*
import com.findit.campus.ui.permission.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** Shared capture/picker workflow. A granted first request resumes the original capture action. */
@Composable
fun PhotoActions(onPhoto: (Bitmap) -> Unit, onError: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, cameraLabel: String = "Take photo", galleryLabel: String = "Choose photo", onDarkSurface: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val feedback = LocalFeedback.current
    var cameraRequested by remember { mutableStateOf(false) }
    val (granted, ask) = rememberPermissionAsk(CameraPermissions,
        "FindIt uses your camera to photograph an item. Photos are shared only when you submit them.",
        "Enable Camera in Settings to take a photo, or choose an existing photo.")
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            try { (context.applicationContext as FindItApplication).graph.photos.readPicker(uri)?.let { bitmap -> onPhoto(bitmap); feedback("Photo selected") } ?: onError("This image could not be opened.") }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onError("This photo could not be loaded. Please try another image.") }
        }
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton({ cameraRequested = true; if (!granted) ask() }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
            Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(cameraLabel, style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton({ picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (onDarkSurface) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.primary,
                disabledContentColor = if (onDarkSurface) MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (onDarkSurface) MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .35f) else MaterialTheme.colorScheme.outline),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
            Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(galleryLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
    if (cameraRequested && granted) {
        CameraCapture(onClose = { cameraRequested = false }, onPhoto = { cameraRequested = false; onPhoto(it); feedback("Photo captured") }, onError = onError)
    }
}

@Composable
fun CameraCapture(onClose: () -> Unit, onPhoto: (Bitmap) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    var useFront by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val camera = remember(useFront) { CameraSession(useFront) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onClose)
        Scaffold(topBar = { PageTopBar("Photograph your item", onClose) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
                FindItCameraPreview(camera, Modifier.fillMaxWidth().weight(1f), onUnavailable = { error = it })
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    error?.let { Notice(it, error = true) }
                    Text("Keep the item in frame. Include a unique mark or label.", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        IconButton({ useFront = !useFront; error = null }, enabled = !capturing) { Icon(Icons.Outlined.Cameraswitch, "Switch camera") }
                        Button({
                            capturing = true
                            camera.takePicture(context, onResult = { capturing = false; onPhoto(it) },
                                onError = { capturing = false; error = it; onError(it) })
                        }, Modifier.weight(1f).heightIn(min = 52.dp), enabled = !capturing) { Text(if (capturing) "Capturing…" else "Use this photo") }
                    }
                }
            }
        }
    }
}
