package com.findit.campus.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.findit.campus.FindItApplication
import com.findit.campus.model.Item
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.ui.format.formatWhen
import com.findit.campus.ui.theme.FindItPalette
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun categoryIcon(name: String): ImageVector = when (name) {
    "Bottles" -> FindItIcons.Bottle
    "Books" -> Icons.AutoMirrored.Outlined.MenuBook
    "Electronics" -> Icons.Outlined.Devices
    "Clothing" -> Icons.Outlined.Checkroom
    "Keys" -> Icons.Outlined.Key
    "IDs" -> Icons.Outlined.Badge
    "Accessories" -> Icons.Outlined.Watch
    "Bags" -> Icons.Outlined.Backpack
    else -> Icons.Outlined.Category
}

@Composable
fun TypeBadge(type: ItemType, status: ItemStatus, modifier: Modifier = Modifier) {
    val palette = FindItPalette.colors
    val (label, container, content) = when {
        status == ItemStatus.REUNITED -> Triple("Reunited", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        type == ItemType.LOST -> Triple("Lost", palette.lostContainer, palette.lost)
        else -> Triple("Found", palette.foundContainer, palette.found)
    }
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun ItemPhoto(
    path: String?,
    title: String,
    category: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var image by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        image = withContext(Dispatchers.IO) {
            if (path.isNullOrBlank()) {
                null
            } else {
                val bitmap: Bitmap? = runCatching { (context.applicationContext as FindItApplication).graph.photos.loadDisplay(path) }.getOrNull()
                bitmap?.asImageBitmap()
            }
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image!!,
                contentDescription = "Photo of $title",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
fun ItemCard(
    item: Item,
    matchCount: Int,
    onClick: () -> Unit,
    onFound: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = FindItPalette.colors
    val tint = if (item.type == ItemType.LOST) palette.lost else palette.found
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ItemPhoto(
                path = item.photoPath,
                title = item.title,
                category = item.category,
                tint = tint,
                modifier = Modifier.size(76.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TypeBadge(item.type, item.status)
                }
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.zoneName.ifBlank { "Campus" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = formatWhen(item.createdAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (matchCount > 0 && item.status == ItemStatus.OPEN) {
                    Text(
                        text = if (matchCount == 1) "1 possible match" else "$matchCount possible matches",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.reunion,
                    )
                }
                if (onFound != null && item.type == ItemType.LOST && item.status == ItemStatus.OPEN) {
                    Button(
                        onClick = onFound,
                        modifier = Modifier.padding(top = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                        ),
                    ) { Text("Found it") }
                }
            }
        }
    }
}

@Composable
fun EmptyBoard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun BoardItemCard(item: Item, onClick: () -> Unit, onAction: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                ItemPhoto(item.photoPath, item.title, item.category, colors.tertiary,
                    Modifier.fillMaxWidth().height(128.dp))
                TypeBadge(item.type, item.status, Modifier.padding(6.dp))
            }
            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, null, Modifier.size(13.dp), tint = colors.onSurfaceVariant)
                Text(item.zoneName.ifBlank { "Campus" }, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatWhen(item.createdAt), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            if (onAction != null && item.status == ItemStatus.OPEN) {
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
                    shape = RoundedCornerShape(12.dp)) {
                    Text(if (item.type == ItemType.LOST) "Found it" else "This is mine", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (item.status == ItemStatus.REUNITED) "View return" else "View report", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
