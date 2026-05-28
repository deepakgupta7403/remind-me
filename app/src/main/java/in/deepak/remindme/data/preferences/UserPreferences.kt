package `in`.deepak.remindme.data.preferences

import android.content.Context

/**
 * User profile bits captured during onboarding (and later, settings).
 *
 * Kept separate from [OnboardingPreferences] because the latter is a one-shot
 * gate, while this is mutable identity that the user can change later — mixing
 * them invites accidental flag resets when the user just edits their name.
 *
 * Storage is plain SharedPreferences; the values are small scalars and we don't
 * need a reactive stream — Home re-reads them per emission (every tick) and the
 * Profile editor reads them once when opened.
 */
class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) { prefs.edit().putString(KEY_NAME, value.trim()).apply() }

    /** Index into the avatar accent palette shown on the Profile screen. */
    var avatarColorIndex: Int
        get() = prefs.getInt(KEY_AVATAR, 0)
        set(value) { prefs.edit().putInt(KEY_AVATAR, value).apply() }

    /** Master switch for the Home greeting line. Off → Home shows only the count. */
    var showGreeting: Boolean
        get() = prefs.getBoolean(KEY_SHOW_GREETING, true)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_GREETING, value).apply() }

    /** Wording style for the greeting when [showGreeting] is on. */
    var greetingStyle: GreetingStyle
        get() = GreetingStyle.fromName(prefs.getString(KEY_GREETING_STYLE, null))
        set(value) { prefs.edit().putString(KEY_GREETING_STYLE, value.name).apply() }

    private companion object {
        const val FILE = "remindme.user"
        const val KEY_NAME = "name"
        const val KEY_AVATAR = "avatar_color"
        const val KEY_SHOW_GREETING = "show_greeting"
        const val KEY_GREETING_STYLE = "greeting_style"
    }
}

/**
 * How the Home greeting reads. Persisted by [GreetingStyle.name] so adding a
 * value never breaks older stored data — an unknown name falls back to
 * [TimeBased] via [fromName].
 */
enum class GreetingStyle(val label: String) {
    /** "Good morning / afternoon / evening" by the clock. */
    TimeBased("Time based"),

    /** Always a plain "Hello". */
    AlwaysHello("Always \"Hello\"");

    /** The next style in the list, wrapping — drives the tap-to-cycle row. */
    fun next(): GreetingStyle = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): GreetingStyle =
            entries.firstOrNull { it.name == name } ?: TimeBased
    }
}
