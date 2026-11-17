package com.findit.campus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val Leaf = Color(0xFF8FD6B0)

private val LightColors = lightColorScheme(
    primary = Color(0xFF107957),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F4EA),
    onPrimaryContainer = Color(0xFF084F37),
    secondary = Color(0xFF148460),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF6EF),
    onSecondaryContainer = Color(0xFF042E2C),
    tertiary = Color(0xFF647BEA),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDF0FF),
    onTertiaryContainer = Color(0xFF241542),
    background = Color(0xFFF5F7F5),
    onBackground = Color(0xFF172B43),
    surface = Color(0xFFF5F7F5),
    onSurface = Color(0xFF172B43),
    surfaceVariant = Color(0xFFEDF1EC),
    onSurfaceVariant = Color(0xFF586779),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F4EF),
    surfaceContainerHigh = Color(0xFFE8EEE7),
    surfaceContainerHighest = Color(0xFFE0E7DF),
    outline = Color(0xFF9AA6B8),
    outlineVariant = Color(0xFFDFE6E2),
    inverseSurface = Color(0xFF153C3A),
    inverseOnSurface = Color(0xFFF4FAF7),
    inversePrimary = Color(0xFF94E0BB),
    error = Color(0xFFB42318),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Leaf,
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF0E4F36),
    onPrimaryContainer = Color(0xFFBFE8D2),
    secondary = Leaf,
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF163E36),
    onSecondaryContainer = Color(0xFFBFE8D2),
    tertiary = Color(0xFFB7C1FF),
    onTertiary = Color(0xFF242F69),
    tertiaryContainer = Color(0xFF313E7A),
    onTertiaryContainer = Color(0xFFE1E5FF),
    background = Color(0xFF121C29),
    onBackground = Color(0xFFEAF0F8),
    surface = Color(0xFF121C29),
    onSurface = Color(0xFFEAF0F8),
    surfaceVariant = Color(0xFF293545),
    onSurfaceVariant = Color(0xFFBBC8D8),
    surfaceContainerLowest = Color(0xFF0B1320),
    surfaceContainerLow = Color(0xFF1B2839),
    surfaceContainer = Color(0xFF223045),
    surfaceContainerHigh = Color(0xFF2A3950),
    surfaceContainerHighest = Color(0xFF31425A),
    outline = Color(0xFF95A5BA),
    outlineVariant = Color(0xFF3B4A5F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Immutable
data class FindItColors(
    val lost: Color,
    val onLost: Color,
    val lostContainer: Color,
    val found: Color,
    val onFound: Color,
    val foundContainer: Color,
    val reunion: Color,
)

private val LightExtra = FindItColors(
    lost = Color(0xFFB5473A),
    onLost = Color.White,
    lostContainer = Color(0xFFFDE8E4),
    found = Color(0xFF148460),
    onFound = Color.White,
    foundContainer = Color(0xFFE7F5EE),
    reunion = Color(0xFF148460),
)

private val DarkExtra = FindItColors(
    lost = Color(0xFFFFB4A4),
    onLost = Color(0xFF5C1A0E),
    lostContainer = Color(0xFF6B2E22),
    found = Leaf,
    onFound = Color(0xFF003824),
    foundContainer = Color(0xFF163E36),
    reunion = Leaf,
)

private val LocalFindItColors = staticCompositionLocalOf { LightExtra }

private val FindItType = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = .3.sp, fontWeight = FontWeight.SemiBold),
)

private val FindItShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
)

object FindItPalette {
    val colors: FindItColors
        @Composable get() = LocalFindItColors.current
}

@Composable
fun FindItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalFindItColors provides if (darkTheme) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = FindItType,
            shapes = FindItShapes,
            content = content,
        )
    }
}
