package com.findit.campus.ui.post

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.findit.campus.FindItApplication
import com.findit.campus.model.ItemType
import com.findit.campus.ui.camera.CameraSession
import com.findit.campus.ui.camera.FindItCameraPreview
import com.findit.campus.ui.components.ItemPhoto
import com.findit.campus.ui.format.formatFull
import com.findit.campus.ui.permission.CameraPermissions
import com.findit.campus.ui.permission.LocationPermissions
import com.findit.campus.ui.permission.rememberPermissionAsk
import com.findit.campus.ui.theme.FindItPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostScreen(
    state: PostUiState,
    onType: (ItemType) -> Unit,
    onTitle: (String) -> Unit,
    onDescription: (String) -> Unit,
    onCategory: (String) -> Unit,
    onWhen: (Long) -> Unit,
    onContact: (String) -> Unit,
    onPlaceName: (String) -> Unit,
    onPrivateDetail: (String) -> Unit,
    onUseLocation: () -> Unit,
    onPhoto: (android.graphics.Bitmap) -> Unit,
    onClearPhoto: () -> Unit,
    onSave: (Boolean) -> Unit,
    onDismissDuplicate: () -> Unit,
    onViewDuplicate: (Long) -> Unit,
    onConsumeMessage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var capturing by remember { mutableStateOf(false) }
    var useFront by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var cameraNote by remember { mutableStateOf<String?>(null) }
    val session = remember(useFront) { CameraSession(useFront) }
    val (cameraGranted, askCamera) = rememberPermissionAsk(
        permissions = CameraPermissions,
        rationale = "FindIt uses the camera to photograph an item and read writing on it.",
        settingsMessage = "Camera access is off. Enable it in Settings to take a photo.",
    )
    val (locationGranted, askLocation) = rememberPermissionAsk(
        permissions = LocationPermissions,
        rationale = "FindIt uses your location to tag where an item was lost or found.",
        settingsMessage = "Location access is off. You can still name the place without a map pin.",
    )
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = (context.applicationContext as FindItApplication).graph.photos.readPicker(uri)
            if (bitmap != null) onPhoto(bitmap) else cameraNote = "That picture could not be opened."
        }
    }
    BackHandler(enabled = capturing) { capturing = false }
    val message = state.message ?: cameraNote
    if (message != null) {
        AlertDialog(
            onDismissRequest = {
                cameraNote = null
                onConsumeMessage()
            },
            title = { Text("FindIt") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    cameraNote = null
                    onConsumeMessage()
                }) { Text("OK") }
            },
        )
    }
    state.duplicate?.let { duplicate ->
        AlertDialog(
            onDismissRequest = onDismissDuplicate,
            title = { Text("This looks familiar") },
            text = { Text("${duplicate.title} is already on the board (${duplicate.score} match).") },
            confirmButton = {
                TextButton(onClick = { onViewDuplicate(duplicate.itemId) }) { Text("See report") }
                TextButton(onClick = { onSave(true) }) { Text("Post anyway") }
            },
            dismissButton = { TextButton(onClick = onDismissDuplicate) { Text("Keep editing") } },
        )
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (capturing) "Take a photo" else "Report an item") },
                navigationIcon = {
                    IconButton(onClick = { if (capturing) capturing = false else onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(targetState = capturing, label = "post-mode", modifier = Modifier.padding(padding)) { showCamera ->
            if (showCamera && cameraGranted) {
                Column(Modifier.fillMaxSize()) {
                    FindItCameraPreview(
                        session = session,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onUnavailable = { cameraNote = it },
                    )
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { useFront = !useFront }) {
                            Icon(Icons.Outlined.Cameraswitch, contentDescription = "Switch camera")
                        }
                        Button(
                            onClick = {
                                session.takePicture(
                                    context,
                                    onResult = { bitmap ->
                                        onPhoto(bitmap)
                                        capturing = false
                                    },
                                    onError = { cameraNote = it },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Use photo") }
                    }
                }
            } else {
                PostFormColumn(
                    state = state,
                    onType = onType,
                    onTitle = onTitle,
                    onDescription = onDescription,
                    onCategory = onCategory,
                    onContact = onContact,
                    onPlaceName = onPlaceName,
                    onPrivateDetail = onPrivateDetail,
                    onPickDate = { showDate = true },
                    onUseLocation = { if (locationGranted) onUseLocation() else askLocation() },
                    onTakePhoto = { if (cameraGranted) capturing = true else askCamera() },
                    onPickPhoto = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                    onClearPhoto = onClearPhoto,
                    onSave = { onSave(false) },
                )
            }
        }
    }
    if (showDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.form.whenMillis)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onWhen(datePickerMillisToLocal(it)) }
                    showDate = false
                }) { Text("Set date") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostFormColumn(
    state: PostUiState,
    onType: (ItemType) -> Unit,
    onTitle: (String) -> Unit,
    onDescription: (String) -> Unit,
    onCategory: (String) -> Unit,
    onContact: (String) -> Unit,
    onPlaceName: (String) -> Unit,
    onPrivateDetail: (String) -> Unit,
    onPickDate: () -> Unit,
    onUseLocation: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onSave: () -> Unit,
) {
    val form = state.form
    val tint = if (form.type == ItemType.LOST) FindItPalette.colors.lost else FindItPalette.colors.found
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = form.type == ItemType.LOST,
                onClick = { onType(ItemType.LOST) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text("Lost") }
            SegmentedButton(
                selected = form.type == ItemType.FOUND,
                onClick = { onType(ItemType.FOUND) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text("Found") }
        }
        OutlinedTextField(
            value = form.title,
            onValueChange = onTitle,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What is it?") },
            supportingText = { Text("${form.title.length}/60") },
            singleLine = true,
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = onDescription,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Details someone could recognize") },
            supportingText = { Text("${form.description.length}/400") },
            minLines = 3,
        )
        Text("Category", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.forEach { name ->
                FilterChip(selected = form.category == name, onClick = { onCategory(name) }, label = { Text(name) })
            }
        }
        TextButton(onClick = onPickDate) { Text(formatFull(form.whenMillis)) }
        if (form.photoPath != null) {
            ItemPhoto(
                path = form.photoPath,
                title = form.title.ifBlank { "Item" },
                category = form.category,
                tint = tint,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            TextButton(onClick = onClearPhoto) { Text("Remove photo") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onTakePhoto) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Text("Camera")
            }
            FilledTonalButton(onClick = onPickPhoto) { Text("Gallery") }
        }
        Text("Where", style = MaterialTheme.typography.titleSmall)
        FilledTonalButton(onClick = onUseLocation, enabled = !state.locating) {
            if (state.locating) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.MyLocation, contentDescription = null)
            }
            Text(if (form.placeLabel.isBlank()) "Use my location" else form.placeLabel)
        }
        state.accuracyMeters?.let { accuracy ->
            Text("Accuracy about ${accuracy.toInt()} m", style = MaterialTheme.typography.bodySmall)
        }
        if (state.likelyPlaces.isNotEmpty()) {
            Text(
                "Often turns up at ${state.likelyPlaces.joinToString(", ")}.",
                color = FindItPalette.colors.reunion,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OutlinedTextField(
            value = form.zoneName,
            onValueChange = onPlaceName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Place on campus") },
            supportingText = { Text("Name the building or area. A map pin is optional.") },
            singleLine = true,
        )
        if (form.type == ItemType.FOUND) {
            OutlinedTextField(
                value = form.privateDetail,
                onValueChange = onPrivateDetail,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Private check (optional)") },
                supportingText = { Text("Only someone who knows this detail can claim the item. It is not shown publicly.") },
            )
        }
        OutlinedTextField(
            value = form.contact,
            onValueChange = onContact,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("How to reach you") },
            singleLine = true,
        )
        Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.saving) "Posting" else "Post report")
        }
    }
}
