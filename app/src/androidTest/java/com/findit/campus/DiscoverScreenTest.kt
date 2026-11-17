package com.findit.campus

import androidx.compose.runtime.*
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import com.findit.campus.model.Item
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType
import com.findit.campus.ui.components.ItemCard
import com.findit.campus.ui.home.DiscoverScreen
import com.findit.campus.ui.home.HomeUiState
import com.findit.campus.model.PulseInsight
import com.findit.campus.ui.theme.FindItTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiscoverScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun cardShowsTitlePlaceAndLostLabel() {
        var opened = false
        rule.setContent {
            FindItTheme {
                ItemCard(
                    item = sample(),
                    matchCount = 1,
                    onClick = { opened = true },
                )
            }
        }
        rule.onNodeWithText("Navy bottle").assertIsDisplayed()
        rule.onNodeWithText("Library").assertIsDisplayed()
        rule.onNodeWithContentDescription("Lost").assertIsDisplayed()
        rule.onNodeWithText("Navy bottle").performClick()
        rule.runOnIdle { assertTrue(opened) }
    }

    @Test
    fun searchFieldAcceptsAQuery() {
        var query = ""
        rule.setContent {
            var search by remember { mutableStateOf("") }
            FindItTheme {
                DiscoverScreen(
                    state = HomeUiState(
                        loading = false,
                        query = search,
                        pulse = PulseInsight(
                            headline = "Bottles turn up most often around Library.",
                            detail = "2 open reports",
                            busiestZone = "Library",
                            topCategory = "Bottles",
                            openCount = 2,
                        ),
                        items = listOf(sample()),
                    ),
                    expanded = false,
                    onQuery = { query = it; search = it },
                    onType = {},
                    onOpenOnly = {},
                    onCategory = {},
                    onClear = {},
                    onOpenItem = {},
                    onSelect = {},
                    onReunite = {},
                    onDelete = {},
                )
            }
        }
        rule.onNode(hasSetTextAction()).performTextInput("keys")
        rule.runOnIdle { assertTrue(query.contains("keys")) }
    }

    private fun sample() = Item(
        id = 7,
        title = "Navy bottle",
        description = "Navy bottle with a mountain sticker",
        category = "Bottles",
        type = ItemType.LOST,
        status = ItemStatus.OPEN,
        zoneName = "Library",
        createdAt = 1_700_000_000_000L,
    )
}
