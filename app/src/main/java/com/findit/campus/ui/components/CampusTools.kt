package com.findit.campus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.findit.campus.R

/** One accessible row pattern for account menus and campus shortcuts. */
@Composable
fun CampusAction(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Row(Modifier.padding(14.dp).heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(11.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusToolsSheet(onDismiss: () -> Unit, onScan: () -> Unit, onMap: () -> Unit, onMessages: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IllustratedHeading("A little help.\nA faster return.", "Tools for finding clues and meeting safely.", R.drawable.art_profile)
            CampusAction(Icons.Outlined.DocumentScanner, "AI photo clues", "Read labels and recognise objects on your device", { onDismiss(); onScan() })
            CampusAction(Icons.Outlined.Map, "Explore campus map", "Nearby reports, your location and walking directions", { onDismiss(); onMap() })
            CampusAction(Icons.AutoMirrored.Outlined.Chat, "Private messages", "Share evidence, a contact or a meeting plan", { onDismiss(); onMessages() })
            TrustNote("AI provides clues. Confirm unique details together before returning an item.")
        }
    }
}
