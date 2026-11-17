package com.findit.campus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared feature surface, so the main screens use one visual language in both themes. */
@Composable
fun FeaturePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = colors.inverseSurface, contentColor = colors.inverseOnSurface) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width * 1.05f, size.height * .25f)
                repeat(3) { index ->
                    drawCircle(colors.inverseOnSurface.copy(alpha = .055f),
                        radius = size.width * (.28f + index * .16f), center = center, style = Stroke(1.dp.toPx()))
                }
            }
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier,
    eyebrow: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            eyebrow?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}

@Composable
fun SectionHeading(icon: ImageVector, title: String, step: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, null, Modifier.padding(9.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
        step?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** The state is supplied by the screen; this is progress information, not a second navigation. */
@Composable
fun WorkflowSteps(labels: List<String>, active: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            val color by animateColorAsState(if (index <= active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline, tween(200), label = "workflow step")
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = CircleShape, color = if (index <= active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer) {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = color,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}
