package az.fleetra.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Fleetra is white + orange only (see Color.kt) — dynamic/system colors and
// a separate dark palette would break that brand rule, so both are disabled
// below regardless of what the device's wallpaper or system theme want.
private val FleetraLightColorScheme = lightColorScheme(
    primary = FleetraOrange,
    onPrimary = FleetraWhite,
    primaryContainer = FleetraOrangeLight,
    onPrimaryContainer = FleetraOrangeDark,
    secondary = FleetraOrangeDark,
    background = FleetraWhite,
    surface = FleetraWhite,
    onBackground = FleetraInk,
    onSurface = FleetraInk,
    error = FleetraDanger,
    errorContainer = FleetraDangerBg,
)

@Composable
fun FleetraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FleetraLightColorScheme,
        typography = Typography,
        content = content
    )
}