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
        item { IllustratedHeading("Every photo\nholds a clue.", "Recognize objects, read labels and search campus reports.", R.drawable.art_report) }
        item { FormCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.DocumentScanner, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Column { Text("AI photo scanner", style = MaterialTheme.typography.titleMedium)
                    Text("Works on your device, even offline", style = MaterialTheme.typography.bodySmall) }
            }
            Text("Take a clear photo of the item or its label. AI suggests object clues and reads visible writing.", style = MaterialTheme.typography.bodyMedium)
            PhotoActions(onPhoto = { photoError = null; onPhoto(it) }, onError = { photoError = it }, enabled = !state.busy)
            if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Finding objects and reading labels…") }
            if (state.objects.isNotEmpty()) {
                Text("AI object clues · tap to search", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.objects.forEach { clue -> FilterChip(selected = state.text == clue,
                        onClick = { onText(clue) }, label = { Text(clue) }, enabled = !state.busy) }
                }
                Text("Suggestions can be mistaken. Check the item and edit your clues.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } }
        (photoError ?: state.message)?.let { note -> item { Notice(note, error = photoError != null || state.messageIsError) } }
        item { FormCard {
            Text("Clues from your photo", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(state.text, { photoError = null; onText(it) }, Modifier.fillMaxWidth(),
                placeholder = { Text("Read a photo, or type a clue…") }, minLines = 3, maxLines = 6, enabled = !state.busy)
            Text("Review the text before using it. Avoid sharing private ID numbers publicly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button({ onUseText(state.text) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = state.text.isNotBlank() && !state.busy) { Text("Use in a lost report") }
        } }
        item { Text("Reports with similar clues", style = MaterialTheme.typography.titleLarge) }
        if (state.hits.isEmpty()) item { EmptyBoard(if (state.text.length < 2) "Follow the little clues" else "No matching reports yet",
            if (state.text.length < 2) "Scan a photo or type a clue to search the campus board." else "Try another object clue or a shorter name.") }
        items(state.hits, key = { it.item.id }) { hit -> ItemCard(hit.item, 0, { onOpen(hit.item.id) }, modifier = Modifier.animateItem()) }
    }
}
