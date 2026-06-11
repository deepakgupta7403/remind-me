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

    /**
     * The alarm sound the user picked in Settings, as a URI string. Three states:
     *  - `null` → never chosen; the alarm falls back to the system alarm tone.
     *  - `""`   → the user explicitly picked "Silent".
     *  - else   → a `content://` ringtone URI.
     *
     * Read at fire time by `ReminderAlertActivity`; the notification channel's
     * own sound is frozen at creation, so the chosen tone is applied by the
     * full-screen alarm's looping ringtone rather than the channel.
     */
    var alarmSoundUri: String?
        get() = if (prefs.contains(KEY_SOUND_URI)) prefs.getString(KEY_SOUND_URI, null).orEmpty() else null
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_SOUND_URI) else putString(KEY_SOUND_URI, value)
            }.apply()
        }

    /** Human-readable name of [alarmSoundUri], shown as the Settings row value. */
    var alarmSoundLabel: String
        get() = prefs.getString(KEY_SOUND_LABEL, DEFAULT_SOUND_LABEL).orEmpty()
        set(value) { prefs.edit().putString(KEY_SOUND_LABEL, value).apply() }

    /**
     * Whether the full-screen alarm vibrates. Defaults to on. Read at fire time
     * by `ReminderAlertActivity`; like the sound, the channel's own vibration is
     * frozen at creation, so this gates the sustained alarm vibration instead.
     */
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) { prefs.edit().putBoolean(KEY_VIBRATION, value).apply() }

    /**
     * How many minutes the alert screen's "Snooze" action defers a reminder by.
     * Defaults to 10. Read at snooze time by `ReminderAlertActivity`.
     */
    var snoozeMinutes: Int
        get() = prefs.getInt(KEY_SNOOZE_MIN, 10)
        set(value) { prefs.edit().putInt(KEY_SNOOZE_MIN, value).apply() }

    /**
     * Quiet hours ("Do not disturb"). When on, reminders still fire and show the
     * full-screen alert during the window — they're just silenced (no sound or
     * vibration). Off by default so upgrading users aren't silently muted.
     */
    var dndEnabled: Boolean
        get() = prefs.getBoolean(KEY_DND, false)
        set(value) { prefs.edit().putBoolean(KEY_DND, value).apply() }

    /** Quiet-hours start as minutes from midnight. Default 22:30. */
    var dndStartMinute: Int
        get() = prefs.getInt(KEY_DND_START, DEFAULT_DND_START)
        set(value) { prefs.edit().putInt(KEY_DND_START, value).apply() }

    /** Quiet-hours end as minutes from midnight. Default 07:00. */
    var dndEndMinute: Int
        get() = prefs.getInt(KEY_DND_END, DEFAULT_DND_END)
        set(value) { prefs.edit().putInt(KEY_DND_END, value).apply() }

    /**
     * True when DND is on and [minuteOfDay] (minutes from midnight) falls inside
     * the quiet-hours window. Handles windows that wrap past midnight, e.g.
     * 22:30 → 07:00, where start > end.
     */
    fun isQuietAt(minuteOfDay: Int): Boolean {
        if (!dndEnabled) return false
        val start = dndStartMinute
        val end = dndEndMinute
        return if (start <= end) minuteOfDay in start until end
        else minuteOfDay >= start || minuteOfDay < end
    }

    /** Light / Dark / follow-System. Drives [in.deepak.remindme.ui.theme.RemindMeTheme]. */
    var themeMode: ThemeMode
        get() = ThemeMode.fromName(prefs.getString(KEY_THEME, null))
        set(value) { prefs.edit().putString(KEY_THEME, value.name).apply() }

    private companion object {
        const val FILE = "remindme.user"
        const val KEY_NAME = "name"
        const val KEY_AVATAR = "avatar_color"
        const val KEY_SHOW_GREETING = "show_greeting"
        const val KEY_GREETING_STYLE = "greeting_style"
        const val KEY_SOUND_URI = "alarm_sound_uri"
        const val KEY_SOUND_LABEL = "alarm_sound_label"
        const val DEFAULT_SOUND_LABEL = "Default alarm"
        const val KEY_VIBRATION = "alarm_vibration"
        const val KEY_SNOOZE_MIN = "snooze_minutes"
        const val KEY_DND = "dnd_enabled"
        const val KEY_DND_START = "dnd_start_minute"
        const val KEY_DND_END = "dnd_end_minute"
        const val DEFAULT_DND_START = 22 * 60 + 30
        const val DEFAULT_DND_END = 7 * 60
        const val KEY_THEME = "theme_mode"
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

/**
 * Which colour scheme the app paints in. Persisted by [ThemeMode.name] so adding
 * a value never breaks stored data — an unknown name falls back to [System].
 */
enum class ThemeMode(val label: String) {
    /** Follow the OS light/dark setting. */
    System("System"),

    /** Always light, regardless of the OS. */
    Light("Light"),

    /** Always dark, regardless of the OS. */
    Dark("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: System
    }
}
