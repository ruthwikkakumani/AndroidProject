package com.findit.campus.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.findit.campus.ui.camera.CameraSession
import com.findit.campus.ui.camera.FindItCameraPreview
import com.findit.campus.ui.components.EmptyBoard
import com.findit.campus.ui.permission.CameraPermissions
import com.findit.campus.ui.permission.rememberPermissionAsk

@Composable
fun ScanScreen(
    state: ScanUiState,
    onText: (String) -> Unit,
    onPhoto: (android.graphics.Bitmap) -> Unit,
    onOpen: (Long) -> Unit,
    onUseText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var useFront by remember { mutableStateOf(false) }
    var cameraNote by remember { mutableStateOf<String?>(null) }
    val session = remember(useFront) { CameraSession(useFront) }
    val (granted, ask) = rememberPermissionAsk(
        permissions = CameraPermissions,
        rationale = "FindIt uses the camera to read names and notes written on found items.",
        settingsMessage = "Camera access is off. Enable it in Settings, or type the writing below.",
    )
    Column(modifier = modifier.fillMaxSize()) {
        if (granted) {
            FindItCameraPreview(
                session = session,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onUnavailable = { cameraNote = it },
            )
        } else {
            EmptyBoard(
                title = "Camera is off",
                body = "Allow the camera to read a name from a notebook, ID, or label. You can also type it.",
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (granted) {
                    IconButton(onClick = { useFront = !useFront }) {
                        Icon(Icons.Outlined.Cameraswitch, contentDescription = "Switch camera")
                    }
                    Button(
                        onClick = {
                            session.takePicture(context, onResult = onPhoto, onError = { cameraNote = it })
                        },
                        enabled = !state.busy,
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                        }
                        Text("Read photo")
                    }
                } else {
                    Button(onClick = ask) { Text("Allow camera") }
                }
            }
            OutlinedTextField(
                value = state.text,
                onValueChange = onText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Writing from the item") },
                minLines = 2,
            )
            val note = cameraNote ?: state.message
            if (note != null) {
                Text(note, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (state.text.isNotBlank()) {
                FilledTonalButton(onClick = { onUseText(state.text) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use this in a report")
                }
            }
            if (state.hits.isEmpty() && state.text.length >= 2) {
                Text("No reports mention that writing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                items(state.hits, key = { it.item.id }) { hit ->
                    ListItem(
                        headlineContent = { Text(hit.item.title) },
                        supportingContent = { Text("${hit.item.zoneName} · ${hit.score}") },
                        modifier = Modifier.clickable { onOpen(hit.item.id) },
                    )
                }
            }
        }
    }
}
