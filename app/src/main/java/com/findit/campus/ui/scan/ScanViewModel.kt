package com.findit.campus.ui.scan

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.FindItLog
import com.findit.campus.data.scaleDown
import com.findit.campus.ui.home.MatchPreview
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ScanUiState(
    val text: String = "",
    val hits: List<MatchPreview> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val objects: List<String> = emptyList(),
    val messageIsError: Boolean = false,
    val confidences: Map<String, Float> = emptyMap(),
    val scene: List<String> = emptyList(),
    val photo: Bitmap? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val repository = graph.repository
    private val state = MutableStateFlow(ScanUiState())

    private val hits = state.map { it.text }.distinctUntilChanged().debounce(200)
        .flatMapLatest { query ->
            if (query.trim().length < 2) flowOf(query to emptyList<MatchPreview>())
            else repository.observeAll().map { items ->
                query to repository.textHits(query, items).mapNotNull { match ->
                    val item = items.firstOrNull { it.id == match.itemId } ?: return@mapNotNull null
                    MatchPreview(item, match.score, match.reasons)
                }
            }
        }

    val uiState: StateFlow<ScanUiState> = combine(state, hits) { current, result ->
        // Hide the preceding query's results while the new search is debouncing.
        current.copy(hits = if (result.first == current.text && !current.busy) result.second else emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanUiState())

    fun onText(value: String) {
        state.update { it.copy(text = value.take(500), message = null, messageIsError = false) }
    }

    fun onPhoto(bitmap: Bitmap) {
        if (state.value.busy) { bitmap.recycle(); return }
        state.update { ScanUiState(busy = true) }
        viewModelScope.launch {
            var scaled: Bitmap? = null
            try {
                val input = withContext(Dispatchers.Default) { scaleDown(bitmap, 1600) }
                scaled = input
                val preview = withContext(Dispatchers.Default) {
                    val small = scaleDown(input, 800)
                    // UI owns a separate, bounded preview; inference bitmaps are recycled below.
                    small.copy(Bitmap.Config.ARGB_8888, false).also { if (small !== input) small.recycle() }
                }
                state.update { it.copy(photo = preview) }
                val clues = graph.checker.scan(input)
                state.update { it.copy(
                    text = clues.text.take(500), objects = clues.objects,
                    confidences = clues.confidences, scene = clues.scene,
                    message = when {
                        clues.text.isNotBlank() -> "Writing found. Review it before searching or posting."
                        clues.objects.isNotEmpty() -> "Item clues found. Choose one that describes your item."
                        clues.scene.isNotEmpty() -> "Only the surroundings were recognized. Move closer to the item or choose another photo."
                        else -> "No clear clues yet. Try a well-lit close-up or type a description."
                    },
                ) }
            } catch (error: TimeoutCancellationException) {
                state.update { it.copy(messageIsError = true, message = "Scanning took too long. Try a smaller, clearer photo.") }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                FindItLog.e("Scan failed", error)
                state.update { it.copy(messageIsError = true, message = "Could not scan this photo. Try another photo, or type a clue below.") }
            } finally {
                if (scaled !== bitmap) scaled?.recycle()
                bitmap.recycle()
                state.update { it.copy(busy = false) }
            }
        }
    }

    fun consumeMessage() { state.update { it.copy(message = null) } }
}
