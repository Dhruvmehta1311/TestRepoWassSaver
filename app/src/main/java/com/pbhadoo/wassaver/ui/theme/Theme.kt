package com.pbhadoo.wassaver.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// WhatsApp-inspired green palette
val Green10 = Color(0xFF002106)
val Green20 = Color(0xFF00390D)
val Green30 = Color(0xFF005317)
val Green40 = Color(0xFF006E21)
val Green80 = Color(0xFF79DD72)
val Green90 = Color(0xFF95F98D)

val Teal40 = Color(0xFF007A6A)
val Teal80 = Color(0xFF53DCCB)
val Teal90 = Color(0xFF70F9E8)

val DarkGreenContainer = Color(0xFF00522B)
val LightGreenContainer = Color(0xFFB7F5B0)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Teal80,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Teal90,
    background = Color(0xFF0E1A0F),
    onBackground = Color(0xFFE0E8E0),
    surface = Color(0xFF121F13),
    onSurface = Color(0xFFE0E8E0),
    surfaceVariant = Color(0xFF1E2D1F),
    onSurfaceVariant = Color(0xFFB8C9B9),
    outline = Color(0xFF4E6B50),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = LightGreenContainer,
    onPrimaryContainer = Green10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EFEA),
    onSecondaryContainer = Color(0xFF00201C),
    background = Color(0xFFF5FBF4),
    onBackground = Color(0xFF0E1A0F),
    surface = Color.White,
    onSurface = Color(0xFF0E1A0F),
    surfaceVariant = Color(0xFFDCEADC),
    onSurfaceVariant = Color(0xFF3F4F40),
    outline = Color(0xFF6F8070),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun WASSaverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
