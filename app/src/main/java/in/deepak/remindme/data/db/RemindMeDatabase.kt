package `in`.deepak.remindme.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The single Room database — system of record for reminders, activity stats,
 * and the template catalog.
 *
 * Small session/identity state (onboarding flag, user profile, search history)
 * deliberately stays in SharedPreferences; it isn't relational "data" and a
 * Flow-driven table would be overkill.
 *
 * `version = 1`: this is the first Room schema. Pre-existing on-disk data
 * (`reminders.json`, the stats prefs blob) is brought in by
 * [LegacyDataImporter] on first launch — it is *not* a Room [androidx.room.migration.Migration],
 * because the old data never lived in a Room schema to migrate from.
 *
 * Adding a new entity is one line in `@Database(entities = ...)` + a `dao()`
 * accessor — the rest of the app reaches data through repositories, not this
 * class, so the seam stays narrow.
 */
@Database(
    entities = [
        ReminderEntity::class,
        ActivityStatEntity::class,
        TemplateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class RemindMeDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun activityDao(): ActivityDao
    abstract fun templateDao(): TemplateDao

    companion object {
        const val NAME = "remindme.db"
    }
}
