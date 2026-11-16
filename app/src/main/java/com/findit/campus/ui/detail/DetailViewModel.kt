package com.findit.campus.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.FindItLog
import com.findit.campus.model.Item
import com.findit.campus.ui.home.MatchPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val item: Item? = null,
    val matches: List<MatchPreview> = emptyList(),
    val missing: Boolean = false,
)

class DetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {
    private val repository = getApplication<FindItApplication>().graph.repository
    private val itemId: Long = savedStateHandle.get<Long>("itemId")
        ?: savedStateHandle.get<String>("itemId")?.toLongOrNull()
        ?: -1L

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeItem(itemId),
        repository.observeAll(),
    ) { item, all ->
        if (item == null) {
            DetailUiState(missing = true)
        } else {
            val previews = repository.matchesFor(item, all).mapNotNull { scored ->
                val other = all.firstOrNull { it.id == scored.itemId } ?: return@mapNotNull null
                MatchPreview(other, scored.score, scored.reasons)
            }
            DetailUiState(item = item, matches = previews)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun reunite() {
        if (itemId < 0) return
        viewModelScope.launch {
            runCatching { repository.markReunited(itemId) }
                .onFailure { FindItLog.e("Could not mark reunited", it) }
        }
    }

    fun delete(onDone: () -> Unit) {
        if (itemId < 0) return
        viewModelScope.launch {
            runCatching { repository.delete(itemId) }
                .onSuccess { onDone() }
                .onFailure { FindItLog.e("Could not delete report", it) }
        }
    }
}
