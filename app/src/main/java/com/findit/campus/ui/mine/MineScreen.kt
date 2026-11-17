package com.findit.campus.ui.mine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.findit.campus.R
import com.findit.campus.model.ItemStatus
import com.findit.campus.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(state: MineUiState, onOpen: (Long) -> Unit, accountName: String = "",
    campusName: String = "", onLogout: () -> Unit = {}, modifier: Modifier = Modifier,
    email: String = "") {
    var returned by rememberSaveable { mutableStateOf(false) }
    var signOut by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    val visible = state.items.filter { (it.status == ItemStatus.REUNITED) == returned }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { IllustratedHeading("Your campus.\nYour community.", "A little help brings things home.", R.drawable.art_profile) }
        item { FormCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(accountName)
                Column(Modifier.weight(1f)) {
                    Text(accountName, style = MaterialTheme.typography.titleLarge)
                    Text(campusName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ProfileCount(state.items.count { it.status != ItemStatus.REUNITED }, "Active reports")
                ProfileCount(state.items.count { it.status == ItemStatus.REUNITED }, "Reunited")
            }
        } }
        item { Notice(state.syncNote) }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("My reports", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton({ help = true }) { Text("How it works") }
        }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Active", "Reunited").forEachIndexed { i, label ->
                    SegmentedButton(selected = returned == (i == 1), onClick = { returned = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                }
            }
        }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!state.loading && visible.isEmpty()) item { EmptyBoard(
            if (returned) "Good things take teamwork" else "Your next good deed starts here",
            if (returned) "Reports move here after both people confirm the handover." else "Tap Report for a lost item. Post a found item from the Found board.") }
        items(visible, key = { it.id }) { report -> ItemCard(report, 0, { onOpen(report.id) }, modifier = Modifier.animateItem()) }
        item { OutlinedButton({ signOut = true }, Modifier.fillMaxWidth().testTag("logout")) { Text("Log out") } }
    }
    if (signOut) AlertDialog(onDismissRequest = { signOut = false }, title = { Text("Log out of FindIt?") },
        text = { Text("Your reports and conversations are saved. Log in again to continue.") },
        confirmButton = { TextButton({ signOut = false; onLogout() }) { Text("Log out") } },
        dismissButton = { TextButton({ signOut = false }) { Text("Stay here") } })
    if (help) ModalBottomSheet(onDismissRequest = { help = false }) {
        Column(Modifier.padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("From lost to reunited", style = MaterialTheme.typography.headlineSmall)
            Text("1. Post your lost item, or check the Found board. Search by name, category or location.")
            Text("2. Tap Found it or This is mine to start a private conversation with the reporter.")
            Text("3. Share photos and unique details. AI gives comparison clues; both people decide whether the evidence is convincing.")
            TrustNote("Meet at a staffed campus location. Both people confirm the physical handover before a report is marked reunited.")
            Button({ help = false }, Modifier.fillMaxWidth()) { Text("Got it") }
        }
    }
}

@Composable private fun ProfileCount(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
