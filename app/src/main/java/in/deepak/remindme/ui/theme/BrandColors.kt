package `in`.deepak.remindme.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fixed brand palette, now theme-aware.
 *
 * Why a Compose [staticCompositionLocalOf] instead of `colorResource`:
 *   - The Theme selector (Settings) lets users force Light or Dark independent
 *     of the OS. `colorResource` resolves against the Activity's `uiMode`, so a
 *     forced "Dark" while the system is Light wouldn't pick up `values-night`.
 *     Driving the palette from a CompositionLocal makes the choice apply
 *     instantly across every screen, regardless of the system setting.
 *   - Accents (primary, danger, the per-type tiles) intentionally keep their
 *     saturated brand hues in both modes; only the neutrals and text invert.
 *
 * The design comp at `app/sampledata/onboarding.png` remains the source of truth
 * for the light palette; the dark values are derived from it and can be re-tuned
 * once a dark comp lands.
 *
 * Call sites are unchanged — `BrandColors.Primary`, `BrandColors.Tile.DailyFg`,
 * etc. still read as composable properties; they now resolve through
 * [LocalBrandPalette], which [RemindMeTheme] provides.
 */
object BrandColors {
    val Primary: Color       @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.primary
    val OnPrimary: Color     @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.onPrimary
    val PrimarySoft: Color   @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.primarySoft
    val Danger: Color        @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.danger

    val TextHeading: Color   @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.textHeading
    val TextBody: Color      @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.textBody
    val DotInactive: Color   @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.dotInactive

    val PageBackground: Color @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.pageBackground
    val SurfaceCard: Color   @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.surfaceCard
    val SurfaceDim: Color    @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.surfaceDim
    val BorderSubtle: Color  @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.borderSubtle

    /** Per-reminder-type tile colours. Background + foreground for the icon. */
    object Tile {
        val IntervalBg: Color @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.intervalBg
        val IntervalFg: Color @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.intervalFg
        val OnceBg: Color     @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.onceBg
        val OnceFg: Color     @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.onceFg
        val DailyBg: Color    @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.dailyBg
        val DailyFg: Color    @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.dailyFg
        val WeeklyBg: Color   @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.weeklyBg
        val WeeklyFg: Color   @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.weeklyFg
        val SkyBg: Color      @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.skyBg
        val SkyFg: Color      @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.skyFg
    }

    /** GitHub-style intensity scale for the Stats heatmap (level 0 = empty). */
    object Heat {
        val Level0: Color @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.heat0
        val Level1: Color @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.heat1
        val Level2: Color @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.heat2
        val Level3: Color @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.heat3
        val Level4: Color @Composable @ReadOnlyComposable get() = LocalBrandPalette.current.heat4
    }
}

/**
 * Every brand colour for one mode. Two instances exist — [LightBrandPalette] and
 * [DarkBrandPalette]. Adding a brand colour = add a field here plus a value in
 * both palettes, then a getter in [BrandColors].
 */
data class BrandPalette(
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val danger: Color,
    val textHeading: Color,
    val textBody: Color,
    val dotInactive: Color,
    val pageBackground: Color,
    val surfaceCard: Color,
    val surfaceDim: Color,
    val borderSubtle: Color,
    val intervalBg: Color, val intervalFg: Color,
    val onceBg: Color, val onceFg: Color,
    val dailyBg: Color, val dailyFg: Color,
    val weeklyBg: Color, val weeklyFg: Color,
    val skyBg: Color, val skyFg: Color,
    val heat0: Color, val heat1: Color, val heat2: Color, val heat3: Color, val heat4: Color,
)

/** Light palette — the original values from `res/values/colors.xml`. */
val LightBrandPalette = BrandPalette(
    primary = Color(0xFF6366F1),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFE0E2FF),
    danger = Color(0xFFE53935),
    textHeading = Color(0xFF111114),
    textBody = Color(0xFF5F6172),
    dotInactive = Color(0x33111114),
    pageBackground = Color(0xFFF7F7FB),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFEEEEF4),
    borderSubtle = Color(0xFFE6E6EE),
    intervalBg = Color(0xFFE0E4FF), intervalFg = Color(0xFF4F46E5),
    onceBg = Color(0xFFFCE7F0), onceFg = Color(0xFFDB2777),
    dailyBg = Color(0xFFFFE9C2), dailyFg = Color(0xFFEA580C),
    weeklyBg = Color(0xFFD4F0DC), weeklyFg = Color(0xFF16A34A),
    skyBg = Color(0xFFE0F2FE), skyFg = Color(0xFF2563EB),
    heat0 = Color(0xFFEAEDED),
    heat1 = Color(0xFFA8DEC5), heat2 = Color(0xFF6BC8A4), heat3 = Color(0xFF2EB37F), heat4 = Color(0xFF15803D),
)

/**
 * Dark palette. Accents (primary, danger, tile foregrounds) keep their hue so
 * the brand still reads; neutrals invert and tile backgrounds drop to muted
 * dark tints so the saturated foregrounds pop against them.
 */
val DarkBrandPalette = BrandPalette(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF11132E),
    primarySoft = Color(0xFF2E2F4D),
    danger = Color(0xFFFF6B66),
    textHeading = Color(0xFFF2F2F5),
    textBody = Color(0xFFA8AAB8),
    dotInactive = Color(0x33FFFFFF),
    pageBackground = Color(0xFF111114),
    surfaceCard = Color(0xFF1C1C22),
    surfaceDim = Color(0xFF26262E),
    borderSubtle = Color(0xFF33343E),
    intervalBg = Color(0xFF262A4D), intervalFg = Color(0xFFA5B4FC),
    onceBg = Color(0xFF3D2433), onceFg = Color(0xFFF9A8D4),
    dailyBg = Color(0xFF3D3320), dailyFg = Color(0xFFFDBA74),
    weeklyBg = Color(0xFF1E3A2A), weeklyFg = Color(0xFF86EFAC),
    skyBg = Color(0xFF1E3147), skyFg = Color(0xFF93C5FD),
    heat0 = Color(0xFF26262E),
    heat1 = Color(0xFF1E5540), heat2 = Color(0xFF2EB37F), heat3 = Color(0xFF4ECf9A), heat4 = Color(0xFF86EFAC),
)

/** Palette for the current mode; [RemindMeTheme] swaps it for light vs dark. */
val LocalBrandPalette = staticCompositionLocalOf { LightBrandPalette }
