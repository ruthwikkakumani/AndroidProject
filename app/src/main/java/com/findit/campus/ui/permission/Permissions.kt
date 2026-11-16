package com.findit.campus.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

fun Context.hasPermissions(permissions: Array<String>): Boolean =
    permissions.any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

@Composable
fun rememberPermissionAsk(
    permissions: Array<String>,
    rationale: String,
    settingsMessage: String,
): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by rememberSaveable { mutableStateOf(context.hasPermissions(permissions)) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, permissions) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = context.hasPermissions(permissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        asked = true
        granted = result.values.any { it }
        if (!granted && permissions.none { permission ->
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    context.findActivity(),
                    permission,
                )
            }
        ) {
            showSettings = true
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Allow access") },
            text = { Text(rationale) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    launcher.launch(permissions)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Not now") }
            },
        )
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Permission needed") },
            text = { Text(settingsMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showSettings = false
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("Close") }
            },
        )
    }

    val ask = {
        when {
            context.hasPermissions(permissions) -> granted = true
            permissions.any { permission ->
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(context.findActivity(), permission)
            } -> showRationale = true
            asked && !context.hasPermissions(permissions) -> showSettings = true
            else -> launcher.launch(permissions)
        }
    }
    return granted to ask
}

private fun Context.findActivity(): android.app.Activity {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    error("Permission request needs an activity.")
}

val CameraPermissions = arrayOf(Manifest.permission.CAMERA)
val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)
