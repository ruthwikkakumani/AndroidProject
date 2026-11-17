package com.findit.campus.ai

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ImageVerdict(
    val sameItem: Boolean,
    val score: Int,
    val explanation: String,
)

fun compareBitmaps(reference: Bitmap, proof: Bitmap): ImageVerdict {
    val size = 16
    return compareSamples(sample(reference, size), sample(proof, size), size)
}

fun compareSamples(reference: IntArray, proof: IntArray, size: Int): ImageVerdict {
    val leftHash = averageHash(reference)
    val rightHash = averageHash(proof)
    val distance = hamming(leftHash, rightHash)
    val colorGap = averageColorDistance(reference, proof)
    val structureScore = (100 - (distance * 100.0 / leftHash.size)).roundToInt().coerceIn(0, 100)
    val colorScore = (100 - (colorGap / 4.41)).roundToInt().coerceIn(0, 100)
    val score = ((structureScore * 0.65) + (colorScore * 0.35)).roundToInt().coerceIn(0, 100)
    val same = distance <= 64 && colorGap < 48
    val explanation = if (same) {
        "The photo lines up with the report photo."
    } else if (colorGap >= 48) {
        "The colors do not match the report photo."
    } else {
        "The shape of the photo does not match the report photo."
    }
    return ImageVerdict(sameItem = same, score = score, explanation = explanation)
}

internal fun averageHash(pixels: IntArray): BooleanArray {
    var total = 0.0
    val gray = DoubleArray(pixels.size)
    pixels.forEachIndexed { index, pixel ->
        val value = grayOf(pixel)
        gray[index] = value
        total += value
    }
    val mean = total / pixels.size.coerceAtLeast(1)
    return BooleanArray(pixels.size) { gray[it] >= mean }
}

internal fun hamming(left: BooleanArray, right: BooleanArray): Int {
    val count = minOf(left.size, right.size)
    var distance = abs(left.size - right.size)
    for (index in 0 until count) {
        if (left[index] != right[index]) distance += 1
    }
    return distance
}

internal fun averageColorDistance(left: IntArray, right: IntArray): Double {
    fun average(pixels: IntArray): Triple<Double, Double, Double> {
        var red = 0.0
        var green = 0.0
        var blue = 0.0
        pixels.forEach { pixel ->
            red += (pixel shr 16) and 0xFF
            green += (pixel shr 8) and 0xFF
            blue += pixel and 0xFF
        }
        val count = pixels.size.coerceAtLeast(1).toDouble()
        return Triple(red / count, green / count, blue / count)
    }
    val a = average(left)
    val b = average(right)
    val dr = a.first - b.first
    val dg = a.second - b.second
    val db = a.third - b.third
    return sqrt(dr * dr + dg * dg + db * db)
}

private fun grayOf(pixel: Int): Double {
    val red = (pixel shr 16) and 0xFF
    val green = (pixel shr 8) and 0xFF
    val blue = pixel and 0xFF
    return red * 0.299 + green * 0.587 + blue * 0.114
}

private fun sample(bitmap: Bitmap, size: Int): IntArray {
    val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
    val pixels = IntArray(size * size)
    scaled.getPixels(pixels, 0, size, 0, 0, size, size)
    if (scaled != bitmap) scaled.recycle()
    return pixels
}
