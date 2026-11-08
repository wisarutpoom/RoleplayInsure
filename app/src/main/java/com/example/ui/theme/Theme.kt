package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SiamDarkColorScheme =
  darkColorScheme(
    primary = GoldLight,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF3B2A08),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color(0xFF083344),
    background = NavyBackground,
    onBackground = SlateTextPrimary,
    surface = NavySurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    error = CoralAlert,
    onError = Color.White,
  )

private val SiamLightColorScheme =
  lightColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = CyanWaveform,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = IvoryTextPrimary,
    surface = IvorySurface,
    onSurface = IvoryTextPrimary,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = IvoryTextSecondary,
    error = CoralAlert,
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) SiamDarkColorScheme else SiamLightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
