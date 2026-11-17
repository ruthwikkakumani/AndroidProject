package com.findit.campus.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.findit.campus.FindItApplication
import com.findit.campus.location.DeviceFix
import com.findit.campus.ui.permission.LocationPermissions
import com.findit.campus.ui.permission.rememberPermissionAsk
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExactLocationLine() {
    val context = LocalContext.current
    val (granted, ask) = rememberPermissionAsk(
        permissions = LocationPermissions,
        rationale = "FindIt shows the exact place you are standing so a report can be tagged honestly.",
        settingsMessage = "Location access is off. Enable precise location in Settings.",
    )
    var line by remember { mutableStateOf("Locating…") }
    LaunchedEffect(granted) {
        if (!granted) {
            line = ""
            return@LaunchedEffect
        }
        val app = context.applicationContext as FindItApplication
        val fix = withContext(Dispatchers.IO) { app.graph.location.current() }
        line = fix?.let { formatFix(app, it) } ?: "Exact location is not available yet."
    }
    if (!granted) {
        TextButton(onClick = ask) { Text("Show my exact location") }
    } else {
        Column {
            Text(
                line.substringBefore('\n'),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val rest = line.substringAfter('\n', "")
            if (rest.isNotBlank()) {
                Text(rest, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private suspend fun formatFix(app: FindItApplication, fix: DeviceFix): String {
    val address = app.graph.location.placeLabel(fix.latitude, fix.longitude)
    val coordinates = String.format(Locale.US, "%.5f, %.5f", fix.latitude, fix.longitude)
    return if (address.isBlank()) coordinates else "$coordinates\n$address"
}
