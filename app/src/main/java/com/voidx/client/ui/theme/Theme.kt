package com.voidx.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.voidx.client.domain.model.ThemeMode

// Original VoidX palette: near-black base, violet accent, Bedrock-style green for PLAY.
val VoidBlack = Color(0xFF07070C)
val VoidSurface = Color(0xFF12121B)
val VoidSurfaceHigh = Color(0xFF1C1C29)
val VoidViolet = Color(0xFF8B5CF6)
val VoidVioletSoft = Color(0xFF3B2A6B)
val VoidGreen = Color(0xFF22C55E)
val VoidGreenDeep = Color(0xFF15803D)
val VoidMuted = Color(0xFF9CA3AF)
val VoidError = Color(0xFFF87171)

private val DarkScheme = darkColorScheme(
    primary = VoidViolet,
    onPrimary = Color.White,
    primaryContainer = VoidVioletSoft,
    onPrimaryContainer = Color(0xFFEDE4FF),
    secondary = VoidGreen,
    onSecondary = Color.Black,
    background = VoidBlack,
    onBackground = Color(0xFFF3F4F6),
    surface = VoidSurface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = VoidSurfaceHigh,
    onSurfaceVariant = VoidMuted,
    error = VoidError,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF6D3BE0),
    onPrimary = Color.White,
    secondary = VoidGreenDeep,
    onSecondary = Color.White,
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
