package com.findit.campus.ui.chat

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.local.*
import com.findit.campus.model.Item
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
    val busy: Boolean = false, val notice: String? = null, val viewerId: Long = 0, val loading: Boolean = true,
)

class ChatViewModel(app: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val state = MutableStateFlow(ChatUiState(draft = savedStateHandle["draft"] ?: ""))
    val uiState: StateFlow<ChatUiState> = state
    init {
        viewModelScope.launch {
            try {
                val account = graph.accounts.observeAccount().first() ?: error("Sign in to open this conversation.")
                state.update { it.copy(viewerId = account.id) }
                val threadId = savedStateHandle.get<Long>("threadId") ?: graph.chat.open(savedStateHandle.get<Long>("itemId") ?: -1, account.id)
                    ?: error("Choose a student's conversation from Messages.")
                val thread = graph.database.chatDao().getThread(threadId) ?: error("This conversation is unavailable.")
                require(account.id == thread.ownerId || account.id == thread.guestId) { "This is a private conversation." }
                val peer = graph.database.accountDao().getById(if (account.id == thread.ownerId) thread.guestId else thread.ownerId)
                state.update { it.copy(thread = thread, personName = peer?.displayName ?: "Campus student", loading = false) }
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
    fun send() = action { current, id -> graph.chat.send(id, current.viewerId, current.draft); onDraft("") }
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
