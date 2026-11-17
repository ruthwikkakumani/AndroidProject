package com.findit.campus.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.palette.graphics.Palette
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.findit.campus.data.cloud.CloudServices
import com.findit.campus.data.cloud.awaitResult
import java.util.UUID

class PhotoStore(private val context: Context, private val cloud: CloudServices) {
    suspend fun save(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "item-${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        file.absolutePath
    }

    suspend fun loadDisplay(path: String, maxSide: Int = 900): Bitmap? = withContext(Dispatchers.IO) {
        if (path.startsWith("gs://")) {
            val storage = cloud.storage ?: return@withContext null
            val bytes = storage.getReferenceFromUrl(path).getBytes(8L * 1024 * 1024).awaitResult()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            val scaled = scaleDown(bitmap, maxSide)
            if (scaled !== bitmap) bitmap.recycle()
            return@withContext scaled
        }
        val file = File(path)
        if (!file.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }

    suspend fun readPicker(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }?.let { bitmap ->
            val scaled = scaleDown(bitmap, 1280)
            if (scaled !== bitmap) bitmap.recycle()
            scaled
        }
    }
}

fun scaleDown(source: Bitmap, maxSide: Int): Bitmap {
    val largest = maxOf(source.width, source.height)
    if (largest <= maxSide) return source
    val scale = maxSide.toFloat() / largest
    val width = (source.width * scale).toInt().coerceAtLeast(1)
    val height = (source.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, width, height, true)
}

fun dominantColorHex(bitmap: Bitmap): String? {
    val palette = Palette.from(bitmap).clearFilters().maximumColorCount(12).generate()
    val rgb = palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb ?: return null
    return String.format(Locale.US, "#%06X", 0xFFFFFF and rgb)
}
