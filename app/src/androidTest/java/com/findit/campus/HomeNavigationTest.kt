package com.findit.campus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.*
import com.findit.campus.ui.navigation.CampusNavigationBar
import com.findit.campus.ui.navigation.openCampusTab
import com.findit.campus.ui.theme.FindItTheme
import org.junit.Rule
import org.junit.Test

class HomeNavigationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun realHomeTabReturnsFromEveryTabAndRemainsRepeatable() {
        compose.setContent {
            FindItTheme {
                val nav = rememberNavController()
                val entry by nav.currentBackStackEntryAsState()
                Column {
                    NavHost(nav, "discover", Modifier.weight(1f)) {
                        listOf("discover", "map", "inbox", "scan", "mine").forEach { route ->
                            composable(route) { Text("Page $route") }
                        }
                    }
                    CampusNavigationBar(entry?.destination?.route.orEmpty()) { nav.openCampusTab(it, "discover") }
                }
            }
        }
        for ((label, route) in listOf("Map" to "map", "Messages" to "inbox", "Scan" to "scan", "Profile" to "mine")) {
            compose.onNodeWithText(label).performClick()
            compose.onNodeWithText("Page $route").assertIsDisplayed()
            compose.onNodeWithText("Home").performClick()
            compose.onNodeWithText("Page discover").assertIsDisplayed()
            compose.onNodeWithText("Home").performClick()
            compose.onNodeWithText("Page discover").assertIsDisplayed()
        }
    }
}
