package com.findit.campus.ui.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.findit.campus.ui.components.EmptyBoard
import com.findit.campus.ui.components.ItemCard

@Composable
fun MineScreen(
    state: MineUiState,
    onOpen: (Long) -> Unit,
    accountName: String = "",
    campusName: String = "",
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "My reports",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }.padding(bottom = 4.dp),
            )
            Text(
                "Items you posted on this phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (accountName.isNotBlank()) {
                Text(accountName, style = MaterialTheme.typography.titleMedium)
                Text(campusName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.TextButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag("logout"),
                ) { Text("Log out") }
            }
        }
        if (!state.loading && state.items.isEmpty()) {
            item {
                EmptyBoard(
                    title = "No reports yet",
                    body = "Tap Report to post an item you lost.",
                )
            }
        }
        items(state.items, key = { it.id }) { item ->
            ItemCard(item = item, matchCount = 0, onClick = { onOpen(item.id) }, modifier = Modifier.animateItem())
        }
    }
}
