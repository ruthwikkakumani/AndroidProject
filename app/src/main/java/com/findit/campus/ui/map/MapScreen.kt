package com.findit.campus.ui.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.findit.campus.BuildConfig
import com.findit.campus.R
import com.findit.campus.location.distanceMeters
import com.findit.campus.model.ItemType
import com.findit.campus.ui.components.*
import com.findit.campus.ui.permission.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(state: MapUiState, onFocus: (MapFocus) -> Unit, onType: (ItemType?) -> Unit,
    onSelect: (Long) -> Unit, onClear: () -> Unit, onOpen: (Long) -> Unit,
    onTracking: (Boolean) -> Unit, onPermission: (Boolean) -> Unit, modifier: Modifier = Modifier,
    onToggleTrail: () -> Unit = {}, onClearTrail: () -> Unit = {}) {
    val (granted, ask) = rememberPermissionAsk(LocationPermissions,
        "FindIt uses your position while this map is open. Precise access improves your walking trail.",
        "Enable location in Settings to see your position. Report pins remain available.")
    val feedback = LocalFeedback.current
    val context = LocalContext.current
    var clearDialog by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, granted) {
        onPermission(granted); onTracking(granted && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { onPermission(granted); onTracking(granted) }
            else if (event == Lifecycle.Event.ON_PAUSE) onTracking(false)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); onTracking(false) }
    }
    val located = state.items.filter { it.hasCoordinates }
    val selected = located.firstOrNull { it.id == state.selectedId }
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        val compact = maxHeight < 480.dp && maxWidth >= 560.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!compact) Text("CAMPUS EXPLORER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Around your campus", style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() })
                    if (!compact) Text("Find the pin. Retrace your steps.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!compact) Illustration(R.drawable.art_map, Modifier.size(72.dp))
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item { FilterChip(state.focus == MapFocus.NEAR_ME, { if (granted) onFocus(MapFocus.NEAR_ME) else ask() },
                    label = { Text("Near me") }, leadingIcon = { Icon(Icons.Outlined.MyLocation, null, Modifier.size(16.dp)) }) }
                item { FilterChip(state.focus == MapFocus.CAMPUS, { onFocus(MapFocus.CAMPUS) }, label = { Text("Campus") }) }
                items(listOf("All", "Lost", "Found")) { name ->
                    val type = when (name) { "Lost" -> ItemType.LOST; "Found" -> ItemType.FOUND; else -> null }
                    FilterChip(state.type == type, { onType(type) }, label = { Text(name) })
                }
            }
            val trailControl: @Composable (Modifier) -> Unit = { controlModifier ->
                TrailControls(state.trail, state.fix?.let { String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude) },
                    { if (granted) { onToggleTrail(); feedback(if (state.trail.recording) "Walking trail paused" else "Walking trail started") } else ask() },
                    { clearDialog = true }, controlModifier)
            }
            val mapContent: @Composable (Modifier) -> Unit = { mapModifier ->
                Surface(mapModifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))) {
                    Box {
                        if (located.isNotEmpty() || state.fix != null || state.trail.points.isNotEmpty()) {
                            if (BuildConfig.MAPS_API_KEY.isNotBlank()) GoogleCampusMap(located, state.fix, state.focus,
                                state.selectedId, granted, onSelect, Modifier.fillMaxSize(), state.trail.points, state.recenterRequest)
                            else CampusMap(located, state.fix, state.focus, state.selectedId, onSelect, Modifier.fillMaxSize(), state.trail.points)
                        } else Column(Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.LocationOn, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("No location pins yet", style = MaterialTheme.typography.titleMedium)
                            Text("Tag a report with a place, or turn on GPS to see yourself here.", style = MaterialTheme.typography.bodyMedium)
                            if (!granted) TextButton(ask) { Text("Allow location") }
                        }
                        Surface(Modifier.align(Alignment.TopStart).padding(12.dp), shape = MaterialTheme.shapes.medium,
                            shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = .96f)) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(if (BuildConfig.MAPS_API_KEY.isNotBlank()) "LIVE MAP" else "PIN PREVIEW", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(state.fix?.let { "You · ±${it.accuracyMeters.toInt()} m" }
                                    ?: if (granted) "Waiting for GPS…" else "Location is off", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        FilledTonalIconButton({ if (granted) { onPermission(true); onFocus(MapFocus.NEAR_ME) } else ask() },
                            Modifier.align(Alignment.TopEnd).padding(12.dp).size(48.dp)) {
                            Icon(Icons.Outlined.MyLocation, "Refresh my location")
                        }
                        Surface(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 28.dp),
                            shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface.copy(alpha = .95f)) {
                            Text("${located.size} ${if (located.size == 1) "report" else "reports"} · tap a pin", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (compact) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                mapContent(Modifier.weight(1f).fillMaxHeight())
                trailControl(Modifier.width(260.dp).verticalScroll(rememberScrollState()))
            } else {
                mapContent(Modifier.fillMaxWidth().weight(1f))
                trailControl(Modifier.fillMaxWidth())
            }
            if (BuildConfig.MAPS_API_KEY.isBlank()) Text("Pin preview · Connect campus maps to see streets.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (selected != null) ModalBottomSheet(onDismissRequest = onClear,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemPhoto(selected.photoPath, selected.title, selected.category, MaterialTheme.colorScheme.primary, Modifier.size(80.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (selected.type == ItemType.LOST) "LOST ITEM" else "FOUND ITEM", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(selected.title, style = MaterialTheme.typography.titleLarge)
                    Text(selected.zoneName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.fix?.let { fix -> Text("${formatTrailDistance(distanceMeters(fix.latitude, fix.longitude, selected.latitude, selected.longitude))} away · straight-line distance",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Button({ onClear(); onOpen(selected.id) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("View report") }
            OutlinedButton({
                if (!openWalkingDirections(context, selected.latitude, selected.longitude))
                    feedback("Install a maps app or browser to open directions")
            }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Walking directions")
            }
            TrustNote("Check ownership together and meet at a public campus spot.")
        }
    }
    if (clearDialog) AlertDialog(onDismissRequest = { clearDialog = false },
        icon = { Icon(Icons.Outlined.Route, null) }, title = { Text("Clear your walking trail?") },
        text = { Text("This removes your saved path from this device. Your reports and chats stay available.") },
        confirmButton = { TextButton({ clearDialog = false; onClearTrail(); feedback("Walking trail cleared") }) { Text("Clear trail") } },
        dismissButton = { TextButton({ clearDialog = false }) { Text("Keep trail") } })
}

@Composable
private fun TrailControls(trail: TrailUiState, coordinates: String?, onToggle: () -> Unit, onClear: () -> Unit, modifier: Modifier) {
    Surface(modifier.animateContentSize(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Outlined.Route, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Your walking trail", style = MaterialTheme.typography.titleMedium)
                    Text(if (trail.recording) "Recording · ${formatTrailDistance(trail.distanceMeters)}" else if (trail.points.isEmpty()) "Retrace where you searched"
                        else "Paused · ${formatTrailDistance(trail.distanceMeters)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (trail.points.isNotEmpty()) IconButton(onClear, enabled = !trail.busy) { Icon(Icons.Outlined.DeleteOutline, "Clear saved trail") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onToggle, Modifier.weight(1f).heightIn(min = 48.dp), enabled = !trail.busy) {
                    AnimatedContent(trail.recording, label = "trail button") { recording ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(if (recording) Icons.Outlined.Pause else Icons.AutoMirrored.Outlined.DirectionsWalk, null, Modifier.size(18.dp))
                            Text(if (recording) "Pause trail" else if (trail.points.isEmpty()) "Start trail" else "Resume trail")
                        }
                    }
                }
            }
            trail.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Text("Private on this device · records only while this map is open", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            coordinates?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

fun formatTrailDistance(meters: Double): String = if (meters >= 1_000) String.format(Locale.US, "%.1f km", meters / 1_000)
    else "${meters.toInt()} m"
