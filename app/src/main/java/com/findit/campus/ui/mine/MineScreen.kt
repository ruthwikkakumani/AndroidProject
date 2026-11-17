package com.findit.campus.ui.mine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.findit.campus.R
import com.findit.campus.model.ItemStatus
import com.findit.campus.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(state: MineUiState, onOpen: (Long) -> Unit, accountName: String = "",
    campusName: String = "", onLogout: () -> Unit = {}, modifier: Modifier = Modifier,
    email: String = "", onScan: () -> Unit = {}, onMap: () -> Unit = {}, onMessages: () -> Unit = {},
    onResetPassword: () -> Unit = {}) {
    var returned by rememberSaveable { mutableStateOf(false) }
    var signOut by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    var tools by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    var recovery by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    val visible = state.items.filter { (it.status == ItemStatus.REUNITED) == returned }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your space", style = MaterialTheme.typography.headlineMedium)
                Text("A little help brings things home.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton({ settings = true }) { Icon(Icons.Outlined.Settings, "Account settings") }
        } }
        item {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer))).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Avatar(accountName, Modifier.size(58.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(accountName.ifBlank { "Campus student" }, style = MaterialTheme.typography.titleLarge)
                            Text(campusName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Illustration(R.drawable.art_profile, Modifier.size(66.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .15f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileCount(state.items.count { it.status != ItemStatus.REUNITED }, "Active reports", !returned, { returned = false }, Modifier.weight(1f))
                        ProfileCount(state.items.count { it.status == ItemStatus.REUNITED }, "Reunited", returned, { returned = true }, Modifier.weight(1f))
                    }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton({ tools = true }, Modifier.weight(1f).heightIn(min = 48.dp)) { Icon(Icons.Outlined.Widgets, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Campus tools") }
            OutlinedButton({ privacy = true }, Modifier.weight(1f).heightIn(min = 48.dp)) { Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Privacy") }
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
        item { Text("Preferences & support", style = MaterialTheme.typography.titleLarge) }
        item { NotificationPreference() }
        item { CampusAction(Icons.Outlined.ManageAccounts, "Account details", "Your campus, email and account recovery", { settings = true }) }
        item { CampusAction(Icons.Outlined.VerifiedUser, "Safer returns", "Photo evidence, private details and handover checks", { help = true }) }
        item { OutlinedButton({ signOut = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("logout")) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Log out")
        } }
    }
    if (signOut) AlertDialog(onDismissRequest = { signOut = false }, title = { Text("Log out of FindIt?") },
        text = { Text("Your reports and conversations are saved. Log in again to continue.") },
        confirmButton = { TextButton({ signOut = false; onLogout() }) { Text("Log out") } },
        dismissButton = { TextButton({ signOut = false }) { Text("Stay here") } })
    if (help) ModalBottomSheet(onDismissRequest = { help = false }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("From lost to reunited", style = MaterialTheme.typography.headlineSmall)
            Text("1. Post your lost item, or check the Found board. Search by name, category or location.")
            Text("2. Tap Found it or This is mine to start a private conversation with the reporter.")
            Text("3. Share photos and unique details. AI gives comparison clues; both people decide whether the evidence is convincing.")
            TrustNote("Meet at a staffed campus location. Both people confirm the physical handover before a report is marked reunited.")
            Button({ help = false }, Modifier.fillMaxWidth()) { Text("Got it") }
        }
    }
    if (tools) CampusToolsSheet({ tools = false }, onScan, onMap, onMessages)
    if (settings || privacy) ModalBottomSheet(onDismissRequest = { settings = false; privacy = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (privacy) "You're in control" else "Account details", style = MaterialTheme.typography.headlineSmall)
            if (privacy) {
                TrustNote("Your phone number is optional and shared only when you choose to send a private contact card.")
                FormCard {
                    Text("Location, on your terms", style = MaterialTheme.typography.titleMedium)
                    Text("Meeting locations go only to the other student in that conversation. Live sharing stops when you leave the chat or after 15 minutes. Older shared pins remain in the conversation.", style = MaterialTheme.typography.bodyMedium)
                    Text("Report locations appear on your campus board. Choose a campus place instead of a personal address.", style = MaterialTheme.typography.bodySmall)
                }
                CampusAction(Icons.Outlined.PermDeviceInformation, "Camera & location permissions", "Manage access in Android settings", {
                    if (runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }.isFailure) feedback("Open FindIt in Android app settings")
                })
                CampusAction(Icons.Outlined.DocumentScanner, "How AI helps", "Try photo clues; review the results yourself", { privacy = false; onScan() })
            } else {
                FormCard {
                    Avatar(accountName)
                    SelectionContainer { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(accountName, style = MaterialTheme.typography.titleLarge)
                        Text(campusName, style = MaterialTheme.typography.bodyMedium)
                        if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.bodyMedium)
                    } }
                    if (email.isNotBlank()) TextButton({
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Account email", email))
                        feedback("Email copied")
                    }) { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Copy email") }
                }
                Text("Your campus keeps reports relevant to your student community.", style = MaterialTheme.typography.bodySmall)
                if (email.isNotBlank()) CampusAction(Icons.Outlined.Password, "Password recovery", "Request a reset email for an email/password account", { recovery = true })
                CampusAction(Icons.Outlined.Lock, "Privacy & permissions", "Private contacts, shared locations and camera access", { settings = false; privacy = true })
            }
            Button({ settings = false; privacy = false }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Done") }
        }
    }
    if (recovery) AlertDialog(onDismissRequest = { recovery = false }, title = { Text("Request a password reset?") },
        text = { Text("Send a recovery request to $email. If you signed in with Google, continue using Google sign-in.") },
        confirmButton = { TextButton({ recovery = false; onResetPassword() }) { Text("Request email") } },
        dismissButton = { TextButton({ recovery = false }) { Text("Cancel") } })
}

@Composable private fun ProfileCount(count: Int, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color by animateColorAsState(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = .45f), label = "Report selection")
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium, color = color) {
        Column(Modifier.padding(vertical = 12.dp).animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
