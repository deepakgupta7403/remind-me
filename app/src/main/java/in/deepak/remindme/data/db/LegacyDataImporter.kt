package `in`.deepak.remindme.data.db

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import `in`.deepak.remindme.data.dto.ReminderDto
import `in`.deepak.remindme.data.dto.ReminderFileEnvelope
import `in`.deepak.remindme.data.mapper.toDomainOrNull
import `in`.deepak.remindme.data.mapper.toEntity
import `in`.deepak.remindme.data.preferences.DayStat
import `in`.deepak.remindme.data.repository.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Brings pre-Room on-disk data into the database, once.
 *
 * Two sources, both from the file-based era this change replaces:
 *   - `reminders.json` (the old [in.deepak.remindme.data.repository] file repo)
 *   - the `remindme.activity` SharedPreferences blob (the old stats store)
 *
 * Reminder ids are preserved on import so historical stat rows (which key by
 * reminder id) still line up. After a successful run the flag in
 * [MigrationPreferences] is set and the legacy files are left inert (renamed /
 * cleared) so a reinstall-free upgrade never double-imports.
 *
 * Template seeding runs unconditionally (it's idempotent — see
 * [TemplateRepository.seedIfEmpty]) so a clean install gets the catalog too.
 */
class LegacyDataImporter(
    private val appContext: Context,
    private val database: RemindMeDatabase,
    private val migrationPrefs: MigrationPreferences,
    private val templateRepository: TemplateRepository,
    private val gson: Gson = Gson(),
) {

    suspend fun runOnce() {
        // Always ensure the catalog is present (clean install or upgrade).
        templateRepository.seedIfEmpty()

        if (migrationPrefs.legacyImported) return

        importReminders()
        importStats()

        migrationPrefs.legacyImported = true
    }

    private suspend fun importReminders() = withContext(Dispatchers.IO) {
        val file = File(appContext.filesDir, LEGACY_REMINDERS_FILE)
        if (!file.exists()) return@withContext

        val envelope = try {
            file.bufferedReader().use { gson.fromJson(it, ReminderFileEnvelope::class.java) }
        } catch (_: JsonSyntaxException) {
            null
        } ?: return@withContext

        val entities = envelope.reminders
            .mapNotNull(ReminderDto::toDomainOrNull)
            .map { it.toEntity() } // ids preserved (non-zero) so REPLACE keeps them
        if (entities.isNotEmpty()) {
            database.reminderDao().insertAll(entities)
        }

        // Tombstone so we never re-read it; keep the bytes for forensics.
        file.renameTo(File(appContext.filesDir, "$LEGACY_REMINDERS_FILE.migrated"))
    }

    private suspend fun importStats() = withContext(Dispatchers.IO) {
        val prefs = appContext.getSharedPreferences(LEGACY_STATS_FILE, Context.MODE_PRIVATE)
        val raw = prefs.getString(LEGACY_STATS_KEY, null) ?: return@withContext

        val days: List<DayStat> = runCatching {
            gson.fromJson<List<DayStat>>(raw, DAY_STAT_LIST_TYPE)
        }.getOrNull().orEmpty()

        val rows = days.flatMap { day ->
            val ids = day.firedByReminder.keys + day.doneByReminder.keys
            ids.map { id ->
                ActivityStatEntity(
                    epochDay = day.epochDay,
                    reminderId = id,
                    fired = day.firedByReminder[id] ?: 0,
                    done = day.doneByReminder[id] ?: 0,
                )
            }
        }
        if (rows.isNotEmpty()) {
            database.activityDao().insertAll(rows)
        }

        prefs.edit().remove(LEGACY_STATS_KEY).apply()
    }

    private companion object {
        // Matched the old FileReminderRepository.DEFAULT_FILE_NAME.
        const val LEGACY_REMINDERS_FILE = "reminders.json"
        // Matched the old ReminderActivityLog FILE / KEY constants.
        const val LEGACY_STATS_FILE = "remindme.activity"
        const val LEGACY_STATS_KEY = "days"
        val DAY_STAT_LIST_TYPE = object : TypeToken<List<DayStat>>() {}.type
    }
}
