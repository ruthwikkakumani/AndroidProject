package com.findit.campus.ui.post

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findit.campus.R
import com.findit.campus.BuildConfig
import com.findit.campus.model.ItemType
import com.findit.campus.ui.components.*
import com.findit.campus.ui.format.formatFull
import com.findit.campus.ui.permission.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(state: PostUiState, onType: (ItemType) -> Unit, onTitle: (String) -> Unit,
    onDescription: (String) -> Unit, onCategory: (String) -> Unit, onWhen: (Long) -> Unit,
    onContact: (String) -> Unit, onPlaceName: (String) -> Unit, onPrivateDetail: (String) -> Unit,
    onUseLocation: () -> Unit, onPhoto: (android.graphics.Bitmap) -> Unit, onClearPhoto: () -> Unit,
    onSave: (Boolean) -> Unit, onDismissDuplicate: () -> Unit, onViewDuplicate: (Long) -> Unit,
    onConsumeMessage: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier, onReadPhoto: () -> Unit = {}) {
    val form = state.form
    val lost = form.type == ItemType.LOST
    var dateOpen by rememberSaveable { mutableStateOf(false) }; var categoryOpen by rememberSaveable { mutableStateOf(false) }
    var photoError by rememberSaveable { mutableStateOf<String?>(null) }; var leaveOpen by rememberSaveable { mutableStateOf(false) }
    var pinRequested by remember { mutableStateOf(false) }
    val (granted, askLocation) = rememberPermissionAsk(LocationPermissions,
        "FindIt uses your current position only when you add a report pin. Choose precise location for a more accurate pin.",
        "Enable Location in Settings, or enter the building name without a pin.")
    LaunchedEffect(granted, pinRequested) { if (granted && pinRequested) { pinRequested = false; onUseLocation() } }
    val back = { if (form.title.isNotBlank() || form.description.isNotBlank() || form.photoPath != null) leaveOpen = true else onBack() }
    BackHandler(onBack = back)
    Scaffold(modifier, topBar = { PageTopBar(if (lost) "Report a lost item" else "Post a found item", back) },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Button({ onSave(false) }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !state.saving && !state.reading) { Text(if (state.saving) "Posting…" else if (lost) "Post lost report" else "Post found item") }
                }
            }
        }) { padding ->
        ReadingPane(Modifier.padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { IllustratedHeading(if (lost) "Let's bring\nit back." else "Someone's\nmissing this.", if (lost) "A clear photo and a few details make all the difference." else "Help its owner recognize it. Keep unique details private.", R.drawable.art_report) }
            item { WorkflowSteps(listOf("Photo", "Details", "Location"),
                if (form.zoneName.isNotBlank()) 2 else if (form.title.isNotBlank()) 1 else 0) }
            item { FormCard {
                SectionHeading(Icons.Outlined.AddAPhoto, "Add a photo", "01")
                if (form.photoPath != null) {
                    ItemPhoto(form.photoPath, form.title.ifBlank { "Item" }, form.category, MaterialTheme.colorScheme.tertiary, Modifier.fillMaxWidth().height(180.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onReadPhoto, enabled = !state.reading, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (state.reading) "Reading…" else "Read writing with AI")
                        }
                        IconButton(onClearPhoto, enabled = !state.reading) { Icon(Icons.Outlined.DeleteOutline, "Remove photo") }
                    }
                } else {
                    TrustNote(if (lost) "Have a photo of your item? It helps the finder compare evidence." else "Photograph the item, keeping private information out of view.")
                }
                PhotoActions(onPhoto, { photoError = it }, enabled = !state.saving && !state.reading)
                if (form.scannedText.isNotBlank()) Notice("Recognized text: ${form.scannedText}")
            } }
            item { FormCard {
                SectionHeading(Icons.Outlined.Inventory2, "The item", "02")
                OutlinedTextField(form.title, onTitle, Modifier.fillMaxWidth(), label = { Text("Item name") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, null) }, shape = MaterialTheme.shapes.small, placeholder = { Text("e.g. Navy bottle with a sticker") }, singleLine = true, supportingText = { Text("${form.title.length}/60") })
                OutlinedButton({ categoryOpen = true }, Modifier.fillMaxWidth()) {
                    Icon(categoryIcon(form.category), null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(form.category, Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore, null)
                }
                OutlinedTextField(form.description, onDescription, Modifier.fillMaxWidth(), label = { Text("Details people can recognize") }, shape = MaterialTheme.shapes.small, minLines = 3, supportingText = { Text("${form.description.length}/400 · Don't include private identifiers") })
            } }
            item { FormCard {
                SectionHeading(Icons.Outlined.Place, if (lost) "Where & when was it lost?" else "Where & when did you find it?", "03")
                OutlinedTextField(form.zoneName, onPlaceName, Modifier.fillMaxWidth(), label = { Text("Place on campus") }, leadingIcon = { Icon(Icons.Outlined.Place, null) }, shape = MaterialTheme.shapes.small, placeholder = { Text("Building, library, or campus area") }, singleLine = true)
                FilledTonalButton({ if (granted) onUseLocation() else { pinRequested = true; askLocation() } }, Modifier.fillMaxWidth(), enabled = !state.locating) {
                    Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (state.locating) "Getting a fresh position…" else if (form.latitude != null) "Refresh my location pin" else "Add my current location")
                }
                if (form.latitude != null && form.longitude != null) {
                    Text(String.format(Locale.US, "%.6f, %.6f", form.latitude, form.longitude), style = MaterialTheme.typography.labelMedium)
                    state.accuracyMeters?.let { Text("GPS accuracy: approximately ${it.toInt()} m", style = MaterialTheme.typography.bodySmall) }
                    if (form.placeLabel.isNotBlank()) Text(form.placeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Only add your current pin if this is where the item was lost or found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton({ dateOpen = true }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(formatFull(form.whenMillis)) }
            } }
            item { FormCard {
                SectionHeading(Icons.Outlined.VerifiedUser, "Keep one detail private")
                OutlinedTextField(form.privateDetail, onPrivateDetail, Modifier.fillMaxWidth(), label = { Text("Unique detail (optional)") }, shape = MaterialTheme.shapes.small, supportingText = { Text("A mark or identifier only you know. Hidden from the board.") })
                TrustNote("Students contact you through private messages. Both of you check evidence before confirming the return.")
            } }
        }
    }
    }
    (state.message ?: photoError)?.let { message -> AlertDialog(onDismissRequest = { onConsumeMessage(); photoError = null }, title = { Text("Your report") }, text = { Text(message) },
        confirmButton = { TextButton({ onConsumeMessage(); photoError = null }) { Text("Got it") } }) }
    state.duplicate?.let { duplicate ->
        AlertDialog(onDismissRequest = onDismissDuplicate, title = { Text("A possible match is nearby") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${duplicate.title} is already on your campus board. Compare it before adding another report.")
                OutlinedButton({ onViewDuplicate(duplicate.itemId) }, Modifier.fillMaxWidth()) { Text("View matching report") }
            } }, confirmButton = { TextButton({ onSave(true) }) { Text("Post anyway") } }, dismissButton = { TextButton(onDismissDuplicate) { Text("Keep editing") } })
    }
    if (categoryOpen) ModalBottomSheet(onDismissRequest = { categoryOpen = false }) {
        LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
            item { Text("Choose a category", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp)) }
            items(state.categories) { name -> ListItem(headlineContent = { Text(name) }, leadingContent = { Icon(categoryIcon(name), null) }, trailingContent = { if (name == form.category) Icon(Icons.Outlined.CheckCircle, null) },
                modifier = Modifier.fillMaxWidth().then(Modifier.clickableCategory { onCategory(name); categoryOpen = false })) }
        }
    }
    if (dateOpen) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = form.whenMillis)
        DatePickerDialog(onDismissRequest = { dateOpen = false }, confirmButton = { TextButton({ picker.selectedDateMillis?.let { onWhen(datePickerMillisToLocal(it)) }; dateOpen = false }) { Text("Set date") } },
            dismissButton = { TextButton({ dateOpen = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
    if (leaveOpen) AlertDialog(onDismissRequest = { leaveOpen = false }, title = { Text("Leave this report?") }, text = { Text("Your report hasn't been posted yet.") },
        confirmButton = { TextButton({ leaveOpen = false; onBack() }) { Text("Leave") } }, dismissButton = { TextButton({ leaveOpen = false }) { Text("Keep editing") } })
}
private fun Modifier.clickableCategory(action: () -> Unit) = this.then(Modifier.clickable(onClick = action))
