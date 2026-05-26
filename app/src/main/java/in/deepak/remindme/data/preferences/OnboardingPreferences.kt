package `in`.deepak.remindme.data.preferences

import android.content.Context

/**
 * One-bit "user finished onboarding" gate.
 *
 * SharedPreferences (not DataStore) because this is a single boolean that
 * flips exactly once — a Flow-driven API would be overkill, and pulling in
 * the DataStore dependency for one key isn't worth it.
 */
class OnboardingPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var hasCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) { prefs.edit().putBoolean(KEY_COMPLETED, value).apply() }

    private companion object {
        const val FILE = "remindme.onboarding"
        const val KEY_COMPLETED = "completed"
    }
}
