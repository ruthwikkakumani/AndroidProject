package com.findit.campus.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.CampusCatalog
import com.findit.campus.data.FindItLog
import com.findit.campus.model.ItemType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    app: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val repository = graph.repository

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val typeName = savedStateHandle.getStateFlow(KEY_TYPE, "ALL")
    private val openOnly = savedStateHandle.getStateFlow(KEY_OPEN, true)
    private val category = savedStateHandle.getStateFlow(KEY_CATEGORY, "")
    private val selectedId = savedStateHandle.getStateFlow(KEY_SELECTED, -1L)

    private val filters = combine(
        query.debounce(180).distinctUntilChanged(),
        typeName,
        openOnly,
        category,
        selectedId,
    ) { q, type, open, cat, selected ->
        FeedFilter(
            query = q.trim(),
            type = when (type) {
                "LOST" -> ItemType.LOST
                "FOUND" -> ItemType.FOUND
                else -> null
            },
            openOnly = open,
            category = cat.ifBlank { null },
            selectedId = selected,
        )
    }

    val uiState: StateFlow<HomeUiState> = filters.flatMapLatest { filter ->
        combine(
            repository.observeSearch(filter.query),
            repository.observeAll(),
            repository.observeCategoryNames(),
        ) { searched, all, names ->
            buildHomeState(filter, searched, all, graph.matcher, graph.insights).copy(
                categories = names.ifEmpty { CampusCatalog.categoryNames },
            )
        }
    }.catch { error ->
        FindItLog.e("Feed failed", error)
        emit(HomeUiState(loading = false, error = "The board could not be loaded."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQuery(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun onType(type: ItemType?) {
        savedStateHandle[KEY_TYPE] = type?.name ?: "ALL"
    }

    fun onOpenOnly(enabled: Boolean) {
        savedStateHandle[KEY_OPEN] = enabled
    }

    fun onCategory(name: String?) {
        savedStateHandle[KEY_CATEGORY] = name.orEmpty()
    }

    fun select(id: Long) {
        savedStateHandle[KEY_SELECTED] = id
    }

    fun clearFilters() {
        savedStateHandle[KEY_TYPE] = "ALL"
        savedStateHandle[KEY_OPEN] = true
        savedStateHandle[KEY_CATEGORY] = ""
        savedStateHandle[KEY_QUERY] = ""
    }

    fun reunite(id: Long) {
        viewModelScope.launch {
            runCatching { repository.markReunited(id) }
                .onFailure { FindItLog.e("Could not mark reunited", it) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onFailure { FindItLog.e("Could not delete report", it) }
        }
    }

    companion object {
        private const val KEY_QUERY = "query"
        private const val KEY_TYPE = "type"
        private const val KEY_OPEN = "openOnly"
        private const val KEY_CATEGORY = "category"
        private const val KEY_SELECTED = "selectedId"
    }
}
