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
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val repository = graph.repository
    private val text = MutableStateFlow("")
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

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

    val uiState: StateFlow<ScanUiState> = kotlinx.coroutines.flow.combine(text, hits, busy, message) { value, found, working, note ->
        ScanUiState(text = value, hits = found, busy = working, message = note)
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
            var scaled: Bitmap? = null
            try {
                scaled = withContext(Dispatchers.Default) { scaleDown(bitmap, 1600) }
                val value = graph.scanner.read(scaled)
                if (value.isBlank()) {
                    message.value = "No writing turned up. Move closer and try again."
                } else {
                    text.value = value.take(500)
                }
            } catch (error: TimeoutCancellationException) {
                message.value = "Reading that photo took too long."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                FindItLog.e("Scan failed", error)
                message.value = "Could not read that photo."
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
