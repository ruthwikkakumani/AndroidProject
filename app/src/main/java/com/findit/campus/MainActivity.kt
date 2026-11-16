package com.findit.campus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.findit.campus.ui.navigation.FindItApp
import com.findit.campus.ui.theme.FindItTheme

@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FindItTheme {
                FindItApp(widthClass = calculateWindowSizeClass(this).widthSizeClass)
            }
        }
    }
}
