package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TrueAmoledColorScheme = darkColorScheme(
    primary = CalibrationEmerald,
    onPrimary = Color(0xFF00210E),
    primaryContainer = CalibrationEmeraldDim,
    onPrimaryContainer = CalibrationEmerald,
    secondary = ScannerAmber,
    onSecondary = Color(0xFF261900),
    secondaryContainer = ScannerAmberDim,
    onSecondaryContainer = ScannerAmber,
    tertiary = OpticalCyan,
    onTertiary = Color(0xFF001F2E),
    tertiaryContainer = OpticalCyanDim,
    onTertiaryContainer = OpticalCyan,
    background = AmoledPitchBlack,
    onBackground = TextPrimary,
    surface = AmoledDeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmoledElevatedCard,
    onSurfaceVariant = TextSecondary,
    outline = AmoledBorderSubtle,
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use True AMOLED pitch-black scheme to minimize subpixel power and glare
    MaterialTheme(
        colorScheme = TrueAmoledColorScheme,
        typography = Typography,
        content = content
    )
}
