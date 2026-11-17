package com.findit.campus.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.findit.campus.R
import com.findit.campus.data.CampusCatalog
import com.findit.campus.model.ItemType
import com.findit.campus.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: HomeUiState, expanded: Boolean,
    onQuery: (String) -> Unit, onType: (ItemType?) -> Unit,
    onOpenOnly: (Boolean) -> Unit, onCategory: (String?) -> Unit, onClear: () -> Unit,
    onOpenItem: (Long) -> Unit, onSelect: (Long) -> Unit, onReunite: (Long) -> Unit, onDelete: (Long) -> Unit,
    displayName: String = "", campusName: String = "", viewerId: Long = 0,
    onFound: (Long) -> Unit = {}, onInbox: () -> Unit = {}, onProfile: () -> Unit = {},
    onPostFound: () -> Unit = {}, modifier: Modifier = Modifier, unreadCount: Int = 0,
) {
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    // A null business filter means the default Lost board. The Found tab remains independently searchable.
    val boardType = state.type ?: ItemType.LOST
    val visible = remember(state.items, boardType) { state.items.filter { it.type == boardType } }
    val gutter = if (expanded) 32.dp else 20.dp
    LazyVerticalGrid(columns = GridCells.Adaptive(158.dp), modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = 12.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Brand(Modifier.weight(1f), campusName.ifBlank { "Campus lost & found" })
                IconButton(onClick = onInbox) {
                    BadgedBox(badge = { if (unreadCount > 0) Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) } }) {
                        Icon(Icons.Outlined.Notifications, "Notifications")
                    }
                }
                IconButton(onClick = onProfile) { Avatar(displayName) }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            IllustratedHeading("Lost something\non campus?", "A little help can bring it home. Find your things, or make someone's day.", R.drawable.art_discover)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(state.query, onQuery, Modifier.fillMaxWidth(),
                placeholder = { Text("Search items or campus places", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { IconButton(onClick = { filtersOpen = true }) { Icon(Icons.Outlined.Tune, "Filters") } },
                singleLine = true, shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(state.category == null, { onCategory(null) }, label = { Text("All items") }) }
                items((state.categories + CampusCatalog.categoryNames).distinct()) { category ->
                    FilterChip(state.category == category, { onCategory(if (state.category == category) null else category) },
                        label = { Text(category) }, leadingIcon = { Icon(categoryIcon(category), null, Modifier.size(16.dp)) })
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(ItemType.LOST to "Lost board", ItemType.FOUND to "Found board").forEachIndexed { index, (type, name) ->
                    SegmentedButton(boardType == type, { onType(type) }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(name) }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (boardType == ItemType.LOST) "Help bring it home" else "Looking for its owner", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${visible.size} reports", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (boardType == ItemType.FOUND) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilledTonalButton(onClick = onPostFound, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Post an item you found")
                }
            }
        }
        state.error?.let { message -> item(span = { GridItemSpan(maxLineSpan) }) { Notice(message, error = true) } }
        if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!state.loading && visible.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyBoard(if (state.query.isNotBlank() || state.category != null) "No matching reports" else "A fresh start for your campus",
                    if (boardType == ItemType.LOST) "Lost reports appear here. Tap Report to add yours." else "Found something? Post it here so its owner can reach you.")
                if (state.query.isNotBlank() || state.category != null) TextButton(onClick = onClear, Modifier.fillMaxWidth()) { Text("Clear search & filters") }
            }
        }
        items(visible, key = { it.id }, contentType = { "report" }) { report ->
            BoardItemCard(report, { onOpenItem(report.id) },
                if (viewerId > 0 && report.ownerId != viewerId) ({ onFound(report.id) }) else null,
                Modifier.animateItem())
        }
    }
    if (filtersOpen) ModalBottomSheet(onDismissRequest = { filtersOpen = false }) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Make it easier to find", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Open reports only"); Text("Hide items already returned", style = MaterialTheme.typography.bodySmall) }
                Switch(state.openOnly, onOpenOnly)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClear, Modifier.weight(1f)) { Text("Reset") }
                Button(onClick = { filtersOpen = false }, Modifier.weight(1f)) { Text("Show reports") }
            }
        }
    }
}
