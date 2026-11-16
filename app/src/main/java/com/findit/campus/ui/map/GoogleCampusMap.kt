package com.findit.campus.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.findit.campus.location.DeviceFix
import com.findit.campus.model.Item
import com.findit.campus.model.ItemType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun GoogleCampusMap(
    items: List<Item>,
    user: DeviceFix?,
    focus: MapFocus,
    selectedId: Long?,
    myLocationEnabled: Boolean,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anchor = items.firstOrNull { it.hasCoordinates }?.let { LatLng(it.latitude, it.longitude) }
        ?: user?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(0.0, 0.0)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(anchor, 16f)
    }
    LaunchedEffect(focus, user?.latitude, user?.longitude, anchor) {
        val target = if (focus == MapFocus.NEAR_ME && user != null) {
            LatLng(user.latitude, user.longitude)
        } else {
            anchor
        }
        camera.animate(CameraUpdateFactory.newLatLngZoom(target, if (focus == MapFocus.NEAR_ME) 17f else 16f))
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(isMyLocationEnabled = myLocationEnabled),
        uiSettings = MapUiSettings(myLocationButtonEnabled = myLocationEnabled, zoomControlsEnabled = false),
    ) {
        items.forEach { item ->
            key(item.id) {
                Marker(
                    state = rememberMarkerState(
                        key = item.id.toString(),
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
