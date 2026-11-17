package com.findit.campus

import android.os.Bundle
import android.content.Intent
import com.findit.campus.notifications.CampusNotifications
import com.findit.campus.notifications.AlertDestination
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.findit.campus.ui.navigation.FindItApp
import com.findit.campus.ui.theme.FindItTheme

@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    private val alert = MutableStateFlow<AlertDestination?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alert.value = CampusNotifications.destination(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        alert.value = CampusNotifications.destination(intent)
        enableEdgeToEdge()
        setContent {
            FindItTheme {
                val destination by alert.collectAsStateWithLifecycle()
                FindItApp(widthClass = calculateWindowSizeClass(this).widthSizeClass,
                    alert = destination, onAlertConsumed = { alert.value = null })
            }
        }
    }
}
