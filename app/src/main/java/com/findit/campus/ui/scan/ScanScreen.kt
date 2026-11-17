package com.findit.campus.ui.scan

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.findit.campus.ui.components.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(state: ScanUiState, onText: (String) -> Unit, onPhoto: (Bitmap) -> Unit,
    onOpen: (Long) -> Unit, onUseText: (String) -> Unit, modifier: Modifier = Modifier) {
    var photoError by remember { mutableStateOf<String?>(null) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val feedback = LocalFeedback.current
    ReadingPane(modifier) {
        LazyColumn(Modifier.fillMaxSize().testTag("scan_list"),
            contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PHOTO ASSISTANT", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("A photo. A little closer.", style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() })
                        Text("Find clues. Search your campus.", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton({ showHelp = true }) { Icon(Icons.AutoMirrored.Outlined.HelpOutline, "How the scanner works") }
                }
            }
            item {
                FormCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Outlined.CenterFocusStrong, null, Modifier.padding(10.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("AI photo scanner", style = MaterialTheme.typography.titleMedium)
                            AnimatedContent(state.busy, label = "scan_status") { busy ->
                                Text(if (busy) "Reading your photo…" else "On-device · works offline",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Outlined.OfflineBolt, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    if (state.photo != null) ScanPhoto(state.photo, state.busy)
                    else Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(Icons.Outlined.AddAPhoto, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Let the item fill the frame", style = MaterialTheme.typography.titleSmall)
                                Text("Use a close-up in good light. Include a label or a unique mark.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    PhotoActions(onPhoto = { photoError = null; onPhoto(it) }, onError = { photoError = it }, enabled = !state.busy,
                        cameraLabel = if (state.photo == null) "Take photo" else "Retake photo")
                    AnimatedVisibility(state.busy, enter = fadeIn(), exit = fadeOut()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("Recognizing objects and reading visible writing…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    AnimatedVisibility(!state.busy && state.objects.isNotEmpty(), enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScanSectionLabel(Icons.Outlined.ImageSearch, "Item clues")
                            Text("Tap a clue to search · model confidence", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.objects.forEach { clue ->
                                    FilterChip(selected = state.text == clue, enabled = !state.busy,
                                        onClick = { onText(clue); feedback("Searching campus reports for $clue") },
                                        leadingIcon = if (state.text == clue) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null,
                                        label = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(clue)
                                                state.confidences[clue]?.let {
                                                    Text("${(it * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        })
                                }
                            }
                            Text("A suggestion, not an exact item identification.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (!state.busy && state.scene.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Background only: ${state.scene.joinToString()}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("These labels are excluded from item suggestions.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            (photoError ?: state.message)?.let { note -> item {
                Box(Modifier.animateItem()) { Notice(note, error = photoError != null || state.messageIsError) }
            } }
            item {
                FormCard {
                    ScanSectionLabel(Icons.AutoMirrored.Outlined.ManageSearch, "Search clues")
                    OutlinedTextField(state.text, { photoError = null; onText(it) },
                        Modifier.fillMaxWidth().testTag("scan_query"), label = { Text("Item name or writing") },
                        placeholder = { Text("e.g. blue bottle, notebook name") }, minLines = 2, maxLines = 5,
                        enabled = !state.busy, shape = MaterialTheme.shapes.small,
                        leadingIcon = { Icon(Icons.Outlined.Search, null) })
                    Text("Edit the writing or choose an item clue above. Keep private ID numbers out of public reports.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button({ onUseText(state.text) }, Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        enabled = state.text.isNotBlank() && !state.busy) {
                        Icon(Icons.Outlined.PostAdd, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text("Use in a lost report")
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Reports with similar clues", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    if (state.hits.isNotEmpty()) Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(state.hits.size.toString(), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            if (state.hits.isEmpty()) item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.TravelExplore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (state.text.trim().length < 2) "Your next clue starts here" else "No matching reports yet",
                            style = MaterialTheme.typography.titleSmall)
                        Text(if (state.text.trim().length < 2) "Choose an item clue or type a description to search."
                            else "Try a shorter name or a different item clue.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.hits, key = { it.item.id }) { hit ->
                ItemCard(hit.item, 0, { onOpen(hit.item.id) }, modifier = Modifier.animateItem())
            }
        }
    }
    if (showHelp) ModalBottomSheet(onDismissRequest = { showHelp = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("A little help from AI", style = MaterialTheme.typography.headlineSmall)
            ScanHelpStep(Icons.Outlined.Memory, "Google ML Kit, on your phone",
                "Bundled image labeling suggests broad categories. Latin text recognition reads visible writing. Neither uses a paid AI API or needs an AI key.")
            ScanHelpStep(Icons.Outlined.CenterFocusStrong, "Photograph the actual item",
                "The whole photo is analyzed, including its background. Move closer and keep one item in frame. The Android emulator camera shows a virtual room; use a real item photo from the gallery for your demo.")
            ScanHelpStep(Icons.Outlined.Search, "You choose the useful clue",
                "Scene labels are kept separate. Select an item clue or edit the writing to search reports. Percentages are model confidence in a label, not an item-match probability.")
            ScanHelpStep(Icons.Outlined.VerifiedUser, "Photos support a careful return",
                "Chat photo checks compare labels, text, colors and image structure. They cannot prove ownership; both students still confirm private details.")
            Button({ showHelp = false }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Got it") }
        }
    }
}

@Composable
private fun ScanSectionLabel(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ScanPhoto(photo: Bitmap, busy: Boolean) {
    val image = remember(photo) { photo.asImageBitmap() }
    Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer).testTag("scan_photo")) {
        Image(image, "Photo being analyzed", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Surface(Modifier.align(Alignment.TopStart).padding(10.dp), shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .94f)) {
            Text("YOUR PHOTO", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        if (busy) {
            val transition = rememberInfiniteTransition(label = "photo_scan")
            val progress by transition.animateFloat(0f, 1f,
                infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "scan_line")
            val color = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * progress
                drawLine(color.copy(alpha = .18f), Offset(0f, y), Offset(size.width, y), strokeWidth = 18.dp.toPx())
                drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

@Composable
private fun ScanHelpStep(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
