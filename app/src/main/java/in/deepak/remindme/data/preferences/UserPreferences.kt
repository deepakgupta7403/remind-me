package `in`.deepak.remindme.data.preferences

import android.content.Context

/**
 * User profile bits captured during onboarding (and later, settings).
 *
 * Kept separate from [OnboardingPreferences] because the latter is a one-shot
 * gate, while this is mutable identity that the user can change later — mixing
 * them invites accidental flag resets when the user just edits their name.
 *
 * Storage is plain SharedPreferences; the values are small strings and we don't
 * need a reactive stream — the home screen reads it once per ViewModel.
 */
class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) { prefs.edit().putString(KEY_NAME, value.trim()).apply() }

    private companion object {
        const val FILE = "remindme.user"
        const val KEY_NAME = "name"
    }
}
