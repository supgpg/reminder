package com.reminder.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Gray100,
    onPrimaryContainer = Gray900,
    secondary = Gray600,
    onSecondary = White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray800,
    tertiary = AccentBlue,
    onTertiary = White,
    tertiaryContainer = AccentBlueContainer,
    onTertiaryContainer = Gray900,
    background = White,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    outline = Gray300,
    outlineVariant = Gray200,
    error = ErrorRed,
    onError = White,
    errorContainer = ErrorContainer,
    onErrorContainer = Gray900,
    scrim = Black,
    inverseSurface = Gray900,
    inverseOnSurface = Gray100,
    inversePrimary = Gray300,
)

@Composable
fun ReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
