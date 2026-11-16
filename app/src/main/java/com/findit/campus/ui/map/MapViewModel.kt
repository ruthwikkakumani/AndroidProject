package com.findit.campus.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.FindItLog
import com.findit.campus.location.DeviceFix
import com.findit.campus.model.Item
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MapFocus { CAMPUS, NEAR_ME }

data class MapUiState(
    val items: List<Item> = emptyList(),
    val focus: MapFocus = MapFocus.CAMPUS,
    val type: ItemType? = null,
    val selectedId: Long = -1L,
    val fix: DeviceFix? = null,
    val tracking: Boolean = false,
    val permission: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    app: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val tracking = MutableStateFlow(false)
    private val permission = MutableStateFlow(graph.location.hasPermission())
    private val fix = MutableStateFlow<DeviceFix?>(null)
    private val focus = savedStateHandle.getStateFlow("mapFocus", MapFocus.CAMPUS.name)
    private val typeName = savedStateHandle.getStateFlow("mapType", "ALL")
    private val selectedId = savedStateHandle.getStateFlow("mapSelected", -1L)

    init {
        viewModelScope.launch {
            combine(tracking, permission) { on, granted -> on && granted }
                .distinctUntilChanged()
                .flatMapLatest { on ->
                    if (on) graph.location.updates() else emptyFlow()
                }
                .catch { error -> FindItLog.e("Location updates stopped", error) }
                .collect { fix.value = it }
        }
    }

    private data class MapFilters(val focus: String, val type: String, val selected: Long)

    val uiState: StateFlow<MapUiState> = combine(
        graph.repository.observeAll(),
        combine(focus, typeName, selectedId) { focusName, type, selected ->
            MapFilters(focusName, type, selected)
        },
        fix,
        tracking,
        permission,
    ) { all, filters, current, trackingOn, granted ->
        val type = when (filters.type) {
            "LOST" -> ItemType.LOST
            "FOUND" -> ItemType.FOUND
            else -> null
        }
        MapUiState(
            items = all.filter { item ->
                item.status == ItemStatus.OPEN && (type == null || item.type == type)
            },
            focus = runCatching { MapFocus.valueOf(filters.focus) }.getOrDefault(MapFocus.CAMPUS),
            type = type,
            selectedId = filters.selected,
            fix = current,
            tracking = trackingOn,
            permission = granted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun setTracking(on: Boolean) {
        tracking.value = on
    }

    fun setPermission(granted: Boolean) {
        permission.value = granted
        if (granted) refreshOnce()
    }

    fun onFocus(value: MapFocus) {
        savedStateHandle["mapFocus"] = value.name
    }

    fun onType(type: ItemType?) {
        savedStateHandle["mapType"] = type?.name ?: "ALL"
    }

    fun select(id: Long) {
        savedStateHandle["mapSelected"] = id
    }

    fun clearSelection() {
        savedStateHandle["mapSelected"] = -1L
    }

    fun refreshOnce() {
        viewModelScope.launch {
            val current = runCatching { graph.location.current() }.getOrNull()
            if (current != null) fix.value = current
        }
    }
}
