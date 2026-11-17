package com.findit.campus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.findit.campus.ai.PhotoCheckEngine
import com.findit.campus.ai.TextScanner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry

class OnDeviceAiTest {
    @Test fun bundledImageRecognitionSuppliesCluesForAnObjectPhoto() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap = context.assets.open("recognition-dog.jpg").use { BitmapFactory.decodeStream(it) }
        try {
            val clues = PhotoCheckEngine(TextScanner()).scan(bitmap)
            assertTrue("The item image should supply at least one recognized object clue", clues.objects.isNotEmpty())
            assertTrue("The bundled model should recognize the photographed dog", clues.objects.any { it.contains("dog") })
        } finally { bitmap.recycle() }
    }

    @Test fun bundledOcrReadsARealBitmapAndPhotoEvidenceRemainsAdvisory() = runBlocking {
        val bitmap = Bitmap.createBitmap(640, 320, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawText("FINDIT BOTTLE", 30f, 120f, Paint().apply { color = Color.BLACK; textSize = 60f; isAntiAlias = true })
            drawText("STUDENT 1284", 30f, 220f, Paint().apply { color = Color.BLACK; textSize = 50f; isAntiAlias = true })
        }
        try {
            val scanner = TextScanner()
            assertTrue(scanner.read(bitmap).uppercase().contains("FINDIT"))
            val checker = PhotoCheckEngine(scanner)
            assertTrue(checker.scan(bitmap).text.uppercase().contains("FINDIT"))
            val evidence = checker.compare(bitmap, bitmap)
            assertTrue(evidence.sameItem)
            assertTrue(evidence.explanation.contains("not an ownership probability"))
            assertTrue(evidence.explanation.contains("Both people must verify"))
        } finally { bitmap.recycle() }
    }
}
