package com.findit.campus.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.findit.campus.BuildConfig
import com.findit.campus.R
import com.findit.campus.model.ItemType
import com.findit.campus.ui.components.*
import com.findit.campus.ui.permission.*
import java.util.Locale

@Composable
fun MapScreen(state: MapUiState, onFocus: (MapFocus) -> Unit, onType: (ItemType?) -> Unit,
    onSelect: (Long) -> Unit, onClear: () -> Unit, onOpen: (Long) -> Unit,
    onTracking: (Boolean) -> Unit, onPermission: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val (granted, ask) = rememberPermissionAsk(LocationPermissions,
        "FindIt uses your position while this screen is open. Precise access improves GPS accuracy.",
        "Enable location in Settings to see your position. Report pins remain available.")
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
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IllustratedHeading("Around\nyour campus", "Real report pins. A little closer to finding it.", R.drawable.art_map)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(state.fix?.let { String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude) } ?: if (granted) "Waiting for GPS…" else "Location access is off", style = MaterialTheme.typography.labelMedium)
                state.fix?.let { Text("Accuracy approximately ${it.accuracyMeters.toInt()} m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            IconButton({ if (granted) { onPermission(true); onFocus(MapFocus.NEAR_ME) } else ask() }) { Icon(Icons.Outlined.Refresh, "Refresh my location") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(state.focus == MapFocus.NEAR_ME, { if (granted) onFocus(MapFocus.NEAR_ME) else ask() }, label = { Text("Near me") }) }
            item { FilterChip(state.focus == MapFocus.CAMPUS, { onFocus(MapFocus.CAMPUS) }, label = { Text("Campus") }) }
            items(listOf("All", "Lost", "Found")) { name ->
                val type = when (name) { "Lost" -> ItemType.LOST; "Found" -> ItemType.FOUND; else -> null }
                FilterChip(state.type == type, { onType(type) }, label = { Text(name) })
            }
        }
        Surface(Modifier.fillMaxWidth().weight(1f), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Box {
                if (located.isNotEmpty() || state.fix != null) {
                    if (BuildConfig.MAPS_API_KEY.isNotBlank()) GoogleCampusMap(located, state.fix, state.focus, state.selectedId, granted, onSelect, Modifier.fillMaxSize())
                    else CampusMap(located, state.fix, state.focus, state.selectedId, onSelect, Modifier.fillMaxSize())
                } else {
                    Column(Modifier.align(Alignment.Center).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.LocationOn, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("No location pins yet", style = MaterialTheme.typography.titleMedium)
                        Text("Add a location when reporting an item, or enable GPS to see your position.", style = MaterialTheme.typography.bodyMedium)
                        if (!granted) TextButton(ask) { Text("Allow location") }
                    }
                }
                if (selected != null) Surface(Modifier.align(Alignment.BottomCenter).padding(12.dp), shape = MaterialTheme.shapes.medium, shadowElevation = 3.dp) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(selected.title, style = MaterialTheme.typography.titleMedium); Text(selected.zoneName, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ onOpen(selected.id) }) { Text("View report") }; TextButton(onClear) { Text("Close") } }
                    }
                }
            }
        }
        if (BuildConfig.MAPS_API_KEY.isBlank()) Text("Pin preview · Live Google map becomes available when campus map services are connected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
