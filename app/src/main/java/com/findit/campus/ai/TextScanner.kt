package com.findit.campus.ai

import android.graphics.Bitmap
import com.findit.campus.data.FindItLog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class TextScanner {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun read(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        withTimeout(20_000) {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { result ->
                        val text = result.text.orEmpty().trim()
                        FindItLog.i("Read ${text.length} characters from a photo")
                        if (continuation.isActive) continuation.resume(text)
                    }
                    .addOnFailureListener { error ->
                        FindItLog.e("Text reading failed", error)
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }
        }
    }
}
