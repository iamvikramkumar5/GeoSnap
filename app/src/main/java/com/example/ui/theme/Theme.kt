package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Default Dark Theme (Deep Slate / Neon Green Accent)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color.Black,
    secondary = Color(0xFF00B0FF),
    onSecondary = Color.Black,
    tertiary = Color(0xFF7C4DFF),
    background = Color(0xFF0C0E12),
    surface = Color(0xFF161A22),
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF222834),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF333D4F)
)

// Snow White / Shadcn UI Light Theme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F172A),       // Shadcn primary slate black
    onPrimary = Color.White,
    secondary = Color(0xFF0284C7),     // Sky blue accent
    onSecondary = Color.White,
    tertiary = Color(0xFF0D9488),      // Teal accent
    background = Color(0xFFFAFAFA),    // Snow white background
    surface = Color(0xFFFFFFFF),       // Pure white card surface
    onBackground = Color(0xFF0F172A),  // Deep slate text
    onSurface = Color(0xFF0F172A),     // Deep slate text
    surfaceVariant = Color(0xFFF1F5F9),// Crisp light slate pill/input
    onSurfaceVariant = Color(0xFF64748B), // Slate gray subtitle
    outline = Color(0xFFE2E8F0)        // Clean border line
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark theme as requested
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
