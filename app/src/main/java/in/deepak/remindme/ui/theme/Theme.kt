package `in`.deepak.remindme.ui.theme

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
 * Top-level theme wrapper.
 *
 * On Android 12+ we honour the user's dynamic colour palette (Material You).
 * On older OS levels we fall back to the static palette below. The static
 * palette is tuned to be calm — reminder apps that scream for attention are
 * exhausting.
 */
@Composable
fun RemindMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor -> if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E4FF),
    secondary = Color(0xFF009688),
    background = Color(0xFFFAFAFC),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB0BAF1),
    onPrimary = Color(0xFF1A237E),
    primaryContainer = Color(0xFF303F9F),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF111114),
    surface = Color(0xFF1B1B1F),
)
