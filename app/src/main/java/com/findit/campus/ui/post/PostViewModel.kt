package com.findit.campus.ui.post

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.CampusCatalog
import com.findit.campus.data.FindItLog
import com.findit.campus.data.dominantColorHex
import com.findit.campus.data.scaleDown
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.model.Matchable
import com.findit.campus.model.toMatchable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DuplicatePrompt(
    val itemId: Long,
    val title: String,
    val score: Int,
)

data class PostUiState(
    val form: PostForm = PostForm(),
    val categories: List<String> = CampusCatalog.categoryNames,
    val likelyPlaces: List<String> = emptyList(),
    val saving: Boolean = false,
    val reading: Boolean = false,
    val locating: Boolean = false,
    val message: String? = null,
    val duplicate: DuplicatePrompt? = null,
    val accuracyMeters: Float? = null,
)

sealed interface PostEvent {
    data class Saved(val id: Long) : PostEvent
}

class PostViewModel(
    app: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    private val repository = graph.repository
    private val events = MutableSharedFlow<PostEvent>(extraBufferCapacity = 1)
    val eventsFlow = events.asSharedFlow()

    private val _uiState = MutableStateFlow(PostUiState(form = restore()))
    val uiState: MutableStateFlow<PostUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeCategoryNames().collect { names ->
                if (names.isNotEmpty()) {
                    _uiState.update { it.copy(categories = names) }
                }
            }
        }
        viewModelScope.launch {
            repository.observeAll().collect { items ->
                val draft = _uiState.value.form
                _uiState.update {
                    it.copy(likelyPlaces = repository.likelyPlaces(items, draft.category, draft.type))
                }
            }
        }
    }

    fun onType(type: ItemType) = edit { it.copy(type = type) }

    fun onTitle(value: String) = edit { it.copy(title = value.take(60)) }

    fun onDescription(value: String) = edit { it.copy(description = value.take(400)) }

    fun onCategory(value: String) = edit { it.copy(category = value) }

    fun onWhen(millis: Long) = edit { it.copy(whenMillis = millis) }

    fun onContact(value: String) = edit { it.copy(contact = value.take(80)) }

    fun onPrivateDetail(value: String) = edit { it.copy(privateDetail = value.take(80)) }

    fun onPlaceName(name: String) = edit {
        it.copy(
            zoneName = name.take(60),
            placeLabel = if (it.placeLabel.isBlank() || it.placeLabel == it.zoneName) name.take(60) else it.placeLabel,
        )
    }

    fun useMyLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(locating = true, message = null) }
            val fix = graph.location.current()
            if (fix == null) {
                _uiState.update {
                    it.copy(locating = false, message = "Location is not available yet. Pick a campus spot instead.")
                }
            } else {
                val label = graph.location.placeLabel(fix.latitude, fix.longitude)
                edit {
                    it.copy(
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        placeLabel = label.ifBlank { it.zoneName.ifBlank { "Pinned location" } },
                        zoneName = it.zoneName.ifBlank { "Pinned location" },
                    )
                }
                _uiState.update { it.copy(locating = false, accuracyMeters = fix.accuracyMeters) }
                FindItLog.d("Pinned a report location")
            }
        }
    }

    fun onPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val scaled = scaleDown(bitmap, 1280)
            val color = runCatching { dominantColorHex(scaled) }.getOrNull()
            val path = runCatching { graph.photos.save(scaled) }.getOrNull()
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            if (path == null) {
                _uiState.update { it.copy(message = "That photo could not be saved.") }
            } else {
                edit { it.copy(photoPath = path, colorHex = color) }
            }
        }
    }

    fun readPhoto() {
        val path = _uiState.value.form.photoPath ?: return
        if (_uiState.value.reading) return
        viewModelScope.launch {
            _uiState.update { it.copy(reading = true) }
            try {
                val bitmap = graph.photos.loadDisplay(path, 1600) ?: error("Photo unavailable.")
                val text = try { graph.scanner.read(bitmap) } finally { bitmap.recycle() }
                edit { it.copy(scannedText = text.take(500),
                    title = it.title.ifBlank { text.lineSequence().firstOrNull().orEmpty().take(60) }) }
                _uiState.update { it.copy(message = if (text.isBlank()) "No readable text found. Try a closer photo of the label." else "Writing recognized. Review it before posting.") }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { _uiState.update { it.copy(message = "Could not read this photo. Try a clearer label.") } }
            finally { _uiState.update { it.copy(reading = false) } }
        }
    }

    fun clearPhoto() = edit { it.copy(photoPath = null, colorHex = null) }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun dismissDuplicate() = _uiState.update { it.copy(duplicate = null) }

    fun save(force: Boolean = false) {
        val draft = _uiState.value.form
        val problem = validatePost(draft)
        if (problem != null) {
            _uiState.update { it.copy(message = problem) }
            return
        }
        if (_uiState.value.saving) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, message = null) }
            runCatching {
                if (!force) {
                    val prompt = findDuplicate(draft)
                    if (prompt != null) {
                        _uiState.update { it.copy(saving = false, duplicate = prompt) }
                        return@runCatching
                    }
                }
                val ownerId = graph.accounts.observeAccount().first()?.id
                if (ownerId == null) {
                    _uiState.update { it.copy(saving = false, message = "Sign in before posting a report.") }
                    return@runCatching
                }
                val id = repository.add(draft, ownerId)
                _uiState.update { it.copy(saving = false, duplicate = null) }
                events.emit(PostEvent.Saved(id))
            }.onFailure { error ->
                FindItLog.e("Could not post report", error)
                _uiState.update { it.copy(saving = false, message = "The report could not be saved.") }
            }
        }
    }

    private suspend fun findDuplicate(draft: PostForm): DuplicatePrompt? {
        val items = repository.observeAll().first()
        val pseudo = Matchable(
            id = -1,
            title = draft.title,
            description = draft.description,
            category = draft.category,
            type = draft.type,
            status = ItemStatus.OPEN,
            latitude = draft.latitude ?: 0.0,
            longitude = draft.longitude ?: 0.0,
            zone = draft.zoneName,
            scannedText = draft.scannedText,
            colorHex = draft.colorHex,
            createdAt = draft.whenMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
        val best = graph.matcher.rank(pseudo, items.map { it.toMatchable() }).firstOrNull() ?: return null
        if (best.score < 72) return null
        val other = items.firstOrNull { it.id == best.itemId } ?: return null
        return DuplicatePrompt(other.id, other.title, best.score)
    }

    private fun edit(block: (PostForm) -> PostForm) {
        _uiState.update { state ->
            val next = block(state.form)
            savedStateHandle[KEY_TITLE] = next.title
            savedStateHandle[KEY_DESCRIPTION] = next.description
            savedStateHandle[KEY_TYPE] = next.type.name
            savedStateHandle[KEY_CATEGORY] = next.category
            savedStateHandle[KEY_ZONE] = next.zoneName
            savedStateHandle[KEY_PLACE] = next.placeLabel
            savedStateHandle[KEY_CONTACT] = next.contact
            savedStateHandle["privateDetail"] = next.privateDetail
            savedStateHandle[KEY_SCANNED] = next.scannedText
            savedStateHandle[KEY_PHOTO] = next.photoPath.orEmpty()
            savedStateHandle[KEY_COLOR] = next.colorHex.orEmpty()
            savedStateHandle[KEY_WHEN] = next.whenMillis
            if (next.latitude != null) {
                savedStateHandle[KEY_LAT] = next.latitude
            } else {
                savedStateHandle.remove<Double>(KEY_LAT)
            }
            if (next.longitude != null) {
                savedStateHandle[KEY_LNG] = next.longitude
            } else {
                savedStateHandle.remove<Double>(KEY_LNG)
            }
            state.copy(form = next)
        }
        viewModelScope.launch {
            val draft = _uiState.value.form
            val items = repository.observeAll().first()
            _uiState.update { it.copy(likelyPlaces = repository.likelyPlaces(items, draft.category, draft.type)) }
        }
    }

    private fun restore(): PostForm {
        val prefill = savedStateHandle.get<String>("prefill").orEmpty()
        val consumed = savedStateHandle.get<Boolean>(KEY_PREFILL_USED) == true
        val scanned = savedStateHandle.get<String>(KEY_SCANNED).orEmpty()
        val description = savedStateHandle.get<String>(KEY_DESCRIPTION).orEmpty()
        val scannedText = if (!consumed && prefill.isNotBlank()) {
            savedStateHandle[KEY_PREFILL_USED] = true
            prefill
        } else {
            scanned
        }
        val resolvedDescription = description.ifBlank { scannedText }
        return PostForm(
            type = runCatching {
                ItemType.valueOf(savedStateHandle.get<String>(KEY_TYPE) ?: savedStateHandle.get<String>("kind") ?: ItemType.LOST.name)
            }.getOrDefault(ItemType.LOST),
            title = savedStateHandle.get<String>(KEY_TITLE).orEmpty(),
            description = resolvedDescription,
            category = savedStateHandle.get<String>(KEY_CATEGORY) ?: "Other",
            whenMillis = savedStateHandle.get<Long>(KEY_WHEN) ?: System.currentTimeMillis(),
            photoPath = savedStateHandle.get<String>(KEY_PHOTO).orEmpty().ifBlank { null },
            colorHex = savedStateHandle.get<String>(KEY_COLOR).orEmpty().ifBlank { null },
            latitude = savedStateHandle.get<Double>(KEY_LAT),
            longitude = savedStateHandle.get<Double>(KEY_LNG),
            placeLabel = savedStateHandle.get<String>(KEY_PLACE).orEmpty(),
            zoneName = savedStateHandle.get<String>(KEY_ZONE).orEmpty(),
            contact = savedStateHandle.get<String>(KEY_CONTACT).orEmpty(),
            scannedText = scannedText,
            privateDetail = savedStateHandle["privateDetail"] ?: "",
        )
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_TYPE = "draftType"
        private const val KEY_CATEGORY = "draftCategory"
        private const val KEY_ZONE = "zone"
        private const val KEY_PLACE = "place"
        private const val KEY_CONTACT = "contact"
        private const val KEY_SCANNED = "scanned"
        private const val KEY_PHOTO = "photo"
        private const val KEY_COLOR = "color"
        private const val KEY_WHEN = "when"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_PREFILL_USED = "prefillConsumed"
    }
}
