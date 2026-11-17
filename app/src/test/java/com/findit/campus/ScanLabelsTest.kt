package com.findit.campus

import com.findit.campus.ai.LabelClue
import com.findit.campus.ai.classifyScanLabels
import org.junit.Assert.*
import org.junit.Test

class ScanLabelsTest {
    @Test fun emulatorRoomCluesDoNotBecomeItemSuggestions() {
        val result = classifyScanLabels(listOf("room", "tile", "shelf", "space", "building")
            .map { LabelClue(it, .95f) })
        assertTrue(result.items.isEmpty())
        assertEquals(listOf("room", "tile", "shelf", "space", "building"), result.scene.map { it.label })
    }

    @Test fun itemSurvivesHigherConfidenceBackgroundAndDuplicateLabels() {
        val result = classifyScanLabels(listOf(LabelClue("Room", .99f), LabelClue("Bag", .87f),
            LabelClue(" bag ", .72f), LabelClue("Bottle", .76f), LabelClue("Product", .98f)))
        assertEquals(listOf("bag", "bottle"), result.items.map { it.label })
        assertEquals(.87f, result.items.first().confidence, .001f)
        assertEquals(listOf("room"), result.scene.map { it.label })
    }

    @Test fun uncertainAndInvalidScoresAreNotShownAsConfidentClues() {
        val result = classifyScanLabels(listOf(LabelClue("wallet", .64f), LabelClue("keys", Float.NaN),
            LabelClue("watch", 1.5f), LabelClue("", .9f), LabelClue("notebook", .65f)))
        assertEquals(listOf("notebook"), result.items.map { it.label })
        assertTrue(result.scene.isEmpty())
    }
}
