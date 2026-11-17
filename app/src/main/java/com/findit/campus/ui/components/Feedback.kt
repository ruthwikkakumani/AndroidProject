package com.findit.campus.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

val LocalFeedback = staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
fun rememberToastFeedback(): (String) -> Unit {
    val context = LocalContext.current.applicationContext
    var toast by remember { mutableStateOf<Toast?>(null) }
    DisposableEffect(Unit) { onDispose { toast?.cancel() } }
    return remember(context) { { text ->
        toast?.cancel()
        toast = Toast.makeText(context, text, Toast.LENGTH_SHORT).also { it.show() }
    } }
}

/** Keeps reading and touch targets comfortable on tablets and in landscape. */
@Composable
fun ReadingPane(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 680.dp).fillMaxSize(), content = content)
    }
}

@Composable
fun FeedbackSnackbar(data: SnackbarData) {
    Snackbar(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium, containerColor = MaterialTheme.colorScheme.inverseSurface,
        action = { data.visuals.actionLabel?.let { TextButton(data::performAction) { Text(it) } } },
        dismissAction = { TextButton(data::dismiss) { Text("Dismiss", color = MaterialTheme.colorScheme.inversePrimary) } }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.inversePrimary)
            Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun NotificationPreference(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = granted
        feedback(if (granted) "Campus notifications enabled" else "Updates are still available in Messages")
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    FormCard(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Stay in the loop", style = MaterialTheme.typography.titleMedium)
                Text(if (enabled) "Campus replies & return updates are enabled" else "Get notified when someone replies or finds your item",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton({
            if (!enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (enabled) "Manage notifications" else "Enable notifications") }
    }
}
