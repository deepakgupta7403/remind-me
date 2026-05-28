package `in`.deepak.remindme.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import `in`.deepak.remindme.R

/**
 * Fixed brand palette, sourced from `res/values/colors.xml`.
 *
 * Why a separate palette instead of `MaterialTheme.colorScheme`:
 *   - The design comp at `app/sampledata/onboarding.png` is the source of
 *     truth. The app theme enables Material You dynamic colours, which would
 *     otherwise recolour these screens from the user's wallpaper.
 *   - Defining the values in XML (not as Kotlin constants) lets them be
 *     reused from drawables, themes, and previews without duplication.
 *
 * Adding a new brand colour:
 *   1. Add `<color name="…">` to `res/values/colors.xml`.
 *   2. Add a `val … : Color @Composable @ReadOnlyComposable get() = …` line
 *      here. That's it — call sites pick it up immediately.
 */
object BrandColors {
    val Primary: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.brand_primary)
    val OnPrimary: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.brand_on_primary)
    val PrimarySoft: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.brand_soft)
    val Danger: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.brand_danger)

    val TextHeading: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.text_heading)
    val TextBody: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.text_body)
    val DotInactive: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.dot_inactive)

    val PageBackground: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.page_background)
    val SurfaceCard: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.surface_card)
    val SurfaceDim: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.surface_dim)
    val BorderSubtle: Color
        @Composable @ReadOnlyComposable get() = colorResource(R.color.border_subtle)

    /** Per-reminder-type tile colours. Background + foreground for the icon. */
    object Tile {
        val IntervalBg: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_interval_bg)
        val IntervalFg: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_interval_fg)
        val OnceBg: Color     @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_once_bg)
        val OnceFg: Color     @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_once_fg)
        val DailyBg: Color    @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_daily_bg)
        val DailyFg: Color    @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_daily_fg)
        val WeeklyBg: Color   @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_weekly_bg)
        val WeeklyFg: Color   @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_weekly_fg)
        val SkyBg: Color      @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_sky_bg)
        val SkyFg: Color      @Composable @ReadOnlyComposable get() = colorResource(R.color.tile_sky_fg)
    }

    /** GitHub-style intensity scale for the Stats heatmap (level 0 = empty). */
    object Heat {
        val Level0: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.heat_0)
        val Level1: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.heat_1)
        val Level2: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.heat_2)
        val Level3: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.heat_3)
        val Level4: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.heat_4)
    }
}
