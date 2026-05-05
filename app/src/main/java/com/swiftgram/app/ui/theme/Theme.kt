package com.swiftgram.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * SwiftGram color palette.
 * 
 * Primary: Telegram blue (#0088CC)
 * Secondary: Accent blue
 * Tertiary: Teal accent
 * 
 * The color scheme is designed to be modern, clean, and consistent
 * with Telegram's visual identity while maintaining Material 3 compliance.
 */

// Light theme colors
private val LightColors = lightColorScheme(
    primary = Color(0xFF0088CC),           // Telegram blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF001F5C),
    
    secondary = Color(0xFF0088CC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E8FF),
    onSecondaryContainer = Color(0xFF001F5C),
    
    tertiary = Color(0xFF00A699),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA0F0E8),
    onTertiaryContainer = Color(0xFF002019),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF666666),
    
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

// Dark theme colors
private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DADE2),           // Light Telegram blue
    onPrimary = Color(0xFF001F5C),
    primaryContainer = Color(0xFF003A8A),
    onPrimaryContainer = Color(0xFFD6E8FF),
    
    secondary = Color(0xFF5DADE2),
    onSecondary = Color(0xFF001F5C),
    secondaryContainer = Color(0xFF003A8A),
    onSecondaryContainer = Color(0xFFD6E8FF),
    
    tertiary = Color(0xFF83D8CE),
    onTertiary = Color(0xFF003A33),
    tertiaryContainer = Color(0xFF00524A),
    onTertiaryContainer = Color(0xFFA0F0E8),
    
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF999999),
    
    error = Color(0xFFF9DEDC),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8B1A1A),
    onErrorContainer = Color(0xFFF9DEDC)
)

/**
 * SwiftGram theme composable.
 * 
 * Supports light, dark, and dynamic theming based on device capabilities.
 * On Android 12+, uses Material You dynamic theming if available.
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use dynamic theming on Android 12+ (defaults to true)
 * @param content The content to apply the theme to
 */
@Composable
fun SwiftGramTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Use dynamic theming on Android 12+
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SwiftGramTypography,
        shapes = SwiftGramShapes,
        content = content
    )
}
