package com.keval.fieldpintask.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Green80, onPrimary = Color(0xFF00391F), primaryContainer = Color(0xFF00522F), onPrimaryContainer = Color(0xFFABF4C9),
    secondary = GreenGrey80, onSecondary = Color(0xFF1F352A), secondaryContainer = Color(0xFF354B40), onSecondaryContainer = Color(0xFFCFE9DA),
    tertiary = Teal80, onTertiary = Color(0xFF033640), tertiaryContainer = Color(0xFF214C57), onTertiaryContainer = Color(0xFFBFE9F8),
    background = Color(0xFF0F1512), onBackground = Color(0xFFDEE4DE),
    surface = Color(0xFF0F1512), onSurface = Color(0xFFDEE4DE),
    surfaceVariant = Color(0xFF3F4943), onSurfaceVariant = Color(0xFFBFC9C1), outline = Color(0xFF89938C),
    surfaceContainerLowest = Color(0xFF0A0F0D), surfaceContainerLow = Color(0xFF171D1A), surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28), surfaceContainerHighest = Color(0xFF303633),
)

private val LightColorScheme = lightColorScheme(
    primary = Green40, onPrimary = Color.White, primaryContainer = Color(0xFFABF4C9), onPrimaryContainer = Color(0xFF002112),
    secondary = GreenGrey40, onSecondary = Color.White, secondaryContainer = Color(0xFFCFE9DA), onSecondaryContainer = Color(0xFF0A1F15),
    tertiary = Teal40, onTertiary = Color.White, tertiaryContainer = Color(0xFFBFE9F8), onTertiaryContainer = Color(0xFF001F27),
    background = Color(0xFFF5FBF5), onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FBF5), onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DD), onSurfaceVariant = Color(0xFF3F4943), outline = Color(0xFF6F7A72),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFEFF5EF), surfaceContainer = Color(0xFFE9F0EA),
    surfaceContainerHigh = Color(0xFFE3EAE4), surfaceContainerHighest = Color(0xFFDEE4DE),
)

@Composable
fun FieldPinTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
