package `in`.deepak.remindme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import `in`.deepak.remindme.data.preferences.ThemeMode

/**
 * Process-wide holder for the active [ThemeMode].
 *
 * It's a Compose [mutableStateOf] so that writing to [mode] (from the Settings
 * picker) recomposes every [RemindMeTheme] in the app immediately — no Activity
 * recreation needed. Seed it once at startup from the stored preference (see
 * `RemindMeApp.onCreate`); the alert Activity can launch cold, so it seeds too.
 */
object ThemeController {
    var mode by mutableStateOf(ThemeMode.System)
    var accent by mutableStateOf(AccentColor.Indigo)
}

/**
 * Top-level theme wrapper.
 *
 * The light/dark decision comes from [ThemeController.mode] by default, so the
 * whole app reacts to the Settings theme picker. "System" follows the OS; the
 * other two force their scheme regardless of the OS setting.
 *
 * On Android 12+ we honour the user's dynamic colour palette (Material You) for
 * the Material surfaces; the app's own [BrandColors] always use the static
 * brand palettes, which carry the real light/dark visuals.
 */
@Composable
fun RemindMeTheme(
    mode: ThemeMode = ThemeController.mode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
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
    val accent = ThemeController.accent
    val base = if (darkTheme) DarkBrandPalette else LightBrandPalette
    val brandPalette = base.copy(
        primary = if (darkTheme) accent.primaryDark else accent.primaryLight,
        onPrimary = if (darkTheme) accent.onPrimaryDark else accent.onPrimaryLight,
        primarySoft = if (darkTheme) accent.softDark else accent.softLight,
    )
    CompositionLocalProvider(LocalBrandPalette provides brandPalette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
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
