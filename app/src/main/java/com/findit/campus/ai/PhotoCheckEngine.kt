package com.findit.campus.ai

import android.graphics.Bitmap
import com.findit.campus.data.cloud.awaitResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Bundled ML Kit inference runs on the phone. Evidence supports a human decision; it never proves ownership. */
class PhotoCheckEngine(private val scanner: TextScanner) {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    data class ScanClues(
        val text: String,
        val objects: List<String>,
        val confidences: Map<String, Float> = emptyMap(),
        val scene: List<String> = emptyList(),
    )

    /** Both models are bundled, so a plain object can supply clues without any visible writing. */
    suspend fun scan(bitmap: Bitmap): ScanClues = withContext(Dispatchers.Default) {
        withTimeout(30_000) {
            coroutineScope {
                val writing = async { scanner.read(bitmap) }
                val objects = async { classifiedLabels(bitmap) }
                val clues = objects.await()
                ScanClues(writing.await(), clues.items.map { it.label },
                    clues.items.associate { it.label to it.confidence }, clues.scene.map { it.label })
            }
        }
    }

    suspend fun compare(reference: Bitmap, proof: Bitmap): ImageVerdict = withContext(Dispatchers.Default) {
        withTimeout(30_000) {
            coroutineScope {
                val left = async { meaningfulLabels(reference) }
                val right = async { meaningfulLabels(proof) }
                val textLeft = async { scanner.read(reference) }
                val textRight = async { scanner.read(proof) }
                val visual = compareBitmaps(reference, proof)
                val common = left.await().toSet().intersect(right.await().toSet())
                val aText = textLeft.await().lowercase().split(Regex("\\W+")).filter { it.length >= 3 }.toSet()
                val bText = textRight.await().lowercase().split(Regex("\\W+")).filter { it.length >= 3 }.toSet()
                val commonText = aText.intersect(bText)
                val supportive = (common.isNotEmpty() && visual.score >= 65) ||
                    (commonText.isNotEmpty() && visual.score >= 50)
                ImageVerdict(supportive, visual.score, buildString {
                    if (common.isNotEmpty()) append("AI sees ${common.take(3).joinToString()} in both photos. ")
                    if (commonText.isNotEmpty()) append("Matching text: ${commonText.take(3).joinToString()}. ")
                    if (!supportive) append("There is not enough matching evidence. Ask about unique marks, contents, or purchase details. ")
                    append("Visual similarity ${visual.score}/100 is not an ownership probability. Both people must verify private details and confirm the handover.")
                })
            }
        }
    }

    private suspend fun meaningfulLabels(bitmap: Bitmap): List<String> = classifiedLabels(bitmap).items.map { it.label }

    private suspend fun classifiedLabels(bitmap: Bitmap): ClassifiedClues =
        classifyScanLabels(labels(bitmap).map { LabelClue(it.text, it.confidence) })

    private suspend fun labels(bitmap: Bitmap): List<com.google.mlkit.vision.label.ImageLabel> {
        val input = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val task = try { labeler.process(InputImage.fromBitmap(input, 0)) }
        catch (error: Exception) { input.recycle(); throw error }
        task.addOnCompleteListener { input.recycle() }
        return task.awaitResult()
    }
}
