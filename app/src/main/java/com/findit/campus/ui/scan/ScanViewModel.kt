package com.findit.campus.ui.scan

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.FindItLog
import com.findit.campus.data.scaleDown
import com.findit.campus.ui.home.MatchPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val text: String = "",
    val hits: List<MatchPreview> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val objects: List<String> = emptyList(),
    val messageIsError: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val repository = graph.repository
    private val text = MutableStateFlow("")
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val objects = MutableStateFlow(emptyList<String>())
    private val messageIsError = MutableStateFlow(false)

    private val hits: StateFlow<List<MatchPreview>> = text
        .debounce(200)
        .distinctUntilChanged()
        .flatMapLatest { value ->
            if (value.trim().length < 2) {
                flowOf(emptyList())
            } else {
                repository.observeAll().map { items ->
                    val scored = repository.textHits(value, items)
                    scored.mapNotNull { match ->
                        val item = items.firstOrNull { it.id == match.itemId } ?: return@mapNotNull null
                        MatchPreview(item, match.score, match.reasons)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val feedback = kotlinx.coroutines.flow.combine(message, objects, messageIsError) { note, clues, error ->
        Triple(note, clues, error)
    }
    val uiState: StateFlow<ScanUiState> = kotlinx.coroutines.flow.combine(text, hits, busy, feedback) { value, found, working, feedback ->
        ScanUiState(text = value, hits = found, busy = working, message = feedback.first,
            objects = feedback.second, messageIsError = feedback.third)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanUiState())

    fun onText(value: String) {
        text.value = value.take(500)
        message.value = null
    }

    fun onPhoto(bitmap: Bitmap) {
        if (busy.value) { bitmap.recycle(); return }
        viewModelScope.launch {
            busy.value = true
            message.value = null
            objects.value = emptyList()
            messageIsError.value = false
            var scaled: Bitmap? = null
            try {
                scaled = withContext(Dispatchers.Default) { scaleDown(bitmap, 1600) }
                val clues = graph.checker.scan(scaled)
                objects.value = clues.objects
                if (clues.text.isNotBlank()) {
                    text.value = clues.text.take(500)
                    message.value = "Photo scanned. Review the writing or tap an object clue to search."
                } else if (clues.objects.isNotEmpty()) {
                    text.value = clues.objects.first()
                    message.value = "No writing visible; AI found object clues instead. Tap a clue to search."
                } else {
                    message.value = "No clear clues detected. Try a closer, well-lit photo or type a description."
                }
            } catch (error: TimeoutCancellationException) {
                messageIsError.value = true
                message.value = "Scanning took too long. Try a smaller, clearer photo."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                FindItLog.e("Scan failed", error)
                messageIsError.value = true
                message.value = "Could not scan this photo. Try another photo, or type a clue below."
            } finally {
                if (scaled !== bitmap) scaled?.recycle()
                bitmap.recycle()
                busy.value = false
            }
        }
    }

    fun consumeMessage() {
        message.update { null }
    }
}
