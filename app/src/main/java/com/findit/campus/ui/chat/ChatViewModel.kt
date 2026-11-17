package com.findit.campus.ui.chat

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.local.MessageEntity
import com.findit.campus.data.local.ThreadEntity
import com.findit.campus.data.scaleDown
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val thread: ThreadEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    val draft: String = "",
    val colorAnswer: String = "",
    val markAnswer: String = "",
    val placeAnswer: String = "",
    val busy: Boolean = false,
    val notice: String? = null,
    val viewerId: Long = 0,
)

class ChatViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val itemId: Long = savedStateHandle.get<Long>("itemId")
        ?: savedStateHandle.get<String>("itemId")?.toLongOrNull()
        ?: -1L
    private val state = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = state

    init {
        viewModelScope.launch {
            val account = graph.accounts.observeAccount().first() ?: return@launch
            state.update { it.copy(viewerId = account.id) }
            val threadId = graph.chat.open(itemId, account.id)
            if (threadId == null) {
                state.update { it.copy(notice = "Open this from the other person's report.") }
                return@launch
            }
            launch {
                graph.chat.observeThread(threadId).collect { thread ->
                    state.update { it.copy(thread = thread) }
                }
            }
            launch {
                graph.chat.observeMessages(threadId).collect { messages ->
                    state.update { it.copy(messages = messages) }
                }
            }
        }
    }

    fun onDraft(value: String) = state.update { it.copy(draft = value.take(500)) }

    fun onColor(value: String) = state.update { it.copy(colorAnswer = value.take(40)) }

    fun onMark(value: String) = state.update { it.copy(markAnswer = value.take(80)) }

    fun onPlace(value: String) = state.update { it.copy(placeAnswer = value.take(80)) }

    fun send() {
        val current = state.value
        val threadId = current.thread?.id ?: return
        viewModelScope.launch {
            graph.chat.send(threadId, current.viewerId, current.draft)
            state.update { it.copy(draft = "") }
        }
    }

    fun sendAnswers() {
        val current = state.value
        val threadId = current.thread?.id ?: return
        val body = listOf(
            "Color: ${current.colorAnswer.ifBlank { "not answered" }}",
            "Mark: ${current.markAnswer.ifBlank { "not answered" }}",
            "Last place: ${current.placeAnswer.ifBlank { "not answered" }}",
        ).joinToString("\n")
        viewModelScope.launch {
            graph.chat.send(threadId, current.viewerId, body)
            state.update { it.copy(colorAnswer = "", markAnswer = "", placeAnswer = "") }
        }
    }

    fun onProof(bitmap: Bitmap) {
        val current = state.value
        val threadId = current.thread?.id ?: return
        viewModelScope.launch {
            state.update { it.copy(busy = true) }
            val scaled = scaleDown(bitmap, 640)
            val path = runCatching { graph.photos.save(scaled) }.getOrNull()
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            if (path == null) {
                state.update { it.copy(busy = false, notice = "That photo could not be saved.") }
                return@launch
            }
            val verdict = graph.chat.checkPhoto(threadId, current.viewerId, path)
            state.update {
                it.copy(
                    busy = false,
                    notice = if (verdict.sameItem) {
                        "Your photo matches. The other person still has to check before it is returned."
                    } else {
                        "Photos do not match. Do not hand the item over yet. Both of you should answer the questions."
                    },
                )
            }
        }
    }

    fun confirmReturn() {
        val current = state.value
        val threadId = current.thread?.id ?: return
        viewModelScope.launch {
            graph.chat.confirmReturn(threadId, current.viewerId)
            state.update { it.copy(notice = "Your check is saved. The item returns only after both of you confirm.") }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    val threads: StateFlow<List<ThreadEntity>> = graph.accounts.observeAccount()
        .flatMapLatest { account ->
            if (account == null) flowOf(emptyList()) else graph.chat.observeThreads(account.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
