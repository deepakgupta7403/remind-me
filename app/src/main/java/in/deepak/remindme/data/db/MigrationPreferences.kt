package `in`.deepak.remindme.data.db

import android.content.Context

/**
 * One-bit "we already imported the pre-Room files" gate, so
 * [LegacyDataImporter] runs its file/prefs migration exactly once.
 *
 * SharedPreferences (not Room) on purpose: the flag must be readable before the
 * database is necessarily touched, and it's a single boolean — the same
 * reasoning as [in.deepak.remindme.data.preferences.OnboardingPreferences].
 */
class MigrationPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var legacyImported: Boolean
        get() = prefs.getBoolean(KEY_IMPORTED, false)
        set(value) { prefs.edit().putBoolean(KEY_IMPORTED, value).apply() }

    private companion object {
        const val FILE = "remindme.migration"
        const val KEY_IMPORTED = "legacy_imported"
    }
}
