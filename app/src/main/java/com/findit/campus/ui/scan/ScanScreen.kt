package com.findit.campus.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findit.campus.R
import com.findit.campus.ui.components.*

@Composable
fun ScanScreen(state: ScanUiState, onText: (String) -> Unit, onPhoto: (Bitmap) -> Unit,
    onOpen: (Long) -> Unit, onUseText: (String) -> Unit, modifier: Modifier = Modifier) {
    var photoError by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { IllustratedHeading("A clue in\nevery label.", "Read writing from an item and search campus reports.", R.drawable.art_report) }
        item { FormCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.DocumentScanner, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Column { Text("AI text scanner", style = MaterialTheme.typography.titleMedium)
                    Text("Works on your device, even offline", style = MaterialTheme.typography.bodySmall) }
            }
            Text("Photograph a notebook name, a label or an identifying note. Keep the writing clear and well lit.", style = MaterialTheme.typography.bodyMedium)
            PhotoActions(onPhoto = { photoError = null; onPhoto(it) }, onError = { photoError = it }, enabled = !state.busy)
            if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Reading the writing…") }
        } }
        (photoError ?: state.message)?.let { note -> item { Notice(note, error = true) } }
        item { FormCard {
            Text("Writing from the item", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(state.text, { photoError = null; onText(it) }, Modifier.fillMaxWidth(),
                placeholder = { Text("Read a photo, or type a clue…") }, minLines = 3, maxLines = 6, enabled = !state.busy)
            Text("Review the text before using it. Avoid sharing private ID numbers publicly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button({ onUseText(state.text) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = state.text.isNotBlank() && !state.busy) { Text("Use in a lost report") }
        } }
        item { Text("Reports with similar clues", style = MaterialTheme.typography.titleLarge) }
        if (state.hits.isEmpty()) item { EmptyBoard(if (state.text.length < 2) "Follow the little clues" else "No matching writing yet",
            if (state.text.length < 2) "Scan or type a name to search the campus board." else "Try a shorter name, or check another label.") }
        items(state.hits, key = { it.item.id }) { hit -> ItemCard(hit.item, 0, { onOpen(hit.item.id) }, modifier = Modifier.animateItem()) }
    }
}
