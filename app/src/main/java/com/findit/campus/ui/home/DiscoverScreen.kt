package com.findit.campus.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.findit.campus.model.Item
import com.findit.campus.model.ItemType
import com.findit.campus.model.matchLabel
import com.findit.campus.ui.components.EmptyBoard
import com.findit.campus.ui.components.ItemCard
import com.findit.campus.ui.components.SectionTitle
import com.findit.campus.ui.detail.ItemDetailBody
import com.findit.campus.ui.theme.FindItPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: HomeUiState,
    expanded: Boolean,
    onQuery: (String) -> Unit,
    onType: (ItemType?) -> Unit,
    onOpenOnly: (Boolean) -> Unit,
    onCategory: (String?) -> Unit,
    onClear: () -> Unit,
    onOpenItem: (Long) -> Unit,
    onSelect: (Long) -> Unit,
    onReunite: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    displayName: String = "",
    campusName: String = "",
    modifier: Modifier = Modifier,
) {
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(expanded, state.items) {
        if (expanded && state.items.isNotEmpty() && state.items.none { it.id == state.selectedId }) {
            onSelect(state.items.first().id)
        }
    }
    Row(modifier = modifier.fillMaxSize()) {
        DiscoverList(
            state = state,
            expanded = expanded,
            onQuery = onQuery,
            onType = onType,
            onOpenFilters = { filtersOpen = true },
            onOpenItem = onOpenItem,
            displayName = displayName,
            campusName = campusName,
            modifier = Modifier.weight(1f),
        )
        if (expanded) {
            val selected = state.items.firstOrNull { it.id == state.selectedId }
            Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface) {
                if (selected == null) {
                    EmptyBoard(
                        title = "Choose a report",
                        body = "The details and likely matches open here.",
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    ItemDetailBody(
                        item = selected,
                        matches = state.selectedMatches,
                        onOpenMatch = onOpenItem,
                        onReunite = { onReunite(selected.id) },
                        onDelete = { onDelete(selected.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
    if (filtersOpen) {
        ModalBottomSheet(onDismissRequest = { filtersOpen = false }, sheetState = sheetState) {
            FilterSheet(
                state = state,
                onType = onType,
                onOpenOnly = onOpenOnly,
                onCategory = onCategory,
                onClear = onClear,
                onDone = { filtersOpen = false },
            )
        }
    }
}

@Composable
private fun DiscoverList(
    state: HomeUiState,
    expanded: Boolean,
    onQuery: (String) -> Unit,
    onType: (ItemType?) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenItem: (Long) -> Unit,
    displayName: String,
    campusName: String,
    modifier: Modifier = Modifier,
) {
    val gutter = if (expanded) 24.dp else 16.dp
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = gutter, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (displayName.isBlank()) "FindIt Campus" else "Hello, $displayName",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = campusName.ifBlank { "Campus not set" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search items, places, names") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = onOpenFilters) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "Filters")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = MaterialTheme.shapes.large,
                )
                TypeRow(selected = state.type, onType = onType)
            }
        }
        item {
            PulseCard(state = state, modifier = Modifier.padding(horizontal = gutter))
        }
        if (state.reunions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Suggested reunions", Modifier.padding(horizontal = gutter))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = gutter),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.reunions, key = { "${it.lost.id}-${it.found.id}" }) { reunion ->
                            ReunionCard(reunion = reunion, onClick = { onOpenItem(reunion.lost.id) })
                        }
                    }
                }
            }
        }
        if (state.error != null) {
            item { Text(state.error, modifier = Modifier.padding(horizontal = gutter), color = MaterialTheme.colorScheme.error) }
        }
        if (!state.loading && state.items.isEmpty()) {
            item {
                EmptyBoard(
                    title = if (state.query.isBlank()) "Nothing on the board" else "No reports match",
                    body = "Try another word, or post what you lost or found.",
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
        }
        items(state.items, key = { it.id }, contentType = { it.type }) { item ->
            ItemCard(
                item = item,
                matchCount = state.matchCounts[item.id] ?: 0,
                onClick = { onOpenItem(item.id) },
                modifier = Modifier
                    .padding(horizontal = gutter)
                    .animateItem(),
            )
        }
    }
}

@Composable
private fun TypeRow(selected: ItemType?, onType: (ItemType?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onType(null) }, label = { Text("All") })
        FilterChip(selected = selected == ItemType.LOST, onClick = { onType(ItemType.LOST) }, label = { Text("Lost") })
        FilterChip(selected = selected == ItemType.FOUND, onClick = { onType(ItemType.FOUND) }, label = { Text("Found") })
    }
}

@Composable
private fun PulseCard(state: HomeUiState, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = !state.loading, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = FindItPalette.colors.reunion)
                    Text("Campus pulse", style = MaterialTheme.typography.titleSmall)
                }
                Text(state.pulse.headline, style = MaterialTheme.typography.bodyLarge)
                Text(
                    state.pulse.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ReunionCard(reunion: ReunionSuggestion, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(matchLabel(reunion.score), style = MaterialTheme.typography.labelMedium, color = FindItPalette.colors.reunion)
            Text(reunion.lost.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "may match ${reunion.found.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterSheet(
    state: HomeUiState,
    onType: (ItemType?) -> Unit,
    onOpenOnly: (Boolean) -> Unit,
    onCategory: (String?) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)
        TypeRow(selected = state.type, onType = onType)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Open reports only")
            Switch(checked = state.openOnly, onCheckedChange = onOpenOnly)
        }
        Text("Category", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.category == null, onClick = { onCategory(null) }, label = { Text("Any") })
            state.categories.forEach { name ->
                val count = state.categoryCounts[name] ?: 0
                FilterChip(
                    selected = state.category == name,
                    onClick = { onCategory(if (state.category == name) null else name) },
                    label = { Text("$name $count") },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onClear) { Text("Clear") }
            TextButton(onClick = onDone) { Text("Done") }
        }
    }
}
