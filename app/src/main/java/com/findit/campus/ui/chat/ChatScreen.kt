package com.findit.campus.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.findit.campus.ui.camera.CameraSession
import com.findit.campus.ui.camera.FindItCameraPreview
import com.findit.campus.ui.permission.CameraPermissions
import com.findit.campus.ui.permission.rememberPermissionAsk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val model: ChatViewModel = viewModel()
    val state by model.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }
    val session = remember { CameraSession(useFront = false) }
    val (cameraGranted, askCamera) = rememberPermissionAsk(
        permissions = CameraPermissions,
        rationale = "FindIt uses the camera to photograph the item before it is returned.",
        settingsMessage = "Camera access is off. Enable it in Settings to check the item.",
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Return conversation") },
                navigationIcon = {
                    IconButton(onClick = { if (showCamera) showCamera = false else onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (showCamera && cameraGranted) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                FindItCameraPreview(session = session, modifier = Modifier.weight(1f).fillMaxWidth())
                Button(
                    onClick = {
                        session.takePicture(
                            context,
                            onResult = {
                                model.onProof(it)
                                showCamera = false
                            },
                            onError = { showCamera = false },
                        )
                    },
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                ) { Text("Use this photo") }
            }
        } else {
            Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
                state.notice?.let { note ->
                    Text(note, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                }
                LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.messages, key = { it.id }) { message ->
                        val mine = message.senderId == state.viewerId
                        Text(
                            message.body,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(12.dp),
                        )
                    }
                }
                val status = state.thread?.status
                if (status == "NEEDS_QUESTIONS") {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("The photos did not match. Answer these before anyone hands the item over.", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(state.colorAnswer, model::onColor, Modifier.fillMaxWidth(), label = { Text("What color is it?") }, singleLine = true)
                        OutlinedTextField(state.markAnswer, model::onMark, Modifier.fillMaxWidth(), label = { Text("What mark or name is on it?") }, singleLine = true)
                        OutlinedTextField(state.placeAnswer, model::onPlace, Modifier.fillMaxWidth(), label = { Text("Where was it last seen?") }, singleLine = true)
                        TextButton(onClick = model::sendAnswers) { Text("Send answers") }
                        if (state.thread?.ownerId == state.viewerId || state.thread?.guestId == state.viewerId) {
                            val mineChecked = if (state.thread?.ownerId == state.viewerId) {
                                state.thread?.ownerChecked == true
                            } else {
                                state.thread?.guestChecked == true
                            }
                            Text(
                                if (state.thread?.ownerChecked == true && state.thread?.guestChecked == true) {
                                    "Both of you checked. The item is returned."
                                } else if (mineChecked) {
                                    "You checked. Waiting for the other person."
                                } else {
                                    "Both of you need to check before the item is returned."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (!mineChecked) {
                                Button(onClick = model::confirmReturn, modifier = Modifier.fillMaxWidth()) {
                                    Text("I checked the answers")
                                }
                            }
                        }
                    }
                }
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { if (cameraGranted) showCamera = true else askCamera() }) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = "Check with a photo")
                    }
                    OutlinedTextField(
                        state.draft,
                        model::onDraft,
                        Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        singleLine = true,
                    )
                    TextButton(onClick = model::send, enabled = state.draft.isNotBlank()) { Text("Send") }
                }
            }
        }
    }
}

@Composable
fun InboxScreen(onOpen: (Long) -> Unit, modifier: Modifier = Modifier) {
    val model: InboxViewModel = viewModel()
    val threads by model.threads.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Messages", style = MaterialTheme.typography.headlineMedium)
        if (threads.isEmpty()) {
            Text(
                "When you find someone's lost item, or they claim an item you found, the conversation shows up here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(threads, key = { it.id }) { thread ->
                TextButton(onClick = { onOpen(thread.itemId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Report #${thread.itemId} · ${thread.status.replace('_', ' ').lowercase()}")
                }
            }
        }
    }
}
