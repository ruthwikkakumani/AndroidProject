package com.findit.campus.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.findit.campus.data.FindItLog
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class DeviceFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)

class LocationDataSource(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    suspend fun current(timeoutMillis: Long = 10_000): DeviceFix? {
        if (!hasPermission()) return null
        return runCatching {
            withTimeout(timeoutMillis) {
                readCurrent() ?: readLast()
            }
        }.onFailure { error ->
            FindItLog.e("Location fix failed", error)
        }.getOrNull()
    }

    suspend fun placeLabel(latitude: Double, longitude: Double): String = runCatching {
        if (!Geocoder.isPresent()) return@runCatching ""
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = withTimeout(8_000) { reverse(geocoder, latitude, longitude) }
        address?.getAddressLine(0).orEmpty()
    }.onFailure { error ->
        FindItLog.e("Place lookup failed", error)
    }.getOrDefault("")

    @SuppressLint("MissingPermission")
    fun updates(): Flow<DeviceFix> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        DeviceFix(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                        ),
                    )
                }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun readCurrent(): DeviceFix? = suspendCancellableCoroutine { continuation ->
        val token = CancellationTokenSource()
        continuation.invokeOnCancellation { token.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { location ->
                if (!continuation.isActive) return@addOnSuccessListener
                continuation.resume(
                    location?.let {
                        DeviceFix(it.latitude, it.longitude, it.accuracy)
                    },
                )
            }
            .addOnFailureListener { error ->
                FindItLog.e("Current location failed", error)
                if (continuation.isActive) continuation.resume(null)
            }
    }

    @SuppressLint("MissingPermission")
    private suspend fun readLast(): DeviceFix? = suspendCancellableCoroutine { continuation ->
        client.lastLocation
            .addOnSuccessListener { location ->
                if (!continuation.isActive) return@addOnSuccessListener
                continuation.resume(location?.let { DeviceFix(it.latitude, it.longitude, it.accuracy) })
            }
            .addOnFailureListener { error ->
                FindItLog.e("Last location failed", error)
                if (continuation.isActive) continuation.resume(null)
            }
    }

    private suspend fun reverse(geocoder: Geocoder, latitude: Double, longitude: Double): Address? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            FindItLog.e("Geocoder: ${errorMessage ?: "unknown"}")
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
            }
        }
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }
    }
}
