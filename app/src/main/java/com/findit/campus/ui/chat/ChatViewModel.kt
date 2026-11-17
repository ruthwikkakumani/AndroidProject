package com.findit.campus.ui.chat

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.local.*
import com.findit.campus.model.Item
import com.findit.campus.location.MeetingPin
import com.findit.campus.data.PrivateContact
import com.findit.campus.data.MeetingPlan
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withTimeoutOrNull
import com.findit.campus.data.scaleDown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatUiState(
    val thread: ThreadEntity? = null, val item: Item? = null, val personName: String = "Campus student",
    val messages: List<MessageEntity> = emptyList(), val draft: String = "",
    val colorAnswer: String = "", val markAnswer: String = "", val placeAnswer: String = "",
    val pendingMessageIds: Set<Long> = emptySet(), val onlineAccount: Boolean = false, val sharingLocation: Boolean = false,
    val syncNote: String? = null,
    val busy: Boolean = false, val notice: String? = null, val viewerId: Long = 0, val loading: Boolean = true,
)

class ChatViewModel(app: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val state = MutableStateFlow(ChatUiState(draft = savedStateHandle["draft"] ?: ""))
    val uiState: StateFlow<ChatUiState> = state
    private var sharingJob: Job? = null
    private var sharingGeneration = 0L
    init {
        viewModelScope.launch {
            try {
                val account = graph.accounts.observeAccount().first() ?: error("Sign in to open this conversation.")
                state.update { it.copy(viewerId = account.id, onlineAccount = account.remoteUid != null) }
                val threadId = savedStateHandle.get<Long>("threadId") ?: graph.chat.open(savedStateHandle.get<Long>("itemId") ?: -1, account.id)
                    ?: error("Choose a student's conversation from Messages.")
                val thread = graph.database.chatDao().getThread(threadId) ?: error("This conversation is unavailable.")
                require(account.id == thread.ownerId || account.id == thread.guestId) { "This is a private conversation." }
                val peer = graph.database.accountDao().getById(if (account.id == thread.ownerId) thread.guestId else thread.ownerId)
                state.update { it.copy(thread = thread, personName = peer?.displayName ?: "Campus student", loading = false) }
                launch {
                    graph.database.accountDao().observeById(if (account.id == thread.ownerId) thread.guestId else thread.ownerId)
                        .collect { peer -> state.update { it.copy(personName = peer?.displayName ?: "Campus student") } }
                }
                launch { graph.database.syncDao().observePending(account.remoteUid.orEmpty()).collect { writes ->
                    state.update { it.copy(pendingMessageIds = writes.filter { w -> w.collection == "messages" }.mapNotNull { w -> w.documentId.toLongOrNull() }.toSet()) }
                } }
                launch { graph.sync.status.collect { sync -> state.update { it.copy(syncNote = sync.error) } } }
                launch { graph.repository.observeItem(thread.itemId).collect { item -> state.update { it.copy(item = item) } } }
                launch { graph.chat.observeThread(threadId).collect { t -> state.update { it.copy(thread = t) } } }
                launch {
                    graph.chat.observeMessages(threadId).collect { messages ->
                        state.update { it.copy(messages = messages) }
                        graph.database.accountDao().markThreadRead(account.id, threadId)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { state.update { it.copy(loading = false, notice = error.message ?: "Conversation unavailable.") } }
        }
    }
    fun onDraft(value: String) { savedStateHandle["draft"] = value.take(1000); state.update { it.copy(draft = value.take(1000)) } }
    fun onColor(value: String) = state.update { it.copy(colorAnswer = value.take(60)) }
    fun onMark(value: String) = state.update { it.copy(markAnswer = value.take(120)) }
    fun onPlace(value: String) = state.update { it.copy(placeAnswer = value.take(120)) }
    fun dismissNotice() = state.update { it.copy(notice = null) }
    fun reportError(message: String) = state.update { it.copy(notice = message) }
    private fun action(block: suspend (ChatUiState, Long) -> Unit) {
        val current = state.value; val threadId = current.thread?.id ?: return
        if (current.busy) return
        viewModelScope.launch {
            state.update { it.copy(busy = true, notice = null) }
            try { block(current, threadId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { state.update { it.copy(notice = error.message ?: "Please try again.") } }
            finally { state.update { it.copy(busy = false) } }
        }
    }
    fun stopSharingLocation() {
        sharingJob?.cancel()
        state.update { it.copy(sharingLocation = false) }
    }
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun shareLocation(live: Boolean) {
        if (sharingJob != null || state.value.thread == null) return
        val generation = ++sharingGeneration
        sharingJob = viewModelScope.launch {
            var lastPin: MeetingPin? = null
            val current = state.value
            val id = current.thread!!.id
            try {
                require(current.onlineAccount) { "Connect your account from Home before sharing a meeting location." }
                val fix = graph.location.current() ?: error("No fresh GPS position. Enable location and try again outdoors.")
                lastPin = MeetingPin(fix.latitude, fix.longitude, fix.accuracyMeters, fix.capturedAtMillis, live)
                graph.chat.send(id, current.viewerId, lastPin.encode())
                state.update { it.copy(sharingLocation = live, notice = if (live) "Sharing while this chat is open, for up to 15 minutes. Tap Stop or leave the chat to end sharing."
                    else "Meeting location saved. It will reach this student when uploaded.") }
                if (live) withTimeoutOrNull(15 * 60_000L) {
                    graph.location.updates().sample(15_000).collect { next ->
                        require(graph.location.hasPermission()) { "Location permission was removed." }
                        if (MeetingPin(next.latitude, next.longitude, next.accuracyMeters, next.capturedAtMillis).recent()) {
                            lastPin = MeetingPin(next.latitude, next.longitude, next.accuracyMeters, next.capturedAtMillis, true)
                            graph.chat.send(id, current.viewerId, lastPin.encode())
                        }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { reportError(error.message ?: "Location could not be shared.") }
            finally {
                // Publish the end of sharing without inventing a newer GPS timestamp.
                if (lastPin?.live == true) withContext(NonCancellable) {
                    withTimeoutOrNull(5_000) {
                        runCatching { graph.chat.send(id, current.viewerId, lastPin!!.copy(live = false).encode()) }
                    }
                }
                if (generation == sharingGeneration) {
                    state.update { it.copy(sharingLocation = false) }
                    sharingJob = null
                }
            }
        }
    }

    fun send() = action { current, id -> graph.chat.send(id, current.viewerId, current.draft); onDraft("") }
    fun shareContact(phone: String, note: String) = action { current, id ->
        graph.chat.send(id, current.viewerId, PrivateContact(phone, note).encode())
        state.update { it.copy(notice = "Contact card saved privately. It reaches this student after upload.") }
    }
    fun shareMeeting(place: String, time: String, note: String) = action { current, id ->
        graph.chat.send(id, current.viewerId, MeetingPlan(place, time, note).encode())
        state.update { it.copy(notice = "Meeting plan saved. Agree together in chat before setting off.") }
    }
    fun sendAnswers() = action { current, id ->
        require(current.colorAnswer.isNotBlank() && current.markAnswer.isNotBlank() && current.placeAnswer.isNotBlank()) { "Answer all three questions before sharing." }
        graph.chat.send(id, current.viewerId, "Ownership details:\nColor: ${current.colorAnswer}\nUnique mark: ${current.markAnswer}\nLast place: ${current.placeAnswer}")
        state.update { it.copy(colorAnswer = "", markAnswer = "", placeAnswer = "", notice = "Details shared privately. Ask follow-up questions if anything is unclear.") }
    }
    fun onProof(bitmap: Bitmap) {
        if (state.value.busy || state.value.thread == null) { bitmap.recycle(); return }
        action { current, id ->
            val path = withContext(Dispatchers.IO) {
                val scaled = scaleDown(bitmap, 1280)
                try { graph.photos.save(scaled) } finally { if (scaled !== bitmap) scaled.recycle(); bitmap.recycle() }
            }
            val verdict = graph.chat.checkPhoto(id, current.viewerId, path)
            state.update { it.copy(notice = verdict.explanation + " Both students must review the evidence; AI cannot confirm ownership.") }
        }
    }
    fun confirmSide() = action { current, id ->
        graph.chat.confirmSide(id, current.viewerId)
        state.update { it.copy(notice = "Your verification is saved. Wait for the other student's check before handover.") }
    }
    fun confirmReturn() = action { current, id ->
        graph.chat.confirmReturn(id, current.viewerId)
        state.update { it.copy(notice = "Handover confirmation saved. The report closes after both students confirm.") }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    val conversations = graph.accounts.observeAccount().flatMapLatest { account ->
        if (account == null) flowOf(emptyList()) else graph.chat.observeConversations(account.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications = graph.accounts.observeAccount().flatMapLatest { account ->
        if (account == null) flowOf(emptyList()) else graph.database.accountDao().observeNotifications(account.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun markRead() { viewModelScope.launch { graph.accounts.observeAccount().first()?.let { graph.database.accountDao().markRead(it.id) } } }
}
