package com.findit.campus.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.findit.campus.R
import com.findit.campus.data.local.*
import com.findit.campus.ui.components.*
import com.findit.campus.ui.format.formatWhen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChatActions(
    val onDraft: (String) -> Unit, val send: () -> Unit, val dismissNotice: () -> Unit,
    val onProof: (android.graphics.Bitmap) -> Unit, val reportError: (String) -> Unit,
    val onColor: (String) -> Unit, val onMark: (String) -> Unit, val onPlace: (String) -> Unit,
    val sendAnswers: () -> Unit, val confirmSide: () -> Unit, val confirmReturn: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val model: ChatViewModel = viewModel(); val state by model.uiState.collectAsStateWithLifecycle()
    ChatContent(state, ChatActions(model::onDraft, model::send, model::dismissNotice, model::onProof,
        model::reportError, model::onColor, model::onMark, model::onPlace, model::sendAnswers,
        model::confirmSide, model::confirmReturn), onBack, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(state: ChatUiState, model: ChatActions, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    var checkOpen by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size) }
    Scaffold(modifier, topBar = { PageTopBar(state.personName, onBack, actions = {
        IconButton({ checkOpen = true }, enabled = state.thread != null) { Icon(Icons.Outlined.VerifiedUser, "Ownership checks") }
    }) }, bottomBar = {
        if (state.thread != null) Surface(shadowElevation = 3.dp) {
            Row(Modifier.imePadding().navigationBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton({ checkOpen = true }, enabled = !state.busy) { Icon(Icons.Outlined.AddPhotoAlternate, "Share evidence") }
                OutlinedTextField(state.draft, model.onDraft, Modifier.weight(1f), placeholder = { Text("Message privately") }, maxLines = 4, shape = MaterialTheme.shapes.large)
                FilledIconButton(model.send, enabled = state.draft.isNotBlank() && !state.busy) { Icon(Icons.AutoMirrored.Filled.Send, "Send message") }
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            state.item?.let { report ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.clickable { checkOpen = true }.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ItemPhoto(report.photoPath, report.title, report.category, MaterialTheme.colorScheme.tertiary, Modifier.size(48.dp))
                        Column(Modifier.weight(1f)) { Text(report.title, style = MaterialTheme.typography.titleSmall); Text("${report.zoneName} · Private return conversation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Icon(Icons.Outlined.ChevronRight, "Open return checks")
                    }
                }
            }
            if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    TrustNote("Ask about a unique mark, name, or last location. Share evidence privately and meet in a public campus place.")
                }
                items(state.messages, key = { it.id }) { message -> MessageBubble(message, message.senderId == state.viewerId) }
            }
            state.thread?.let { t ->
                val checked = t.ownerChecked && t.guestChecked
                val returned = t.ownerReturned && t.guestReturned
                TextButton({ checkOpen = true }, Modifier.fillMaxWidth()) {
                    Icon(if (returned) Icons.Outlined.CheckCircle else Icons.Outlined.VerifiedUser, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(if (returned) "Returned to its owner" else if (checked) "Both checks complete · Arrange handover" else "Verify together before returning")
                }
            }
        }
    }
    state.notice?.let { message -> AlertDialog(onDismissRequest = model.dismissNotice, title = { Text("Return conversation") },
        text = { Text(message) }, confirmButton = { TextButton(model.dismissNotice) { Text("Got it") } }) }
    if (checkOpen && state.thread != null) ModalBottomSheet(onDismissRequest = { checkOpen = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        OwnershipSheet(state, model, onConfirm = { confirm = it })
    }
    confirm?.let { kind ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(if (kind == "check") "Confirm your item check?" else "Has the handover happened?") },
            text = { Text(if (kind == "check") "Confirm only after you have reviewed the other student's evidence and private answers. Similar photos alone do not prove ownership."
                else "Confirm only after you have physically given or received the item. The other student must confirm too.") },
            confirmButton = { TextButton({ confirm = null; checkOpen = false; if (kind == "check") model.confirmSide() else model.confirmReturn() }) { Text("Confirm") } },
            dismissButton = { TextButton({ confirm = null }) { Text("Keep checking") } })
    }
}

@Composable
private fun MessageBubble(message: MessageEntity, mine: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Surface(Modifier.widthIn(max = 290.dp), shape = MaterialTheme.shapes.medium,
            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message.photoPath?.let { ItemPhoto(it, "shared evidence", "Other", MaterialTheme.colorScheme.tertiary, Modifier.fillMaxWidth().height(170.dp)) }
                if (message.body.isNotBlank()) Text(message.body, style = MaterialTheme.typography.bodyMedium)
                Text(Instant.ofEpochMilli(message.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
                    Modifier.align(Alignment.End), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OwnershipSheet(state: ChatUiState, model: ChatActions, onConfirm: (String) -> Unit) {
    val thread = state.thread ?: return
    val mineOwner = state.viewerId == thread.ownerId
    val mineChecked = if (mineOwner) thread.ownerChecked else thread.guestChecked
    val peerChecked = if (mineOwner) thread.guestChecked else thread.ownerChecked
    val mineReturned = if (mineOwner) thread.ownerReturned else thread.guestReturned
    var questions by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxWidth().imePadding(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("A thoughtful return", style = MaterialTheme.typography.titleLarge); Text("Two people. Two checks. One happy reunion.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { FormCard {
            Text("1  Share photo evidence", style = MaterialTheme.typography.titleMedium)
            Text("AI compares image labels and readable text with the report photo. It offers clues, and never confirms ownership.", style = MaterialTheme.typography.bodyMedium)
            PhotoActions(model.onProof, model.reportError, enabled = !state.busy && thread.status != "RETURNED")
        } }
        item { FormCard {
            Text("2  Ask what only the owner knows", style = MaterialTheme.typography.titleMedium)
            TextButton({ questions = !questions }) { Text(if (questions) "Hide questions" else "Share private identifying details") }
            if (questions) {
                OutlinedTextField(state.colorAnswer, model.onColor, Modifier.fillMaxWidth(), label = { Text("Color and appearance") })
                OutlinedTextField(state.markAnswer, model.onMark, Modifier.fillMaxWidth(), label = { Text("A unique mark, name, or sticker") })
                OutlinedTextField(state.placeAnswer, model.onPlace, Modifier.fillMaxWidth(), label = { Text("Where was it last seen?") })
                Button(model.sendAnswers, Modifier.fillMaxWidth(), enabled = !state.busy && thread.status != "RETURNED") { Text("Share answers privately") }
            }
            TrustNote("If photos are inconclusive, ask follow-up questions in chat until both of you are confident.")
        } }
        item { FormCard {
            Text("3  Review together", style = MaterialTheme.typography.titleMedium)
            CheckLine("You", mineChecked)
            CheckLine(state.personName, peerChecked)
            if (!mineChecked) Button({ onConfirm("check") }, Modifier.fillMaxWidth(), enabled = !state.busy) { Text("I verified the item & answers") }
        } }
        item { FormCard {
            Text("4  Confirm physical handover", style = MaterialTheme.typography.titleMedium)
            Text(if (thread.ownerReturned && thread.guestReturned) "Both confirmed. This item is reunited." else if (mineReturned) "You confirmed. Waiting for the other student." else "After both checks, meet and return the item. Confirm here afterward.", style = MaterialTheme.typography.bodyMedium)
            if (!mineReturned) Button({ onConfirm("return") }, Modifier.fillMaxWidth(), enabled = mineChecked && peerChecked && !state.busy) { Text("The handover is complete") }
        } }
    }
}

@Composable
private fun CheckLine(name: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        Text("$name · ${if (checked) "Verified" else "Waiting for verification"}", style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(onOpen: (Long) -> Unit, modifier: Modifier = Modifier, onOpenReport: (Long) -> Unit = {}) {
    val model: InboxViewModel = viewModel()
    val conversations by model.conversations.collectAsStateWithLifecycle(); val notifications by model.notifications.collectAsStateWithLifecycle()
    InboxContent(conversations, notifications, onOpen, model::markRead, modifier, onOpenReport)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxContent(conversations: List<ConversationSummary>, notifications: List<NotificationEntity>,
    onOpen: (Long) -> Unit, onMarkRead: () -> Unit, modifier: Modifier = Modifier, onOpenReport: (Long) -> Unit = {}) {
    var alerts by rememberSaveable { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Brand() }
        item { IllustratedHeading("Messages &\nhappy returns", "Keep every conversation and item check in one place.", R.drawable.art_messages) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(!alerts, { alerts = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Messages") }
                SegmentedButton(alerts, { alerts = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Alerts (${notifications.count { !it.read }})") }
            }
        }
        if (alerts) {
            if (notifications.isEmpty()) item { EmptyBoard("All caught up", "Matches, replies, and return requests will appear here.") }
            else item { TextButton(onMarkRead, Modifier.fillMaxWidth()) { Text("Mark all as read") } }
            items(notifications, key = { it.id }) { alert ->
                Surface(onClick = { if (alert.threadId != null) onOpen(alert.threadId) else alert.itemId?.let(onOpenReport) }, shape = MaterialTheme.shapes.medium, color = if (alert.read) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(alert.title, style = MaterialTheme.typography.titleSmall); Text(alert.body, style = MaterialTheme.typography.bodyMedium)
                        Text(formatWhen(alert.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            if (conversations.isEmpty()) item { EmptyBoard("Every reunion starts with hello", "Tap Found it on a lost report, or This is mine on a found report, to start a private conversation.") }
            items(conversations, key = { it.id }) { chat ->
                Surface(onClick = { onOpen(chat.id) }, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ItemPhoto(chat.photoPath, chat.title, "Other", MaterialTheme.colorScheme.tertiary, Modifier.size(56.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(chat.personName, style = MaterialTheme.typography.titleSmall)
                            Text(chat.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(chat.preview ?: "Start checking the item together", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (chat.unread > 0) Badge { Text(chat.unread.toString()) } else Icon(Icons.Outlined.ChevronRight, null)
                    }
                }
            }
        }
    }
}
