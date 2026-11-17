package com.findit.campus.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.findit.campus.BuildConfig
import com.findit.campus.data.MeetingPlan
import com.findit.campus.data.PrivateContact
import com.findit.campus.data.local.MessageEntity
import com.findit.campus.location.MeetingPin
import com.findit.campus.location.distanceMeters
import com.findit.campus.ui.components.*
import com.findit.campus.ui.format.formatWhen
import com.findit.campus.ui.map.formatTrailDistance
import com.findit.campus.ui.map.openWalkingDirections
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

/** An ended share wins over an earlier live fix, including when timestamps are equal. */
internal fun latestMeetingMessages(messages: List<MessageEntity>): List<MessageEntity> = messages
    .sortedWith(compareByDescending<MessageEntity> { it.createdAt }.thenByDescending { it.id })
    .filter { MeetingPin.decode(it.body) != null }.distinctBy { it.senderId }

@Composable
internal fun rememberMeetingClock(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(15_000); value = System.currentTimeMillis() }
    }
    return now
}

@Composable
internal fun LiveDot(live: Boolean, color: Color = MaterialTheme.colorScheme.primary) {
    val scale = if (live) {
        val transition = rememberInfiniteTransition(label = "Live location")
        val pulse by transition.animateFloat(.8f, 1.25f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "Location pulse")
        pulse
    } else 1f
    Box(Modifier.size(9.dp).scale(scale).clip(CircleShape).background(if (live) color else MaterialTheme.colorScheme.outline))
}

@Composable
internal fun ContactCard(contact: PrivateContact) {
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.ContactPhone, null, tint = MaterialTheme.colorScheme.primary)
        Text("Private contact", style = MaterialTheme.typography.titleSmall)
    }
    Text(contact.phone, style = MaterialTheme.typography.titleMedium)
    if (contact.note.isNotBlank()) Text(contact.note, style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton({
            val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", contact.dialNumber, null))
            if (runCatching { context.startActivity(intent) }.isFailure) feedback("No phone dialer is available on this device")
        }) { Icon(Icons.Outlined.Call, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Call") }
        TextButton({
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Private contact", contact.phone))
            feedback("Phone number copied")
        }) { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copy") }
    }
}

@Composable
internal fun MeetingPlanCard(plan: MeetingPlan) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Handshake, null, tint = MaterialTheme.colorScheme.primary)
        Text("Let's meet", style = MaterialTheme.typography.titleSmall)
    }
    Text(plan.place, style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp))
        Text(plan.time, style = MaterialTheme.typography.bodyMedium)
    }
    if (plan.note.isNotBlank()) Text(plan.note, style = MaterialTheme.typography.bodySmall)
    Text("Proposed · agree together in chat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatDetailsSheet(state: ChatUiState, model: ChatActions, initialPage: String, onDismiss: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var phone by rememberSaveable { mutableStateOf("") }
    var contactNote by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val contactValid = PrivateContact.validPhone(phone)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(if (page == "contact") Icons.Outlined.ContactPhone else Icons.Outlined.Handshake, null, Modifier.padding(14.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Make the return easier", style = MaterialTheme.typography.titleLarge)
                    Text("Only you and ${state.personName}", style = MaterialTheme.typography.bodySmall)
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(page == "contact", { page = "contact" }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Contact") }
                SegmentedButton(page == "meeting", { page = "meeting" }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Meeting plan") }
            }
            if (page == "contact") {
                Text("Share a number only if you're comfortable. It stays in this private conversation.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(phone, { phone = it.take(32) }, Modifier.fillMaxWidth(), label = { Text("Phone number") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true,
                    isError = phone.isNotBlank() && !contactValid, supportingText = { Text("Include country code for international calls") })
                OutlinedTextField(contactNote, { contactNote = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Availability or extra details (optional)") }, maxLines = 3)
                Button({ model.shareContact(phone, contactNote); onDismiss() }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = contactValid && !state.busy && state.onlineAccount) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Share contact privately")
                }
            } else {
                Text("Suggest a public spot and a time. The other student can agree or suggest a change in chat.", style = MaterialTheme.typography.bodyMedium)
                LazyMeetingPlaces(onPlace = { place = it })
                OutlinedTextField(place, { place = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Public meeting place") }, leadingIcon = { Icon(Icons.Outlined.LocationOn, null) }, maxLines = 2)
                OutlinedTextField(time, { time = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Day and time") }, placeholder = { Text("Today, 3:30 PM") }, leadingIcon = { Icon(Icons.Outlined.Schedule, null) }, maxLines = 2)
                OutlinedTextField(note, { note = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("How to find you (optional)") }, placeholder = { Text("I'll wait beside the security desk") }, maxLines = 4)
                Button({ model.shareMeeting(place, time, note); onDismiss() }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = place.isNotBlank() && time.isNotBlank() && !state.busy && state.onlineAccount) {
                    Icon(Icons.Outlined.Handshake, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Send meeting plan")
                }
            }
            if (!state.onlineAccount) Notice("Connect your account from Home before sharing with another phone.", error = true)
        }
    }
}

@Composable
private fun LazyMeetingPlaces(onPlace: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(3) { index ->
            val place = listOf("Library entrance", "Campus security", "Student services")[index]
            SuggestionChip({ onPlace(place) }, label = { Text(place) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeetingMapSheet(state: ChatUiState, messages: List<MessageEntity>, now: Long, onDismiss: () -> Unit, onShare: () -> Unit) {
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    val pins = messages.mapNotNull { m -> MeetingPin.decode(m.body)?.let { m.senderId to it } }.toMap()
    val mine = pins[state.viewerId]
    val peer = pins.entries.firstOrNull { it.key != state.viewerId }?.value
    val points = pins.values.map { LatLng(it.latitude, it.longitude) }
    val camera = rememberCameraPositionState { points.firstOrNull()?.let { position = CameraPosition.fromLatLngZoom(it, 16f) } }
    var mapLoaded by remember { mutableStateOf(false) }
    var recenter by remember { mutableIntStateOf(0) }
    LaunchedEffect(points, mapLoaded, recenter) {
        if (mapLoaded && points.isNotEmpty()) {
            if (points.distinct().size == 1) camera.animate(CameraUpdateFactory.newLatLngZoom(points.first(), 16f))
            else camera.animate(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().apply { points.forEach(::include) }.build(), 100))
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val mapHeight = if (maxHeight < 620.dp) 220.dp else 300.dp
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Meet halfway", style = MaterialTheme.typography.titleLarge)
                        Text("Locations shared in this chat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.Handshake, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                }
                if (BuildConfig.MAPS_API_KEY.isNotBlank() && points.isNotEmpty()) Surface(shape = MaterialTheme.shapes.large) {
                    Box {
                        GoogleMap(Modifier.fillMaxWidth().height(mapHeight), cameraPositionState = camera, onMapLoaded = { mapLoaded = true },
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)) {
                            pins.forEach { (sender, pin) ->
                                val point = LatLng(pin.latitude, pin.longitude)
                                val own = sender == state.viewerId
                                val color = if (own) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                Marker(state = rememberUpdatedMarkerState(point), title = if (own) "You" else state.personName,
                                    snippet = "GPS ±${pin.accuracyMeters.toInt()} m", icon = BitmapDescriptorFactory.defaultMarker(if (own) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_AZURE))
                                Circle(center = point, radius = pin.accuracyMeters.toDouble(), fillColor = color.copy(alpha = .13f), strokeColor = color, strokeWidth = 2f)
                            }
                            if (points.size == 2) Polyline(points = points, color = MaterialTheme.colorScheme.primary.copy(alpha = .45f), width = 4f,
                                pattern = listOf(com.google.android.gms.maps.model.Dash(16f), com.google.android.gms.maps.model.Gap(12f)))
                        }
                        FilledTonalIconButton({ recenter++ }, Modifier.align(Alignment.TopEnd).padding(12.dp)) { Icon(Icons.Outlined.CenterFocusStrong, "Show both locations") }
                    }
                } else Notice(if (points.isEmpty()) "Share a location to see a meeting pin." else "Map tiles are unavailable. Shared coordinates and directions still work.")
                if (mine != null && peer != null) Text("${formatTrailDistance(distanceMeters(mine.latitude, mine.longitude, peer.latitude, peer.longitude))} between shared pins · straight-line distance",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                LocationPerson("You", mine, now, MaterialTheme.colorScheme.primary)
                LocationPerson(state.personName, peer, now, MaterialTheme.colorScheme.tertiary)
                if (peer != null) Button({ if (!openWalkingDirections(context, peer.latitude, peer.longitude)) feedback("Install a maps app or browser to open directions") }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Outlined.Route, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Directions to ${state.personName}")
                }
                OutlinedButton(onShare, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (mine == null) "Share my location" else "Manage my location") }
                Text("Live updates arrive while both chats are open and connected. Stale pins show their last update. The dotted line is not a walking route.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LocationPerson(name: String, pin: MeetingPin?, now: Long, color: Color) {
    Surface(Modifier.testTag("meeting-person-$name"), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LiveDot(pin?.live == true && pin.recent(now), color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(if (pin == null) "Not shared yet" else if (pin.live && pin.recent(now)) "Live update · ±${pin.accuracyMeters.toInt()} m" else "Last shared · ${formatWhen(pin.capturedAt)} · ±${pin.accuracyMeters.toInt()} m",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (pin != null) Text("%.5f, %.5f".format(java.util.Locale.US, pin.latitude, pin.longitude), style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Outlined.PersonPinCircle, null, tint = color)
        }
    }
}
