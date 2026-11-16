package com.findit.campus.ui.map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findit.campus.location.Campus
import com.findit.campus.location.DeviceFix
import com.findit.campus.location.MapBounds
import com.findit.campus.location.distanceMeters
import com.findit.campus.location.enclosingBounds
import com.findit.campus.model.Item
import com.findit.campus.model.ItemType
import com.findit.campus.ui.theme.FindItPalette
import kotlin.math.roundToInt

@Composable
fun CampusMap(
    items: List<Item>,
    user: DeviceFix?,
    focus: MapFocus,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bounds = rememberBounds(items, user, focus)
    val palette = FindItPalette.colors
    val land = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val pathColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = with(LocalDensity.current) { 11.sp.toPx() }
        color = labelColor.toArgb()
    }
    BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val padding = with(density) { 28.dp.toPx() }
        fun project(latitude: Double, longitude: Double): Offset {
            val widthSpan = (bounds.east - bounds.west).takeIf { it > 1e-8 } ?: 1e-4
            val heightSpan = (bounds.north - bounds.south).takeIf { it > 1e-8 } ?: 1e-4
            val x = padding + ((longitude - bounds.west) / widthSpan).toFloat() * (widthPx - padding * 2)
            val y = padding + ((bounds.north - latitude) / heightSpan).toFloat() * (heightPx - padding * 2)
            return Offset(x, y)
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(land)
        }
        items.forEach { item ->
            val point = project(item.latitude, item.longitude)
            val selected = item.id == selectedId
            val scale by animateFloatAsState(if (selected) 1.25f else 1f, label = "marker")
            val color = if (item.type == ItemType.LOST) palette.lost else palette.found
            val markerSize = 18.dp * scale
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (point.x - with(density) { markerSize.toPx() } / 2f).roundToInt(),
                            (point.y - with(density) { markerSize.toPx() } / 2f).roundToInt(),
                        )
                    }
                    .size(markerSize)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(item.id) }
                    .semantics { contentDescription = "${item.title}, ${item.zoneName}" },
            )
        }
        if (user != null && shouldDrawUser(user, focus)) {
            val point = project(user.latitude, user.longitude)
            val pulse by rememberInfiniteTransition(label = "puck").animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
                label = "pulse",
            )
            Box(
                modifier = Modifier
                    .offset {
                        val size = with(density) { 22.dp.toPx() }
                        IntOffset((point.x - size / 2f).roundToInt(), (point.y - size / 2f).roundToInt())
                    }
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f * pulse)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun rememberBounds(items: List<Item>, user: DeviceFix?, focus: MapFocus): MapBounds {
    val points = mutableListOf<Pair<Double, Double>>()
    val located = items.filter { it.hasCoordinates }
    if (focus == MapFocus.NEAR_ME && user != null) {
        points += user.latitude to user.longitude
        located.filter { distanceMeters(it.latitude, it.longitude, user.latitude, user.longitude) < 3_000 }
            .forEach { points += it.latitude to it.longitude }
    } else {
        located.forEach { points += it.latitude to it.longitude }
        if (user != null) points += user.latitude to user.longitude
    }
    return enclosingBounds(points.ifEmpty { listOf(0.0 to 0.0) })
}

private fun shouldDrawUser(user: DeviceFix, focus: MapFocus): Boolean = true
