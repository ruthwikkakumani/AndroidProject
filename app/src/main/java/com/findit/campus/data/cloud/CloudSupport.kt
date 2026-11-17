package com.findit.campus.data.cloud

import android.content.Context
import com.findit.campus.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

fun campusKey(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

private const val SAFE_ID_MASK = 0x1FFFFFFFFFFFFFL
fun newId(): Long = (SecureRandom().nextLong() and SAFE_ID_MASK).coerceAtLeast(1L)
fun stableId(value: String): Long = (ByteBuffer.wrap(
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)),
).long and SAFE_ID_MASK).coerceAtLeast(1L)

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

/** Configuration is read from ignored local.properties, never baked into source. */
class CloudServices(context: Context, enabled: Boolean = true) {
    private val app: FirebaseApp? = if (
        enabled && BuildConfig.FIREBASE_APPLICATION_ID.isNotBlank() &&
        BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
        BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
        BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank()
    ) {
        FirebaseApp.getApps(context).firstOrNull { it.name == "FindIt" }
            ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
            .build(), "FindIt")
    } else null

    val configured: Boolean get() = app != null
    val auth: FirebaseAuth? = app?.let(FirebaseAuth::getInstance)
    val store: FirebaseFirestore? = app?.let(FirebaseFirestore::getInstance)
    val storage: FirebaseStorage? = app?.let(FirebaseStorage::getInstance)
}
