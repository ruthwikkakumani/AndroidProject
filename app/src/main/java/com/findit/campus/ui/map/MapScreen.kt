package com.findit.campus.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findit.campus.BuildConfig
import com.findit.campus.location.distanceMeters
import com.findit.campus.location.formatDistance
import com.findit.campus.model.ItemType
import com.findit.campus.ui.components.TypeBadge
import com.findit.campus.ui.permission.LocationPermissions
import com.findit.campus.ui.permission.rememberPermissionAsk

@Composable
fun MapScreen(
    state: MapUiState,
    onFocus: (MapFocus) -> Unit,
    onType: (ItemType?) -> Unit,
    onSelect: (Long) -> Unit,
    onClear: () -> Unit,
    onOpen: (Long) -> Unit,
    onTracking: (Boolean) -> Unit,
    onPermission: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (granted, ask) = rememberPermissionAsk(
        permissions = LocationPermissions,
        rationale = "FindIt uses your location to show where you are against campus reports.",
        settingsMessage = "Location access is off. Enable it in Settings to see your spot on the map.",
    )
    DisposableEffect(granted) {
        onPermission(granted)
        onTracking(true)
        onDispose { onTracking(false) }
    }
    val selected = state.items.firstOrNull { it.id == state.selectedId }
    val itemPins = state.items.any { it.hasCoordinates }
    Box(modifier = modifier.fillMaxSize()) {
        if (!itemPins) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("No lost-item pins yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.fix != null) {
                        "You are at ${String.format(java.util.Locale.US, "%.5f, %.5f", state.fix.latitude, state.fix.longitude)}. Pins appear when a report includes a location."
                    } else {
                        "Allow location, or add a pin when you report a lost item."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!granted) TextButton(onClick = ask) { Text("Allow location") }
            }
        } else if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
            GoogleCampusMap(
                items = state.items,
                user = state.fix,
                focus = state.focus,
                selectedId = state.selectedId,
                myLocationEnabled = granted,
                onSelect = onSelect,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CampusMap(
                items = state.items,
                user = state.fix,
                focus = state.focus,
                selectedId = state.selectedId.takeIf { it > 0 },
                onSelect = onSelect,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = state.focus == MapFocus.CAMPUS, onClick = { onFocus(MapFocus.CAMPUS) }, label = { Text("Campus") })
                FilterChip(selected = state.focus == MapFocus.NEAR_ME, onClick = {
                    if (granted) onFocus(MapFocus.NEAR_ME) else ask()
                }, label = { Text("Near me") })
                FilterChip(selected = state.type == null, onClick = { onType(null) }, label = { Text("All") })
                FilterChip(selected = state.type == ItemType.LOST, onClick = { onType(ItemType.LOST) }, label = { Text("Lost") })
                FilterChip(selected = state.type == ItemType.FOUND, onClick = { onType(ItemType.FOUND) }, label = { Text("Found") })
            }
            if (!granted) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Location is off", modifier = Modifier.weight(1f))
                        TextButton(onClick = ask) { Text("Allow") }
                    }
                }
            }
        }
        selected?.let { item ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TypeBadge(item.type, item.status)
                    }
                    val distance = state.fix?.let { fix ->
                        formatDistance(distanceMeters(fix.latitude, fix.longitude, item.latitude, item.longitude))
                    }
                    Text(
                        listOfNotNull(item.zoneName, distance).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpen(item.id) }) { Text("Open") }
                        TextButton(onClick = onClear) { Text("Close") }
                    }
                }
            }
        }
    }
}
