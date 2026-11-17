package com.findit.campus.ui.detail

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.findit.campus.model.Item
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.model.matchLabel
import com.findit.campus.ui.components.ItemPhoto
import com.findit.campus.ui.components.SectionTitle
import com.findit.campus.ui.components.TypeBadge
import com.findit.campus.ui.format.formatFull
import com.findit.campus.ui.format.shareText
import com.findit.campus.ui.home.MatchPreview
import com.findit.campus.ui.theme.FindItPalette

private enum class Pending { REUNITE, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onOpenMatch: (Long) -> Unit,
    onReunite: () -> Unit,
    onDelete: () -> Unit,
    viewerId: Long = 0,
    onReachOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.item?.title ?: "Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val item = state.item
        if (item == null) {
            Text(
                text = if (state.missing) "This report is no longer on the board." else "Loading report",
                modifier = Modifier.padding(padding).padding(20.dp),
            )
        } else {
            ItemDetailBody(
                item = item,
                matches = state.matches,
                onOpenMatch = onOpenMatch,
                onReunite = onReunite,
                onDelete = onDelete,
                viewerId = viewerId,
                onReachOut = onReachOut,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
fun ItemDetailBody(
    item: Item,
    matches: List<MatchPreview>,
    onOpenMatch: (Long) -> Unit,
    onReunite: () -> Unit,
    onDelete: () -> Unit,
    viewerId: Long = 0,
    onReachOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val palette = FindItPalette.colors
    val tint = if (item.type.name == "LOST") palette.lost else palette.found
    var pending by remember { mutableStateOf<Pending?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ItemPhoto(
                path = item.photoPath,
                title = item.title,
                category = item.category,
                tint = tint,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeBadge(item.type, item.status)
                Text(item.category, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            }
        }
        item { Text(item.description, style = MaterialTheme.typography.bodyLarge) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.zoneName, style = MaterialTheme.typography.titleMedium)
                if (item.placeLabel.isNotBlank() && item.placeLabel != item.zoneName) {
                    Text(item.placeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatFull(item.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.scannedText.isNotBlank()) {
                    Text("Writing on the item: ${item.scannedText}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (item.contact.isNotBlank()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Contact: ${item.contact}", modifier = Modifier.weight(1f))
                    if (item.contact.any { it.isDigit() }) {
                        TextButton(onClick = {
                            val number = item.contact.filter { it.isDigit() || it == '+' }
                            context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
                        }) {
                            Icon(Icons.Outlined.Call, contentDescription = null)
                            Text("Call")
                        }
                    }
                }
            }
        }
        item {
            if (viewerId > 0 && item.ownerId != viewerId && item.status == ItemStatus.OPEN) {
                Button(onClick = onReachOut, modifier = Modifier.fillMaxWidth()) {
                    Text(if (item.type == ItemType.LOST) "I found this — message them" else "This is mine — message them")
                }
            }
            if (item.hasCoordinates) {
                Text(
                    "Exact pin: ${String.format(java.util.Locale.US, "%.5f, %.5f", item.latitude, item.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, item.shareText())
                    }
                    context.startActivity(Intent.createChooser(send, "Share report"))
                }) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Text("Share")
                }
                if (item.status == ItemStatus.OPEN) {
                    TextButton(onClick = { pending = Pending.REUNITE }) { Text("Mark reunited") }
                }
                TextButton(onClick = { pending = Pending.DELETE }) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text("Remove")
                }
            }
        }
        item { SectionTitle(if (matches.isEmpty()) "No likely matches yet" else "Likely matches") }
        items(matches, key = { it.item.id }) { preview ->
            ListItem(
                headlineContent = { Text(preview.item.title) },
                supportingContent = {
                    Text("${matchLabel(preview.score)} · ${preview.reasons.take(3).joinToString(" · ")}")
                },
                trailingContent = { Text("${preview.score}") },
                modifier = Modifier.clickableMatch { onOpenMatch(preview.item.id) },
            )
        }
    }
    when (pending) {
        Pending.REUNITE -> AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Mark as reunited?") },
            text = { Text("It stays on the board as resolved.") },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    onReunite()
                }) { Text("Reunited") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
        Pending.DELETE -> AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Remove this report?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    onDelete()
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
        null -> Unit
    }
}

private fun Modifier.clickableMatch(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
