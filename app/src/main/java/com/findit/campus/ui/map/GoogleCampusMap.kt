package com.findit.campus.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.findit.campus.location.DeviceFix
import com.findit.campus.model.Item
import com.findit.campus.model.ItemType
import com.findit.campus.R
import com.findit.campus.data.local.TrailPoint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
fun GoogleCampusMap(
    items: List<Item>,
    user: DeviceFix?,
    focus: MapFocus,
    selectedId: Long?,
    myLocationEnabled: Boolean,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    trail: List<TrailPoint> = emptyList(),
    recenterRequest: Int = 0,
    onMapLoaded: () -> Unit = {},
) {
    val campusAnchor = items.firstOrNull { it.hasCoordinates }?.let { LatLng(it.latitude, it.longitude) }
    val anchor = campusAnchor
        ?: user?.let { LatLng(it.latitude, it.longitude) }
        ?: trail.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(0.0, 0.0)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(anchor, 16f)
    }
    var following by remember { mutableStateOf(focus == MapFocus.NEAR_ME) }
    var mapReady by remember { mutableStateOf(false) }
    LaunchedEffect(camera) {
        snapshotFlow { camera.cameraMoveStartedReason }.collect { reason ->
            if (reason == CameraMoveStartedReason.GESTURE) following = false
        }
    }
    LaunchedEffect(focus, recenterRequest) { following = focus == MapFocus.NEAR_ME }
    val target = if (focus == MapFocus.NEAR_ME && user != null) {
            LatLng(user.latitude, user.longitude)
        } else {
            campusAnchor ?: anchor
        }
    LaunchedEffect(mapReady, focus, recenterRequest, campusAnchor, if (following) target else null) {
        if (!mapReady) return@LaunchedEffect
        if (focus == MapFocus.CAMPUS || following) {
            camera.animate(CameraUpdateFactory.newLatLngZoom(target, if (focus == MapFocus.NEAR_ME) 17f else 16f))
        }
    }
    val context = LocalContext.current
    val style = remember(context) { MapStyleOptions.loadRawResourceStyle(context, R.raw.campus_map_style) }
    val routeColor = MaterialTheme.colorScheme.primary
    val segments = remember(trail) { trail.groupBy { it.segmentId }.values.map { points -> points.map { LatLng(it.latitude, it.longitude) } } }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(isMyLocationEnabled = myLocationEnabled, mapStyleOptions = style),
        uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false, mapToolbarEnabled = false),
        onMapLoaded = {
            mapReady = true
            onMapLoaded()
        },
    ) {
        user?.takeIf { it.accuracyMeters.isFinite() && it.accuracyMeters > 0 }?.let {
            Circle(center = LatLng(it.latitude, it.longitude), radius = it.accuracyMeters.toDouble(),
                fillColor = routeColor.copy(alpha = .10f), strokeColor = routeColor.copy(alpha = .25f), strokeWidth = 1f)
        }
        segments.filter { it.size > 1 }.forEach { points ->
            Polyline(points = points, color = routeColor, width = 10f, geodesic = true)
        }
        items.filter { it.hasCoordinates }.forEach { item ->
            key(item.id) {
                Marker(
                    state = rememberUpdatedMarkerState(
                        position = LatLng(item.latitude, item.longitude),
                    ),
                    title = item.title,
                    snippet = item.zoneName,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when {
                            item.id == selectedId -> BitmapDescriptorFactory.HUE_GREEN
                            item.type == ItemType.LOST -> BitmapDescriptorFactory.HUE_ORANGE
                            else -> BitmapDescriptorFactory.HUE_AZURE
                        },
                    ),
                    onClick = {
                        onSelect(item.id)
                        true
                    },
                )
            }
        }
    }
}
