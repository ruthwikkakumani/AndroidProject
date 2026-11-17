package com.findit.campus.ui.mine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.model.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MineUiState(
    val items: List<Item> = emptyList(),
    val loading: Boolean = true,
    val syncNote: String = "Reports are saved on this device.",
)

@OptIn(ExperimentalCoroutinesApi::class)
class MineViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    val uiState: StateFlow<MineUiState> = graph.accounts.observeAccount()
        .flatMapLatest { account ->
            if (account == null) {
                flowOf(MineUiState(loading = false))
            } else {
                combine(graph.repository.observeMine(account.id), graph.sync.status) { items, sync ->
                    MineUiState(items = items, loading = false, syncNote = when {
                        !sync.configured -> "Offline mode · Reports and chats are saved on this device. Connect campus services to reach students on other phones."
                        account.remoteUid == null -> "Local account · Create a campus account online to use shared reports and messaging."
                        sync.error != null -> sync.error
                        sync.connected -> "Campus services connected · Your changes sync automatically."
                        else -> "Connecting to campus services · Your changes are saved locally."
                    })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MineUiState())
}
