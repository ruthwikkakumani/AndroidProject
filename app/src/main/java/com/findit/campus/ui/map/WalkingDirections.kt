package com.findit.campus.ui.map

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

fun openWalkingDirections(context: Context, latitude: Double, longitude: Double): Boolean = try {
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    true
} catch (_: ActivityNotFoundException) { false }
catch (_: SecurityException) { false }
