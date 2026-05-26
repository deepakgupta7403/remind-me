package `in`.deepak.remindme.data.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Recent search history (last [MAX] queries, most recent first).
 *
 * Stored in SharedPreferences as a JSON array because:
 *  - The list mutates atomically (record → take 5 → save) so we don't want a
 *    bespoke delimiter that could collide with whatever the user typed.
 *  - Gson is already on the classpath for [in.deepak.remindme.data.dto.ReminderDto];
 *    pulling in DataStore for one list isn't worth it.
 */
class SearchPreferences(context: Context, private val gson: Gson = Gson()) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun recent(): List<String> {
        val raw = prefs.getString(KEY_RECENT, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<String>>(raw, TYPE)
        }.getOrNull() ?: emptyList()
    }

    /** Pushes [query] to the front; dedupes; caps at [MAX]. Blank/short queries ignored. */
    fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val next = (listOf(trimmed) + recent().filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX)
        prefs.edit().putString(KEY_RECENT, gson.toJson(next)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_RECENT).apply()
    }

    private companion object {
        const val FILE = "remindme.search"
        const val KEY_RECENT = "recent"
        const val MAX = 5
        val TYPE = object : TypeToken<List<String>>() {}.type
    }
}
