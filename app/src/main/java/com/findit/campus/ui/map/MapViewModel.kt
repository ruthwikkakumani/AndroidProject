package com.findit.campus.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.FindItLog
import com.findit.campus.location.DeviceFix
import com.findit.campus.location.trailDistanceMeters
import com.findit.campus.data.local.TrailPoint
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

enum class MapFocus { CAMPUS, NEAR_ME }

data class TrailUiState(
    val points: List<TrailPoint> = emptyList(),
    val recording: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val distanceMeters: Double = 0.0,
)

data class MapUiState(
    val items: List<Item> = emptyList(),
    val focus: MapFocus = MapFocus.CAMPUS,
    val type: ItemType? = null,
    val selectedId: Long = -1L,
    val fix: DeviceFix? = null,
    val tracking: Boolean = false,
    val permission: Boolean = false,
    val trail: TrailUiState = TrailUiState(),
    val recenterRequest: Int = 0,
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
    private val recenterRequest = MutableStateFlow(0)
    private val accountId = graph.accounts.observeAccount().map { it?.id }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val segment = MutableStateFlow<String?>(null)
    private val trailBusy = MutableStateFlow(false)
    private val trailMessage = MutableStateFlow<String?>(null)
    private val trailPoints = accountId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else graph.trails.observe(id)
    }
    private val trailState = combine(trailPoints, segment, trailBusy, trailMessage) { points, active, busy, message ->
        TrailUiState(points, active != null, busy, message, trailDistanceMeters(points))
    }

    init {
        viewModelScope.launch { accountId.collect { segment.value = null } }
        viewModelScope.launch {
            combine(tracking, permission) { on, granted -> on && granted }
                .distinctUntilChanged()
                .flatMapLatest { on ->
                    if (on) graph.location.updates().catch { error -> FindItLog.e("Location updates stopped", error) } else emptyFlow()
                }
                .collect { current ->
                    fix.value = current
                    recordFix(current)
                }
        }
    }

    private data class MapFilters(val focus: String, val type: String, val selected: Long)

    private val mapState = combine(
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
    }
    val uiState: StateFlow<MapUiState> = combine(mapState, trailState, recenterRequest) { map, trail, request -> map.copy(trail = trail, recenterRequest = request) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun setTracking(on: Boolean) {
        tracking.value = on
        if (!on) segment.value = null
    }

    fun setPermission(granted: Boolean) {
        permission.value = granted
        if (granted) refreshOnce() else { fix.value = null; segment.value = null }
    }

    fun onFocus(value: MapFocus) {
        savedStateHandle["mapFocus"] = value.name
        recenterRequest.value += 1
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
            val current = graph.location.current()
            if (current != null) { fix.value = current; recordFix(current) }
        }
    }

    fun toggleTrail() {
        if (segment.value != null) { segment.value = null; return }
        if (!permission.value || !tracking.value || accountId.value == null || trailBusy.value) return
        segment.value = UUID.randomUUID().toString()
        trailMessage.value = null
        onFocus(MapFocus.NEAR_ME)
        refreshOnce()
    }

    fun clearTrail() {
        val id = accountId.value ?: return
        segment.value = null
        trailBusy.value = true
        viewModelScope.launch {
            try { graph.trails.clear(id); trailMessage.value = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { FindItLog.e("Trail clear failed", error); trailMessage.value = "Could not clear the trail. Try again." }
            finally { trailBusy.value = false }
        }
    }

    private suspend fun recordFix(current: DeviceFix) {
        val active = segment.value ?: return
        val id = accountId.value ?: return
        try {
            if (!graph.trails.append(id, active, current)) {
                segment.value = null
                trailMessage.value = "Trail is full. Clear it to start another walk."
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            FindItLog.e("Trail recording failed", error)
            segment.value = null
            trailMessage.value = "Trail paused. Could not save this location."
        }
    }
}
