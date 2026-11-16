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
import kotlinx.coroutines.flow.stateIn

data class MineUiState(
    val items: List<Item> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MineViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = getApplication<FindItApplication>().graph
    val uiState: StateFlow<MineUiState> = graph.accounts.observeAccount()
        .flatMapLatest { account ->
            if (account == null) {
                flowOf(MineUiState(loading = false))
            } else {
                graph.repository.observeMine(account.id).map { MineUiState(items = it, loading = false) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MineUiState())
}
