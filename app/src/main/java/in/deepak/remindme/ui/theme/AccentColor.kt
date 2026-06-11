package `in`.deepak.remindme.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Selectable accent colours — the app's primary hue, applied on top of the
 * light/dark [BrandPalette] by [RemindMeTheme]. An accent overrides only the
 * three primary-related slots (`primary`, `onPrimary`, `primarySoft`); the
 * neutrals, per-type tiles, and heatmap are unaffected.
 *
 * Each accent carries both a light and a dark triad: in light mode the primary
 * is saturated with white content; in dark mode it lightens (so it reads on the
 * dark surface) and its content darkens. [Indigo] reproduces the original
 * palette exactly, so existing users see no change until they pick another.
 *
 * Persisted by [name] (see `UserPreferences.accentColorName`); an unknown name
 * falls back to [Indigo] via [fromName].
 */
enum class AccentColor(
    val label: String,
    val primaryLight: Color,
    val onPrimaryLight: Color,
    val softLight: Color,
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val softDark: Color,
) {
    Indigo(
        label = "Indigo",
        primaryLight = Color(0xFF6366F1), onPrimaryLight = Color(0xFFFFFFFF), softLight = Color(0xFFE0E2FF),
        primaryDark = Color(0xFF818CF8), onPrimaryDark = Color(0xFF11132E), softDark = Color(0xFF2E2F4D),
    ),
    Blue(
        label = "Blue",
        primaryLight = Color(0xFF2563EB), onPrimaryLight = Color(0xFFFFFFFF), softLight = Color(0xFFDBEAFE),
        primaryDark = Color(0xFF60A5FA), onPrimaryDark = Color(0xFF06122B), softDark = Color(0xFF172554),
    ),
    Teal(
        label = "Teal",
        primaryLight = Color(0xFF0D9488), onPrimaryLight = Color(0xFFFFFFFF), softLight = Color(0xFFCCFBF1),
        primaryDark = Color(0xFF2DD4BF), onPrimaryDark = Color(0xFF04231F), softDark = Color(0xFF134E4A),
    ),
    Emerald(
        label = "Emerald",
        primaryLight = Color(0xFF059669), onPrimaryLight = Color(0xFFFFFFFF), softLight = Color(0xFFD1FAE5),
        primaryDark = Color(0xFF34D399), onPrimaryDark = Color(0xFF032417), softDark = Color(0xFF064E3B),
    ),
    Amber(
        label = "Amber",
        primaryLight = Color(0xFFD97706), onPrimaryLight = Color(0xFFFFFFFF), softLight = Color(0xFFFEF3C7),
        primaryDark = Color(0xFFFBBF24), onPrimaryDark = Color(0xFF2A1A03), softDark = Color(0xFF422006),
    ),
    Rose(
        label = "Rose",
        primaryLight = Color(0xFFE11D48), onPrimaryLight = Color(0xFFFFFFFF), softLight = Color(0xFFFFE4E6),
        primaryDark = Color(0xFFFB7185), onPrimaryDark = Color(0xFF2A0710), softDark = Color(0xFF4C1D27),
    ),
    Violet(
        label = "Violet",
        primaryLight = Color(0xFF7C3AED), onPrimaryLight = Color(0xFFFFFFFF), softLight = Color(0xFFEDE9FE),
        primaryDark = Color(0xFFA78BFA), onPrimaryDark = Color(0xFF1A0B33), softDark = Color(0xFF2E1065),
    );

    companion object {
        fun fromName(name: String?): AccentColor =
            entries.firstOrNull { it.name == name } ?: Indigo
    }
}
