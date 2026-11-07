package com.voidx.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.voidx.client.domain.model.ThemeMode

// Original VoidX palette: deep purple-black base, violet accent, blue details, green PLAY.
val VoidBlack = Color(0xFF0C0716)
val VoidSurface = Color(0xFF181026)
val VoidSurfaceHigh = Color(0xFF241838)
val VoidViolet = Color(0xFFA855F7)
val VoidVioletSoft = Color(0xFF3B1F5C)
val VoidBlue = Color(0xFF60A5FA)
val VoidGreen = Color(0xFF22C55E)
val VoidGreenDeep = Color(0xFF15803D)
val VoidMuted = Color(0xFFA79BBF)
val VoidError = Color(0xFFF87171)

private val DarkScheme = darkColorScheme(
    primary = VoidViolet,
    onPrimary = Color.White,
    primaryContainer = VoidVioletSoft,
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = VoidGreen,
    onSecondary = Color.Black,
    tertiary = VoidBlue,
    onTertiary = Color.Black,
    background = VoidBlack,
    onBackground = Color(0xFFF5F3FF),
    surface = VoidSurface,
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = VoidSurfaceHigh,
    onSurfaceVariant = VoidMuted,
    error = VoidError,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF7E22CE),
    onPrimary = Color.White,
    secondary = VoidGreenDeep,
    onSecondary = Color.White,
    tertiary = Color(0xFF2563EB),
    onTertiary = Color.White,
    error = Color(0xFFB91C1C),
)

@Composable
fun VoidXTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content,
    )
}
