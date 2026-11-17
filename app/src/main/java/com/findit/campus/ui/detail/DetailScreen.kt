package com.findit.campus.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.findit.campus.BuildConfig
import com.findit.campus.model.*
import com.findit.campus.ui.components.*
import com.findit.campus.ui.format.*
import com.findit.campus.ui.home.MatchPreview
import com.findit.campus.ui.map.*
import java.util.Locale

@Composable
fun DetailScreen(state: DetailUiState, onBack: () -> Unit, onOpenMatch: (Long) -> Unit,
    onReunite: () -> Unit, onDelete: () -> Unit, viewerId: Long = 0, onReachOut: () -> Unit = {}, modifier: Modifier = Modifier) {
    Scaffold(modifier, topBar = { PageTopBar("Item details", onBack) }) { padding ->
        state.item?.let {
            ItemDetailBody(it, state.matches, onOpenMatch, onReunite, onDelete, viewerId, onReachOut, Modifier.padding(padding))
        } ?: Box(Modifier.padding(padding).padding(24.dp)) { Text(if (state.missing) "This report is no longer available." else "Loading report…") }
    }
}

@Composable
fun ItemDetailBody(item: Item, matches: List<MatchPreview>, onOpenMatch: (Long) -> Unit,
    onReunite: () -> Unit, onDelete: () -> Unit, viewerId: Long = 0, onReachOut: () -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = viewerId > 0 && item.ownerId == viewerId
    var remove by remember { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ItemPhoto(item.photoPath, item.title, item.category, MaterialTheme.colorScheme.tertiary, Modifier.fillMaxWidth().height(250.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { TypeBadge(item.type, item.status); Text(item.category, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp)) }
            Spacer(Modifier.height(12.dp)); Text(item.title, style = MaterialTheme.typography.headlineMedium)
        }
        item { FormCard {
            Text("About this item", style = MaterialTheme.typography.titleMedium)
            Text(item.description, style = MaterialTheme.typography.bodyLarge)
            Text(item.zoneName, style = MaterialTheme.typography.titleSmall)
            if (item.placeLabel.isNotBlank() && item.placeLabel != item.zoneName) Text(item.placeLabel, style = MaterialTheme.typography.bodyMedium)
            Text(formatFull(item.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.scannedText.isNotBlank()) Notice("Writing on the item: ${item.scannedText}")
        } }
        if (item.hasCoordinates) item { FormCard {
            Text("Campus location", style = MaterialTheme.typography.titleMedium)
            if (BuildConfig.MAPS_API_KEY.isNotBlank()) GoogleCampusMap(listOf(item), null, MapFocus.CAMPUS, item.id, false, {}, Modifier.fillMaxWidth().height(170.dp))
            else CampusMap(listOf(item), null, MapFocus.CAMPUS, item.id, {}, Modifier.fillMaxWidth().height(150.dp))
            Text(String.format(Locale.US, "Report pin: %.6f, %.6f", item.latitude, item.longitude), style = MaterialTheme.typography.bodySmall)
            TextButton({ context.startActivity(Intent(Intent.ACTION_VIEW, "geo:${item.latitude},${item.longitude}?q=${item.latitude},${item.longitude}".toUri())) }) { Text("Open in maps") }
        } }
        item {
            if (item.status == ItemStatus.OPEN && viewerId > 0) Button(onReachOut, Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(if (owner) "View conversations" else if (item.type == ItemType.LOST) "Found it · Message reporter" else "This is mine · Message finder")
            }
            Spacer(Modifier.height(12.dp))
            TrustNote(if (item.status == ItemStatus.REUNITED) "Both students confirmed this return." else "Check photos and private details together. Both students must verify the item and confirm its physical return.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton({ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, item.shareText()) }, "Share report")) }, Modifier.weight(1f)) { Icon(Icons.Outlined.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Share") }
                if (owner) TextButton({ remove = true }) { Text("Remove report") }
            }
        }
        if (matches.isNotEmpty()) item { Text("Worth checking", style = MaterialTheme.typography.titleLarge) }
        items(matches, key = { it.item.id }) { match -> ItemCard(match.item, 0, { onOpenMatch(match.item.id) }) }
    }
    if (remove) AlertDialog(onDismissRequest = { remove = false }, title = { Text("Remove your report?") }, text = { Text("It will no longer appear on the campus board. Existing conversations stay available.") },
        confirmButton = { TextButton({ remove = false; onDelete() }) { Text("Remove") } }, dismissButton = { TextButton({ remove = false }) { Text("Keep report") } })
}
